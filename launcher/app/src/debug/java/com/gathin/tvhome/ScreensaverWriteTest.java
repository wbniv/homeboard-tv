package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Screensavers Stage 0, question 5: the only probe check that changes a TV, and only when the caller asked for it.
 * It needs WRITE_SECURE_SETTINGS already granted (the coordinator's secure-settings-grant); without it it reports
 * that and touches nothing. Otherwise it records both originals, writes the screensaver component and the idle
 * time, reads each back, restores both and verifies they equal the originals. The restore always runs once any
 * write was attempted, and a failed restore is reported first (ScreensaverProbeReport orders it).
 *
 * Restart persistence is not checked here: it needs a restart between two jobs (open point in the recipe).
 */
final class ScreensaverWriteTest {
    static final String COMPONENTS = ScreensaverGateway.COMPONENTS;
    static final String SCREEN_OFF_TIMEOUT = "screen_off_timeout";

    /** One read: readable false means Android refused or failed it, with error saying how. */
    static final class Value {
        final boolean readable;
        final String value, error;
        private Value(boolean readable, String value, String error) {
            this.readable = readable; this.value = value; this.error = error;
        }
        static Value of(String value) { return new Value(true, value, null); }
        static Value unreadable(String error) { return new Value(false, null, error); }
    }

    /** The settings calls the test makes; the Android implementation lives in ScreensaverProbeActivity. */
    interface Settings {
        boolean mayWriteSecure();
        Value readSecure(String key);
        Value readSystem(String key);
        /** null when Android accepted the write, otherwise what it said (exception or false return). */
        String writeSecure(String key, String value);
        String writeSystem(String key, String value);
    }

    private ScreensaverWriteTest() { }

    /** Idle time to write: one minute longer than the original, or ten minutes when it is unset or not a number. */
    static String testTimeout(String original) {
        try { return String.valueOf(Long.parseLong(original.trim()) + 60_000L); }
        catch (RuntimeException notANumber) { return "600000"; }
    }

    /** First candidate that differs from the original, or null when none does. */
    static String testComponent(String original, List<String> candidates) {
        for (String c : candidates) if (c != null && !c.isEmpty() && !c.equals(original)) return c;
        return null;
    }

    static List<ScreensaverProbeReport.Result> run(Settings s, List<String> componentCandidates) {
        List<ScreensaverProbeReport.Result> out = new ArrayList<>();
        String expectedWrite = "write accepted and read back equal to the written value";
        if (!s.mayWriteSecure()) {
            out.add(ScreensaverProbeReport.Result.fail("q5", "WRITE_SECURE_SETTINGS not granted; nothing written",
                    "WRITE_SECURE_SETTINGS granted before the write test", "permission not granted"));
            return out;
        }
        Value component = s.readSecure(COMPONENTS), timeout = s.readSystem(SCREEN_OFF_TIMEOUT);
        if (!component.readable || !timeout.readable) {
            out.add(ScreensaverProbeReport.Result.fail("q5", originals(component, timeout),
                    "both originals readable before any write",
                    "original unreadable; nothing written: " + (component.readable ? timeout.error : component.error)));
            return out;
        }
        String newComponent = testComponent(component.value, componentCandidates);
        String newTimeout = testTimeout(timeout.value);
        boolean wroteComponent = false, wroteTimeout = false;
        try {
            if (newComponent == null) {
                out.add(ScreensaverProbeReport.Result.notRun("q5-component", expectedWrite,
                        "no screensaver component differing from the original"));
            } else {
                wroteComponent = true;
                out.add(writeAndReadBack("q5-component", COMPONENTS, newComponent, component.value, true, s));
            }
            wroteTimeout = true;
            out.add(writeAndReadBack("q5-idle", SCREEN_OFF_TIMEOUT, newTimeout, timeout.value, false, s));
        } catch (RuntimeException unexpected) {
            out.add(ScreensaverProbeReport.Result.fail("q5", null, expectedWrite, "write test threw: " + unexpected));
        } finally {
            if (wroteComponent) out.add(restore("q5-restore-component", COMPONENTS, component.value, true, s));
            if (wroteTimeout) out.add(restore("q5-restore-idle", SCREEN_OFF_TIMEOUT, timeout.value, false, s));
        }
        return out;
    }

    private static ScreensaverProbeReport.Result writeAndReadBack(String id, String key, String value, String original,
                                                                 boolean secure, Settings s) {
        String error = secure ? s.writeSecure(key, value) : s.writeSystem(key, value);
        Value back = secure ? s.readSecure(key) : s.readSystem(key);
        boolean equal = back.readable && Objects.equals(value, back.value);
        String why = error != null ? "write refused: " + error : !back.readable ? "read-back failed: " + back.error
                : equal ? null : "read back a different value";
        return ScreensaverProbeReport.Result.of(id, error == null && equal,
                obj("key", key, "original", original, "written", value, "readBack", back.readable ? back.value : null),
                "write accepted and read back equal to the written value", why);
    }

    private static ScreensaverProbeReport.Result restore(String id, String key, String original, boolean secure,
                                                         Settings s) {
        String error;
        try { error = secure ? s.writeSecure(key, original) : s.writeSystem(key, original); }
        catch (RuntimeException threw) { error = threw.toString(); }
        Value back;
        try { back = secure ? s.readSecure(key) : s.readSystem(key); }
        catch (RuntimeException threw) { back = Value.unreadable(threw.toString()); }
        boolean equal = back.readable && Objects.equals(original, back.value);
        String why = equal ? null : !back.readable ? "RESTORE NOT VERIFIED, read-back failed: " + back.error
                : "RESTORE FAILED: reads " + back.value + ", original " + original
                + (error != null ? " (restore write: " + error + ")" : "");
        return ScreensaverProbeReport.Result.restore(id, equal,
                obj("key", key, "original", original, "readBack", back.readable ? back.value : null),
                "reads back equal to the original", why);
    }

    private static JSONObject originals(Value component, Value timeout) {
        return obj(COMPONENTS, component.readable ? component.value : "unreadable: " + component.error,
                SCREEN_OFF_TIMEOUT, timeout.readable ? timeout.value : "unreadable: " + timeout.error);
    }

    static JSONObject obj(Object... pairs) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i < pairs.length; i += 2) o.put((String) pairs[i], pairs[i + 1] == null ? JSONObject.NULL : pairs[i + 1]);
        } catch (JSONException impossible) { throw new IllegalStateException(impossible); }
        return o;
    }
}
