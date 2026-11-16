package com.gathin.tvhome;

import org.json.JSONObject;
import org.json.JSONException;

/** Independent, undoable settings; caches are deliberately outside Home layout state. */
final class RainSettings {
    double latitude = Double.NaN, longitude = Double.NaN;
    String name = "", label = "";
    int hours = 24, days = 6;
    boolean inches, animation = true;
    static boolean coordinates(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon) && Math.abs(lat) <= 90 && Math.abs(lon) <= 180;
    }
    boolean located() { return coordinates(latitude, longitude); }
    String locationKey() { return latitude + "," + longitude; }
    String displayName() { return label.isEmpty() ? name : label; }
    RainSettings copy() { return parse(json()); }
    static RainSettings parse(JSONObject j) {
        RainSettings s = new RainSettings();
        if (j == null) return s;
        double lat = j.optDouble("latitude", Double.NaN), lon = j.optDouble("longitude", Double.NaN);
        if (coordinates(lat, lon)) { s.latitude = lat; s.longitude = lon; }
        s.name = j.optString("name", ""); s.label = j.optString("label", "");
        int hours = j.optInt("hours", 24); s.hours = hours == 6 || hours == 12 || hours == 48 ? hours : 24;
        s.days = Math.max(0, Math.min(6, j.optInt("days", 6)));
        s.inches = j.optBoolean("inches", false); s.animation = j.optBoolean("animation", true);
        return s;
    }
    JSONObject json() {
        try {
            JSONObject j = new JSONObject().put("name", name).put("label", label).put("hours", hours)
                    .put("days", days).put("inches", inches).put("animation", animation);
            if (located()) j.put("latitude", latitude).put("longitude", longitude);
            return j;
        } catch (JSONException e) { throw new IllegalStateException(e); }
    }
}
