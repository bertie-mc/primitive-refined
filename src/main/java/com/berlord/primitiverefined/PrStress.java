package com.berlord.primitiverefined;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The stress cost of a primitive network.
 *
 * <p>The controller itself is free. Everything hanging off it is not, and the controller
 * simply reports the sum as its own Create stress impact - so a network you have not
 * built yet costs nothing to spin, and one full of crafting grids will stall a
 * waterwheel. That is the whole design: you pay for the parts, not for the box.
 */
public final class PrStress {

    private PrStress() {
    }

    /** Cables only carry the network; they cost nothing to keep turning. */
    public static final float CABLE = 0f;

    /** External storage - one per attached inventory. */
    public static final float EXTERNAL_STORAGE = 1f;

    /** A plain grid. */
    public static final float GRID = 5f;

    /** A crafting grid, the most expensive part in the demo tier. */
    public static final float CRAFTING_GRID = 10f;

    /**
     * The controller block itself. Deliberately zero: an unattached controller lights up
     * on any amount of rotational force, because it does not do any work on its own.
     */
    public static final float CONTROLLER = 0f;

    /** Implemented by every primitive network block that adds load to its controller. */
    public interface Part {
        float primitiveStressDemand();
    }

    /**
     * Total stress the network attached to {@code controllerPos} demands.
     *
     * <p>Demo build: the cable, grid, crafting grid and external storage blocks do not
     * exist yet, so there is nothing to walk and the answer is always {@link #CONTROLLER}.
     * When the cable block lands this becomes a flood fill from the controller over
     * connected cables, summing {@link Part#primitiveStressDemand()} for everything it
     * reaches. The constants above are already the values that walk will use.
     */
    public static float totalDemand(Level level, BlockPos controllerPos) {
        return CONTROLLER;
    }
}
