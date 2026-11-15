package com.gathin.tvhome;

import org.json.JSONException;
import org.junit.Test;
import java.time.Instant;
import static org.junit.Assert.*;

public class AirQualityReadingTest {
    private static final long NOW = Instant.parse("2026-11-15T11:00:00Z").toEpochMilli();
    private String feed(String value, String time) {
        return "{\"status\":\"Success\",\"message\":[{\"MeasIndex\":\"114\",\"PM2.5\":"
                + value + ",\"DateTime\":\"" + time + "\"}]}";
    }
    @Test public void parsesStationAndBangkokTimestamp() throws Exception {
        AirQualityReading r = AirQualityReading.parse(feed("22.0", "2026-11-15 17:00:00"), "114", NOW);
        assertEquals(22.0, r.pm25, 0.001);
        assertEquals(Instant.parse("2026-11-15T10:00:00Z").toEpochMilli(), r.timeMillis);
        assertFalse(r.stale(NOW));
        assertTrue(r.stale(NOW + 3 * 60 * 60_000L));
    }
    @Test public void zeroIsValidButMissingAndSentinelsAreNot() throws Exception {
        assertEquals(0, AirQualityReading.parse(feed("0", "2026-11-15 17:00:00"), "114", NOW).pm25, 0);
        for (String v : new String[]{"null", "-1", "\"-\"", "\"NaN\"", "\"Infinity\""}) {
            assertThrows(JSONException.class, () -> AirQualityReading.parse(feed(v, "2026-11-15 17:00:00"), "114", NOW));
        }
    }
    @Test public void rejectsWrongStationAndFutureOrMalformedTimes() {
        assertThrows(JSONException.class, () -> AirQualityReading.parse(feed("22", "2026-11-15 17:00:00"), "119", NOW));
        for (String t : new String[]{"2026-11-16 17:00:00", "missing", "2026-02-30 17:00:00"}) {
            assertThrows(JSONException.class, () -> AirQualityReading.parse(feed("22", t), "114", NOW));
        }
    }
    @Test public void rejectsErrorEnvelope() {
        assertThrows(JSONException.class, () -> AirQualityReading.parse("{\"status\":\"Error\",\"message\":[]}", "114", NOW));
    }

    @Test public void conditionBandsUseThaiPcdTwentyFourHourThresholds() {
        assertEquals(AirQualityReading.Condition.VERY_GOOD, new AirQualityReading(15.0, NOW).condition());
        assertEquals(AirQualityReading.Condition.GOOD, new AirQualityReading(15.1, NOW).condition());
        assertEquals(AirQualityReading.Condition.MODERATE, new AirQualityReading(25.1, NOW).condition());
        assertEquals(AirQualityReading.Condition.HIGH, new AirQualityReading(37.6, NOW).condition());
        assertTrue(new AirQualityReading(38, NOW).conditionLabel().contains("limit outdoor time"));
        assertEquals(AirQualityReading.Condition.VERY_HIGH, new AirQualityReading(75.1, NOW).condition());
        assertTrue(new AirQualityReading(80, NOW).conditionLabel().contains("avoid outdoor activity"));
    }

    @Test public void modelEstimateUsesHourlyBandsAndLabelsItsSource() {
        assertEquals(AirQualityReading.Condition.VERY_GOOD, AirQualityReading.modeled(5, NOW).condition());
        assertEquals(AirQualityReading.Condition.GOOD, AirQualityReading.modeled(10, NOW).condition());
        assertEquals(AirQualityReading.Condition.MODERATE, AirQualityReading.modeled(35, NOW).condition());
        assertEquals(AirQualityReading.Condition.HIGH, AirQualityReading.modeled(75, NOW).condition());
        assertEquals(AirQualityReading.Condition.VERY_HIGH, AirQualityReading.modeled(100, NOW).condition());
        assertTrue(AirQualityReading.modeled(22, NOW).conditionLabel().startsWith("MODEL ·"));
    }
}
