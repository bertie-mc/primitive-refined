package com.berlord.primitiverefined.content.controller;

import com.berlord.primitiverefined.PrRegistry;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Primitive Controller - the core of a primitive network.
 *
 * <p>Topologically this is Create's rotational speed controller: rotation runs through it
 * along {@link #HORIZONTAL_AXIS}, and a large cogwheel sits on top. The cogwheel is what
 * makes it operable; without one the controller stays dark no matter how fast the shaft
 * line under it is turning.
 */
public class PControllerBlock extends HorizontalAxisKineticBlock implements IBE<PControllerBlockEntity> {

    /** Lit whenever the controller is actually running - drives the emissive overlay. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /**
     * The top two pixel rows are empty so the large cogwheel has somewhere to sit, exactly
     * as on Create's speed controller.
     */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public PControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(LIT));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Rotation enters and leaves along the horizontal axis only. Up is reserved for the
     * cogwheel, which is wired by hand in
     * {@link PControllerBlockEntity#propagateRotationTo} because Create's propagator
     * hardcodes its own block for that connection.
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(HORIZONTAL_AXIS);
    }

    /**
     * Whether a large cogwheel is installed on top in a usable orientation.
     *
     * <p>Same three conditions Create checks for its own speed controller: it must be a
     * large cog, its axis must be horizontal, and it must be perpendicular to the
     * controller's own axis - a cogwheel lined up with the shaft below it would just be
     * fighting the shaft.
     */
    public static boolean hasValidCogwheelAbove(LevelReader level, BlockPos pos, BlockState state) {
        BlockState above = level.getBlockState(pos.above());
        if (!ICogWheel.isLargeCog(above)) {
            return false;
        }
        Direction.Axis cogAxis = ((IRotate) above.getBlock()).getRotationAxis(above);
        return !cogAxis.isVertical() && cogAxis != state.getValue(HORIZONTAL_AXIS);
    }

    @Override
    public Class<PControllerBlockEntity> getBlockEntityClass() {
        return PControllerBlockEntity.class;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<? extends PControllerBlockEntity> getBlockEntityType() {
        return PrRegistry.P_CONTROLLER_BE.get();
    }
}
