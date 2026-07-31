package com.berlord.primitiverefined;

import com.berlord.primitiverefined.content.cogwheel.PrCogwheels;
import com.berlord.primitiverefined.content.controller.PControllerBlock;
import com.berlord.primitiverefined.content.controller.PControllerBlockEntity;
import com.berlord.primitiverefined.content.gearbox.ArcaneticGearboxBlock;
import com.berlord.primitiverefined.content.gearbox.ArcaneticGearboxBlockEntity;
import com.berlord.primitiverefined.content.gearbox.VerticalArcaneticGearboxItem;
import com.berlord.primitiverefined.content.grid.PCraftingGridBlockEntity;
import com.berlord.primitiverefined.content.grid.PGridBlock;
import com.berlord.primitiverefined.content.reader.ExternalReaderBlock;
import com.berlord.primitiverefined.content.reader.ExternalReaderBlockEntity;
import com.berlord.primitiverefined.content.grid.PGridBlockEntity;
import com.berlord.primitiverefined.content.shaft.SoulstainedShaftBlock;
import com.berlord.primitiverefined.network.ArcaneticRelayBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PrRegistry {

    private PrRegistry() {
    }

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PrimitiveRefined.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PrimitiveRefined.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PrimitiveRefined.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PrimitiveRefined.MOD_ID);

    // --- Blocks -----------------------------------------------------------------

    public static final DeferredBlock<PControllerBlock> P_CONTROLLER = BLOCKS.register("p_controller",
            () -> new PControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .sound(SoundType.WOOD)
                    .strength(2.0F)
                    // The top two pixel rows are empty and the middle band is inset, so
                    // this can never be treated as a full cube for lighting or culling.
                    .noOcclusion()));

    public static final DeferredBlock<SoulstainedShaftBlock> SOULSTAINED_SHAFT = BLOCKS.register("soulstained_shaft",
            () -> new SoulstainedShaftBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .sound(SoundType.METAL)
                    .strength(0.5F)
                    .noOcclusion()));

    public static final DeferredBlock<PGridBlock> P_GRID = BLOCKS.register("p_grid",
            () -> new PGridBlock(gridProperties(), PrStress.GRID, () -> PrRegistry.P_GRID_BE.get()));

    public static final DeferredBlock<PGridBlock> P_CRAFTING_GRID = BLOCKS.register("p_crafting_grid",
            () -> new PGridBlock(gridProperties(), PrStress.CRAFTING_GRID, () -> PrRegistry.P_CRAFTING_GRID_BE.get()));


    /**
     * The External Reader. {@code noOcclusion} for the same reason the grids have it: its
     * shaft is a Flywheel instance, and an instance is lit from the light at the block's
     * own position, which an occluding block leaves at zero.
     */
    public static final DeferredBlock<ExternalReaderBlock> EXTERNAL_READER = BLOCKS.register("external_reader",
            () -> new ExternalReaderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .strength(2.0F)));

    /** Create's gearbox in our materials. Not a full cube - the core is inset. */
    public static final DeferredBlock<ArcaneticGearboxBlock> ARCANETIC_GEARBOX =
            BLOCKS.register("arcanetic_gearbox", () -> new ArcaneticGearboxBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PODZOL)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
                            .strength(2.0F)));

    private static BlockBehaviour.Properties gridProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_YELLOW)
                .sound(SoundType.METAL)
                // Without this the shaft and the cogwheel render pitch black. They are
                // Flywheel instances, and an instance is lit from the light value at the
                // block's *own* position; an occluding block blocks light, so that value
                // is zero. The body looks fine either way, because chunk-mesh faces are
                // lit from the neighbouring position instead - which is exactly what
                // makes this look like a texture fault rather than a lighting one.
                // Correct on its own terms too: the rims have a see-through window and
                // the back is recessed, so this was never a solid cube.
                .noOcclusion()
                .strength(2.0F);
    }

    // --- Items ------------------------------------------------------------------

    public static final DeferredItem<BlockItem> P_CONTROLLER_ITEM =
            ITEMS.registerSimpleBlockItem("p_controller", P_CONTROLLER);

    public static final DeferredItem<BlockItem> SOULSTAINED_SHAFT_ITEM =
            ITEMS.registerSimpleBlockItem("soulstained_shaft", SOULSTAINED_SHAFT);

    public static final DeferredItem<BlockItem> P_GRID_ITEM =
            ITEMS.registerSimpleBlockItem("p_grid", P_GRID);

    public static final DeferredItem<BlockItem> P_CRAFTING_GRID_ITEM =
            ITEMS.registerSimpleBlockItem("p_crafting_grid", P_CRAFTING_GRID);

    public static final DeferredItem<BlockItem> EXTERNAL_READER_ITEM =
            ITEMS.registerSimpleBlockItem("external_reader", EXTERNAL_READER);

    public static final DeferredItem<BlockItem> ARCANETIC_GEARBOX_ITEM =
            ITEMS.registerSimpleBlockItem("arcanetic_gearbox", ARCANETIC_GEARBOX);

    /**
     * The same block, placed on a horizontal axis. One block, two items - which is how
     * Create ships its own vertical gearbox, and why this needs no second block entity.
     */
    public static final DeferredItem<VerticalArcaneticGearboxItem> ARCANETIC_GEARBOX_VERTICAL_ITEM =
            ITEMS.register("arcanetic_gearbox_vertical",
                    () -> new VerticalArcaneticGearboxItem(ARCANETIC_GEARBOX.get(),
                            new net.minecraft.world.item.Item.Properties()));


    // --- Block entities ---------------------------------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PControllerBlockEntity>> P_CONTROLLER_BE =
            BLOCK_ENTITIES.register("p_controller", () -> BlockEntityType.Builder
                    .of((pos, state) -> new PControllerBlockEntity(PrRegistry.P_CONTROLLER_BE.get(), pos, state),
                            P_CONTROLLER.get())
                    .build(null));

    /**
     * Create's own shaft uses {@code BracketedKineticBlockEntity}, and
     * {@code AbstractSimpleShaftBlock} was written against it - notably
     * {@code removeBracket}, which expects the bracket behaviour to be present.
     * {@link ArcaneticRelayBlockEntity} extends that class rather than replacing it, so the
     * shaft stays behaviourally identical and gains a network node.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneticRelayBlockEntity>> SOULSTAINED_SHAFT_BE =
            BLOCK_ENTITIES.register("soulstained_shaft", () -> BlockEntityType.Builder
                    .of((pos, state) -> new ArcaneticRelayBlockEntity(PrRegistry.SOULSTAINED_SHAFT_BE.get(), pos, state),
                            SOULSTAINED_SHAFT.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PGridBlockEntity>> P_GRID_BE =
            BLOCK_ENTITIES.register("p_grid", () -> BlockEntityType.Builder
                    .of((pos, state) -> new PGridBlockEntity(PrRegistry.P_GRID_BE.get(), pos, state), P_GRID.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PCraftingGridBlockEntity>> P_CRAFTING_GRID_BE =
            BLOCK_ENTITIES.register("p_crafting_grid", () -> BlockEntityType.Builder
                    .of((pos, state) -> new PCraftingGridBlockEntity(PrRegistry.P_CRAFTING_GRID_BE.get(), pos, state),
                            P_CRAFTING_GRID.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExternalReaderBlockEntity>> EXTERNAL_READER_BE =
            BLOCK_ENTITIES.register("external_reader", () -> BlockEntityType.Builder
                    .of((pos, state) -> new ExternalReaderBlockEntity(PrRegistry.EXTERNAL_READER_BE.get(), pos, state),
                            EXTERNAL_READER.get())
                    .build(null));

    /** Create's own GearboxBlockEntity, extended with a network node. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneticGearboxBlockEntity>> ARCANETIC_GEARBOX_BE =
            BLOCK_ENTITIES.register("arcanetic_gearbox", () -> BlockEntityType.Builder
                    .of((pos, state) -> new ArcaneticGearboxBlockEntity(PrRegistry.ARCANETIC_GEARBOX_BE.get(), pos, state),
                            ARCANETIC_GEARBOX.get())
                    .build(null));

    // --- Creative tab -----------------------------------------------------------

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.primitive_refined"))
                    .icon(() -> new ItemStack(P_CONTROLLER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(P_CONTROLLER_ITEM.get());
                        output.accept(SOULSTAINED_SHAFT_ITEM.get());
                        output.accept(P_GRID_ITEM.get());
                        output.accept(P_CRAFTING_GRID_ITEM.get());
                        output.accept(EXTERNAL_READER_ITEM.get());
                        output.accept(ARCANETIC_GEARBOX_ITEM.get());
                        output.accept(ARCANETIC_GEARBOX_VERTICAL_ITEM.get());
                        PrCogwheels.ITEMS.values().forEach(i -> output.accept(i.get()));
                    })
                    .build());

    public static void register(IEventBus modBus) {
        // Runs the cogwheel family's static block, which registers into the deferred
        // registers below. Must happen before they are handed to the bus.
        PrCogwheels.init();

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
        PrMenus.register(modBus);
    }

    /** Convenience for anything that wants the block list without touching the holders. */
    public static Block[] allBlocks() {
        return new Block[] { P_CONTROLLER.get(), SOULSTAINED_SHAFT.get() };
    }
}
