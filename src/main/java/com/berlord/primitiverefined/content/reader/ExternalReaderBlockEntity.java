package com.berlord.primitiverefined.content.reader;

import java.util.Collections;
import java.util.Iterator;

import com.berlord.primitiverefined.PrStress;
import com.berlord.primitiverefined.network.PrNode;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.berlord.primitiverefined.network.PrNodes;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.impl.node.externalstorage.ExternalStorageNetworkNode;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.externalstorage.ExternalStorageProviderFactory;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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

    public ExternalReaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

        boolean shouldBeLit = node.isActive();
        if (state.getValue(ExternalReaderBlock.LIT) != shouldBeLit) {
            KineticBlockEntity.switchToBlockState(level, worldPosition,
                    state.setValue(ExternalReaderBlock.LIT, shouldBeLit));
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
        for (ExternalStorageProviderFactory factory : RefinedStorageApi.INSTANCE
                .getExternalStorageProviderFactories()) {
            ExternalStorageProvider provider = factory.create(serverLevel, target, facing.getOpposite());
            if (provider != null) {
                storageNode.initialize(provider);
                return;
            }
        }
        storageNode.initialize(EMPTY);
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
