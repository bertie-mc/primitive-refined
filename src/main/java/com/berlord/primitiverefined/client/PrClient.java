package com.berlord.primitiverefined.client;

import com.berlord.primitiverefined.PrRegistry;
import com.berlord.primitiverefined.PrimitiveRefined;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.ShaftVisual;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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

    /**
     * The fallback path, for when Flywheel's backend is off. Create pairs both of these on
     * its own shafts, so we do the same.
     */
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PrRegistry.SOULSTAINED_SHAFT_BE.get(), ShaftRenderer::new);
    }

    /**
     * The reason the shaft was standing still.
     *
     * <p>A kinetic block does not spin because it is kinetic - it spins because something
     * draws it spinning. Create's shaft has a Flywheel visual doing that, and registering
     * one is also what stops the static blockstate model being drawn on top of the
     * animated one. Without this the block renders from its chunk mesh and never moves,
     * no matter how healthy its kinetic network is.
     *
     * <p>There is no registration event for this; Flywheel expects the call during client
     * setup.
     */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SimpleBlockEntityVisualizer
                .builder(PrRegistry.SOULSTAINED_SHAFT_BE.get())
                .factory(ShaftVisual::new)
                .apply());
    }
}
