package dev.roope.survivalstats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DistanceUnitTest {

    @Test
    void fromConfigValueIsLenient() {
        assertSame(DistanceUnit.METRIC, DistanceUnit.fromConfigValue("metric"));
        assertSame(DistanceUnit.METRIC, DistanceUnit.fromConfigValue("METRIC"));
        assertSame(DistanceUnit.METRIC, DistanceUnit.fromConfigValue("  metric  "));
        assertSame(DistanceUnit.IMPERIAL, DistanceUnit.fromConfigValue("imperial"));
    }

    @Test
    void fromConfigValueFallsBackToDefault() {
        assertSame(DistanceUnit.DEFAULT, DistanceUnit.fromConfigValue(null));
        assertSame(DistanceUnit.DEFAULT, DistanceUnit.fromConfigValue(""));
        assertSame(DistanceUnit.DEFAULT, DistanceUnit.fromConfigValue("nonsense"));
    }

    @Test
    void metricConvertsCentimetersToMeters() {
        assertEquals(0, DistanceUnit.METRIC.convertFromCentimeters(0));
        assertEquals(1, DistanceUnit.METRIC.convertFromCentimeters(100));
        assertEquals(10, DistanceUnit.METRIC.convertFromCentimeters(1_000));
        assertEquals(1_000, DistanceUnit.METRIC.convertFromCentimeters(100_000)); // 1 km
        assertEquals(123_456, DistanceUnit.METRIC.convertFromCentimeters(12_345_600));
    }

    @Test
    void imperialConvertsCentimetersToFeet() {
        // 1 foot = 30.48 cm
        assertEquals(0, DistanceUnit.IMPERIAL.convertFromCentimeters(0));
        assertEquals(1, DistanceUnit.IMPERIAL.convertFromCentimeters(30));     // ~0.984 ft, rounds to 1
        assertEquals(1, DistanceUnit.IMPERIAL.convertFromCentimeters(31));     // ~1.017 ft, rounds to 1
        assertEquals(33, DistanceUnit.IMPERIAL.convertFromCentimeters(1_000)); // ~32.81 ft, rounds to 33
        // 1 mile = 5280 ft = 160_934.4 cm. The OLD code divided by 1609.344, which gave ~528000 (100x off).
        assertEquals(5_280, DistanceUnit.IMPERIAL.convertFromCentimeters(160_934));
    }

    @Test
    void displayNamesAreUnitAware() {
        assertEquals("Walked (m)", DistanceUnit.METRIC.displayName());
        assertEquals("Walked (ft)", DistanceUnit.IMPERIAL.displayName());
    }
}
