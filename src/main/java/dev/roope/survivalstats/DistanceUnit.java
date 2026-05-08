package dev.roope.survivalstats;

import java.util.Locale;

public enum DistanceUnit {
    METRIC("metric", "Walked (m)", 100.0D),
    IMPERIAL("imperial", "Walked (ft)", 30.48D);

    public static final DistanceUnit DEFAULT = METRIC;

    private final String configValue;
    private final String displayName;
    private final double centimetersPerUnit;

    DistanceUnit(String configValue, String displayName, double centimetersPerUnit) {
        this.configValue = configValue;
        this.displayName = displayName;
        this.centimetersPerUnit = centimetersPerUnit;
    }

    public String configValue() {
        return configValue;
    }

    public String displayName() {
        return displayName;
    }

    public static DistanceUnit fromConfigValue(String s) {
        if (s == null) return DEFAULT;
        String normalized = s.trim().toLowerCase(Locale.ROOT);
        for (DistanceUnit u : values()) {
            if (u.configValue.equals(normalized)) return u;
        }
        return DEFAULT;
    }

    public int convertFromCentimeters(int rawCm) {
        if (rawCm == 0) return 0;
        long rounded = Math.round(rawCm / centimetersPerUnit);
        if (rounded > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (rounded < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) rounded;
    }
}
