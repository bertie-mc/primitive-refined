package com.berlord.primitiverefined.content.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The scan pacing copied from Refined Storage. What matters is that it is bounded at both
 * ends: it never scans more often than every fifth tick, and never gives up scanning.
 */
class ExternalStorageWorkRateTest {

    @Test
    void startsAtOneScanEverySecond() {
        assertEquals(20, ticksUntilWork(new ExternalStorageWorkRate()));
    }

    @Test
    void speedsUpToEveryFifthTickAndNoFurther() {
        ExternalStorageWorkRate rate = new ExternalStorageWorkRate();
        for (int i = 0; i < 10; i++) {
            rate.faster();
        }
        assertEquals(5, ticksUntilWork(rate));
    }

    @Test
    void slowsDownToEveryFortiethTickAndNoFurther() {
        ExternalStorageWorkRate rate = new ExternalStorageWorkRate();
        for (int i = 0; i < 10; i++) {
            rate.slower();
        }
        assertEquals(40, ticksUntilWork(rate));
    }

    @Test
    void oneChangeIsEnoughToTightenTheInterval() {
        ExternalStorageWorkRate rate = new ExternalStorageWorkRate();
        rate.faster();
        assertEquals(10, ticksUntilWork(rate));
    }

    /** Ticks the rate until it says to work, and reports how many that took. */
    private static int ticksUntilWork(ExternalStorageWorkRate rate) {
        for (int tick = 1; tick <= 1000; tick++) {
            if (rate.canDoWork()) {
                return tick;
            }
        }
        throw new AssertionError("work rate never allowed any work");
    }
}
