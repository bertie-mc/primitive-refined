package com.berlord.primitiverefined.content.controller;

import com.berlord.primitiverefined.PrStress;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PControllerBlockEntity extends KineticBlockEntity {

    /** Cached total demand of the attached network, so the network is not re-walked per tick. */
    private float demand = PrStress.CONTROLLER;

    public PControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * The controller reports the whole network's cost as its own impact. With nothing
     * attached this is zero, which is what makes a bare controller spin up on any amount
     * of rotational force.
     */
    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = demand;
        return demand;
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
     * Re-reads the network cost and the cogwheel, and flips the lit state if either
     * changed. Cheap enough for the lazy tick; the only expensive part is the network walk
     * and that is what {@link #demand} caches.
     */
    private void refresh() {
        if (level == null || level.isClientSide) {
            return;
        }

        float newDemand = PrStress.totalDemand(level, worldPosition);
        if (newDemand != demand) {
            demand = newDemand;
            // Tell Create to recompute the network's stress budget; without this the old
            // impact stays in the network total until something else disturbs it.
            networkDirty = true;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PControllerBlock)) {
            return;
        }

        boolean shouldBeLit = PControllerBlock.hasValidCogwheelAbove(level, worldPosition, state)
                && getSpeed() != 0
                && !isOverStressed();

        if (state.getValue(PControllerBlock.LIT) != shouldBeLit) {
            // switchToBlockState rather than setBlock: it swaps the state without tearing
            // down and rebuilding the kinetic network the controller is part of.
            KineticBlockEntity.switchToBlockState(level, worldPosition,
                    state.setValue(PControllerBlock.LIT, shouldBeLit));
        }
    }

    /**
     * Hand-wires the large cogwheel sitting on top.
     *
     * <p>Create's {@code RotationPropagator} has a special case for a large cog above a
     * speed controller, but it is hardcoded to {@code AllBlocks.ROTATION_SPEED_CONTROLLER},
     * so an addon block gets nothing for free. This hook is the supported way back in:
     * returning a non-zero modifier declares the connection and its ratio.
     *
     * <p>Note the direction. This fires for controller -> cogwheel, which is the direction
     * the propagator asks when power arrives through the shaft line, the normal case for a
     * speed-controller-shaped block. Driving the controller <em>from</em> the cogwheel
     * would need the mirror case on Create's own cogwheel entity, which we cannot touch.
     */
    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo,
                                     BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        if (!isCogwheelAbove(diff, stateFrom, stateTo)) {
            return 0;
        }
        return 1;
    }

    @Override
    public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
        BlockPos diff = other.getBlockPos().subtract(worldPosition);
        return isCogwheelAbove(diff, state, otherState);
    }

    private static boolean isCogwheelAbove(BlockPos diff, BlockState controllerState, BlockState cogState) {
        if (!diff.equals(BlockPos.ZERO.above())) {
            return false;
        }
        if (!ICogWheel.isLargeCog(cogState)) {
            return false;
        }
        if (!(controllerState.getBlock() instanceof PControllerBlock)) {
            return false;
        }
        Direction.Axis cogAxis = ((IRotate) cogState.getBlock()).getRotationAxis(cogState);
        return !cogAxis.isVertical()
                && cogAxis != controllerState.getValue(PControllerBlock.HORIZONTAL_AXIS);
    }

    /**
     * Reports every condition the lit state depends on, through the goggles.
     *
     * <p>"It does not glow" has three possible causes and they are indistinguishable from
     * the outside, so the block states them itself rather than us guessing at them one
     * build at a time.
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (level == null) {
            return true;
        }

        BlockState state = getBlockState();
        BlockState above = level.getBlockState(worldPosition.above());
        boolean isController = state.getBlock() instanceof PControllerBlock;
        boolean cogOk = isController && PControllerBlock.hasValidCogwheelAbove(level, worldPosition, state);

        tooltip.add(Component.literal(" ").append(
                Component.literal("Primitive Controller").withStyle(ChatFormatting.GRAY)));
        line(tooltip, "cogwheel above", cogOk,
                above.isAir() ? "nothing there"
                        : above.getBlock().getName().getString()
                          + (above.hasProperty(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS)
                             ? " axis=" + above.getValue(
                                     com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS)
                             : ""));
        if (isController) {
            line(tooltip, "controller axis", true,
                    state.getValue(PControllerBlock.HORIZONTAL_AXIS).toString());
        }
        line(tooltip, "speed", getSpeed() != 0, String.format("%.1f rpm", getSpeed()));
        line(tooltip, "not overstressed", !isOverStressed(), isOverStressed() ? "overstressed" : "ok");
        line(tooltip, "network demand", true, String.format("%.1f su", demand));
        if (isController) {
            line(tooltip, "lit", state.getValue(PControllerBlock.LIT),
                    state.getValue(PControllerBlock.LIT) ? "on" : "off");
        }
        return true;
    }

    private static void line(List<Component> tooltip, String label, boolean ok, String detail) {
        tooltip.add(Component.literal("    ")
                .append(Component.literal(ok ? "✔ " : "✘ ")
                        .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal(label + ": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(detail).withStyle(ChatFormatting.WHITE)));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putFloat("Demand", demand);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        demand = tag.getFloat("Demand");
        super.read(tag, registries, clientPacket);
    }
}
