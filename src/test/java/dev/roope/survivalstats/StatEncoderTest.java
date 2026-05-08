package dev.roope.survivalstats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatEncoderTest {

    @Test
    void distanceDelegatesToUnit() {
        assertEquals(10, StatEncoder.distance(1_000, DistanceUnit.METRIC));     // 10 m
        assertEquals(33, StatEncoder.distance(1_000, DistanceUnit.IMPERIAL));   // ~32.8 ft -> 33
    }

    @Test
    void playTimeMinutesIsTotalMinutes() {
        assertEquals(0, StatEncoder.playTimeMinutes(0));
        assertEquals(0, StatEncoder.playTimeMinutes(-100));
        assertEquals(0, StatEncoder.playTimeMinutes(20 * 30)); // 30 s -> 0 min
        assertEquals(1, StatEncoder.playTimeMinutes(20 * 60)); // 60 s -> 1 min
        assertEquals(2, StatEncoder.playTimeMinutes(20 * 60 * 2 + 5)); // 2 min + 0.25 s
        assertEquals(60, StatEncoder.playTimeMinutes(20 * 60 * 60)); // 1 h -> 60 min
        assertEquals(60_000, StatEncoder.playTimeMinutes(20 * 60 * 60_000)); // 1 000 h -> 60 000 min
    }

    @Test
    void playTimeMinutesIsMonotonic() {
        // The old HHmmss encoding was non-monotonic (e.g., 25h was 250000, but 100m was also 10000).
        // Verify the new encoding strictly increases with input ticks.
        int prev = -1;
        for (int ticks = 0; ticks < 20 * 60 * 1_000; ticks += 20 * 30) {
            int minutes = StatEncoder.playTimeMinutes(ticks);
            assertTrue(minutes >= prev, "encoding regressed at " + ticks);
            prev = minutes;
        }
    }
}
