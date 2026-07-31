package com.berlord.primitiverefined.content.cogwheel;

import java.util.function.Supplier;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A small cogwheel in a different material.
 *
 * <p>Extends Create's own {@code CogWheelBlock} rather than reimplementing it, so meshing,
 * placement validation, the shifting-gears advancement and the "cogwheels cannot go here"
 * rules are all Create's and stay correct. Only the block entity type and the textures
 * differ.
 */
public class PrCogWheelBlock extends CogWheelBlock {

    private final Supplier<BlockEntityType<? extends KineticBlockEntity>> blockEntityType;

    public PrCogWheelBlock(Properties properties,
                           Supplier<BlockEntityType<? extends KineticBlockEntity>> blockEntityType) {
        super(false, properties);
        this.blockEntityType = blockEntityType;
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return blockEntityType.get();
    }

    /**
     * Same reason as the Soulstained Shaft: without this the static chunk model is drawn
     * underneath the rotating one and the cogwheel both spins and stands still.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
