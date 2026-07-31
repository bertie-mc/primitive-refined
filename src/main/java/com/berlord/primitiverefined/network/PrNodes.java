package com.berlord.primitiverefined.network;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

/** Small shared helpers for the block entities that sit on a primitive network. */
public final class PrNodes {

    private PrNodes() {
    }

    /**
     * Whether this block is actually being driven.
     *
     * <p>Turning at all, and the kinetic network not over its stress budget - because
     * overstressed means the stress units are not in fact being supplied, which is the
     * unpowered case however fast the shaft says it is going. Every lit-state check in this
     * mod already used exactly this pair; the network's activeness now uses it too, so a
     * grid that has gone dark is a grid that has left the network rather than a grid that
     * merely looks like it has.
     */
    public static boolean isPowered(KineticBlockEntity be) {
        return be.getSpeed() != 0 && !be.isOverStressed();
    }
}
