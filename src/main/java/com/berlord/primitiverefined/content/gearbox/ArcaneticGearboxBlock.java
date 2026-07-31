package com.berlord.primitiverefined.content.gearbox;

import com.berlord.primitiverefined.PrKinetics;
import com.berlord.primitiverefined.PrRegistry;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlock;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

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
}
