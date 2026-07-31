package com.berlord.primitiverefined.content.shaft;

import com.berlord.primitiverefined.PrRegistry;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A shaft in soulstained steel.
 *
 * <p>Functionally this is Create's shaft and nothing else: same axis placement and
 * alignment, same kinetic relay behaviour, same 6-voxel pole shape. Extending
 * {@code AbstractSimpleShaftBlock} - the class Create's own {@code ShaftBlock} extends -
 * is what buys that, rather than reimplementing any of it.
 *
 * <p>It exists as a separate block so primitive machines can be wired with a visually
 * distinct line, and so a later version can restrict which shafts carry a network.
 */
public class SoulstainedShaftBlock extends AbstractSimpleShaftBlock {

    private static final VoxelShape X_SHAPE = Block.box(0, 6, 6, 16, 10, 10);
    private static final VoxelShape Y_SHAPE = Block.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.box(6, 6, 0, 10, 10, 16);

    public SoulstainedShaftBlock(Properties properties) {
        super(properties);
    }

    /**
     * Keeps the block out of the chunk mesh, so only the animated copy is drawn.
     *
     * <p>Otherwise the static model and the rotating one are both rendered in the same
     * place - a shaft that spins and stands still at once. A 4x4 pole rotated 45 degrees
     * does not contain its own unrotated corners, so the stationary one is plainly visible
     * poking out of the moving one.
     *
     * <p>Both animated paths are covered, so nothing is left invisible: the Flywheel visual
     * when the backend is on, and {@code KineticBlockEntityRenderer} when it is off.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X_SHAPE;
            case Y -> Y_SHAPE;
            case Z -> Z_SHAPE;
        };
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return PrRegistry.SOULSTAINED_SHAFT_BE.get();
    }
}
