package com.berlord.primitiverefined.content.reader;

import com.berlord.primitiverefined.PrStress;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Same shape as the grids': a fixed stress impact, and a lit state that follows whether
 * the network is actually supplying it. Overstressed counts as unpowered, because the
 * stress units are not in fact being delivered.
 */
public class ExternalReaderBlockEntity extends KineticBlockEntity {

    public ExternalReaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = PrStress.EXTERNAL_READER;
        return PrStress.EXTERNAL_READER;
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

    private void refresh() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ExternalReaderBlock)) {
            return;
        }
        boolean shouldBeLit = getSpeed() != 0 && !isOverStressed();
        if (state.getValue(ExternalReaderBlock.LIT) != shouldBeLit) {
            KineticBlockEntity.switchToBlockState(level, worldPosition,
                    state.setValue(ExternalReaderBlock.LIT, shouldBeLit));
        }
    }
}
