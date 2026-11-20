package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A snapshot of Android's screensaver settings that Homeboard may change: the component list, and enabled and
 * activate-on-sleep when they could be read. A key that is present was read (its value null when not set); a key
 * that is absent was unreadable and is never written. Used for the kept original, for each Undo entry's previous
 * value and for the target of a change.
 */
final class ScreensaverValues {
    static final String[] KEYS = {ScreensaverGateway.COMPONENTS, ScreensaverGateway.ENABLED, ScreensaverGateway.ACTIVATE_ON_SLEEP};

    private final Map<String, String> values;

    private ScreensaverValues(Map<String, String> values) { this.values = Collections.unmodifiableMap(values); }

    /** Reads every key; null when the component itself cannot be read, since nothing can then be kept or verified. */
    static ScreensaverValues read(ScreensaverGateway gateway) {
        Map<String, String> found = new LinkedHashMap<>();
        for (String key : KEYS) {
            ScreensaverGateway.Reading reading = gateway.readSecure(key);
            if (reading.readable) found.put(key, reading.value);
            else if (ScreensaverGateway.COMPONENTS.equals(key)) return null;
        }
        return new ScreensaverValues(found);
    }

    boolean has(String key) { return values.containsKey(key); }
    String get(String key) { return values.get(key); }
    Iterable<String> keys() { return values.keySet(); }
    String component() { return values.get(ScreensaverGateway.COMPONENTS); }

    /**
     * The values that make Android start component in every app: the component replaced, and enabled and
     * activate-on-sleep switched on only where they read as explicitly off ("0"). An unset value follows the
     * TV's default and is left alone, as is anything unreadable.
     */
    ScreensaverValues target(String component) {
        Map<String, String> next = new LinkedHashMap<>(values);
        next.put(ScreensaverGateway.COMPONENTS, component);
        for (String key : new String[]{ScreensaverGateway.ENABLED, ScreensaverGateway.ACTIVATE_ON_SLEEP})
            if (values.containsKey(key) && "0".equals(values.get(key))) next.put(key, "1");
        return new ScreensaverValues(next);
    }

    JSONObject json() {
        JSONObject json = new JSONObject();
        try { for (Map.Entry<String, String> e : values.entrySet()) json.put(e.getKey(), e.getValue() == null ? JSONObject.NULL : e.getValue()); }
        catch (JSONException e) { throw new IllegalStateException(e); }
        return json;
    }

    /** null when json is null or does not hold a component. Unknown keys are ignored, so nothing else is ever written. */
    static ScreensaverValues parse(JSONObject json) {
        if (json == null || !json.has(ScreensaverGateway.COMPONENTS)) return null;
        Map<String, String> found = new LinkedHashMap<>();
        for (String key : KEYS) if (json.has(key)) found.put(key, json.isNull(key) ? null : json.optString(key));
        return new ScreensaverValues(found);
    }

    static ScreensaverValues parse(String encoded) {
        if (encoded == null) return null;
        try { return parse(new JSONObject(encoded)); } catch (JSONException invalid) { return null; }
    }

    /** For tests and the fake gateway. */
    static ScreensaverValues of(Map<String, String> values) { return new ScreensaverValues(new LinkedHashMap<>(values)); }

    @Override public boolean equals(Object other) {
        return other instanceof ScreensaverValues && values.equals(((ScreensaverValues) other).values);
    }
    @Override public int hashCode() { return Objects.hash(values); }
    @Override public String toString() { return values.toString(); }
}
