package dev.roope.survivalstats.display;

import dev.roope.survivalstats.DistanceUnit;
import dev.roope.survivalstats.StatDef;
import dev.roope.survivalstats.StatSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public final class StatFormatter {
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final long TICKS_PER_SECOND = 20L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;

    private StatFormatter() {
    }

    public static String formatValue(StatDef def, long raw, DistanceUnit unit) {
        if (def == StatDef.DISTANCE) {
            String suffix = unit == DistanceUnit.METRIC ? " m" : " ft";
            long converted = Math.round(raw / (unit == DistanceUnit.METRIC ? 100.0D : 30.48D));
            return NUMBER_FORMAT.format(Math.max(0L, converted)) + suffix;
        }
        if (def == StatDef.PLAYTIME) {
            long totalMinutes = Math.max(0L, raw) / (TICKS_PER_SECOND * SECONDS_PER_MINUTE);
            long hours = totalMinutes / MINUTES_PER_HOUR;
            long minutes = totalMinutes % MINUTES_PER_HOUR;
            return hours + "h" + minutes + "m";
        }
        return NUMBER_FORMAT.format(Math.max(0L, raw));
    }

    public static String formatRow(StatDef def, StatSnapshot snapshot, DistanceUnit unit) {
        return def.displayName() + ": " + formatValue(def, snapshot.get(def), unit);
    }
}
