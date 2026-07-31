package com.berlord.primitiverefined.content.grid;

import java.util.function.Supplier;

import com.berlord.primitiverefined.PrKinetics;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

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
 *
 * <p>It is an {@link ICogWheel}, for the same reason Create's own mechanical crafter is
 * one: there is a cogwheel turning in the gap across its middle, its teeth reach out
 * through the window in the rims, and a cogwheel laid alongside is expected to mesh with
 * it. Small cog, not large, and not a dedicated cogwheel - those defaults are right.
 */
public class PGridBlock extends HorizontalKineticBlock
        implements IBE<PGridBlockEntity>, ICogWheel, PrKinetics.Arcanetic {

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

    /**
     * Only the back. That is the one face with a shaft standing in its well; the sides
     * carry the cogwheel's teeth instead, and meshing there is {@link ICogWheel}'s job,
     * not this method's.
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    /**
     * Opens the grid.
     *
     * <p>It opens whether or not the network is running, the way Refined Storage's own grid
     * does - a dark, empty grid is a readable answer to "is this thing on", and closing the
     * screen in the player's face is not.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PGridBlockEntity grid)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(grid, buf -> grid.getMenuCodec().encode(buf, grid.getMenuData()));
        return InteractionResult.CONSUME;
    }

    /** The crafting grid's 3x3 is a real inventory, so breaking the block has to give it back. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PCraftingGridBlockEntity crafting) {
            crafting.dropMatrix();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
