package com.berlord.primitiverefined.content.reader;

/**
 * How often a reader looks at the chest in front of it.
 *
 * <p>A reader cannot be told when its target changes - a hopper feeding a chest, a player
 * taking something out by hand, another mod's machine emptying it - so it has to look. The
 * cost of looking is a full iteration of the inventory, so looking every tick is what makes
 * a wall of readers a server's problem.
 *
 * <p><b>Refined Storage's own {@code ExternalStorageWorkRate}</b>, copied because the
 * original is package-private. MIT, credited in NOTICE. The scheme is adaptive: it backs off
 * to once every two seconds while nothing is happening and closes up to once every quarter
 * second while something is, so a chest being actively filled is followed nearly live and an
 * idle one costs almost nothing. {@link #faster()} is also called directly when a neighbour
 * changes, which is how a hopper's first delivery is noticed at once rather than up to two
 * seconds later.
 */
final class ExternalStorageWorkRate {

    /** Ticks between scans, slowest first. */
    private static final int[] OPERATION_COUNTS = new int[] {40, 30, 20, 10, 5};

    private int idx = 2;
    private int counter;
    private int threshold = OPERATION_COUNTS[idx];

    boolean canDoWork() {
        counter++;
        if (counter >= threshold) {
            counter = 0;
            return true;
        }
        return false;
    }

    void faster() {
        if (idx + 1 < OPERATION_COUNTS.length) {
            idx++;
            threshold = OPERATION_COUNTS[idx];
        }
    }

    void slower() {
        if (idx - 1 >= 0) {
            idx--;
            threshold = OPERATION_COUNTS[idx];
        }
    }
}
