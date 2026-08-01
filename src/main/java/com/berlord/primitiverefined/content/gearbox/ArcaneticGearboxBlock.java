package com.berlord.primitiverefined.content.gearbox;

import java.util.List;

import com.berlord.primitiverefined.PrKinetics;
import com.berlord.primitiverefined.PrRegistry;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlock;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * Create's gearbox, in this mod's materials.
 *
 * <p>Behaviour is Create's outright - the class is extended rather than reimplemented, so
 * the awkward part, redirecting rotation between perpendicular shafts and reversing it
 * across the block, stays Create's problem and stays correct. Only the block entity type
 * is ours, and even that holds Create's own {@link GearboxBlockEntity}: a type can be
 * registered against our block while reusing their behaviour, the same trick the
 * Arcanetic Shaft already plays with {@code BracketedKineticBlockEntity}.
 *
 * <p>What is genuinely ours is the look: brass casing where Create has andesite, and a
 * panel whose wood comes from the sequenced gearshift.
 */
public class ArcaneticGearboxBlock extends GearboxBlock implements PrKinetics.Arcanetic {

    public ArcaneticGearboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends GearboxBlockEntity> getBlockEntityType() {
        return PrRegistry.ARCANETIC_GEARBOX_BE.get();
    }

    /**
     * One block, two items - so the block has to decide which one it gives back, and
     * Create's inherited answer is Create's item.
     *
     * <p>{@code GearboxBlock#getDrops} bypasses the loot table whenever the axis is
     * horizontal and hands back {@code AllItems.VERTICAL_GEARBOX} directly. Inherited
     * unchanged, that made an Arcanetic Gearbox placed on its side drop a <em>Create</em>
     * Vertical Gearbox - the loot table was never consulted and was never wrong.
     *
     * <p>This is the same rule with our items: vertical axis falls through to the loot
     * table, which is the horizontal item, and a horizontal axis hands back the vertical
     * one. Bypassing the loot table for that case is Create's behaviour and is kept, so a
     * sideways gearbox is not silk-touch- or explosion-conditional while an upright one is.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (state.getValue(AXIS).isVertical()) {
            return super.getDrops(state, builder);
        }
        return List.of(new ItemStack(PrRegistry.ARCANETIC_GEARBOX_VERTICAL_ITEM.get()));
    }
}
