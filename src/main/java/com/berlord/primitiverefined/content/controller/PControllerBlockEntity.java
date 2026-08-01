package com.berlord.primitiverefined.content.controller;

import com.berlord.primitiverefined.PrStress;
import com.berlord.primitiverefined.network.PrControllerNode;
import com.berlord.primitiverefined.network.PrNetworkNodeContainer;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.refinedmods.refinedstorage.api.network.Network;
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

/**
 * The controller is the only way rotational force gets into a primitive network.
 *
 * <p>A Create large cogwheel drives it from above, through Create's own kinetics; the shaft
 * line running through it underneath is arcanetic, and everything downstream of that is the
 * network. The block is the bridge between the two kinetic families and the host of the
 * storage network at once.
 */
public class PControllerBlockEntity extends KineticBlockEntity implements PrNodeHost {

    private final PrNetworkNodeContainer node =
            new PrNetworkNodeContainer(this, new PrControllerNode(), "primitive_controller");

    /**
     * The network's total cost, for the goggle readout. Never charged - see {@link PrStress}.
     *
     * <p>Computed on the server and shipped, along with {@link #controllers}, because
     * Create's goggle tooltip is a client HUD and a primitive network is only ever built
     * server-side. Asked on the client, both questions answer about a network that does not
     * exist there - which looks like a readout and is not one.
     */
    private float demand = PrStress.CONTROLLER;

    /** How many controllers this network has. One is the only working answer. */
    private int controllers;

    public PControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public PrNetworkNodeContainer prNode() {
        return node;
    }

    /**
     * Zero. Every part on the network charges its own impact where it stands, so the sum
     * is already on Create's stress network by the time this is asked; billing it here as
     * well would charge the same grids twice.
     */
    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = PrStress.CONTROLLER;
        return PrStress.CONTROLLER;
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
     * Re-reads the cogwheel and the network, and flips the lit state if either changed.
     *
     * <p>The lit state is not simply the node's activeness here, which is why the container
     * is asked to leave the block state alone: the controller also wants a valid cogwheel
     * above it. That is all but implied - without one nothing is turning this line, and a
     * node that is not turning is not active - but the goggles distinguish the two, and so
     * does the model.
     */
    private void refresh() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        node.update(state, null);

        Network network = node.getNetwork();
        float newDemand = PrStress.networkDemand(network);
        int newControllers = network == null ? 0 : PrNetworkNodeContainer.controllerCount(network);
        if (newDemand != demand || newControllers != controllers) {
            demand = newDemand;
            controllers = newControllers;
            sendData();
        }

        if (!(state.getBlock() instanceof PControllerBlock)) {
            return;
        }

        boolean shouldBeLit = node.isActive()
                && PControllerBlock.hasValidCogwheelAbove(level, worldPosition, state);

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
     * <p>"It does not glow" has four possible causes now and they are indistinguishable
     * from the outside, so the block states them itself rather than us guessing at them one
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

        line(tooltip, "controllers on network", controllers == 1,
                controllers == 0 ? "none - not on a network"
                        : controllers == 1 ? "1" : controllers + " - a system takes one");
        line(tooltip, "network demand", true, String.format("%.1f su", demand));
        if (isController) {
            line(tooltip, "lit", state.getValue(PControllerBlock.LIT),
                    state.getValue(PControllerBlock.LIT) ? "on" : "off");
        }
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("Demand", demand);
        tag.putInt("Controllers", controllers);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        demand = tag.getFloat("Demand");
        controllers = tag.getInt("Controllers");
    }

    private static void line(List<Component> tooltip, String label, boolean ok, String detail) {
        tooltip.add(Component.literal("    ")
                .append(Component.literal(ok ? "✔ " : "✘ ")
                        .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal(label + ": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(detail).withStyle(ChatFormatting.WHITE)));
    }
}
