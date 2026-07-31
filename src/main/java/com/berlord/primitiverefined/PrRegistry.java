package com.berlord.primitiverefined;

import com.berlord.primitiverefined.content.controller.PControllerBlock;
import com.berlord.primitiverefined.content.controller.PControllerBlockEntity;
import com.berlord.primitiverefined.content.shaft.SoulstainedShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;

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

    // --- Items ------------------------------------------------------------------

    public static final DeferredItem<BlockItem> P_CONTROLLER_ITEM =
            ITEMS.registerSimpleBlockItem("p_controller", P_CONTROLLER);

    public static final DeferredItem<BlockItem> SOULSTAINED_SHAFT_ITEM =
            ITEMS.registerSimpleBlockItem("soulstained_shaft", SOULSTAINED_SHAFT);

    // --- Block entities ---------------------------------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PControllerBlockEntity>> P_CONTROLLER_BE =
            BLOCK_ENTITIES.register("p_controller", () -> BlockEntityType.Builder
                    .of((pos, state) -> new PControllerBlockEntity(PrRegistry.P_CONTROLLER_BE.get(), pos, state),
                            P_CONTROLLER.get())
                    .build(null));

    /**
     * Create's own shaft uses {@code BracketedKineticBlockEntity}, and
     * {@code AbstractSimpleShaftBlock} was written against it - notably
     * {@code removeBracket}, which expects the bracket behaviour to be present. Reusing it
     * keeps the soulstained shaft behaviourally identical instead of almost identical.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BracketedKineticBlockEntity>> SOULSTAINED_SHAFT_BE =
            BLOCK_ENTITIES.register("soulstained_shaft", () -> BlockEntityType.Builder
                    .of((pos, state) -> new BracketedKineticBlockEntity(PrRegistry.SOULSTAINED_SHAFT_BE.get(), pos, state),
                            SOULSTAINED_SHAFT.get())
                    .build(null));

    // --- Creative tab -----------------------------------------------------------

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.primitive_refined"))
                    .icon(() -> new ItemStack(P_CONTROLLER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(P_CONTROLLER_ITEM.get());
                        output.accept(SOULSTAINED_SHAFT_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
    }

    /** Convenience for anything that wants the block list without touching the holders. */
    public static Block[] allBlocks() {
        return new Block[] { P_CONTROLLER.get(), SOULSTAINED_SHAFT.get() };
    }
}
