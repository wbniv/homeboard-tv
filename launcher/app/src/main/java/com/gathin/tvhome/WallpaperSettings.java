package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

/** Host-owned background preferences; independent from a provider's private preferences. */
final class WallpaperSettings {
    static final int SUPPORTED_EVENTS = 1 | 4 | 16;
    static final float PIE_ALPHA = .95f;
    String component = "", uuid = "";
    int rotationMinutes = 15, dimPercent = 35;

    static int percent(int value) { return Math.max(0, Math.min(100, value)); }
    static int opacityStep(int value, int arm) {
        return arm == 0 ? percent(value + 5) : arm == 1 ? 100 : arm == 2 ? percent(value - 5) : 75;
    }
    static boolean usableEvents(int mask) { return (mask & SUPPORTED_EVENTS) != 0; }
    static String missingEvents(int mask) {
        String result = "";
        if ((mask & 2) != 0) result = "now playing";
        if ((mask & 8) != 0) result += (result.isEmpty() ? "" : ", ") + "program focus";
        if ((mask & ~31) != 0) result += (result.isEmpty() ? "" : ", ") + "unknown events";
        return result;
    }
    static WallpaperSettings parse(JSONObject json) {
        WallpaperSettings s = new WallpaperSettings();
        if (json == null) return s;
        s.component = json.optString("component", ""); s.uuid = json.optString("uuid", "");
        s.rotationMinutes = Math.max(1, Math.min(1440, json.optInt("rotationMinutes", 15)));
        s.dimPercent = percent(json.optInt("dimPercent", 35));
        return s;
    }
    JSONObject json() {
        try { return new JSONObject().put("component", component).put("uuid", uuid)
                .put("rotationMinutes", rotationMinutes).put("dimPercent", dimPercent); }
        catch (JSONException e) { throw new IllegalStateException(e); }
    }
    WallpaperSettings copy() { return parse(json()); }
    boolean solid() { return component.isEmpty(); }
    String identity() { return component + "|" + uuid; }
}
