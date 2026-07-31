package com.berlord.primitiverefined;

import com.berlord.primitiverefined.content.grid.PGridBlock;
import com.berlord.primitiverefined.content.reader.ExternalReaderBlock;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;

import javax.annotation.Nullable;

/**
 * What a primitive network costs to keep turning.
 *
 * <p>The controller itself is free. Everything hanging off it is not - and each part
 * charges its own cost, on Create's kinetic network, at the block that incurs it. The sum
 * is therefore Create's to compute and Create's to enforce: run more grids than the
 * waterwheel can carry and the whole line stalls, exactly as it would with any other
 * overloaded Create machine.
 *
 * <p>{@link #networkDemand} exists for the controller's goggle readout only. It is the same
 * number Create is already charging, gathered so the controller can state it in one place -
 * "this network wants 21 su" is a more useful thing to read off the box than a stress
 * figure per block. <b>It is never charged.</b> Adding it to the controller's own impact
 * would bill every grid twice.
 */
public final class PrStress {

    private PrStress() {
    }

    /** Cables only carry the network; they cost nothing to keep turning. */
    public static final float CABLE = 0f;

    /** The External Reader - one per attached inventory. */
    public static final float EXTERNAL_READER = 1f;

    /** A plain grid. */
    public static final float GRID = 5f;

    /** A crafting grid, the most expensive part in the demo tier. */
    public static final float CRAFTING_GRID = 10f;

    /**
     * The controller block itself. Deliberately zero: an unattached controller lights up
     * on any amount of rotational force, because it does not do any work on its own.
     */
    public static final float CONTROLLER = 0f;

    /**
     * Total stress the parts on this network demand, for display.
     *
     * <p>Read off the block states rather than the block entities, because a container's
     * block entity may be unloaded while the network still holds it, and a stress cost is a
     * property of the block either way.
     */
    public static float networkDemand(@Nullable Network network) {
        if (network == null) {
            return CONTROLLER;
        }
        float total = CONTROLLER;
        for (NetworkNodeContainer container : network.getComponent(GraphNetworkComponent.class).getContainers()) {
            if (!(container instanceof InWorldNetworkNodeContainer inWorld)) {
                continue;
            }
            total += costOf(inWorld);
        }
        return total;
    }

    private static float costOf(InWorldNetworkNodeContainer container) {
        var block = container.getBlockState().getBlock();
        if (block instanceof PGridBlock grid) {
            return grid.stressImpact();
        }
        if (block instanceof ExternalReaderBlock) {
            return EXTERNAL_READER;
        }
        return CABLE;
    }
}
