package com.berlord.primitiverefined.network;

import com.berlord.primitiverefined.PrKinetics;
import com.refinedmods.refinedstorage.common.api.support.network.ConnectionSink;
import com.refinedmods.refinedstorage.common.api.support.network.ConnectionStrategy;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shafts and cogs <em>are</em> the cables.
 *
 * <p>Refined Storage builds a network by asking each node which neighbours it connects to.
 * This answers that question by asking Create which neighbours it is already turning:
 * a primitive network is exactly the arcanetic kinetic network, so there is one topology
 * rather than two that have to be kept in agreement. A shaft line that carries rotation
 * carries the network; break the line and the network splits in the same tick, because it
 * is the same break.
 *
 * <p>{@link RotationPropagator#isConnected} is the whole test. It covers shafts along an
 * axis, cogs meshing side by side, the gearbox turning a line a corner, the grids' rear
 * shafts and the controller's two ends - every rule Create has, including the ones added by
 * a block's own {@code propagateRotationTo}. Nothing about kinetic adjacency is restated
 * here.
 *
 * <p>Six faces only. Create also meshes large cogwheels diagonally; the arcanetic family
 * has no large cogwheel, so that case cannot arise inside a primitive network. The one
 * large cogwheel in the design sits on top of the controller and belongs to Create - it
 * carries force in, never network.
 */
public final class KineticConnectionStrategy implements ConnectionStrategy {

    private final BlockEntity blockEntity;

    public KineticConnectionStrategy(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public void addOutgoingConnections(ConnectionSink sink) {
        Level level = blockEntity.getLevel();
        if (level == null || !(blockEntity instanceof KineticBlockEntity mine)) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = pos.relative(direction);
            if (!level.isLoaded(neighbourPos)) {
                continue;
            }
            // The face of the neighbour that points back at us has to be arcanetic. This
            // is what keeps the network off the controller's Create side: a large cogwheel
            // sitting on top of one drives it, and joins nothing.
            if (!PrKinetics.isArcanetic(level.getBlockState(neighbourPos), direction.getOpposite())) {
                continue;
            }
            if (!(level.getBlockEntity(neighbourPos) instanceof KineticBlockEntity theirs)) {
                continue;
            }
            if (!RotationPropagator.isConnected(mine, theirs)) {
                continue;
            }
            sink.tryConnectInSameDimension(neighbourPos, direction);
        }
    }

    /**
     * The other end of the same rule. The offering side has already checked that the two
     * are kinetically joined, so all that is left is the family - and refusing anything
     * that is not ours is what makes the primitive network a closed system rather than an
     * extension cord for a real Refined Storage network.
     */
    @Override
    public boolean canAcceptIncomingConnection(Direction incomingDirection, BlockState connectingState) {
        return PrKinetics.isArcaneticBlock(connectingState);
    }
}
