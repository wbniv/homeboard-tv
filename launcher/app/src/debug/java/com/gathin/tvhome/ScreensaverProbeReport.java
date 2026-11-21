package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one place that defines the screensaver probe's result shape (Screensavers Stage 0,
 * docs/plans/2026-11-20-screensavers-stage0-probe.md). The coordinator side is not settled, so every field name
 * lives here and nowhere else. Pure Java plus org.json, so it is unit tested on the JVM.
 *
 * <pre>
 * {"schema":1, "probe":"screensaver_probe", "status":"done|running|restore-failed",
 *  "timestamp":"2026-11-21T10:00:00Z", "device":{...}, "build":{...},
 *  "results":[{"id":"q4","pass":true,"observed":...,"expected":...,"error":"..."}]}
 * </pre>
 *
 * A failed restore (Q5) is always the first result and sets status to restore-failed, so a reader sees it before
 * any answer, as the recipe's stop criteria require.
 */
final class ScreensaverProbeReport {
    static final int SCHEMA = 1;
    static final String PROBE = "screensaver_probe";
    static final String RUNNING = "running", DONE = "done", RESTORE_FAILED = "restore-failed";

    /** One question's assertion. observed and expected are any JSON value (String, Number, JSONObject...). */
    static final class Result {
        final String id;
        final boolean pass;
        final Object observed, expected;
        final String error;
        /** True for a Q5 restore check: its failure is moved to the front of the report. */
        final boolean restore;

        private Result(String id, boolean pass, Object observed, Object expected, String error, boolean restore) {
            this.id = id; this.pass = pass; this.observed = observed; this.expected = expected; this.error = error;
            this.restore = restore;
        }
        static Result pass(String id, Object observed, Object expected) {
            return new Result(id, true, observed, expected, null, false);
        }
        static Result fail(String id, Object observed, Object expected, String error) {
            return new Result(id, false, observed, expected, error, false);
        }
        static Result of(String id, boolean pass, Object observed, Object expected, String error) {
            return new Result(id, pass, observed, expected, error, false);
        }
        /** A check the probe did not run; pass is false and the reason starts with "not run". */
        static Result notRun(String id, Object expected, String reason) {
            return new Result(id, false, null, expected, "not run: " + reason, false);
        }
        static Result restore(String id, boolean pass, Object observed, Object expected, String error) {
            return new Result(id, pass, observed, expected, error, true);
        }
    }

    private final Map<String, Object> device = new LinkedHashMap<>(), build = new LinkedHashMap<>();
    private final List<Result> results = new ArrayList<>();

    ScreensaverProbeReport device(String key, Object value) { device.put(key, value); return this; }
    ScreensaverProbeReport build(String key, Object value) { build.put(key, value); return this; }
    ScreensaverProbeReport add(Result result) { results.add(result); return this; }
    ScreensaverProbeReport addAll(List<Result> more) { results.addAll(more); return this; }
    List<Result> results() { return new ArrayList<>(results); }
    Result get(String id) {
        for (Result r : results) if (r.id.equals(id)) return r;
        return null;
    }
    /** Replace the result with this id in place, or add it when there is none. */
    ScreensaverProbeReport put(Result result) {
        for (int i = 0; i < results.size(); i++) if (results.get(i).id.equals(result.id)) { results.set(i, result); return this; }
        results.add(result);
        return this;
    }

    boolean restoreFailed() {
        for (Result r : results) if (r.restore && !r.pass) return true;
        return false;
    }

    /** Failed restores first, then every other result in the order it was added. */
    List<Result> ordered() {
        List<Result> first = new ArrayList<>(), rest = new ArrayList<>();
        for (Result r : results) (r.restore && !r.pass ? first : rest).add(r);
        first.addAll(rest);
        return first;
    }

    JSONObject toJson(String timestamp, boolean finished) throws JSONException {
        JSONArray list = new JSONArray();
        for (Result r : ordered()) {
            JSONObject item = new JSONObject().put("id", r.id).put("pass", r.pass)
                    .put("observed", r.observed == null ? JSONObject.NULL : r.observed)
                    .put("expected", r.expected == null ? JSONObject.NULL : r.expected);
            if (r.error != null) item.put("error", r.error);
            list.put(item);
        }
        String status = restoreFailed() ? RESTORE_FAILED : finished ? DONE : RUNNING;
        return new JSONObject().put("schema", SCHEMA).put("probe", PROBE).put("status", status)
                .put("timestamp", timestamp).put("device", new JSONObject(device)).put("build", new JSONObject(build))
                .put("results", list);
    }
}
