package com.example.survivalstats;

import java.util.List;

public class StatDef {
    public final String id;
    public final String criterion;
    public final String displayName;

    public StatDef(String id, String criterion, String displayName) {
        this.id = id;
        this.criterion = criterion;
        this.displayName = displayName;
    }

    public static final List<StatDef> ALL = List.of(
        new StatDef("Deaths",      "deathCount",                              "Deaths"),
        new StatDef("PlayerKills", "playerKillCount",                         "Player Kills"),
        new StatDef("MobKills",    "totalKillCount",                          "Mob Kills"),
        new StatDef("Jumps",       "minecraft.custom:minecraft.jump",         "Jumps"),
        new StatDef("Distance",    "dummy",                                   "Walked (km)"),
        new StatDef("PlayTime",    "dummy",                                   "Playtime (HHmmss)"),
        new StatDef("DistanceRaw", "minecraft.custom:minecraft.walk_one_cm",  "Distance Raw"),
        new StatDef("PlayTimeRaw", "minecraft.custom:minecraft.play_time",    "Playtime Raw"),
        new StatDef("DmgDealt",    "minecraft.custom:minecraft.damage_dealt", "Damage Dealt"),
        new StatDef("DmgTaken",    "minecraft.custom:minecraft.damage_taken", "Damage Taken"),
        new StatDef("Sleeps",      "minecraft.custom:minecraft.sleep_in_bed", "Times Slept"),
        new StatDef("Fish",        "minecraft.custom:minecraft.fish_caught",  "Fish Caught")
    );
}
