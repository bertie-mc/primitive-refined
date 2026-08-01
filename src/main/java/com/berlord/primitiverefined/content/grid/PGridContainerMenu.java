package com.berlord.primitiverefined.content.grid;

import com.berlord.primitiverefined.PrMenus;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.GridData;

import net.minecraft.world.entity.player.Inventory;

/**
 * The Mechanical Grid's menu - Refined Storage's grid menu under our own menu type.
 *
 * <p>Nothing is reimplemented. Sorting, the search box and its query language, the view
 * modes, the synchronizers, insert, extract, scroll and shift-click all live in
 * {@link AbstractGridContainerMenu}, and this is a two-constructor shim so they can be
 * reached through a menu type this mod registers. The screen is RS's too - see
 * {@code PrClient}. A grid opened off a waterwheel is the same grid, because it is the
 * same code.
 *
 * <p>Two constructors, as RS's own grid has: the {@link GridData} one is what the client
 * builds from the packet, the {@link Grid} one is what the block entity opens on the
 * server.
 */
public class PGridContainerMenu extends AbstractGridContainerMenu {

    public PGridContainerMenu(int syncId, Inventory playerInventory, GridData gridData) {
        super(PrMenus.GRID.get(), syncId, playerInventory, gridData);
    }

    public PGridContainerMenu(int syncId, Inventory playerInventory, Grid grid) {
        super(PrMenus.GRID.get(), syncId, playerInventory, grid);
        giveTheServerItsSlots(this, playerInventory);
    }

    /** Gives the server the same player-inventory slots that the client screen creates. */
    static void giveTheServerItsSlots(AbstractGridContainerMenu menu, Inventory playerInventory) {
        if (!playerInventory.player.level().isClientSide) {
            menu.resized(0, 0, 0);
        }
    }
}
