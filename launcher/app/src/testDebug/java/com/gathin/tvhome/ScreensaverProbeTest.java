package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Screensavers Stage 0 probe: the result shape and the Q5 write test's restore rules (debug-only classes). */
public final class ScreensaverProbeTest {

    // ---- Result shape ----

    @Test public void shapeHasTopLevelFieldsAndOneEntryPerQuestion() throws Exception {
        JSONObject json = new ScreensaverProbeReport().device("model", "Chromecast").build("versionName", "0.1.200")
                .add(ScreensaverProbeReport.Result.pass("q4", "read", "readable"))
                .add(ScreensaverProbeReport.Result.fail("q2", null, "resolves", "no activity"))
                .toJson("2026-11-21T10:00:00Z", true);
        assertEquals(1, json.getInt("schema"));
        assertEquals("screensaver_probe", json.getString("probe"));
        assertEquals("done", json.getString("status"));
        assertEquals("2026-11-21T10:00:00Z", json.getString("timestamp"));
        assertEquals("Chromecast", json.getJSONObject("device").getString("model"));
        assertEquals("0.1.200", json.getJSONObject("build").getString("versionName"));
        JSONArray results = json.getJSONArray("results");
        assertEquals(2, results.length());
        JSONObject q4 = results.getJSONObject(0);
        assertEquals("q4", q4.getString("id"));
        assertTrue(q4.getBoolean("pass"));
        assertEquals("read", q4.getString("observed"));
        assertEquals("readable", q4.getString("expected"));
        assertFalse("error is omitted when there is none", q4.has("error"));
        JSONObject q2 = results.getJSONObject(1);
        assertFalse(q2.getBoolean("pass"));
        assertTrue(q2.isNull("observed"));
        assertEquals("no activity", q2.getString("error"));
    }

    @Test public void notRunIsAFailWithReason() throws Exception {
        JSONObject r = new ScreensaverProbeReport().add(ScreensaverProbeReport.Result.notRun("q8", "plays", "not implemented"))
                .toJson("t", true).getJSONArray("results").getJSONObject(0);
        assertFalse(r.getBoolean("pass"));
        assertEquals("not run: not implemented", r.getString("error"));
    }

    @Test public void putReplacesInPlace() throws Exception {
        ScreensaverProbeReport report = new ScreensaverProbeReport()
                .add(ScreensaverProbeReport.Result.fail("q7", null, "x", "not yet"))
                .add(ScreensaverProbeReport.Result.pass("q10", "y", "y"))
                .put(ScreensaverProbeReport.Result.pass("q7", "seen", "x"));
        JSONArray results = report.toJson("t", true).getJSONArray("results");
        assertEquals(2, results.length());
        assertEquals("q7", results.getJSONObject(0).getString("id"));
        assertTrue(results.getJSONObject(0).getBoolean("pass"));
    }

    @Test public void failedRestoreIsReportedFirstAndSetsStatus() throws Exception {
        JSONObject json = new ScreensaverProbeReport()
                .add(ScreensaverProbeReport.Result.pass("q4", "read", "readable"))
                .add(ScreensaverProbeReport.Result.pass("q5-idle", "w", "w"))
                .add(ScreensaverProbeReport.Result.restore("q5-restore-idle", false, "x", "original", "RESTORE FAILED"))
                .toJson("t", true);
        assertEquals("restore-failed", json.getString("status"));
        assertEquals("q5-restore-idle", json.getJSONArray("results").getJSONObject(0).getString("id"));
    }

    @Test public void passingRestoreKeepsItsPlace() throws Exception {
        JSONObject json = new ScreensaverProbeReport()
                .add(ScreensaverProbeReport.Result.pass("q4", "read", "readable"))
                .add(ScreensaverProbeReport.Result.restore("q5-restore-idle", true, "x", "original", null))
                .toJson("t", false);
        assertEquals("running", json.getString("status"));
        assertEquals("q4", json.getJSONArray("results").getJSONObject(0).getString("id"));
    }

    // ---- Q5 write test ----

    /** In-memory settings; failRestoreOf makes the write back of an original to that key silently not stick. */
    private static final class FakeSettings implements ScreensaverWriteTest.Settings {
        final Map<String, String> secure = new HashMap<>(), system = new HashMap<>();
        final List<String> writes = new ArrayList<>();
        boolean granted = true, secureUnreadable;
        String refuseKey, failRestoreOf;
        final Map<String, String> originals = new HashMap<>();

        @Override public boolean mayWriteSecure() { return granted; }
        @Override public ScreensaverWriteTest.Value readSecure(String key) {
            return secureUnreadable ? ScreensaverWriteTest.Value.unreadable("SecurityException")
                    : ScreensaverWriteTest.Value.of(secure.get(key));
        }
        @Override public ScreensaverWriteTest.Value readSystem(String key) { return ScreensaverWriteTest.Value.of(system.get(key)); }
        @Override public String writeSecure(String key, String value) { return write(secure, key, value); }
        @Override public String writeSystem(String key, String value) { return write(system, key, value); }
        private String write(Map<String, String> table, String key, String value) {
            writes.add(key + "=" + value);
            if (key.equals(refuseKey)) return "SecurityException";
            if (key.equals(failRestoreOf) && java.util.Objects.equals(value, originals.get(key))) return null;
            table.put(key, value);
            return null;
        }
        FakeSettings with(String component, String timeout) {
            secure.put(ScreensaverWriteTest.COMPONENTS, component);
            system.put(ScreensaverWriteTest.SCREEN_OFF_TIMEOUT, timeout);
            originals.put(ScreensaverWriteTest.COMPONENTS, component);
            originals.put(ScreensaverWriteTest.SCREEN_OFF_TIMEOUT, timeout);
            return this;
        }
    }

    private static final List<String> CANDIDATES = Arrays.asList("net.biohack.homeboard.debug/com.gathin.tvhome.ScreensaverDream");
    private static final String GOOGLE = "com.google.android.backdrop/.Backdrop";

    private static Map<String, ScreensaverProbeReport.Result> byId(List<ScreensaverProbeReport.Result> results) {
        Map<String, ScreensaverProbeReport.Result> m = new HashMap<>();
        for (ScreensaverProbeReport.Result r : results) m.put(r.id, r);
        return m;
    }

    @Test public void withoutPermissionNothingIsWrittenOrRead() {
        FakeSettings s = new FakeSettings().with(GOOGLE, "300000");
        s.granted = false;
        List<ScreensaverProbeReport.Result> out = ScreensaverWriteTest.run(s, CANDIDATES);
        assertEquals(1, out.size());
        assertFalse(out.get(0).pass);
        assertEquals("permission not granted", out.get(0).error);
        assertTrue(s.writes.isEmpty());
    }

    @Test public void unreadableOriginalStopsBeforeAnyWrite() {
        FakeSettings s = new FakeSettings().with(GOOGLE, "300000");
        s.secureUnreadable = true;
        List<ScreensaverProbeReport.Result> out = ScreensaverWriteTest.run(s, CANDIDATES);
        assertEquals(1, out.size());
        assertFalse(out.get(0).pass);
        assertTrue(out.get(0).error.startsWith("original unreadable"));
        assertTrue(s.writes.isEmpty());
    }

    @Test public void happyPathWritesReadsBackAndRestoresBoth() {
        FakeSettings s = new FakeSettings().with(GOOGLE, "300000");
        Map<String, ScreensaverProbeReport.Result> r = byId(ScreensaverWriteTest.run(s, CANDIDATES));
        assertTrue(r.get("q5-component").pass);
        assertTrue(r.get("q5-idle").pass);
        assertTrue(r.get("q5-restore-component").pass);
        assertTrue(r.get("q5-restore-idle").pass);
        assertEquals(GOOGLE, s.secure.get(ScreensaverWriteTest.COMPONENTS));
        assertEquals("300000", s.system.get(ScreensaverWriteTest.SCREEN_OFF_TIMEOUT));
        assertEquals(Arrays.asList(ScreensaverWriteTest.COMPONENTS + "=" + CANDIDATES.get(0),
                ScreensaverWriteTest.SCREEN_OFF_TIMEOUT + "=360000",
                ScreensaverWriteTest.COMPONENTS + "=" + GOOGLE,
                ScreensaverWriteTest.SCREEN_OFF_TIMEOUT + "=300000"), s.writes);
    }

    @Test public void unsetOriginalIsRestoredAsUnset() {
        FakeSettings s = new FakeSettings().with(null, null);
        Map<String, ScreensaverProbeReport.Result> r = byId(ScreensaverWriteTest.run(s, CANDIDATES));
        assertTrue(r.get("q5-restore-component").pass);
        assertTrue(r.get("q5-restore-idle").pass);
        assertNull(s.secure.get(ScreensaverWriteTest.COMPONENTS));
        assertNull(s.system.get(ScreensaverWriteTest.SCREEN_OFF_TIMEOUT));
    }

    @Test public void refusedWriteStillRestoresAndReportsTheRefusal() {
        FakeSettings s = new FakeSettings().with(GOOGLE, "300000");
        s.refuseKey = ScreensaverWriteTest.SCREEN_OFF_TIMEOUT;
        Map<String, ScreensaverProbeReport.Result> r = byId(ScreensaverWriteTest.run(s, CANDIDATES));
        assertTrue(r.get("q5-component").pass);
        assertFalse(r.get("q5-idle").pass);
        assertTrue(r.get("q5-idle").error.startsWith("write refused"));
        assertTrue(r.containsKey("q5-restore-idle"));
        assertTrue(r.get("q5-restore-component").pass);
    }

    @Test public void restoreFailureIsReportedFirstInTheReport() throws Exception {
        FakeSettings s = new FakeSettings().with(GOOGLE, "300000");
        s.failRestoreOf = ScreensaverWriteTest.SCREEN_OFF_TIMEOUT;
        ScreensaverProbeReport report = new ScreensaverProbeReport()
                .add(ScreensaverProbeReport.Result.pass("q4", "read", "readable"))
                .addAll(ScreensaverWriteTest.run(s, CANDIDATES));
        JSONObject json = report.toJson("t", true);
        assertEquals("restore-failed", json.getString("status"));
        JSONObject first = json.getJSONArray("results").getJSONObject(0);
        assertEquals("q5-restore-idle", first.getString("id"));
        assertFalse(first.getBoolean("pass"));
        assertTrue(first.getString("error").startsWith("RESTORE FAILED"));
        // The component restore still ran and verified.
        assertTrue(byId(report.results()).get("q5-restore-component").pass);
    }

    @Test public void noDistinctComponentSkipsThatWriteButStillTestsIdle() {
        FakeSettings s = new FakeSettings().with(CANDIDATES.get(0), "300000");
        Map<String, ScreensaverProbeReport.Result> r = byId(ScreensaverWriteTest.run(s, CANDIDATES));
        assertTrue(r.get("q5-component").error.startsWith("not run"));
        assertFalse(r.containsKey("q5-restore-component"));
        assertTrue(r.get("q5-idle").pass);
        assertTrue(r.get("q5-restore-idle").pass);
    }

    @Test public void testValues() {
        assertEquals("360000", ScreensaverWriteTest.testTimeout("300000"));
        assertEquals("600000", ScreensaverWriteTest.testTimeout(null));
        assertEquals("600000", ScreensaverWriteTest.testTimeout("never"));
        assertNull(ScreensaverWriteTest.testComponent("a", Collections.singletonList("a")));
        assertEquals("b", ScreensaverWriteTest.testComponent("a", Arrays.asList("a", "", "b")));
    }
}
