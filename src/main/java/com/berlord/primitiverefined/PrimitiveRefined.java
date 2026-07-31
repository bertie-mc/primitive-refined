package com.berlord.primitiverefined;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Primitive Refined - an early-game, Create-powered precursor to Refined Storage.
 *
 * <p>The machines here are one-to-one in behaviour with their Refined Storage
 * counterparts because they <em>are</em> Refined Storage: every block is an RS network
 * node, and the grids drive RS's own menus and screens. What is ours is the power and the
 * wiring. There is no energy anywhere in the mod - the network runs on rotational force,
 * each part charges its cost as a Create stress impact, and the shafts and cogs that carry
 * that force are the cables that carry the network.
 *
 * <p>Two families of kinetics, and they do not mix. Arcanetic parts refuse to mesh with
 * Create's own, and a block placed where they would have met pops off. The one crossing is
 * the large cogwheel on the controller's roof, which is how force gets in at all.
 */
@Mod(PrimitiveRefined.MOD_ID)
public class PrimitiveRefined {

    public static final String MOD_ID = "primitive_refined";

    public PrimitiveRefined(IEventBus modBus, ModContainer container) {
        PrRegistry.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
