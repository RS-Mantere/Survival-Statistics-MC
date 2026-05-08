package dev.roope.survivalstats;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatSnapshotTest {
    @Test
    void returnsZeroForMissingStats() {
        StatSnapshot snapshot = new StatSnapshot(Map.of(StatDef.DEATHS, 12L));
        assertEquals(12L, snapshot.get(StatDef.DEATHS));
        assertEquals(0L, snapshot.get(StatDef.JUMPS));
    }
}
