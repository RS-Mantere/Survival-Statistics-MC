package com.example.survivalstats;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public int rotationIntervalTicks = 100;
    public String tabListObjective = "Deaths";
    public String belowNameObjective = "MobKills";
    public List<String> rotation = new ArrayList<>(List.of(
        "Deaths",
        "PlayerKills",
        "MobKills",
        "Distance",
        "PlayTime"
    ));
}
