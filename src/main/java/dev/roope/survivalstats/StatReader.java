package dev.roope.survivalstats;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StatReader {
    private final Map<StatDef, Stat<Identifier>> mappedStats;

    public StatReader() {
        Map<StatDef, Stat<Identifier>> map = new LinkedHashMap<>();
        for (StatDef def : StatDef.USER_STATS) {
            Identifier key = Identifier.tryParse(def.customStatKey());
            if (key == null) {
                continue;
            }
            if (!BuiltInRegistries.CUSTOM_STAT.containsKey(key)) {
                continue;
            }
            map.put(def, Stats.CUSTOM.get(key));
        }
        this.mappedStats = Map.copyOf(map);
    }

    public StatSnapshot read(ServerPlayer player) {
        Map<StatDef, Long> values = new LinkedHashMap<>();
        for (StatDef def : StatDef.USER_STATS) {
            Stat<Identifier> stat = mappedStats.get(def);
            long value = stat == null ? 0L : player.getStats().getValue(stat);
            values.put(def, value);
        }
        return new StatSnapshot(values);
    }
}
