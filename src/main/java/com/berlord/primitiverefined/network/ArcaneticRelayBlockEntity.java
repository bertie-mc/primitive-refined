package com.berlord.primitiverefined.network;

import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Arcanetic Shaft and the Arcanetic Cog - the network's cable.
 *
 * <p>Create's {@code BracketedKineticBlockEntity} with a network node bolted on. It was
 * Create's class outright until the storage half landed; the shaft still behaves exactly as
 * Create's does, and the only thing added is that it carries a network as well as a
 * rotation. That is the whole conceit of the mod: the cable is the driveshaft.
 *
 * <p>One class for both blocks. They keep separate block entity <em>types</em>, because
 * Flywheel binds a visual per type and they draw different models, but there is nothing
 * different about them to write twice.
 *
 * <p>Zero energy usage and no stress impact of its own. A cable costs nothing to keep
 * turning; the machines hanging off it are what a waterwheel pays for.
 */
public class ArcaneticRelayBlockEntity extends BracketedKineticBlockEntity implements PrNodeHost {

    private final PrNode node = new PrNode(this, new SimpleNetworkNode(0L), "arcanetic_relay");

    public ArcaneticRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public PrNode prNode() {
        return node;
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
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        node.refresh(PrNodes.isPowered(this));
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        node.refresh(PrNodes.isPowered(this));
    }
}
