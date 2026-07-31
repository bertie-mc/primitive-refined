package com.berlord.primitiverefined.content.gearbox;

import com.berlord.primitiverefined.network.PrNode;
import com.berlord.primitiverefined.network.PrNodeHost;
import com.berlord.primitiverefined.network.PrNodes;
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create's gearbox entity with a network node on it - the cable that turns a corner.
 *
 * <p>Everything about the rotation stays Create's, including the sign flips across the
 * block. The network does not care which way a shaft turns, only that the two are joined,
 * and {@code RotationPropagator.isConnected} says they are on all four faces the gearbox
 * relays across.
 *
 * <p>Held as Create's own class until this build for the same reason the shaft was; it is
 * still Create's class, with the storage half added rather than substituted.
 */
public class ArcaneticGearboxBlockEntity extends GearboxBlockEntity implements PrNodeHost {

    private final PrNode node = new PrNode(this, new SimpleNetworkNode(0L), "arcanetic_gearbox");

    public ArcaneticGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
