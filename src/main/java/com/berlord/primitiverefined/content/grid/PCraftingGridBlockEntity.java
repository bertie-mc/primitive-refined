package com.berlord.primitiverefined.content.grid;

import java.util.List;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import com.refinedmods.refinedstorage.common.grid.DirectCommitExtractTransaction;
import com.refinedmods.refinedstorage.common.grid.ExtractTransaction;
import com.refinedmods.refinedstorage.common.grid.SnapshotExtractTransaction;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Mechanical Crafting Grid - a grid with a 3x3 matrix that crafts out of the network.
 *
 * <p>Everything about the matrix is Refined Storage's {@link RecipeMatrix}: the recipe
 * lookup, the result slot, EMI and JEI recipe transfer, clearing into storage, and the
 * remaining-items rules that put a bucket back after cake. What is written here is only the
 * seven-method bridge {@link CraftingGrid} asks for, and it is the same bridge RS's own
 * crafting grid block entity writes.
 *
 * <p>Everything storage-side is inherited from {@link PGridBlockEntity}, and everything
 * kinetic with it - it is one block, more expensive to turn.
 */
public class PCraftingGridBlockEntity extends PGridBlockEntity implements CraftingGrid {

    private static final String TAG_MATRIX = "matrix";

    private final RecipeMatrix<CraftingRecipe, CraftingInput> craftingRecipe =
            RecipeMatrix.crafting(this::setChanged, this::getLevel);

    public PCraftingGridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public RecipeMatrixContainer getCraftingMatrix() {
        return craftingRecipe.getMatrix();
    }

    @Override
    public ResultContainer getCraftingResult() {
        return craftingRecipe.getResult();
    }

    /** The block's level, not the player's, as RS does - they are the same level, and the
     *  block is the thing whose recipe manager is being asked. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(Player player, CraftingInput input) {
        return craftingRecipe.getRemainingItems(level, player, input);
    }

    /**
     * Where the ingredients come from when the result slot is taken.
     *
     * <p>The flag's sense is Refined Storage's and it is the opposite way round to the
     * obvious guess: <b>true means commit directly</b>, which is what taking a single
     * result does, and false means take a snapshot, which is what the shift-click "craft as
     * many as you can" loop does so that a run which turns out to be short of an ingredient
     * half way through leaves the network as it found it. This matches RS's crafting grid.
     */
    @Override
    public ExtractTransaction startExtractTransaction(Player player, boolean directCommit) {
        RootStorage rootStorage = rootStorage();
        if (rootStorage == null) {
            return ExtractTransaction.NOOP;
        }
        return directCommit
                ? new DirectCommitExtractTransaction(rootStorage)
                : new SnapshotExtractTransaction(player, rootStorage, getCraftingMatrix());
    }

    @Override
    public boolean clearMatrix(Player player, boolean toPlayerInventory) {
        if (toPlayerInventory) {
            return getCraftingMatrix().clearToPlayerInventory(player);
        }
        RootStorage rootStorage = rootStorage();
        return rootStorage != null && getCraftingMatrix().clearIntoStorage(rootStorage, player);
    }

    @Override
    public void transferRecipe(Player player, List<List<ItemResource>> recipe) {
        getCraftingMatrix().transferRecipe(player, rootStorage(), recipe);
    }

    /**
     * What happens to a stack the player crafted but has no room for: into the network, and
     * whatever the network would not take goes on the floor.
     */
    @Override
    public void acceptQuickCraft(Player player, ItemStack stack) {
        if (player.getInventory().add(stack)) {
            return;
        }
        RootStorage rootStorage = rootStorage();
        long inserted = rootStorage == null ? 0L : rootStorage.insert(
                ItemResource.ofItemStack(stack), stack.getCount(), Action.EXECUTE, new PlayerActor(player));
        if (inserted < stack.getCount()) {
            player.drop(stack.copyWithCount((int) (stack.getCount() - inserted)), false);
        }
    }

    private RootStorage rootStorage() {
        return network().map(n -> (RootStorage) n.getComponent(StorageNetworkComponent.class)).orElse(null);
    }

    // --- Persistence ------------------------------------------------------------

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!clientPacket) {
            tag.put(TAG_MATRIX, craftingRecipe.writeToTag(registries));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(TAG_MATRIX)) {
            craftingRecipe.readFromTag(tag.getCompound(TAG_MATRIX), registries);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        craftingRecipe.updateResult(level);
    }

    /** The matrix is a real inventory, so breaking the block has to give it back. */
    public void dropMatrix() {
        if (level == null) {
            return;
        }
        Containers.dropContents(level, worldPosition, getCraftingMatrix());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        return new PCraftingGridContainerMenu(syncId, inventory, this);
    }
}
