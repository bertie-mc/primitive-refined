package com.berlord.primitiverefined.content.gearbox;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Vertical Arcanetic Gearbox: the same block, placed on a horizontal axis.
 *
 * <p>A gearbox is one block with an axis, so "vertical" is a placement, not a second
 * block - exactly how Create ships its own vertical gearbox. Create's
 * {@code VerticalGearboxItem} cannot be reused because it takes only item properties and
 * resolves Create's own gearbox internally, so this is the same idea rewritten against
 * ours.
 *
 * <p>The name reads oddly against what it does: the block's axis ends up horizontal. It
 * is named for how the shafts run, which is the way round a player sees it.
 */
public class VerticalArcaneticGearboxItem extends BlockItem {

    public VerticalArcaneticGearboxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        Direction.Axis axis = context.getHorizontalDirection().getAxis();
        return state.setValue(BlockStateProperties.AXIS, axis);
    }
}
