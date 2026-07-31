package com.berlord.primitiverefined.content.grid;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.berlord.primitiverefined.network.PrNode;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.berlord.primitiverefined.network.PrNodes;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.autocrafting.preview.Preview;
import com.refinedmods.refinedstorage.api.autocrafting.preview.TreePreview;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskId;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.grid.GridNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.grid.GridOperations;
import com.refinedmods.refinedstorage.api.network.node.grid.GridWatcher;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.TrackedResourceAmount;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.storage.root.FuzzyRootStorage;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import com.refinedmods.refinedstorage.common.grid.FuzzyGridOperations;
import com.refinedmods.refinedstorage.common.grid.GridData;
import com.refinedmods.refinedstorage.common.support.containermenu.ExtendedMenuProvider;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Mechanical Grid - Refined Storage's grid, hung off a driveshaft.
 *
 * <p>The block is Create's: it takes rotation through the shaft in its back, charges stress
 * for it, and goes dark when the network is overstressed. Everything through the screen is
 * RS's: {@link Grid} is the interface its own grid block entity implements, and
 * implementing it here is what makes RS's menu and RS's screen work against this block
 * without either knowing it is not one of theirs.
 *
 * <p>The Mechanical Crafting Grid extends this - see {@link PCraftingGridBlockEntity}. The
 * two differ in stress, in the textures their models name, and in whether there is a 3x3
 * matrix on the front.
 *
 * <p><b>Autocrafting is not implemented</b> and deliberately so: there is no pattern
 * provider and no autocrafter in this mod, so a primitive network has nothing to craft
 * with. The four {@code PreviewProvider} methods answer "nothing" rather than throwing, and
 * the grid simply never shows an autocraftable resource.
 */
public class PGridBlockEntity extends KineticBlockEntity
        implements PrNodeHost, Grid, ExtendedMenuProvider<GridData> {

    private final GridNetworkNode gridNode = new GridNetworkNode(0L);
    private final PrNode node;

    public PGridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.node = new PrNode(this, gridNode, "primitive_grid");
    }

    @Override
    public PrNode prNode() {
        return node;
    }

    private float impact() {
        return getBlockState().getBlock() instanceof PGridBlock grid ? grid.stressImpact() : 0f;
    }

    @Override
    public float calculateStressApplied() {
        float impact = impact();
        this.lastStressApplied = impact;
        return impact;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        node.onClearRemoved();
    }

    /**
     * {@code remove}, not {@code setRemoved}: Create's {@code SmartBlockEntity} makes the
     * latter final and calls this from inside it, which is the hook it leaves open.
     */
    @Override
    public void remove() {
        super.remove();
        node.onSetRemoved();
    }

    @Override
    public void initialize() {
        super.initialize();
        refresh();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        refresh();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        refresh();
    }

    /**
     * Flips the lit state, and pushes the same condition into the network.
     *
     * <p>The screen now means something it did not before: lit is exactly "on a working
     * network". Turning and not overstressed is still the kinetic half of that, and the
     * network adds the other half - one controller, and connected to it.
     */
    private void refresh() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PGridBlock)) {
            return;
        }
        node.refresh(PrNodes.isPowered(this));

        boolean shouldBeLit = gridNode.isActive();
        if (state.getValue(PGridBlock.LIT) != shouldBeLit) {
            KineticBlockEntity.switchToBlockState(level, worldPosition,
                    state.setValue(PGridBlock.LIT, shouldBeLit));
        }
    }

    // --- Grid -------------------------------------------------------------------

    protected Optional<Network> network() {
        return gridNode.isActive() ? Optional.ofNullable(gridNode.getNetwork()) : Optional.empty();
    }

    @Override
    public boolean isGridActive() {
        return gridNode.isActive();
    }

    @Override
    public void addWatcher(GridWatcher watcher, Class<? extends Actor> actorType) {
        gridNode.addWatcher(watcher, actorType);
    }

    @Override
    public void removeWatcher(GridWatcher watcher) {
        gridNode.removeWatcher(watcher);
    }

    @Override
    public Storage getItemStorage() {
        Network network = gridNode.getNetwork();
        if (network == null) {
            throw new IllegalStateException("Grid is not on a network");
        }
        return network.getComponent(StorageNetworkComponent.class);
    }

    @Override
    public List<TrackedResourceAmount> getResources(Class<? extends Actor> actorType) {
        Network network = gridNode.getNetwork();
        if (network == null) {
            return List.of();
        }
        return network.getComponent(StorageNetworkComponent.class).getResources(actorType);
    }

    /**
     * No autocrafting on a primitive network, so nothing is ever autocraftable. RS reads
     * this to decide which resources get the blue "craftable" treatment in the grid.
     */
    @Override
    public Set<PlatformResourceKey> getAutocraftableResources() {
        return Set.of();
    }

    /**
     * How the grid inserts and extracts.
     *
     * <p>The fuzzy wrapper is RS's, and is what makes shift-clicking a damaged tool find
     * the other damaged ones. The security wrapper RS also applies is skipped: there is no
     * security card, no security manager and no network owner anywhere in this mod, so
     * there is nothing for it to consult.
     */
    @Override
    public GridOperations createOperations(ResourceType resourceType, ServerPlayer player) {
        Network network = gridNode.getNetwork();
        if (network == null) {
            throw new IllegalStateException("Grid is not on a network");
        }
        RootStorage rootStorage = network.getComponent(StorageNetworkComponent.class);
        GridOperations operations = resourceType.createGridOperations(rootStorage, new PlayerActor(player));
        if (rootStorage instanceof FuzzyRootStorage fuzzyRootStorage) {
            return new FuzzyGridOperations(player, fuzzyRootStorage, operations);
        }
        return operations;
    }

    @Override
    public boolean canMenuStayOpen(Player player) {
        return !isRemoved() && level != null
                && level.getBlockState(worldPosition).getBlock() instanceof PGridBlock;
    }

    // --- PreviewProvider: autocrafting, which this mod does not have -------------

    @Override
    public CompletableFuture<Optional<Preview>> getPreview(ResourceKey resource, long amount,
                                                           CancellationToken cancellationToken) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<TreePreview>> getTreePreview(ResourceKey resource, long amount,
                                                                   CancellationToken cancellationToken) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Long> getMaxAmount(ResourceKey resource, CancellationToken cancellationToken) {
        return CompletableFuture.completedFuture(0L);
    }

    @Override
    public Optional<TaskId> startTask(ResourceKey resource, long amount, Actor actor, boolean notify,
                                      CancellationToken cancellationToken) {
        return Optional.empty();
    }

    // --- Menu -------------------------------------------------------------------

    @Override
    public GridData getMenuData() {
        return GridData.of(this);
    }

    @Override
    public StreamEncoder<RegistryFriendlyByteBuf, GridData> getMenuCodec() {
        return GridData.STREAM_CODEC;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        return new PGridContainerMenu(syncId, inventory, this);
    }
}
