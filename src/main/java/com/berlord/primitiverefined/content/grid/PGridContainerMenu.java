package com.berlord.primitiverefined.content.grid;

import com.berlord.primitiverefined.PrMenus;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.GridData;
import com.refinedmods.refinedstorage.api.network.node.grid.GridInsertMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    private static final Logger LOGGER = LoggerFactory.getLogger(PGridContainerMenu.class);

    public PGridContainerMenu(int syncId, Inventory playerInventory, GridData gridData) {
        super(PrMenus.GRID.get(), syncId, playerInventory, gridData);
    }

    public PGridContainerMenu(int syncId, Inventory playerInventory, Grid grid) {
        super(PrMenus.GRID.get(), syncId, playerInventory, grid);
        giveTheServerItsSlots(this, playerInventory);
    }

    /**
     * Puts the player inventory into the <em>server's</em> copy of a grid menu.
     *
     * <p>{@code AbstractGridContainerMenu.resized} is what calls {@code addPlayerInventory},
     * and the only thing that calls {@code resized} is {@code AbstractStretchingScreen} —
     * a screen, so the client. Neither of RS's constructors adds a slot. The server menu
     * therefore starts with an empty slot list and keeps it, which is fine for extraction
     * (the resource comes from the packet and goes straight into the player's inventory)
     * and fatal for insertion: {@code GridInsertPacket} carries no resource, so
     * {@code ItemGridInsertionStrategy} reads {@code containerMenu.getCarried()} — and a
     * menu with no slots is a menu the server never let you pick anything up in, so the
     * carried stack is always empty and {@code onInsert} returns false without a word.
     *
     * <p>That is exactly the reported symptom: the storage is willing — the reader's probe
     * says so — and the grid still refuses items.
     *
     * <p>Only the first argument is read by the grid, as the y for the inventory, and
     * y is a rendering concern the server has no opinion about. What matters is that the
     * <b>same thirty-six slots exist in the same order on both sides</b>, which is what
     * makes a click on one of them a click the server will honour.
     *
     * <p>Verified in game: with this, a pick-up puts 64 stone on the server's cursor and
     * the insert that follows succeeds. With the slots taken away again, the same flow
     * reports "no stone in any of the menu's 0 slots".
     */
    static void giveTheServerItsSlots(AbstractGridContainerMenu menu, Inventory playerInventory) {
        if (!playerInventory.player.level().isClientSide) {
            menu.resized(0, 0, 0);
        }
    }

    /**
     * Says what each side saw when an insert was attempted. DEBUG, so it costs nothing in
     * normal play and is there in {@code debug.log} when the answer is needed.
     *
     * <p>Kept past the fix deliberately: this is the one call whose failure is silent by
     * design - {@code onInsert} returns false and nothing anywhere says why - and that
     * silence is what let the bug survive three releases.
     */
    @Override
    public boolean onInsert(GridInsertMode mode, boolean tryAlternatives) {
        boolean inserted = super.onInsert(mode, tryAlternatives);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("grid insert on {}: carried={} slots={} active={} -> {}",
                    playerInventory.player.level().isClientSide ? "client" : "server",
                    getCarried(), slots.size(), isActive(), inserted);
        }
        return inserted;
    }
}
