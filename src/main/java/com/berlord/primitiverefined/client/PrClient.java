package com.berlord.primitiverefined.client;

import java.util.HashMap;
import java.util.Map;

import com.berlord.primitiverefined.PrRegistry;
import com.berlord.primitiverefined.content.cogwheel.PrCogwheels;
import com.berlord.primitiverefined.PrimitiveRefined;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = PrimitiveRefined.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PrClient {

    private PrClient() {
    }

    /**
     * Declared as a static field so it is created while this class is loaded during mod
     * construction. {@link PartialModel#of} has to run before models are baked, or there
     * is nothing for Flywheel to populate it from.
     */
    private static final PartialModel SOULSTAINED_SHAFT_MODEL =
            PartialModel.of(PrimitiveRefined.id("block/soulstained_shaft"));

    /** One partial per cogwheel variant - Flywheel binds a visual per block entity type. */
    private static final Map<String, PartialModel> COGWHEEL_MODELS = new HashMap<>();

    static {
        for (String name : PrCogwheels.NAMES) {
            COGWHEEL_MODELS.put(name, PartialModel.of(PrimitiveRefined.id("block/" + name)));
        }
    }

    /** The controller's two shaft stubs, split out of the body so they can spin on their own. */
    private static final PartialModel CONTROLLER_SHAFT_STUBS =
            PartialModel.of(PrimitiveRefined.id("block/p_controller_shaft_stubs"));

    /**
     * The Flywheel visual - the path that actually runs in normal play.
     *
     * <p>{@link KineticBlockEntityRenderer#renderSafe} opens with
     * {@code if (VisualizationManager.supportsVisualization(be.getLevel())) return;}, so
     * Create's block entity renderer is a <em>fallback for when Flywheel is off</em> and
     * nothing else. Registering the renderer alone left the shaft with no one drawing it
     * spinning whenever Flywheel was active.
     *
     * <p>{@code SingleAxisRotatingVisual.of} takes our own partial model rather than
     * Create's shaft, so this draws our block, not a grey Create one.
     */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SimpleBlockEntityVisualizer
                    .builder(PrRegistry.SOULSTAINED_SHAFT_BE.get())
                    .factory(SingleAxisRotatingVisual.of(SOULSTAINED_SHAFT_MODEL))
                    .apply();

            // The controller's stubs. Only the stubs are drawn here - the body keeps
            // coming from the chunk mesh, which a visual does not suppress (that is why
            // the shaft used to spin and stand still at once, and why the controller does
            // not vanish now).
            SimpleBlockEntityVisualizer
                    .builder(PrRegistry.P_CONTROLLER_BE.get())
                    .factory(SingleAxisRotatingVisual.of(CONTROLLER_SHAFT_STUBS))
                    .apply();

            for (String name : PrCogwheels.NAMES) {
                SimpleBlockEntityVisualizer
                        .builder(PrCogwheels.BLOCK_ENTITIES.get(name).get())
                        .factory(SingleAxisRotatingVisual.of(COGWHEEL_MODELS.get(name)))
                        .apply();
            }
        });
    }

    /**
     * The fallback for when Flywheel's backend is off.
     *
     * <p>Plain {@code KineticBlockEntityRenderer}, not Create's {@code ShaftRenderer}: the
     * latter overrides {@code getRenderedBlockState()} to return Create's own shaft state,
     * which is what once drew a grey Create shaft on top of ours. The base class returns
     * the block entity's own state, so it renders our model.
     */
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PrRegistry.SOULSTAINED_SHAFT_BE.get(),
                KineticBlockEntityRenderer::new);
        for (String name : PrCogwheels.NAMES) {
            event.registerBlockEntityRenderer(PrCogwheels.BLOCK_ENTITIES.get(name).get(),
                    KineticBlockEntityRenderer::new);
        }
    }
}
