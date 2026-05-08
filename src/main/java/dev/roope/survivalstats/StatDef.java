package dev.roope.survivalstats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record StatDef(String id, String customStatKey, String displayName) {
    public static final StatDef DEATHS = new StatDef("Deaths", "minecraft:deaths", "Deaths");
    public static final StatDef PLAYER_KILLS = new StatDef("PlayerKills", "minecraft:player_kills", "Player Kills");
    public static final StatDef MOB_KILLS = new StatDef("MobKills", "minecraft:mob_kills", "Mob Kills");
    public static final StatDef JUMPS = new StatDef("Jumps", "minecraft:jump", "Jumps");
    public static final StatDef DISTANCE = new StatDef("Distance", "minecraft:walk_one_cm", "Distance");
    public static final StatDef PLAYTIME = new StatDef("PlayTime", "minecraft:play_time", "Play Time");
    public static final StatDef DAMAGE_DEALT = new StatDef("DmgDealt", "minecraft:damage_dealt", "Damage Dealt");
    public static final StatDef DAMAGE_TAKEN = new StatDef("DmgTaken", "minecraft:damage_taken", "Damage Taken");
    public static final StatDef SLEEPS = new StatDef("Sleeps", "minecraft:sleep_in_bed", "Times Slept");
    public static final StatDef FISH = new StatDef("Fish", "minecraft:fish_caught", "Fish Caught");

    public static final List<StatDef> USER_STATS = List.of(
        DEATHS, PLAYER_KILLS, MOB_KILLS, JUMPS, DISTANCE, PLAYTIME, DAMAGE_DEALT, DAMAGE_TAKEN, SLEEPS, FISH
    );

    public static final Map<String, StatDef> BY_ID;

    static {
        Map<String, StatDef> byId = new LinkedHashMap<>();
        for (StatDef def : USER_STATS) {
            byId.put(def.id, def);
        }
        BY_ID = Map.copyOf(byId);
    }
}
