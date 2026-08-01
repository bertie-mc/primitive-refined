package com.berlord.primitiverefined.content.reader;

import java.util.List;
import java.util.Objects;

import com.berlord.primitiverefined.PrStress;
import com.berlord.primitiverefined.network.PrNetworkNodeContainer;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.externalstorage.ExternalStorageNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
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
 *
 * <p>Everything storage-side follows RS's {@code AbstractExternalStorageBlockEntity}: the
 * provider is loaded once, when the node first goes active, and again when the block is
 * turned; scanning is paced by {@link ExternalStorageWorkRate}; and what a player last
 * touched is remembered by {@link ExternalStorageTrackedStorageRepository} across a save.
 * What is left out is RS's configuration - filters, fuzzy mode, access mode, priority, void
 * excess - which arrives through a menu this block does not have.
 */
public class ExternalReaderBlockEntity extends KineticBlockEntity implements PrNodeHost {

    private static final String TAG_TRACKED_RESOURCES = "tr";

    /**
     * The clock behind the storage's change tracking - what makes a grid able to sort by
     * "last modified". Wall-clock milliseconds, matching what RS's own external storage
     * records.
     */
    private final ExternalStorageNetworkNode storageNode =
            new ExternalStorageNetworkNode(0L, System::currentTimeMillis);

    private final ExternalStorageTrackedStorageRepository trackedStorageRepository =
            new ExternalStorageTrackedStorageRepository(this::setChanged);

    private final ExternalStorageWorkRate workRate = new ExternalStorageWorkRate();

    private final PrNetworkNodeContainer node =
            new PrNetworkNodeContainer(this, storageNode, "external_reader");

    /** Whether the provider has been resolved once. RS's flag, and RS's use for it. */
    private boolean initialized;

    // --- Readout, computed on the server and shipped to the goggles --------------
    /** Slots in the target's item handler, or -1 if it has none on the face we touch. */
    private int targetSlots = -1;
    private int targetEmptySlots;
    /** Whether the network would take a resource it does not already hold. */
    private boolean networkAccepts;

    public ExternalReaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Without a tracking repository the node quietly records nothing, and a grid sorted
        // by "last modified" never moves.
        storageNode.setTrackingRepository(trackedStorageRepository);
        // RS does its first storage load from its activenessChanged override. Composition
        // has no override, so the same work hangs off the container's listener.
        node.setActivenessListener(this::activenessChanged);
    }

    @Override
    public PrNetworkNodeContainer prNode() {
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
        node.clearRemoved();
    }

    /**
     * {@code remove}, not {@code setRemoved}: Create's {@code SmartBlockEntity} makes the
     * latter final and calls this from inside it, which is the hook it leaves open.
     */
    @Override
    public void remove() {
        super.remove();
        node.setRemoved();
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

    /**
     * Re-resolves the provider when the block itself is turned.
     *
     * <p>RS hangs this off {@code setBlockState} too, guarded on the direction actually
     * changing. The guard is not optional here: the lit state flips through
     * {@code switchToBlockState}, which sets the block, which lands in this method - so
     * without it every light change would re-resolve the storage.
     *
     * <p>Vanilla marks the method deprecated to mean "the game calls this, you do not". RS
     * overrides it here for exactly this, which is what it is for.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState newBlockState) {
        BlockState oldBlockState = getBlockState();
        super.setBlockState(newBlockState);
        if (level instanceof ServerLevel serverLevel && initialized && facingChanged(oldBlockState, newBlockState)) {
            loadStorage(serverLevel);
        }
    }

    private static boolean facingChanged(BlockState oldBlockState, BlockState newBlockState) {
        return oldBlockState.hasProperty(ExternalReaderBlock.HORIZONTAL_FACING)
                && newBlockState.hasProperty(ExternalReaderBlock.HORIZONTAL_FACING)
                && oldBlockState.getValue(ExternalReaderBlock.HORIZONTAL_FACING)
                != newBlockState.getValue(ExternalReaderBlock.HORIZONTAL_FACING);
    }

    /**
     * A block next to us changed - very often the chest we are reading.
     *
     * <p>Nothing is re-resolved: the providers RS builds hold a NeoForge capability cache,
     * which invalidates itself when the block it points at changes, so a chest swapped for a
     * barrel is followed without being told. What this does is wind the scan rate back up,
     * so the change is seen on the next tick or two rather than up to two seconds later.
     */
    public void neighborChanged() {
        workRate.faster();
    }

    /**
     * RS's {@code doWork}: scan the target for changes, and let how often it changes decide
     * how often to look again.
     */
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !storageNode.isActive()) {
            return;
        }
        if (workRate.canDoWork()) {
            if (storageNode.detectChanges()) {
                workRate.faster();
            } else {
                workRate.slower();
            }
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
        node.update(getBlockState(), ExternalReaderBlock.LIT);
        refreshReadout(serverLevel, getBlockState());
    }

    /** RS's {@code activenessChanged}: the first time this block works, resolve what it reads. */
    private void activenessChanged(boolean newActive) {
        if (!initialized && level instanceof ServerLevel serverLevel) {
            loadStorage(serverLevel);
            initialized = true;
        }
    }

    /**
     * Hands the block in front to Refined Storage to wrap.
     *
     * <p>Every registered factory is tried, in order, rather than only the item-handler
     * one: that is the extension point other mods add themselves to, so a reader picks up
     * anything an RS External Storage would have. This is RS's own {@code loadStorage},
     * minus its special case for RS's Interface block, which cannot join a primitive network.
     */
    private void loadStorage(ServerLevel serverLevel) {
        BlockState state = getBlockState();
        if (!state.hasProperty(ExternalReaderBlock.HORIZONTAL_FACING)) {
            return;
        }
        Direction facing = state.getValue(ExternalReaderBlock.HORIZONTAL_FACING);
        BlockPos target = worldPosition.relative(facing);
        // The side of the target that we touch, which is the direction pointing from it
        // back at us. Same sense RS passes for its own external storage.
        Direction incomingDirection = facing.getOpposite();

        storageNode.initialize(new CompositeExternalStorageProvider(
                RefinedStorageApi.INSTANCE.getExternalStorageProviderFactories()
                        .stream()
                        .map(factory -> factory.create(serverLevel, target, incomingDirection))
                        .filter(Objects::nonNull)
                        .toList()));
    }

    // --- Readout ----------------------------------------------------------------

    /**
     * Re-reads what the block can see and sends it to the client if it moved.
     *
     * <p>The readout has to be computed here and shipped, not computed where it is drawn.
     * Create's goggle tooltip is a client HUD: on that side the network does not exist,
     * because a primitive network is only ever built server-side, and a chest's contents
     * are not there either. Asking those questions in {@code addToGoggleTooltip} would read
     * the client's empty copy of the world and display incorrect state.
     */
    private void refreshReadout(ServerLevel serverLevel, BlockState state) {
        if (!state.hasProperty(ExternalReaderBlock.HORIZONTAL_FACING)) {
            return;
        }
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
     * either the storage refuses, or the menu never gets as far as asking it.
     *
     * <p>A <b>simulated</b> insert of one stone at the root storage - the same call
     * {@code TransferHelper} makes on the player's behalf. Stone rather than something the
     * chest already holds, because "will you take a resource you do not already have" is
     * the case that fails when a chest is full of full stacks. Simulated, so looking costs
     * nothing and changes nothing.
     */
    private boolean probeInsert() {
        Network network = node.getNetwork();
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

    // --- Persistence ------------------------------------------------------------

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("TargetSlots", targetSlots);
        tag.putInt("TargetEmptySlots", targetEmptySlots);
        tag.putBoolean("NetworkAccepts", networkAccepts);
        if (!clientPacket) {
            tag.put(TAG_TRACKED_RESOURCES, trackedStorageRepository.toTag(registries));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targetSlots = tag.contains("TargetSlots") ? tag.getInt("TargetSlots") : -1;
        targetEmptySlots = tag.getInt("TargetEmptySlots");
        networkAccepts = tag.getBoolean("NetworkAccepts");
        if (!clientPacket && tag.contains(TAG_TRACKED_RESOURCES)) {
            trackedStorageRepository.fromTag(
                    Objects.requireNonNull(tag.get(TAG_TRACKED_RESOURCES)), registries);
        }
    }

    private static void line(List<Component> tooltip, String label, boolean ok, String detail) {
        tooltip.add(Component.literal("    ")
                .append(Component.literal(ok ? "✔ " : "✘ ")
                        .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal(label + ": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(detail).withStyle(ChatFormatting.WHITE)));
    }
}
