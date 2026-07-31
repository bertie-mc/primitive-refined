package com.berlord.primitiverefined.content.reader;

import com.berlord.primitiverefined.PrKinetics;
import com.berlord.primitiverefined.PrRegistry;
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
 * The External Reader.
 *
 * <p>Create's threshold switch, driven by rotation instead of by a comparator: a shaft
 * stands in the middle of its back, and the indicator down each side flickers while it is
 * turning. Refined Storage's External Storage is what it is meant to become.
 *
 * <p>Same shape as the grids - screen towards the player, shaft on the face away from
 * them - so it is a {@link HorizontalKineticBlock} rather than something that can be
 * stuck on a ceiling the way Create's own switch can. That is a deliberate narrowing: the
 * shaft has to arrive somewhere, and a horizontal line is where the rest of this mod's
 * machines put it.
 */
public class ExternalReaderBlock extends HorizontalKineticBlock
        implements IBE<ExternalReaderBlockEntity>, PrKinetics.Arcanetic {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ExternalReaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(LIT));
    }

    /** Face towards the player, so the shaft on the back points away from them. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    /** Only the back: that is the one face with a shaft standing in it. */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Class<ExternalReaderBlockEntity> getBlockEntityClass() {
        return ExternalReaderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ExternalReaderBlockEntity> getBlockEntityType() {
        return PrRegistry.EXTERNAL_READER_BE.get();
    }
}
