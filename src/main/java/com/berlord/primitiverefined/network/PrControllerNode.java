package com.berlord.primitiverefined.network;

import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;

/**
 * The controller's network node. Carries no behaviour - it exists so the network can be
 * asked how many controllers are on it, which is the one rule Refined Storage enforces
 * about controllers and the one this mod has to enforce for itself.
 *
 * <p>Zero energy usage, because a primitive network does not run on energy. What a node
 * costs is Create stress, and it charges that through its own block entity, on the kinetic
 * network the RS network is laid over.
 */
public class PrControllerNode extends SimpleNetworkNode {

    public PrControllerNode() {
        super(0L);
    }
}
