package dev.roope.survivalstats;

import java.util.ArrayList;

public class Config {
    public static final int MIN_INTERVAL_TICKS = 1;
    public static final int DEFAULT_INTERVAL_TICKS = 20;

    private int refreshIntervalTicks = DEFAULT_INTERVAL_TICKS;
    private boolean displayEnabled = true;
    private String distanceUnit = DistanceUnit.DEFAULT.configValue();

    // Legacy fields retained only for deserialization compatibility with old configs.
    @SuppressWarnings("unused")
    private Integer rotationIntervalTicks;
    @SuppressWarnings("unused")
    private String tabListObjective;
    @SuppressWarnings("unused")
    private String belowNameObjective;
    @SuppressWarnings("unused")
    private ArrayList<String> rotation;

    public int refreshIntervalTicks() {
        return refreshIntervalTicks;
    }

    public boolean displayEnabled() {
        return displayEnabled;
    }

    public DistanceUnit distanceUnit() {
        return DistanceUnit.fromConfigValue(distanceUnit);
    }

    public void setRefreshIntervalTicks(int ticks) {
        this.refreshIntervalTicks = Math.max(MIN_INTERVAL_TICKS, ticks);
    }

    public void setDisplayEnabled(boolean enabled) {
        this.displayEnabled = enabled;
    }

    public void setDistanceUnit(DistanceUnit unit) {
        this.distanceUnit = unit.configValue();
    }

    /**
     * Coerces all fields to valid values. Always safe to call on freshly-deserialized data.
     */
    public void normalize() {
        if (refreshIntervalTicks < MIN_INTERVAL_TICKS) refreshIntervalTicks = DEFAULT_INTERVAL_TICKS;
        if (distanceUnit == null || distanceUnit.isBlank()) {
            distanceUnit = DistanceUnit.DEFAULT.configValue();
        } else {
            distanceUnit = DistanceUnit.fromConfigValue(distanceUnit).configValue();
        }

        // Migrate from the old config layout if this was an old config file.
        if (rotationIntervalTicks != null && refreshIntervalTicks == DEFAULT_INTERVAL_TICKS) {
            refreshIntervalTicks = Math.max(MIN_INTERVAL_TICKS, rotationIntervalTicks);
        }
    }
}
