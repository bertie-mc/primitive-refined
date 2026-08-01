package com.berlord.primitiverefined.content.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class GridMenuTest {
    @Test
    void serverGridMenuContainsThePlayerInventory(MinecraftServer server) {
        Inventory inventory = serverInventory();

        PGridContainerMenu menu = new PGridContainerMenu(1, inventory, mock(Grid.class));

        assertEquals(36, menu.slots.size());
    }

    @Test
    void serverCraftingGridMenuContainsMatrixResultAndPlayerSlots(MinecraftServer server) {
        Inventory inventory = serverInventory();

        PCraftingGridContainerMenu menu =
                new PCraftingGridContainerMenu(1, inventory, mock(CraftingGrid.class));

        assertEquals(46, menu.slots.size());
    }

    private static Inventory serverInventory() {
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.level()).thenReturn(mock(Level.class));
        Inventory inventory = new Inventory(player);
        when(player.getInventory()).thenReturn(inventory);
        return inventory;
    }
}
