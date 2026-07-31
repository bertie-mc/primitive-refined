package com.berlord.primitiverefined;

import com.berlord.primitiverefined.content.grid.PCraftingGridContainerMenu;
import com.berlord.primitiverefined.content.grid.PGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.AbstractCraftingGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.GridData;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The two grid menu types.
 *
 * <p>Registered by this mod rather than borrowed from Refined Storage, because a menu type
 * is what binds a menu to a screen and RS's types are bound to RS's blocks. The menus and
 * the screens on either side of these are RS's own.
 *
 * <p>{@code IMenuTypeExtension.create} rather than {@code MenuType::new}: opening a grid
 * ships the whole visible resource list to the client as extended screen data, and only the
 * extended factory gets a buffer to read it out of. {@link GridData#STREAM_CODEC} is the
 * same codec RS's own grids are opened with.
 */
public final class PrMenus {

    private PrMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, PrimitiveRefined.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PGridContainerMenu>> GRID =
            MENUS.register("p_grid", () -> IMenuTypeExtension.create(
                    (syncId, inventory, buf) ->
                            new PGridContainerMenu(syncId, inventory, GridData.STREAM_CODEC.decode(buf))));

    /**
     * Typed as the abstract menu, not ours.
     *
     * <p>{@code CraftingGridScreen} is not generic - it is an
     * {@code AbstractGridScreen<AbstractCraftingGridContainerMenu>}, so it is a
     * {@code MenuAccess} of the abstract type and nothing narrower. Registering it against
     * a {@code MenuType<PCraftingGridContainerMenu>} does not typecheck; widening the menu
     * type is what lets RS's screen be used unchanged, which is the point. The plain grid's
     * screen <em>is</em> generic, so its menu type stays narrow; it pays for that with an
     * explicit type argument at the registration site instead.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractCraftingGridContainerMenu>> CRAFTING_GRID =
            MENUS.register("p_crafting_grid", () -> IMenuTypeExtension.<AbstractCraftingGridContainerMenu>create(
                    (syncId, inventory, buf) ->
                            new PCraftingGridContainerMenu(syncId, inventory, GridData.STREAM_CODEC.decode(buf))));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
