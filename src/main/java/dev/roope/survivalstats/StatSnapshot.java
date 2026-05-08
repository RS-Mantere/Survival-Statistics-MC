package dev.roope.survivalstats;

import java.util.Map;

public record StatSnapshot(Map<StatDef, Long> values) {
    public StatSnapshot {
        values = Map.copyOf(values);
    }

    public long get(StatDef def) {
        return values.getOrDefault(def, 0L);
    }
}
