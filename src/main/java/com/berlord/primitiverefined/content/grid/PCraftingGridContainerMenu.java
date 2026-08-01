package com.berlord.primitiverefined.content.grid;

import com.berlord.primitiverefined.PrMenus;
import com.refinedmods.refinedstorage.common.grid.AbstractCraftingGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import com.refinedmods.refinedstorage.common.grid.GridData;

import net.minecraft.world.entity.player.Inventory;

/**
 * The Mechanical Crafting Grid's menu - RS's crafting grid menu under our own menu type.
 *
 * <p>Same shim as {@link PGridContainerMenu}, over the subclass that adds the 3x3 matrix,
 * the result slot, recipe transfer from EMI or JEI, and the "craft straight out of the
 * network" behaviour that is the whole point of a crafting grid.
 */
public class PCraftingGridContainerMenu extends AbstractCraftingGridContainerMenu {

    public PCraftingGridContainerMenu(int syncId, Inventory playerInventory, GridData gridData) {
        super(PrMenus.CRAFTING_GRID.get(), syncId, playerInventory, gridData);
        resized(0, 0, 0);
    }

    /**
     * Same as the plain grid, and it matters more here: this menu's {@code resized} adds
     * the 3x3 matrix and the result slot as well as the player inventory, so without it the
     * server has nothing to craft in either.
     */
    public PCraftingGridContainerMenu(int syncId, Inventory playerInventory, CraftingGrid craftingGrid) {
        super(PrMenus.CRAFTING_GRID.get(), syncId, playerInventory, craftingGrid);
        resized(0, 0, 0);
    }
}
