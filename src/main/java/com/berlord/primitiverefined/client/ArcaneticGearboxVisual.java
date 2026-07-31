package com.berlord.primitiverefined.client;

import java.util.EnumMap;
import java.util.function.Consumer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Draws the Arcanetic Gearbox's four shafts.
 *
 * <p>Create's own {@code GearboxVisual} would have done, except that it names
 * {@code AllPartialModels.SHAFT_HALF} inside its constructor and builds its instance map
 * there, so there is no way to subclass it and swap the model. This is that class's shape
 * rewritten against our partial - the arrangement is Create's, the steel is ours.
 *
 * <p>The speed rule is the gearbox's whole point and is worth stating plainly: a shaft on
 * the same axis as the one driving the block turns with it if it faces the same way and
 * against it if it faces back; a shaft on a perpendicular axis turns against it when the
 * two point the same way along their axes. That is what makes a gearbox reverse across
 * and redirect around.
 */
public class ArcaneticGearboxVisual extends KineticBlockEntityVisual<GearboxBlockEntity> {

    private final EnumMap<Direction, RotatingInstance> shafts = new EnumMap<>(Direction.class);
    private final Direction.Axis axis;
    private Direction sourceFacing;

    public ArcaneticGearboxVisual(VisualizationContext context, GearboxBlockEntity blockEntity,
                                  float partialTick, PartialModel shaft) {
        super(context, blockEntity, partialTick);
        axis = blockState.getValue(BlockStateProperties.AXIS);
        updateSourceFacing();

        Instancer<RotatingInstance> instancer =
                instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(shaft));
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == axis) {
                continue;
            }
            RotatingInstance shaftInstance = instancer.createInstance();
            shaftInstance.setup(blockEntity, direction.getAxis(), speedOf(direction))
                    .setPosition(getVisualPosition())
                    // The partial points south, so each copy is turned onto its own face.
                    .rotateToFace(Direction.SOUTH, direction)
                    .setChanged();
            shafts.put(direction, shaftInstance);
        }
    }

    /**
     * Which neighbour is driving this gearbox. With nothing driving it, the positive end
     * of its own axis stands in, so the shafts at least agree with each other.
     */
    private void updateSourceFacing() {
        if (blockEntity.hasSource()) {
            BlockPos towards = blockEntity.source.subtract(blockEntity.getBlockPos());
            sourceFacing = Direction.getNearest(towards.getX(), towards.getY(), towards.getZ());
        } else {
            sourceFacing = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        }
    }

    private float speedOf(Direction direction) {
        float speed = blockEntity.getSpeed();
        if (speed == 0 || sourceFacing == null) {
            return 0;
        }
        if (sourceFacing.getAxis() == direction.getAxis()) {
            return sourceFacing == direction ? speed : -speed;
        }
        return sourceFacing.getAxisDirection() == direction.getAxisDirection() ? -speed : speed;
    }

    @Override
    public void update(float partialTick) {
        updateSourceFacing();
        shafts.forEach((direction, instance) ->
                instance.setup(blockEntity, direction.getAxis(), speedOf(direction)).setChanged());
    }

    @Override
    public void updateLight(float partialTick) {
        // The array form, not the Iterator one: relight takes Iterator<FlatLit>, and
        // generics are invariant, so an Iterator<RotatingInstance> will not fit however
        // plainly RotatingInstance implements FlatLit. Arrays are covariant, so they do.
        relight(shafts.values().toArray(new RotatingInstance[0]));
    }

    @Override
    protected void _delete() {
        shafts.values().forEach(RotatingInstance::delete);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        shafts.values().forEach(consumer);
    }
}
