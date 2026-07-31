package com.berlord.primitiverefined.network;

import com.berlord.primitiverefined.PrKinetics;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * One block's membership of a primitive network - the Refined Storage half of every block
 * entity in this mod.
 *
 * <p>It is composed rather than inherited (see {@link PrNodeHost}) and it does the four
 * things RS's own {@code AbstractNetworkNodeContainerBlockEntity} does: build a container
 * around the node, join the network when the block entity enters the world, leave it when
 * it goes, and rebuild the connections when they move.
 *
 * <p>What it adds is the part that is ours: <b>activeness comes from Create.</b> A node is
 * active when its block is turning, the kinetic network is not overstressed, and exactly
 * one controller is on the primitive network. There is no RS energy anywhere in this mod -
 * rotational force is the power, an overstressed network is the brownout, and stress is
 * charged where it is incurred, by each block entity, on Create's own network.
 */
public final class PrNode {

    private final BlockEntity blockEntity;
    private final AbstractNetworkNode node;
    private final NetworkNodeContainerProvider containers;

    /**
     * The set of faces this block was last connected across, as a six-bit mask. Comparing
     * it is how a topology change is noticed: connectivity here depends on the
     * <em>neighbours'</em> states, so our own block state changing is not the signal RS
     * assumes it is.
     */
    private int connections = -1;

    private boolean joined;

    public PrNode(BlockEntity blockEntity, AbstractNetworkNode node, String name) {
        this.blockEntity = blockEntity;
        this.node = node;
        this.containers = RefinedStorageApi.INSTANCE.createNetworkNodeContainerProvider();
        this.containers.addContainer(RefinedStorageApi.INSTANCE
                .createNetworkNodeContainer(blockEntity, node)
                .name(name)
                .connectionStrategy(new KineticConnectionStrategy(blockEntity))
                .build());
    }

    public NetworkNodeContainerProvider containers() {
        return containers;
    }

    public AbstractNetworkNode node() {
        return node;
    }

    public Network network() {
        return node.getNetwork();
    }

    public boolean isActive() {
        return node.isActive();
    }

    // --- Lifecycle --------------------------------------------------------------

    /** From {@code BlockEntity#clearRemoved} - the block entity has entered a level. */
    public void onClearRemoved() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        containers.initialize(level, () -> { });
        joined = true;
        connections = currentConnections(level);
    }

    /** From {@code BlockEntity#setRemoved} - broken, or unloaded with the chunk. */
    public void onSetRemoved() {
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
     * <p>Called from the block entity's lazy tick, which is once a second. That is the
     * latency on noticing a shaft placed against an unpowered line - a change that moves no
     * speed and so fires no Create callback. Anything that <em>does</em> move speed already
     * arrives through {@code onSpeedChanged}, immediately.
     *
     * @param powered whether the block is turning and its kinetic network is within budget
     */
    public void refresh(boolean powered) {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide || !joined) {
            return;
        }

        int now = currentConnections(level);
        if (now != connections) {
            connections = now;
            containers.update(level);
        }

        boolean active = powered && hasExactlyOneController();
        if (node.isActive() != active) {
            node.setActive(active);
        }
    }

    /**
     * Whether this node's network has exactly one controller.
     *
     * <p>Refined Storage's rule, and berlord's: a system has one controller. Zero means
     * nothing is feeding it and one means it works; more than one and every node on the
     * network goes inactive rather than one of them being picked as the real one, because
     * which one that would be is not a question with an answer.
     */
    public boolean hasExactlyOneController() {
        Network network = node.getNetwork();
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

    /**
     * A six-bit mask of the faces across which this block is kinetically joined to an
     * arcanetic neighbour - the same test {@link KineticConnectionStrategy} offers RS,
     * folded into one comparable number.
     */
    private int currentConnections(Level level) {
        if (!(blockEntity instanceof KineticBlockEntity mine)) {
            return 0;
        }
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
            if (RotationPropagator.isConnected(mine, theirs)) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }
}
