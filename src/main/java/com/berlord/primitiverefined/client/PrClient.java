package com.berlord.primitiverefined.client;

import java.util.HashMap;
import java.util.Map;

import com.berlord.primitiverefined.PrMenus;
import com.berlord.primitiverefined.PrRegistry;
import com.berlord.primitiverefined.content.cogwheel.PrCogwheels;
import com.berlord.primitiverefined.PrimitiveRefined;
import com.berlord.primitiverefined.content.grid.PGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.screen.CraftingGridScreen;
import com.refinedmods.refinedstorage.common.grid.screen.GridScreen;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

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

    /** The grid's rear shaft and the cogwheel in its gap - one partial, one rotation. */
    private static final PartialModel GRID_KINETICS =
            PartialModel.of(PrimitiveRefined.id("block/p_grid_kinetics"));

    /** The External Reader's rear shaft. */
    private static final PartialModel READER_SHAFT =
            PartialModel.of(PrimitiveRefined.id("block/external_reader_shaft"));

    /** One half-shaft; the gearbox visual makes four copies and turns each onto a face. */
    private static final PartialModel GEARBOX_SHAFT =
            PartialModel.of(PrimitiveRefined.id("block/arcanetic_gearbox_shaft"));

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

            // The grids. Not SingleAxisRotatingVisual: that one turns the model onto the
            // rotation *axis*, and an axis has no sign, so a model with a shaft at only
            // one end lands on the wrong end for half the four facings. The controller's
            // stubs get away with it by being symmetric. OrientedRotatingVisual's
            // backHorizontal turns SOUTH onto HORIZONTAL_FACING.getOpposite() instead,
            // which is a direction, and is the face our shaft is on. It is what Create
            // drives its own mechanical crafter with.
            SimpleBlockEntityVisualizer
                    .builder(PrRegistry.P_GRID_BE.get())
                    .factory(OrientedRotatingVisual.backHorizontal(GRID_KINETICS))
                    .apply();
            SimpleBlockEntityVisualizer
                    .builder(PrRegistry.P_CRAFTING_GRID_BE.get())
                    .factory(OrientedRotatingVisual.backHorizontal(GRID_KINETICS))
                    .apply();

            // The External Reader's shaft, on the same footing as the grids': one end
            // only, so it needs the direction-aware visual rather than the axis one.
            SimpleBlockEntityVisualizer
                    .builder(PrRegistry.EXTERNAL_READER_BE.get())
                    .factory(OrientedRotatingVisual.backHorizontal(READER_SHAFT))
                    .apply();

            // Four shafts at four different speeds, so none of the stock visuals fit.
            SimpleBlockEntityVisualizer
                    .builder(PrRegistry.ARCANETIC_GEARBOX_BE.get())
                    .factory((ctx, be, partialTick) ->
                            new ArcaneticGearboxVisual(ctx, be, partialTick, GEARBOX_SHAFT))
                    .apply();
        });
    }

    /**
     * The grids' screens are Refined Storage's, unmodified.
     *
     * <p>This is the line that makes "the interfaces open the same" literally true: the
     * sorting buttons, the search box and its query syntax, the view modes, the resource
     * tooltips and the 3x3 matrix are all RS's screen classes, bound to menu types this mod
     * registered. Nothing about the grid's front end is reimplemented, so nothing about it
     * can drift from RS's.
     */
    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        // The type argument is spelled out: GridScreen is generic in its menu, and an
        // unparameterised GridScreen::new leaves javac unable to infer the screen half of
        // ScreenConstructor's two type variables from the menu half.
        event.register(PrMenus.GRID.get(), GridScreen<PGridContainerMenu>::new);
        event.register(PrMenus.CRAFTING_GRID.get(), CraftingGridScreen::new);
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
