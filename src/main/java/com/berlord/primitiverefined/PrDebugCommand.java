package com.berlord.primitiverefined;

import com.berlord.primitiverefined.content.grid.PCraftingGridBlockEntity;
import com.berlord.primitiverefined.content.grid.PGridBlockEntity;
import com.berlord.primitiverefined.content.grid.PGridContainerMenu;
import com.berlord.primitiverefined.network.PrNode;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode;
import com.refinedmods.refinedstorage.api.network.node.grid.GridInsertMode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.TrackedResourceAmount;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Server-side self-tests for the storage layer, driven from chat.
 *
 * <p><b>Why this exists.</b> The insertion path fails silently by design - RS's
 * {@code onInsert} returns false and nothing anywhere says why - and the only way to
 * exercise it as a player is to click a slot in a GUI. That makes it untestable by any
 * means except a human with a mouse, which is exactly how it stayed broken through three
 * releases while every piece of it read as correct.
 *
 * <p>These commands drive the same server-side calls the GUI drives, without the GUI:
 * {@code /prdebug insert} builds the real menu, puts a stack on its cursor and calls the
 * real {@code onInsert}. If it works here and not in the GUI, the fault is above the menu;
 * if it fails here, the fault is at or below it.
 *
 * <p>Operator-only ({@code /prdebug} needs permission level 2).
 */
@EventBusSubscriber(modid = PrimitiveRefined.MOD_ID)
public final class PrDebugCommand {

    private PrDebugCommand() {
    }

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> root =
                Commands.literal("prdebug").requires(source -> source.hasPermission(2));

        root.then(Commands.literal("insert")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> insert(ctx.getSource().getPlayerOrException(),
                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")))));

        root.then(Commands.literal("flow")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> flow(ctx.getSource().getPlayerOrException(),
                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")))));

        root.then(Commands.literal("extract")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> extract(ctx.getSource().getPlayerOrException(),
                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")))));

        root.then(Commands.literal("craft")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> craft(ctx.getSource().getPlayerOrException(),
                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")))));

        root.then(Commands.literal("net")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> describe(ctx.getSource().getPlayerOrException(),
                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")))));

        event.getDispatcher().register(root);
    }

    /** Builds the grid's real server menu and runs the real insert against it. */
    private static int insert(ServerPlayer player, BlockPos pos) {
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof PGridBlockEntity grid)) {
            player.sendSystemMessage(Component.literal("not a grid at " + pos));
            return 0;
        }
        PGridContainerMenu menu = new PGridContainerMenu(90, player.getInventory(), grid);
        menu.setCarried(new ItemStack(Items.STONE, 64));
        boolean ok;
        String failure = "";
        try {
            ok = menu.onInsert(GridInsertMode.ENTIRE_RESOURCE, false);
        } catch (RuntimeException e) {
            ok = false;
            failure = " threw " + e;
        }
        player.sendSystemMessage(Component.literal(
                "insert=" + ok
                        + " slots=" + menu.slots.size()
                        + " active=" + menu.isActive()
                        + " carriedAfter=" + menu.getCarried().getCount()
                        + failure));
        menu.removed(player);
        return 1;
    }

    /**
     * The whole player flow, server-side: open the grid for real, pick a stack up off an
     * inventory slot through vanilla's own click handler, then insert it.
     *
     * <p>{@code AbstractContainerMenu.clicked} is exactly what the server runs when a click
     * packet arrives, so a pick-up that works here is a pick-up that works for a player -
     * and it is the step that was impossible before the server menu had slots, because
     * vanilla refuses a click on a slot index the server does not have.
     */
    private static int flow(ServerPlayer player, BlockPos pos) {
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof PGridBlockEntity grid)) {
            player.sendSystemMessage(Component.literal("not a grid at " + pos));
            return 0;
        }
        player.getInventory().add(new ItemStack(Items.STONE, 64));
        player.openMenu(grid, buf -> grid.getMenuCodec().encode(buf, grid.getMenuData()));

        if (!(player.containerMenu instanceof AbstractGridContainerMenu menu)) {
            player.sendSystemMessage(Component.literal(
                    "openMenu did not give a grid menu, got " + player.containerMenu.getClass().getSimpleName()));
            return 0;
        }

        int stoneSlot = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            if (menu.slots.get(i).getItem().is(Items.STONE)) {
                stoneSlot = i;
                break;
            }
        }
        if (stoneSlot < 0) {
            player.sendSystemMessage(Component.literal(
                    "no stone in any of the menu's " + menu.slots.size() + " slots"));
            return 0;
        }

        menu.clicked(stoneSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        int carried = menu.getCarried().getCount();
        boolean inserted = menu.onInsert(GridInsertMode.ENTIRE_RESOURCE, false);

        player.sendSystemMessage(Component.literal(
                "slots=" + menu.slots.size()
                        + " pickedUpFromSlot=" + stoneSlot
                        + " carriedAfterPickup=" + carried
                        + " insert=" + inserted
                        + " carriedAfterInsert=" + menu.getCarried().getCount()));
        return 1;
    }

    /**
     * Pulls a stack back out of the network onto the cursor.
     *
     * <p>Extraction never went through the menu's slots - the packet names the resource and
     * RS puts it straight into the player's inventory - which is why it kept working while
     * insertion did not. Checked anyway, because giving the server menu slots changed the
     * menu that both paths run on.
     */
    private static int extract(ServerPlayer player, BlockPos pos) {
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof PGridBlockEntity grid)) {
            player.sendSystemMessage(Component.literal("not a grid at " + pos));
            return 0;
        }
        player.openMenu(grid, buf -> grid.getMenuCodec().encode(buf, grid.getMenuData()));
        if (!(player.containerMenu instanceof AbstractGridContainerMenu menu)) {
            player.sendSystemMessage(Component.literal("openMenu gave no grid menu"));
            return 0;
        }
        boolean ok = menu.onExtract(ItemResource.ofItemStack(new ItemStack(Items.STONE)),
                GridExtractMode.ENTIRE_RESOURCE, true);
        player.sendSystemMessage(Component.literal(
                "extract=" + ok + " carried=" + menu.getCarried().getCount()
                        + "x" + menu.getCarried().getItem()));
        return 1;
    }

    /**
     * The crafting grid's 3x3, which the same slot bug would have removed entirely.
     *
     * <p>{@code AbstractCraftingGridContainerMenu.resized} adds the matrix and the result
     * slot as well as the player inventory, so a server menu that was never resized had no
     * matrix to craft in at all. Puts one log in the matrix and asks what came out.
     */
    private static int craft(ServerPlayer player, BlockPos pos) {
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof PCraftingGridBlockEntity grid)) {
            player.sendSystemMessage(Component.literal("not a crafting grid at " + pos));
            return 0;
        }
        player.openMenu(grid, buf -> grid.getMenuCodec().encode(buf, grid.getMenuData()));
        if (!(player.containerMenu instanceof AbstractGridContainerMenu menu)) {
            player.sendSystemMessage(Component.literal("openMenu gave no grid menu"));
            return 0;
        }
        grid.getCraftingMatrix().setItem(0, new ItemStack(Items.OAK_LOG, 1));
        grid.getCraftingMatrix().changed();
        ItemStack result = grid.getCraftingResult().getItem(0);
        player.sendSystemMessage(Component.literal(
                "slots=" + menu.slots.size()
                        + " matrixSize=" + grid.getCraftingMatrix().getContainerSize()
                        + " result=" + (result.isEmpty() ? "(empty)"
                                : result.getCount() + "x" + result.getItem())));
        grid.getCraftingMatrix().setItem(0, ItemStack.EMPTY);
        grid.getCraftingMatrix().changed();
        return 1;
    }

    /** Dumps what the network at this block actually contains. */
    private static int describe(ServerPlayer player, BlockPos pos) {
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof PrNodeHost host)) {
            player.sendSystemMessage(Component.literal("no primitive node at " + pos));
            return 0;
        }
        PrNode node = host.prNode();
        Network network = node.network();
        if (network == null) {
            player.sendSystemMessage(Component.literal("node at " + pos + " is on no network"));
            return 0;
        }
        int containers = 0;
        StringBuilder names = new StringBuilder();
        for (NetworkNodeContainer container : network.getComponent(GraphNetworkComponent.class).getContainers()) {
            containers++;
            if (container instanceof InWorldNetworkNodeContainer inWorld) {
                names.append(inWorld.getName()).append(' ');
            }
        }
        StringBuilder resources = new StringBuilder();
        for (TrackedResourceAmount amount : network.getComponent(StorageNetworkComponent.class)
                .getResources(PlayerActor.class)) {
            resources.append(amount.resourceAmount().resource()).append('x')
                    .append(amount.resourceAmount().amount()).append(' ');
        }
        player.sendSystemMessage(Component.literal(
                "containers=" + containers
                        + " controllers=" + PrNode.controllerCount(network)
                        + " active=" + node.isActive()
                        + " | " + names));
        player.sendSystemMessage(Component.literal("stored: "
                + (resources.length() == 0 ? "(nothing)" : resources.toString())));
        return 1;
    }
}
