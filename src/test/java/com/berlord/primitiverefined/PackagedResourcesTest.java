package com.berlord.primitiverefined;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PackagedResourcesTest {
    private static final List<String> BLOCKS = List.of(
            "arcanetic_gearbox",
            "external_reader",
            "obsidiansteel_cogwheel_soulstained",
            "p_controller",
            "p_crafting_grid",
            "p_grid",
            "soulstained_shaft");

    @Test
    void everyBlockHasItsPlayerFacingResources() throws IOException {
        String language = read("assets/primitive_refined/lang/en_us.json");
        for (String block : BLOCKS) {
            assertResource("assets/primitive_refined/blockstates/" + block + ".json");
            assertResource("assets/primitive_refined/models/block/" + block + ".json");
            assertResource("assets/primitive_refined/models/item/" + block + ".json");
            assertResource("data/primitive_refined/loot_table/blocks/" + block + ".json");
            assertTrue(language.contains("\"block.primitive_refined." + block + "\""),
                    () -> "Missing English name for " + block);
        }
    }

    @Test
    void verticalGearboxItemHasAModelAndName() throws IOException {
        assertResource("assets/primitive_refined/models/item/arcanetic_gearbox_vertical.json");
        assertTrue(read("assets/primitive_refined/lang/en_us.json")
                .contains("\"item.primitive_refined.arcanetic_gearbox_vertical\""));
    }

    private static void assertResource(String path) {
        assertNotNull(PackagedResourcesTest.class.getClassLoader().getResource(path),
                () -> "Missing packaged resource " + path);
    }

    private static String read(String path) throws IOException {
        try (InputStream stream = PackagedResourcesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, () -> "Missing packaged resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
