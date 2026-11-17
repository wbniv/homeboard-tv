package com.gathin.tvhome;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class RainLocationTest {
    private static final long NOW = 1_800_000_000_000L;

    @Test public void absentPlaceDefaultsToAutomaticButSavedCoordinatesStayFixed() throws Exception {
        assertTrue(new RainSettings().automatic);
        assertTrue(RainSettings.parse(new JSONObject()).automatic);
        RainSettings fixed = RainSettings.parse(new JSONObject().put("latitude", 13.7).put("longitude", 100.5));
        assertFalse(fixed.automatic);
        assertFalse(fixed.copy().automatic);
        assertTrue(RainSettings.parse(new JSONObject().put("locationSource", "device").put("latitude", 13.7).put("longitude", 100.5)).automatic);
    }
    @Test public void permissionRevocationMakesEvenACachedAutomaticFixUnavailable() {
        RainLocation fix = new RainLocation();
        assertTrue(fix.accept(13.7563, 100.5018, NOW, NOW));
        RainSettings automatic = new RainSettings();
        assertTrue(fix.effective(automatic, true).located());
        assertFalse(fix.effective(automatic, false).located());
        assertFalse(automatic.located()); // A runtime fix never edits the layout.
    }
    @Test public void explicitPlaceIsIndependentOfDevicePermissionAndFix() {
        RainSettings place = new RainSettings(); place.automatic = false; place.latitude = 40; place.longitude = -3;
        RainLocation fix = new RainLocation(); fix.accept(13, 100, NOW, NOW);
        assertEquals("40.0,-3.0", fix.effective(place, false).locationKey());
        assertEquals("40.0,-3.0", fix.effective(place, true).locationKey());
    }
    @Test public void rejectsExpiredFutureAndInvalidFixesWithoutExtendingTheirAge() {
        RainLocation fix = new RainLocation();
        assertFalse(fix.accept(13, 100, NOW - RainLocation.MAX_AGE, NOW));
        assertFalse(fix.accept(13, 100, NOW + 1, NOW));
        assertFalse(fix.accept(91, 100, NOW, NOW));
        assertFalse(fix.accept(Double.NaN, 100, NOW, NOW));
        assertTrue(fix.accept(13, 100, NOW - 1000, NOW));
        assertEquals(NOW - 1000, fix.fixedAt);
        assertFalse(fix.usable(NOW + RainLocation.MAX_AGE));
        assertTrue(fix.located()); // Allows a visibly marked last-known forecast.
    }
    @Test public void normalizesJitterButDistinctPlacesHaveDifferentForecastKeys() {
        RainLocation fix = new RainLocation(); RainSettings automatic = new RainSettings();
        fix.accept(13.75631, 100.50181, NOW, NOW);
        String original = fix.effective(automatic, true).locationKey();
        fix.accept(13.75632, 100.50182, NOW, NOW);
        assertEquals(original, fix.effective(automatic, true).locationKey());
        fix.accept(14, 101, NOW, NOW);
        assertNotEquals(original, fix.effective(automatic, true).locationKey());
    }
    @Test public void boundsPendingRequestsRefreshAndFailureRetries() {
        RainLocation fix = new RainLocation();
        int token = fix.begin(0, false); assertTrue(token > 0);
        assertEquals(-1, fix.begin(1, true)); // Manual actions also share the pending lookup.
        assertTrue(fix.finish(token, false));
        assertEquals(-1, fix.begin(RainLocation.RETRY - 1, false));
        token = fix.begin(RainLocation.RETRY, false); assertTrue(token > 0);
        assertTrue(fix.finish(token, true));
        assertEquals(-1, fix.begin(RainLocation.RETRY + RainLocation.REFRESH - 1, false));
        assertTrue(fix.begin(RainLocation.RETRY + RainLocation.REFRESH, false) > 0);
    }
    @Test public void cancelledOrSupersededLookupCannotComplete() {
        RainLocation fix = new RainLocation();
        int first = fix.begin(0, false); fix.cancel();
        assertFalse(fix.finish(first, true));
        int second = fix.begin(1, false);
        assertFalse(fix.finish(first, true)); assertTrue(fix.pending);
        assertTrue(fix.finish(second, true)); assertFalse(fix.pending);
    }
    @Test public void modeAndManualPlaceSurviveLayoutUndoSnapshots() throws Exception {
        HomeLayout original = HomeLayout.firstRun(); original.addPanel(HomeLayout.RAIN);
        RainSettings fixed = original.rainSettings.get(HomeLayout.RAIN);
        fixed.automatic = false; fixed.latitude = 40; fixed.longitude = -3; fixed.label = "Home";
        String before = original.encode();
        fixed.automatic = true; fixed.latitude = Double.NaN; fixed.longitude = Double.NaN;
        HomeLayout next = HomeLayout.parse(original.encode());
        assertTrue(next.rainSettings.get(HomeLayout.RAIN).automatic);
        RainSettings restored = HomeLayout.parse(before).rainSettings.get(HomeLayout.RAIN);
        assertFalse(restored.automatic); assertEquals("40.0,-3.0", restored.locationKey()); assertEquals("Home", restored.label);
    }
}
