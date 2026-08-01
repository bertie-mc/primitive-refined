package com.berlord.primitiverefined.content.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.berlord.primitiverefined.PrStress;
import com.berlord.primitiverefined.network.PrNode;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.berlord.primitiverefined.network.PrNodes;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.externalstorage.ExternalStorageNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import com.refinedmods.refinedstorage.api.storage.tracked.InMemoryTrackedStorageRepository;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.storage.externalstorage.ExternalStorageProviderFactory;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The External Reader - Refined Storage's External Storage, driven by rotation.
 *
 * <p>It exposes the inventory <b>in front of it</b> to the network. Rotation arrives
 * through the shaft on its back, so the block sits in a straight line between the two:
 * shaft, reader, chest. That is the whole arrangement, and it is why the facing that puts
 * the display towards the player also puts the reader's face against whatever it is
 * reading.
 *
 * <p>This is the only storage medium in the mod. There are no disks and no storage blocks;
 * what a primitive network holds is whatever its readers can see, which is the early-game
 * shape of the thing - you are wiring up the chests you already have.
 */
public class ExternalReaderBlockEntity extends KineticBlockEntity implements PrNodeHost {

    /**
     * The clock behind the storage's change tracking - what makes a grid able to sort by
     * "last modified". Wall-clock milliseconds, matching what RS's own external storage
     * records.
     */
    private final ExternalStorageNetworkNode storageNode =
            new ExternalStorageNetworkNode(0L, System::currentTimeMillis);

    private final PrNode node = new PrNode(this, storageNode, "external_reader");

    /** The block last resolved a provider from, so a chest swapped for a barrel is noticed. */
    private BlockState observedTarget;

    // --- Diagnosis, computed on the server and shipped to the goggles ------------
    /** Slots in the target's item handler, or -1 if it has none on the face we touch. */
    private int targetSlots = -1;
    private int targetEmptySlots;
    /** Whether the network would take a resource it does not already hold. */
    private boolean networkAccepts;

    public ExternalReaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Without one the node quietly records nothing, and a grid sorted by "last
        // modified" never moves. RS's own repository for this is package-private; the
        // in-memory one behind it is not, and is what it extends.
        storageNode.setTrackingRepository(new InMemoryTrackedStorageRepository());
    }

    @Override
    public PrNode prNode() {
        return node;
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = PrStress.EXTERNAL_READER;
        return PrStress.EXTERNAL_READER;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        node.onClearRemoved();
    }

    /**
     * {@code remove}, not {@code setRemoved}: Create's {@code SmartBlockEntity} makes the
     * latter final and calls this from inside it, which is the hook it leaves open.
     */
    @Override
    public void remove() {
        super.remove();
        node.onSetRemoved();
    }

    @Override
    public void initialize() {
        super.initialize();
        refresh();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        refresh();
    }

    @Override
    public void tick() {
        super.tick();
        // Whatever is in the chest changed, or someone took something out by hand. RS's own
        // external storage rate-limits this against an adaptive work rate; a primitive
        // network is small enough that every tick is affordable and every tick is what
        // makes the grid feel live.
        if (level != null && !level.isClientSide && storageNode.isActive()) {
            storageNode.detectChanges();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        refresh();
    }

    private void refresh() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ExternalReaderBlock)) {
            return;
        }

        node.refresh(PrNodes.isPowered(this));

        BlockState target = serverLevel.getBlockState(targetPos(state));
        if (observedTarget == null || !target.equals(observedTarget)) {
            observedTarget = target;
            loadStorage(serverLevel, state);
        }

        refreshDiagnosis(serverLevel, state);

        boolean shouldBeLit = node.isActive();
        if (state.getValue(ExternalReaderBlock.LIT) != shouldBeLit) {
            KineticBlockEntity.switchToBlockState(level, worldPosition,
                    state.setValue(ExternalReaderBlock.LIT, shouldBeLit));
        }
    }

    /**
     * Re-reads what the block can see and sends it to the client if it moved.
     *
     * <p>The readout has to be computed here and shipped, not computed where it is drawn.
     * Create's goggle tooltip is a client HUD: on that side the network does not exist,
     * because a primitive network is only ever built server-side, and a chest's contents
     * are not there either. Asking those questions in {@code addToGoggleTooltip} answers
     * about the client's empty copy of the world, which is worse than not asking - it looks
     * like a diagnosis and is not one.
     */
    private void refreshDiagnosis(ServerLevel serverLevel, BlockState state) {
        Direction side = state.getValue(ExternalReaderBlock.HORIZONTAL_FACING).getOpposite();
        IItemHandler handler = serverLevel.getCapability(
                Capabilities.ItemHandler.BLOCK, targetPos(state), side);

        int slots = handler == null ? -1 : handler.getSlots();
        int empty = 0;
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (handler.getStackInSlot(slot).isEmpty()) {
                    empty++;
                }
            }
        }
        boolean accepts = probeInsert();

        if (slots != targetSlots || empty != targetEmptySlots || accepts != networkAccepts) {
            targetSlots = slots;
            targetEmptySlots = empty;
            networkAccepts = accepts;
            sendData();
        }
    }

    /**
     * Asks the network, for real, whether it would take one more item.
     *
     * <p>"The grid will not accept items" has two halves that look identical from outside:
     * either the storage refuses, or the menu never gets as far as asking it. Every link
     * between the grid's insert and this block's chest was read out of Refined Storage's
     * own bytecode and found to be the same code RS's own grid runs, so the answer is in
     * state that reading does not reach. This asks.
     *
     * <p>A <b>simulated</b> insert of one stone at the root storage - the same call
     * {@code TransferHelper} makes on the player's behalf. Stone rather than something the
     * chest already holds, because "will you take a resource you do not already have" is
     * the case that fails when a chest is full of full stacks. Simulated, so looking costs
     * nothing and changes nothing.
     */
    private boolean probeInsert() {
        Network network = node.network();
        if (network == null) {
            return false;
        }
        try {
            return network.getComponent(StorageNetworkComponent.class).insert(
                    ItemResource.ofItemStack(new ItemStack(Items.STONE)), 1,
                    Action.SIMULATE, new PlayerActor("goggles")) > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private BlockPos targetPos(BlockState state) {
        return worldPosition.relative(state.getValue(ExternalReaderBlock.HORIZONTAL_FACING));
    }

    /**
     * Hands the block in front to Refined Storage to wrap.
     *
     * <p>Every registered factory is tried, in order, rather than only the item-handler
     * one: that is the extension point other mods add themselves to, so a reader picks up
     * anything an RS External Storage would have.
     */
    private void loadStorage(ServerLevel serverLevel, BlockState state) {
        Direction facing = state.getValue(ExternalReaderBlock.HORIZONTAL_FACING);
        BlockPos target = targetPos(state);
        // The side of the target that we touch, which is the direction pointing from it
        // back at us. Same sense RS passes for its own external storage.
        Direction side = facing.getOpposite();

        List<ExternalStorageProvider> providers = new ArrayList<>();
        for (ExternalStorageProviderFactory factory : RefinedStorageApi.INSTANCE
                .getExternalStorageProviderFactories()) {
            ExternalStorageProvider provider = factory.create(serverLevel, target, side);
            if (provider != null) {
                providers.add(provider);
            }
        }
        storageNode.initialize(providers.isEmpty() ? EMPTY : new CompositeStorageProvider(providers));
    }

    /**
     * States what the reader can actually see, through the goggles.
     *
     * <p>The same reasoning as the controller's readout: "the grid will not take my items"
     * has several indistinguishable causes, and the block is the only thing that knows
     * which. It reports what it is pointed at, whether that block hands out an item
     * handler on the face it touches, how many slots have room, and whether the node is on
     * a live network - which between them separate "not powered", "not connected", "not an
     * inventory" and "the chest is full".
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (level == null) {
            return true;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ExternalReaderBlock)) {
            return true;
        }

        BlockState targetState = level.getBlockState(targetPos(state));

        tooltip.add(Component.literal(" ").append(
                Component.literal("External Reader").withStyle(ChatFormatting.GRAY)));
        line(tooltip, "reading", !targetState.isAir(),
                targetState.isAir() ? "nothing in front" : targetState.getBlock().getName().getString());
        line(tooltip, "item handler", targetSlots >= 0,
                targetSlots < 0 ? "none on the face this touches"
                        : targetSlots + " slots, " + targetEmptySlots + " empty");
        line(tooltip, "turning", getSpeed() != 0, String.format("%.1f rpm", getSpeed()));
        line(tooltip, "not overstressed", !isOverStressed(), isOverStressed() ? "overstressed" : "ok");
        line(tooltip, "would accept 1 stone", networkAccepts,
                networkAccepts ? "yes - the storage is willing"
                        : "no - nothing on this network will take it");
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("TargetSlots", targetSlots);
        tag.putInt("TargetEmptySlots", targetEmptySlots);
        tag.putBoolean("NetworkAccepts", networkAccepts);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targetSlots = tag.contains("TargetSlots") ? tag.getInt("TargetSlots") : -1;
        targetEmptySlots = tag.getInt("TargetEmptySlots");
        networkAccepts = tag.getBoolean("NetworkAccepts");
    }

    private static void line(List<Component> tooltip, String label, boolean ok, String detail) {
        tooltip.add(Component.literal("    ")
                .append(Component.literal(ok ? "✔ " : "✘ ")
                        .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal(label + ": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(detail).withStyle(ChatFormatting.WHITE)));
    }

    /**
     * What a reader facing a wall exposes. A node with no delegate at all would leave the
     * previous chest's contents on the network after the chest was mined.
     */
    private static final ExternalStorageProvider EMPTY = new ExternalStorageProvider() {
        @Override
        public Iterator<ResourceAmount> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
            return 0L;
        }

        @Override
        public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
            return 0L;
        }
    };
}
