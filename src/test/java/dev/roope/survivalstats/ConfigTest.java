package dev.roope.survivalstats;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

    @Test
    void defaultsAreSensible() {
        Config c = new Config();
        c.normalize();
        assertEquals(Config.DEFAULT_INTERVAL_TICKS, c.refreshIntervalTicks());
        assertTrue(c.displayEnabled());
        assertSame(DistanceUnit.METRIC, c.distanceUnit());
    }

    @Test
    void intervalIsClampedToMinimum() {
        Config c = new Config();
        c.setRefreshIntervalTicks(0);
        assertEquals(Config.MIN_INTERVAL_TICKS, c.refreshIntervalTicks());
        c.setRefreshIntervalTicks(-50);
        assertEquals(Config.MIN_INTERVAL_TICKS, c.refreshIntervalTicks());
        c.setRefreshIntervalTicks(42);
        assertEquals(42, c.refreshIntervalTicks());
    }

    @Test
    void normalizeRecoversFromCorruption() {
        Config c = new Config();
        c.setRefreshIntervalTicks(0);
        c.setDisplayEnabled(false);
        c.setDistanceUnit(DistanceUnit.IMPERIAL);
        c.normalize();

        assertEquals(Config.MIN_INTERVAL_TICKS, c.refreshIntervalTicks());
        assertEquals(false, c.displayEnabled());
        assertSame(DistanceUnit.IMPERIAL, c.distanceUnit());
    }

    @Test
    void gsonRoundTripPreservesValuesAndNormalizesGarbage() {
        Gson gson = new Gson();
        String json = "{ "
            + "\"rotationIntervalTicks\": -10, "
            + "\"displayEnabled\": true, "
            + "\"distanceUnit\": \"FUNKY\", "
            + "\"rotation\": [\"Deaths\", null, \"\", \"Jumps\"] }";

        Config loaded = gson.fromJson(json, Config.class);
        assertNotNull(loaded);
        loaded.normalize();

        assertEquals(Config.MIN_INTERVAL_TICKS, loaded.refreshIntervalTicks());
        assertTrue(loaded.displayEnabled());
        assertSame(DistanceUnit.METRIC, loaded.distanceUnit());
    }

    @Test
    void gsonReadsValidConfig() {
        Gson gson = new Gson();
        String json = "{ "
            + "\"refreshIntervalTicks\": 200, "
            + "\"displayEnabled\": false, "
            + "\"distanceUnit\": \"imperial\", "
            + "\"rotation\": [\"Deaths\", \"PlayerKills\"] }";

        Config loaded = gson.fromJson(json, Config.class);
        loaded.normalize();
        assertEquals(200, loaded.refreshIntervalTicks());
        assertEquals(false, loaded.displayEnabled());
        assertSame(DistanceUnit.IMPERIAL, loaded.distanceUnit());
    }
}
