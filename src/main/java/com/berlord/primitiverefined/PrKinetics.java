package com.berlord.primitiverefined;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The arcanetic kinetic family: which blocks are in it, and how it refuses to mesh with
 * Create's own.
 *
 * <p>Arcanetic shafts, cogs and machines carry rotation the way Create's do, but they carry
 * it in a <em>separate</em> family. The two never connect, and a block of one family placed
 * where it would have driven a block of the other pops off and drops, the same way Create
 * pops a cogwheel asked to turn two ways at once.
 *
 * <p><b>The Primitive Controller is the single sanctioned bridge.</b> Its top face belongs
 * to Create - that is where a Create large cogwheel goes, and that cogwheel is how
 * rotational force gets into an arcanetic network at all. Every other face of it, and every
 * face of every other block here, is arcanetic.
 */
public final class PrKinetics {

    private PrKinetics() {
    }

    /**
     * Implemented by every block in the arcanetic family.
     *
     * <p>Membership is per <em>face</em>, not per block, because the controller is a
     * bridge: rotation arrives on its Create side and leaves on its arcanetic one.
     */
    public interface Arcanetic {
        /**
         * Whether the given face carries arcanetic rotation. {@code face} is null when the
         * two blocks are not orthogonally adjacent - large-cog diagonal meshing - in which
         * case the block answers for itself as a whole.
         */
        default boolean isArcaneticFace(BlockState state, @Nullable Direction face) {
            return true;
        }
    }

    /**
     * Set while {@link #wouldConnectIgnoringFamily} asks Create whether two blocks mesh, so
     * that the veto in {@code RotationPropagatorMixin} steps out of the way for the length
     * of that one question. Without it the answer is always "no" - the veto is exactly what
     * we are trying to see past.
     */
    private static final ThreadLocal<Boolean> VETO_SUPPRESSED = ThreadLocal.withInitial(() -> false);

    public static boolean vetoSuppressed() {
        return VETO_SUPPRESSED.get();
    }

    /** True if this face of this block carries arcanetic rotation. */
    public static boolean isArcanetic(BlockState state, @Nullable Direction face) {
        return state.getBlock() instanceof Arcanetic arcanetic && arcanetic.isArcaneticFace(state, face);
    }

    /** True if any face of this block is arcanetic - i.e. it is one of ours at all. */
    public static boolean isArcaneticBlock(BlockState state) {
        return state.getBlock() instanceof Arcanetic;
    }

    /**
     * Whether the two blocks belong to different families across the face between them.
     *
     * @param fromTo the face of {@code from} that points at {@code to}, or null if they are
     *               not orthogonally adjacent
     */
    public static boolean crossFamily(BlockState from, BlockState to, @Nullable Direction fromTo) {
        boolean fromArcanetic = isArcanetic(from, fromTo);
        boolean toArcanetic = isArcanetic(to, fromTo == null ? null : fromTo.getOpposite());
        return fromArcanetic != toArcanetic;
    }

    /**
     * Convenience for the mixin, which holds block entities rather than states.
     *
     * <p>The early return is not a micro-optimisation for its own sake: this runs inside
     * {@code getRotationSpeedModifier}, which every Create network in the world asks
     * several times per block per propagation. Two Create blocks are the overwhelmingly
     * common case and must cost one {@code instanceof} each.
     */
    public static boolean crossFamily(KineticBlockEntity from, KineticBlockEntity to) {
        BlockState fromState = from.getBlockState();
        BlockState toState = to.getBlockState();
        if (!isArcaneticBlock(fromState) && !isArcaneticBlock(toState)) {
            return false;
        }
        BlockPos diff = to.getBlockPos().subtract(from.getBlockPos());
        return crossFamily(fromState, toState, faceOf(diff));
    }

    /** The single face a neighbour offset names, or null if the offset is diagonal. */
    @Nullable
    public static Direction faceOf(BlockPos diff) {
        for (Direction direction : Direction.values()) {
            if (diff.equals(direction.getNormal())) {
                return direction;
            }
        }
        return null;
    }

    /**
     * Whether Create would have meshed these two, had they been in the same family.
     *
     * <p>Asked by suppressing our own veto and putting the question to Create, rather than
     * by reimplementing its meshing rules. Those rules cover shafts on an axis, small cogs
     * side by side, large-to-small stacks, large-to-large diagonals, chain drives and every
     * block's own {@code propagateRotationTo}; a copy of them here would be a second set to
     * keep in step with every Create release.
     */
    public static boolean wouldConnectIgnoringFamily(KineticBlockEntity a, KineticBlockEntity b) {
        VETO_SUPPRESSED.set(true);
        try {
            return RotationPropagator.isConnected(a, b) || RotationPropagator.isConnected(b, a);
        } finally {
            VETO_SUPPRESSED.set(false);
        }
    }

    /**
     * Every position Create would consider a rotational neighbour of this block: the six
     * faces, plus whatever the block itself adds - the diagonals a large cogwheel meshes
     * across.
     *
     * <p>{@code addPropagationLocations} is the same public hook Create's own
     * {@code getPotentialNeighbourLocations} calls, so this list is that list.
     */
    public static List<BlockPos> potentialNeighbours(KineticBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        List<BlockPos> neighbours = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            neighbours.add(pos.relative(direction));
        }
        if (be.getBlockState().getBlock() instanceof IRotate rotate) {
            be.addPropagationLocations(rotate, be.getBlockState(), neighbours);
        }
        return neighbours;
    }

    /**
     * Pops the block at {@code pos} if it touches a kinetic block of the other family.
     *
     * <p>{@code destroyBlock(pos, true)} is the same call Create makes when a cogwheel is
     * asked to turn two ways at once, so the two failures look and sound alike - which is
     * the point: it reads as "that does not go there", not as a bug.
     *
     * @return true if the block was destroyed
     */
    public static boolean popIfMismatched(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity placed)) {
            return false;
        }
        BlockState placedState = placed.getBlockState();
        for (BlockPos neighbourPos : potentialNeighbours(placed)) {
            if (!level.isLoaded(neighbourPos)) {
                continue;
            }
            if (!(level.getBlockEntity(neighbourPos) instanceof KineticBlockEntity neighbour)) {
                continue;
            }
            BlockState neighbourState = neighbour.getBlockState();
            if (!isArcaneticBlock(placedState) && !isArcaneticBlock(neighbourState)) {
                // Two Create blocks. Not our business.
                continue;
            }
            if (!crossFamily(placedState, neighbourState, faceOf(neighbourPos.subtract(pos)))) {
                continue;
            }
            if (!wouldConnectIgnoringFamily(placed, neighbour)) {
                continue;
            }
            level.destroyBlock(pos, true);
            return true;
        }
        return false;
    }
}
