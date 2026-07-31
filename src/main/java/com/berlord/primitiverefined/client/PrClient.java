package com.berlord.primitiverefined.client;

import com.berlord.primitiverefined.PrRegistry;
import com.berlord.primitiverefined.PrimitiveRefined;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = PrimitiveRefined.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PrClient {

    private PrClient() {
    }

    /**
     * Soulstained steel, as a multiply tint over Create's grey axis texture.
     *
     * <p>Chosen rather than eyeballed. Create's axis greys run about 90-143 per channel;
     * a multiply can only darken, so the tint is the soulstained hue normalised so its
     * strongest channel is 255. That maps the brightest axis pixel onto Malum's dominant
     * soul stained steel tone (#744b90) almost exactly, and drops the rest of the texture
     * into the darker end of the same palette - which is where a cast metal shaft belongs
     * anyway, the ingot being the brightest form of the material.
     */
    public static final int SOULSTAINED_TINT = 0xCD85FF;

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> SOULSTAINED_TINT,
                PrRegistry.SOULSTAINED_SHAFT.get());
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> SOULSTAINED_TINT,
                PrRegistry.SOULSTAINED_SHAFT_ITEM.get());
    }
}
