package com.berlord.primitiverefined.mixin;

import com.berlord.primitiverefined.content.controller.PControllerBlock;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets a large cogwheel drive the Primitive Controller sitting under it.
 *
 * <p>Create supports this for its own speed controller through
 * {@code isLargeCogToSpeedController}, which is hardcoded to
 * {@code AllBlocks.ROTATION_SPEED_CONTROLLER}. An addon cannot reach it: the propagator
 * only ever asks the <em>upstream</em> block for a custom connection
 * ({@code from.propagateRotationTo(to, ...)}), so when the cogwheel is upstream the
 * question goes to Create's cogwheel entity, which knows nothing about us. Our own hook
 * covers the opposite direction only - the controller driving the cogwheel.
 *
 * <p>So we add the missing case ourselves, at the one point that serves every caller:
 * {@code getConveyedSpeed} multiplies the source speed by
 * {@code getRotationSpeedModifier}, and {@code isConnected} tests the same method for
 * non-zero. Injecting here makes the pair both connected and driven.
 *
 * <p>The conditions are deliberately identical to Create's own four, and to
 * {@link PControllerBlock#hasValidCogwheelAbove}, so a cogwheel that lights the controller
 * is exactly a cogwheel that powers it.
 *
 * <p><b>This is a mixin into Create internals.</b> {@code getRotationSpeedModifier} is
 * private, so it is not API and may be renamed or restructured by any Create release.
 * Re-check it on every Create bump; if it stops applying, the mixin fails loudly rather
 * than silently, because {@code injectors.defaultRequire} is 1.
 */
@Mixin(RotationPropagator.class)
public class RotationPropagatorMixin {

    @Inject(method = "getRotationSpeedModifier", at = @At("HEAD"), cancellable = true, remap = false)
    private static void primitive_refined$largeCogToPrimitiveController(
            KineticBlockEntity from, KineticBlockEntity to, CallbackInfoReturnable<Float> cir) {

        // Only the exact arrangement: our controller directly beneath the cogwheel.
        if (!to.getBlockPos().subtract(from.getBlockPos()).equals(BlockPos.ZERO.below())) {
            return;
        }

        BlockState controllerState = to.getBlockState();
        if (!(controllerState.getBlock() instanceof PControllerBlock)) {
            return;
        }

        BlockState cogState = from.getBlockState();
        if (!ICogWheel.isLargeCog(cogState) || !(cogState.getBlock() instanceof IRotate cog)) {
            return;
        }

        Direction.Axis cogAxis = cog.getRotationAxis(cogState);
        if (cogAxis.isVertical()
                || cogAxis == controllerState.getValue(PControllerBlock.HORIZONTAL_AXIS)) {
            return;
        }

        // 1:1, matching what the controller already returns for the reverse direction in
        // propagateRotationTo. The two must agree or Create sees a speed conflict and
        // tears the network down.
        cir.setReturnValue(1f);
    }
}
