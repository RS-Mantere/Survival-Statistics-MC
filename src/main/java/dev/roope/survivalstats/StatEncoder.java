package dev.roope.survivalstats;

public final class StatEncoder {
    public static final int TICKS_PER_SECOND = 20;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int TICKS_PER_MINUTE = TICKS_PER_SECOND * SECONDS_PER_MINUTE;

    private StatEncoder() {
    }

    public static int distance(int rawCm, DistanceUnit unit) {
        return unit.convertFromCentimeters(rawCm);
    }

    public static int playTimeMinutes(int rawTicks) {
        if (rawTicks <= 0) return 0;
        return rawTicks / TICKS_PER_MINUTE;
    }
}
