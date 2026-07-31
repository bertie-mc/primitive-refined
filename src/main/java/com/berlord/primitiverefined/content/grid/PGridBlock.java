package com.berlord.primitiverefined.content.grid;

import java.util.function.Supplier;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Primitive Grid and Primitive Crafting Grid.
 *
 * <p>A mechanical crafter body with a Refined Storage screen on the front and the sequenced
 * gearshift's shaft face on the back. Rotation enters along the facing axis, through that
 * shaft, and the screen lights once the network is actually supplying the stress the block
 * demands.
 *
 * <p>One class serves both; they differ only in stress and in which textures their models
 * name.
 */
public class PGridBlock extends HorizontalKineticBlock implements IBE<PGridBlockEntity> {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final float stressImpact;
    private final Supplier<BlockEntityType<? extends PGridBlockEntity>> blockEntityType;

    public PGridBlock(Properties properties, float stressImpact,
                      Supplier<BlockEntityType<? extends PGridBlockEntity>> blockEntityType) {
        super(properties);
        this.stressImpact = stressImpact;
        this.blockEntityType = blockEntityType;
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    public float stressImpact() {
        return stressImpact;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(LIT));
    }

    /** Screen towards the player, so the shaft on the back points away from them. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Rotation runs front to back, arriving through the shaft on the rear face. */
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Class<PGridBlockEntity> getBlockEntityClass() {
        return PGridBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PGridBlockEntity> getBlockEntityType() {
        return blockEntityType.get();
    }
}
