package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AirQualityHistoryTest {
    @Test public void keepsRecentUniqueStationUpdatesAcrossPersistence() throws Exception {
        AirQualityHistory history = new AirQualityHistory();
        for (int i = 1; i <= AirQualityHistory.LIMIT + 2; i++)
            history.add(new AirQualityReading(i, i * 1000L));
        assertEquals(AirQualityHistory.LIMIT, history.readings().size());
        assertEquals(3, history.readings().get(0).pm25, 0.001);
        history.add(new AirQualityReading(42, (AirQualityHistory.LIMIT + 2) * 1000L));
        assertEquals(AirQualityHistory.LIMIT, history.readings().size());
        AirQualityHistory restored = AirQualityHistory.parse(history.encode());
        assertEquals(42, restored.readings().get(restored.readings().size() - 1).pm25, 0.001);
    }

    @Test public void circularBufferKeepsChronologicalLastThirtyDays() {
        AirQualityHistory history = new AirQualityHistory();
        for (int i = 0; i < AirQualityHistory.LIMIT; i++)
            history.add(new AirQualityReading(i, i * 3_600_000L + 1));
        history.add(new AirQualityReading(999, AirQualityHistory.LIMIT * 3_600_000L + 1));
        assertEquals(720, history.readings().size());
        assertEquals(1, history.readings().get(0).pm25, 0.001);
        assertEquals(999, history.readings().get(719).pm25, 0.001);
        history.add(new AirQualityReading(0, 1)); // Too old to evict anything newer.
        assertEquals(1, history.readings().get(0).pm25, 0.001);
    }

    @Test public void backfillCanMergeOutOfOrderPointsAndReplaceDuplicateTimestamp() {
        AirQualityHistory history = new AirQualityHistory();
        history.add(new AirQualityReading(30, 30_000));
        history.add(new AirQualityReading(10, 10_000));
        history.add(new AirQualityReading(20, 20_000));
        history.add(new AirQualityReading(21, 20_000));
        assertEquals(3, history.readings().size());
        assertEquals(10, history.readings().get(0).pm25, 0.001);
        assertEquals(21, history.readings().get(1).pm25, 0.001);
        assertEquals(30, history.readings().get(2).pm25, 0.001);
    }

    @Test public void persistsModelConditionsWithoutChangingStationSamples() throws Exception {
        AirQualityHistory history = new AirQualityHistory();
        history.add(AirQualityReading.modeled(60, 1000));
        history.add(new AirQualityReading(60, 2000));
        AirQualityHistory restored = AirQualityHistory.parse(history.encode());
        assertTrue(restored.readings().get(0).modelEstimate);
        assertEquals(AirQualityReading.Condition.HIGH, restored.readings().get(0).condition());
        assertFalse(restored.readings().get(1).modelEstimate);
        assertEquals(AirQualityReading.Condition.HIGH, restored.readings().get(1).condition());
    }
}
