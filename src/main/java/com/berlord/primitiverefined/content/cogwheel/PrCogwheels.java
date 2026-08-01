package com.berlord.primitiverefined.content.cogwheel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.berlord.primitiverefined.PrRegistry;
import com.berlord.primitiverefined.network.ArcaneticRelayBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Registers the configured arcanetic cogwheel variants and their Flywheel block entities. */
public final class PrCogwheels {

    private PrCogwheels() {
    }

    /** Registration ids, which are also the model and texture names. */
    public static final List<String> NAMES = List.of("obsidiansteel_cogwheel_soulstained");

    public static final Map<String, DeferredBlock<PrCogWheelBlock>> BLOCKS = new LinkedHashMap<>();
    /**
     * Deliberately {@link CogwheelBlockItem}, not a plain {@code BlockItem}. Its
     * {@code onItemUseFirst} is what places a cogwheel meshed against another one; with a
     * plain block item the ghost preview still showed but the block was placed flat
     * against the clicked face, as if it were not a cogwheel at all.
     */
    public static final Map<String, DeferredItem<CogwheelBlockItem>> ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneticRelayBlockEntity>>>
            BLOCK_ENTITIES = new LinkedHashMap<>();

    private static BlockBehaviour.Properties properties(String name) {
        return BlockBehaviour.Properties.of()
                .mapColor(name.startsWith("obsidiansteel") ? MapColor.COLOR_BLACK
                        : name.startsWith("rose_gold") ? MapColor.COLOR_PINK
                        : MapColor.PODZOL)
                .sound(name.equals("cogwheel") || name.equals("cogwheel_soulstained")
                        ? SoundType.WOOD : SoundType.METAL)
                .strength(0.5F)
                .noOcclusion();
    }

    static {
        for (String name : NAMES) {
            // Registered before the block so the block's supplier can close over it; both
            // resolve lazily, so the mutual reference is fine.
            BLOCK_ENTITIES.put(name, PrRegistry.BLOCK_ENTITIES.register(name,
                    () -> BlockEntityType.Builder
                            .of((pos, state) -> new ArcaneticRelayBlockEntity(
                                            BLOCK_ENTITIES.get(name).get(), pos, state),
                                    BLOCKS.get(name).get())
                            .build(null)));

            BLOCKS.put(name, PrRegistry.BLOCKS.register(name,
                    () -> new PrCogWheelBlock(properties(name),
                            () -> BLOCK_ENTITIES.get(name).get())));

            ITEMS.put(name, PrRegistry.ITEMS.register(name,
                    () -> new CogwheelBlockItem(BLOCKS.get(name).get(), new Item.Properties())));
        }
    }

    /** Touching this class runs the static block above. Called from {@link PrRegistry}. */
    public static void init() {
    }
}
