package com.berlord.primitiverefined.client;

import com.berlord.primitiverefined.PrRegistry;
import com.berlord.primitiverefined.PrimitiveRefined;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = PrimitiveRefined.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PrClient {

    private PrClient() {
    }

    /**
     * Makes the soulstained shaft turn.
     *
     * <p>Plain {@code KineticBlockEntityRenderer}, deliberately, not Create's
     * {@code ShaftRenderer}: the latter overrides {@code getRenderedBlockState()} to return
     * Create's <em>own</em> shaft state, which is what previously drew a grey Create shaft
     * on top of ours. The base class returns the block entity's own state, so it renders
     * our model.
     *
     * <p>This only produces the right colour because the shaft now carries a real
     * soulstained texture rather than a tint. {@code KineticBlockEntityRenderer} bakes the
     * model into a {@code SuperByteBuffer} and applies no colour whatsoever, so a
     * {@code tintindex} recolour is silently dropped and any spinning copy comes out grey -
     * which is exactly what happened before.
     *
     * <p>No Flywheel visual is registered. Without one, Flywheel leaves the block entity
     * renderer alone, so this path runs whichever backend is active.
     */
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PrRegistry.SOULSTAINED_SHAFT_BE.get(),
                KineticBlockEntityRenderer::new);
    }
}
