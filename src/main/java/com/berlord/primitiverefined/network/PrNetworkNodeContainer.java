package com.berlord.primitiverefined.network;

import java.util.function.Consumer;

import com.berlord.primitiverefined.PrKinetics;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * One block's membership of a primitive network - the Refined Storage half of every block
 * entity in this mod.
 *
 * <p>This is Refined Storage's {@code AbstractNetworkNodeContainerBlockEntity} and the
 * network-facing half of its {@code AbstractBaseNetworkNodeContainerBlockEntity}, held as a
 * field instead of inherited as a superclass, and with its method names kept so the two can
 * be read side by side. Composition rather than inheritance because Java has one superclass
 * to give and Create has taken it - see {@link PrNodeHost}. RS finds a node through a
 * NeoForge capability rather than an {@code instanceof} on its own base class, so nothing is
 * lost by not extending it.
 *
 * <p>Everything RS's base class does that a primitive network has a use for is here:
 * building the container around the node, joining the network when the block entity enters
 * the world, leaving it when it goes, rebuilding the connections when they move, and
 * {@link #updateActiveness} deciding activeness and the lit block state together. What is
 * left behind is what a primitive network has no concept of - redstone mode, configuration
 * cards, custom names, the network item, the debug network id.
 *
 * <p>Two things are ours rather than RS's, and both follow from the power being rotation:
 *
 * <ul>
 * <li><b>{@link #calculateActive()}</b>. RS asks whether the redstone mode is satisfied and
 *     the network has the stored energy for the node's usage. There is no RS energy anywhere
 *     in this mod: a node is active when its block is turning, the kinetic network is not
 *     overstressed, and exactly one controller is on the primitive network. Stress is
 *     charged where it is incurred, by each block entity, on Create's own network.</li>
 * <li><b>{@link #updateConnections}</b>. RS rebuilds a node's connections when its own block
 *     state changes, which is sound when connectivity is a property of the block. Ours is a
 *     property of the <em>pair</em> - Create decides whether two blocks mesh - so our own
 *     state changing is not the signal, and the mask below is re-read instead.</li>
 * </ul>
 */
public class PrNetworkNodeContainer {

    /** No mask has been read yet - not a possible value of one, which is 0 to 63. */
    private static final int UNKNOWN = -1;

    private final KineticBlockEntity blockEntity;
    private final AbstractNetworkNode mainNetworkNode;
    private final NetworkNodeContainerProvider containers;

    /**
     * The set of faces this block was last connected across, as a six-bit mask. Comparing it
     * is how a topology change is noticed.
     */
    private int connections = UNKNOWN;

    private boolean joined;

    @Nullable
    private Consumer<Boolean> activenessListener;

    public PrNetworkNodeContainer(KineticBlockEntity blockEntity, AbstractNetworkNode mainNetworkNode,
                                  String name) {
        this(blockEntity, mainNetworkNode, name, 0);
    }

    /**
     * @param priority the order this container is torn down and re-seeded in when a network
     *                 splits, highest first. RS gives its grids {@code Integer.MAX_VALUE} so
     *                 an open grid seeds the network it ends up on rather than being handed
     *                 whichever one a cable happened to form first.
     */
    public PrNetworkNodeContainer(KineticBlockEntity blockEntity, AbstractNetworkNode mainNetworkNode,
                                  String name, int priority) {
        this.blockEntity = blockEntity;
        this.mainNetworkNode = mainNetworkNode;
        this.containers = RefinedStorageApi.INSTANCE.createNetworkNodeContainerProvider();
        this.containers.addContainer(RefinedStorageApi.INSTANCE
                .createNetworkNodeContainer(blockEntity, mainNetworkNode)
                .name(name)
                .priority(priority)
                .connectionStrategy(new KineticConnectionStrategy(blockEntity))
                .build());
    }

    public NetworkNodeContainerProvider getContainerProvider() {
        return containers;
    }

    public AbstractNetworkNode getNode() {
        return mainNetworkNode;
    }

    @Nullable
    public Network getNetwork() {
        return mainNetworkNode.getNetwork();
    }

    public boolean isActive() {
        return mainNetworkNode.isActive();
    }

    /**
     * RS's {@code AbstractExternalStorageBlockEntity} overrides {@code activenessChanged} to
     * do its first storage load. Composition has no override, so the hook is a listener -
     * the same shape RS's own crafting grid menu uses for the same reason.
     */
    public void setActivenessListener(@Nullable Consumer<Boolean> activenessListener) {
        this.activenessListener = activenessListener;
    }

    // --- Lifecycle --------------------------------------------------------------

    /** From {@code BlockEntity#clearRemoved} - the block entity has entered a level. */
    public void clearRemoved() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        containers.initialize(level, () -> { });
        joined = true;
        // Deliberately not currentConnections(level). This runs while the chunk's block
        // entities are still being created, and asking the level for a neighbour's block
        // entity there creates it, which runs its clearRemoved, which asks for its
        // neighbours - one stack frame per block along a shaft line until the stack ends.
        // The first lazy tick reads the mask instead, a second later at worst, in a level
        // that has finished loading; because UNKNOWN matches no mask, that first read also
        // pushes the topology into RS, which is what a load wants anyway.
        connections = UNKNOWN;
    }

    /**
     * From {@code BlockEntity#setRemoved} - broken, or unloaded with the chunk. Create's
     * {@code SmartBlockEntity} makes {@code setRemoved} final and calls {@code remove} from
     * inside it, so the hosts route this from there.
     */
    public void setRemoved() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide || !joined) {
            return;
        }
        containers.remove(level);
        joined = false;
    }

    // --- Per-tick upkeep --------------------------------------------------------

    /**
     * Re-reads the block's kinetic state and pushes it into the network.
     *
     * <p>Called from the block entity's lazy tick, which is once a second, and again from
     * {@code onSpeedChanged} the moment Create moves any speed. The lazy tick is therefore
     * only the latency on a change that moves <em>no</em> speed - a shaft placed against an
     * unpowered line - and never on losing or gaining rotation.
     *
     * @param activenessProperty the lit property to keep in step, or null for a block with
     *                           no lit state or one that lights on more than activeness
     */
    public void update(BlockState state, @Nullable BooleanProperty activenessProperty) {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide || !joined) {
            return;
        }
        updateConnections(level);
        updateActiveness(state, activenessProperty);
    }

    /**
     * RS's {@code updateActiveness}, minus its rate limit.
     *
     * <p>RS only lets activeness change every twentieth tick, because its condition is
     * stored energy against a per-tick draw and a network sitting on the threshold would
     * otherwise flip every tick. Ours has no such oscillation to damp: it is called from the
     * lazy tick, which is already once a second, and from {@code onSpeedChanged}, which is
     * an edge and not a poll. Adding RS's counter on top would only delay a stalled shaft
     * going dark by up to another second.
     */
    public void updateActiveness(BlockState state, @Nullable BooleanProperty activenessProperty) {
        boolean newActive = calculateActive();
        if (mainNetworkNode.isActive() != newActive) {
            activenessChanged(newActive);
        }
        if (activenessProperty != null && state.hasProperty(activenessProperty)
                && state.getValue(activenessProperty) != newActive) {
            updateActivenessBlockState(state, activenessProperty, newActive);
        }
    }

    private void activenessChanged(boolean newActive) {
        mainNetworkNode.setActive(newActive);
        if (activenessListener != null) {
            activenessListener.accept(newActive);
        }
    }

    /**
     * {@code switchToBlockState} rather than RS's {@code setBlockAndUpdate}: it swaps the
     * state without tearing down and rebuilding the kinetic network the block is part of,
     * which setting the block outright would do once a second, forever.
     */
    private void updateActivenessBlockState(BlockState state, BooleanProperty activenessProperty,
                                            boolean active) {
        Level level = blockEntity.getLevel();
        if (level != null) {
            KineticBlockEntity.switchToBlockState(level, blockEntity.getBlockPos(),
                    state.setValue(activenessProperty, active));
        }
    }

    /**
     * Turning, within the kinetic network's stress budget, and on a network with exactly one
     * controller.
     *
     * <p>Overstressed counts as unpowered however fast the shaft says it is going, because
     * overstressed means the stress units are not in fact being supplied.
     *
     * <p>One controller is Refined Storage's rule and berlord's. Zero means nothing is
     * feeding it; more than one and every node goes inactive rather than one of them being
     * picked as the real one, because which one that would be is not a question with an
     * answer.
     */
    public boolean calculateActive() {
        return blockEntity.getSpeed() != 0
                && !blockEntity.isOverStressed()
                && hasExactlyOneController();
    }

    public boolean hasExactlyOneController() {
        Network network = mainNetworkNode.getNetwork();
        return network != null && controllerCount(network) == 1;
    }

    public static int controllerCount(Network network) {
        GraphNetworkComponent graph = network.getComponent(GraphNetworkComponent.class);
        int count = 0;
        for (NetworkNodeContainer container : graph.getContainers()) {
            if (container.getNode() instanceof PrControllerNode) {
                count++;
            }
        }
        return count;
    }

    // --- Topology ---------------------------------------------------------------

    /** Pushes the connections into RS if the set of meshed faces has moved since last read. */
    private void updateConnections(Level level) {
        int now = currentConnections(level);
        if (now != connections) {
            connections = now;
            containers.update(level);
        }
    }

    /**
     * A six-bit mask of the faces across which this block is kinetically joined to an
     * arcanetic neighbour - the same test {@link KineticConnectionStrategy} offers RS,
     * folded into one comparable number.
     */
    private int currentConnections(Level level) {
        BlockPos pos = blockEntity.getBlockPos();
        int mask = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = pos.relative(direction);
            if (!level.isLoaded(neighbourPos)) {
                continue;
            }
            if (!PrKinetics.isArcanetic(level.getBlockState(neighbourPos), direction.getOpposite())) {
                continue;
            }
            if (!(level.getBlockEntity(neighbourPos) instanceof KineticBlockEntity theirs)) {
                continue;
            }
            if (RotationPropagator.isConnected(blockEntity, theirs)) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }
}
