package com.berlord.primitiverefined.content.grid;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.berlord.primitiverefined.network.PrNetworkNodeContainer;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.autocrafting.preview.Preview;
import com.refinedmods.refinedstorage.api.autocrafting.preview.TreePreview;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskId;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.autocrafting.AutocraftingNetworkComponent;
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

import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
 * <p>The {@code Grid} implementation below is Refined Storage's {@code AbstractGridBlockEntity}
 * method for method, down to the autocrafting delegation. <b>There is no autocrafting on a
 * primitive network</b> - no pattern provider, no autocrafter, and no RS block can join one
 * because {@code KineticConnectionStrategy} refuses everything outside the arcanetic family -
 * so RS's own code returns nothing here without needing to be told to. That is a better
 * answer than a stub: it is right for the same reason RS's is, rather than by assertion.
 */
public class PGridBlockEntity extends KineticBlockEntity
        implements PrNodeHost, Grid, ExtendedMenuProvider<GridData> {

    private final GridNetworkNode gridNode = new GridNetworkNode(0L);
    private final PrNetworkNodeContainer node;

    public PGridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Integer.MAX_VALUE is RS's own priority for a grid: when a network splits, the grid
        // is re-seeded first, so an open screen keeps the half it is standing on.
        this.node = new PrNetworkNodeContainer(this, gridNode, "primitive_grid", Integer.MAX_VALUE);
    }

    @Override
    public PrNetworkNodeContainer prNode() {
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
        node.clearRemoved();
    }

    /**
     * {@code remove}, not {@code setRemoved}: Create's {@code SmartBlockEntity} makes the
     * latter final and calls this from inside it, which is the hook it leaves open.
     */
    @Override
    public void remove() {
        super.remove();
        node.setRemoved();
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
     * Pushes the kinetic state into the network, and lets the lit state follow it.
     *
     * <p>The screen means exactly one thing: lit is "on a working network". Turning and not
     * overstressed is the kinetic half of that and one controller is the other, and both are
     * {@code calculateActive}'s business rather than this method's - which is why the lit
     * property is handed over rather than compared here.
     */
    private void refresh() {
        node.update(getBlockState(), PGridBlock.LIT);
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
     * Which resources get the blue "craftable" treatment in the grid. RS's own answer,
     * which on a primitive network is the empty set because nothing on it holds a pattern.
     */
    @Override
    public Set<PlatformResourceKey> getAutocraftableResources() {
        Network network = gridNode.getNetwork();
        if (network == null) {
            return Set.of();
        }
        return network.getComponent(AutocraftingNetworkComponent.class)
                .getOutputs()
                .stream()
                .filter(PlatformResourceKey.class::isInstance)
                .map(PlatformResourceKey.class::cast)
                .collect(Collectors.toSet());
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

    /**
     * RS's own test, which is {@code Container}'s: the block entity is still there, it is
     * still this block, and the player has not walked more than eight blocks away. The last
     * of those is the part worth keeping - a grid screen that stays open across the map is
     * a grid screen operating on a network the player cannot see.
     */
    @Override
    public boolean canMenuStayOpen(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    // --- PreviewProvider --------------------------------------------------------
    // RS's AbstractGridBlockEntity verbatim. A primitive network carries no patterns, so
    // every one of these answers "nothing" of its own accord.

    @Override
    public CompletableFuture<Optional<Preview>> getPreview(ResourceKey resource, long amount,
                                                           CancellationToken cancellationToken) {
        return autocrafting()
                .map(component -> component.getPreview(resource, amount, cancellationToken))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
    }

    @Override
    public CompletableFuture<Optional<TreePreview>> getTreePreview(ResourceKey resource, long amount,
                                                                   CancellationToken cancellationToken) {
        return autocrafting()
                .map(component -> component.getTreePreview(resource, amount, cancellationToken))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
    }

    @Override
    public CompletableFuture<Long> getMaxAmount(ResourceKey resource, CancellationToken cancellationToken) {
        return autocrafting()
                .map(component -> component.getMaxAmount(resource, cancellationToken))
                .orElseGet(() -> CompletableFuture.completedFuture(0L));
    }

    @Override
    public Optional<TaskId> startTask(ResourceKey resource, long amount, Actor actor, boolean notify,
                                      CancellationToken cancellationToken) {
        return autocrafting()
                .flatMap(component -> component.startTask(resource, amount, actor, notify, cancellationToken));
    }

    private Optional<AutocraftingNetworkComponent> autocrafting() {
        return Optional.ofNullable(gridNode.getNetwork())
                .map(network -> network.getComponent(AutocraftingNetworkComponent.class));
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
