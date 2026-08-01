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
 * server. Both call {@code resized(0, 0, 0)}, which is what lays the slots out; RS's own
 * {@code GridContainerMenu} does exactly this in exactly these two places. On the client the
 * screen calls {@code resized} again with real coordinates once it knows its own height, and
 * on the server nothing ever will - which is why leaving it out left the server with a menu
 * that had no slots to click.
 */
public class PGridContainerMenu extends AbstractGridContainerMenu {

    public PGridContainerMenu(int syncId, Inventory playerInventory, GridData gridData) {
        super(PrMenus.GRID.get(), syncId, playerInventory, gridData);
        resized(0, 0, 0);
    }

    public PGridContainerMenu(int syncId, Inventory playerInventory, Grid grid) {
        super(PrMenus.GRID.get(), syncId, playerInventory, grid);
        resized(0, 0, 0);
    }
}
