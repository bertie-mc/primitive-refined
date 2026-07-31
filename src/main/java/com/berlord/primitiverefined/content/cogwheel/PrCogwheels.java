package com.berlord.primitiverefined.content.cogwheel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.berlord.primitiverefined.PrRegistry;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The six cogwheel variants: three gear materials by two shaft materials.
 *
 * <p>Each gets its own block entity type. That looks wasteful next to one shared type, but
 * Flywheel registers a visual per block entity type, and the visual is what draws the
 * spinning gear - a shared type would make all six render the same model.
 */
public final class PrCogwheels {

    private PrCogwheels() {
    }

    /** Registration ids, which are also the model and texture names. */
    public static final List<String> NAMES = List.of(
            "cogwheel", "cogwheel_soulstained",
            "obsidiansteel_cogwheel", "obsidiansteel_cogwheel_soulstained",
            "rose_gold_cogwheel", "rose_gold_cogwheel_soulstained");

    public static final Map<String, DeferredBlock<PrCogWheelBlock>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<BlockEntityType<?>, BlockEntityType<BracketedKineticBlockEntity>>>
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
                            .of((pos, state) -> new BracketedKineticBlockEntity(
                                            BLOCK_ENTITIES.get(name).get(), pos, state),
                                    BLOCKS.get(name).get())
                            .build(null)));

            BLOCKS.put(name, PrRegistry.BLOCKS.register(name,
                    () -> new PrCogWheelBlock(properties(name),
                            () -> BLOCK_ENTITIES.get(name).get())));

            ITEMS.put(name, PrRegistry.ITEMS.registerSimpleBlockItem(name, BLOCKS.get(name)));
        }
    }

    /** Touching this class runs the static block above. Called from {@link PrRegistry}. */
    public static void init() {
    }
}
