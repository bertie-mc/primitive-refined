package com.berlord.primitiverefined.content.grid;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared block entity for the Primitive Grid and Primitive Crafting Grid.
 *
 * <p>Same shape as the controller: the block reports a fixed stress impact, and lights up
 * once it is actually turning and the network can carry it. The difference is that a grid's
 * cost is its own rather than a sum - the controller pays for nothing itself and totals up
 * what is attached, and these are the things attached.
 */
public class PGridBlockEntity extends KineticBlockEntity {

    public PGridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private float impact() {
        return getBlockState().getBlock() instanceof PGridBlock grid ? grid.stressImpact() : 0f;
    }

    @Override
    public float calculateStressApplied() {
        float impact = impact();
        this.lastStressApplied = impact;
        return impact;
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
     * Flips the lit state. Unlike the controller there is no cogwheel to check - rotation
     * arrives through the shaft on the back - so it comes down to turning at all, and the
     * network not being over its budget. An overstressed network is exactly the case
     * berlord asked to show as unpowered: the stress units are not being supplied.
     */
    private void refresh() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PGridBlock)) {
            return;
        }
        boolean shouldBeLit = getSpeed() != 0 && !isOverStressed();
        if (state.getValue(PGridBlock.LIT) != shouldBeLit) {
            KineticBlockEntity.switchToBlockState(level, worldPosition,
                    state.setValue(PGridBlock.LIT, shouldBeLit));
        }
    }
}
