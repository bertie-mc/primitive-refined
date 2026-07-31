package com.berlord.primitiverefined;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Primitive Refined - an early-game, Create-powered precursor to Refined Storage.
 *
 * <p>The machines here are one-to-one in behaviour with their Refined Storage
 * counterparts, but they run on rotational force: the controller reports the network's
 * total cost as a Create stress impact instead of drawing FE.
 *
 * <p>This build is the demo: the Primitive Controller and the Soulstained Shaft.
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
