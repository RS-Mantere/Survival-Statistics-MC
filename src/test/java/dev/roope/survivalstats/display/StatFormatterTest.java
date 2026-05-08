package dev.roope.survivalstats.display;

import dev.roope.survivalstats.DistanceUnit;
import dev.roope.survivalstats.StatDef;
import dev.roope.survivalstats.StatSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatFormatterTest {
    @Test
    void formatsDistanceMetricAndImperial() {
        assertEquals("1,000 m", StatFormatter.formatValue(StatDef.DISTANCE, 100_000L, DistanceUnit.METRIC));
        assertEquals("3,281 ft", StatFormatter.formatValue(StatDef.DISTANCE, 100_000L, DistanceUnit.IMPERIAL));
    }

    @Test
    void formatsPlaytimeAsHoursAndMinutes() {
        assertEquals("0h0m", StatFormatter.formatValue(StatDef.PLAYTIME, 0L, DistanceUnit.METRIC));
        assertEquals("12h34m", StatFormatter.formatValue(StatDef.PLAYTIME, 905_280L, DistanceUnit.METRIC));
        assertEquals("1234h56m", StatFormatter.formatValue(StatDef.PLAYTIME, 88_916_160L, DistanceUnit.METRIC));
    }

    @Test
    void formatsGenericCounters() {
        assertEquals("0", StatFormatter.formatValue(StatDef.DEATHS, -5L, DistanceUnit.METRIC));
        assertEquals("2,345,678", StatFormatter.formatValue(StatDef.JUMPS, 2_345_678L, DistanceUnit.METRIC));
    }

    @Test
    void formatsRowsFromSnapshot() {
        StatSnapshot snapshot = new StatSnapshot(Map.of(
            StatDef.DEATHS, 8L,
            StatDef.DISTANCE, 42_000L
        ));
        assertEquals("Deaths: 8", StatFormatter.formatRow(StatDef.DEATHS, snapshot, DistanceUnit.METRIC));
        assertEquals("Distance: 420 m", StatFormatter.formatRow(StatDef.DISTANCE, snapshot, DistanceUnit.METRIC));
    }
}
