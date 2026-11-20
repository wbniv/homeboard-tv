package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The screensaver the person chose and its options, stored in HomeLayout beside the wallpaper and kept apart
 * from Home's own background settings, so dimming the screensaver never dims Home. The single source for the
 * Home idle timer, ScreensaverActivity and ClockDream; it replaced the theme values clock_glow,
 * clock_wallpaper, clock_wallpaper_dim and home_idle_clock_minutes.
 */
final class ScreensaverSettings {
    static final String BACKDROP_GLOW = "glow", BACKDROP_HOME = "background", BACKDROP_BLACK = "black";
    static final String[] BACKDROPS = {BACKDROP_GLOW, BACKDROP_HOME, BACKDROP_BLACK};
    /** "Start on Home after": 0 is "Android decides", which runs no Homeboard timer (the default). */
    static final int ANDROID_DECIDES = 0;
    static final int[] IDLE_MINUTES = {ANDROID_DECIDES, 1, 2, 5, 10, 15, 30};
    static final int DIM_STEP = 5, MAX_DIM = 90;
    static final int DEFAULT_CLOCK_DIM = 35, DEFAULT_BACKGROUND_DIM = 20;

    /** A ScreensaverRegistry id; anything unknown is read as Clock. */
    String choice = ScreensaverRegistry.CLOCK;
    String clockBackdrop = BACKDROP_GLOW;
    /** Darkening of Home's background behind the clock, used only with the Home background backdrop. */
    int clockDimPercent = DEFAULT_CLOCK_DIM;
    boolean backgroundShowsClock = true;
    int backgroundDimPercent = DEFAULT_BACKGROUND_DIM;
    int idleMinutes = ANDROID_DECIDES;

    static int dim(int value) { return Math.max(0, Math.min(MAX_DIM, value)); }

    /** The nearest allowed "Start on Home after" value (the shorter one on a tie); anything below 1 is Android decides. */
    static int idle(int minutes) {
        if (minutes <= 0) return ANDROID_DECIDES;
        int best = IDLE_MINUTES[1];
        for (int i = 1; i < IDLE_MINUTES.length; i++)
            if (Math.abs(IDLE_MINUTES[i] - minutes) < Math.abs(best - minutes)) best = IDLE_MINUTES[i];
        return best;
    }

    static String backdrop(String value) {
        for (String known : BACKDROPS) if (known.equals(value)) return known;
        return BACKDROP_GLOW;
    }

    static ScreensaverSettings parse(JSONObject json) {
        ScreensaverSettings s = new ScreensaverSettings();
        if (json == null) return s;
        s.choice = ScreensaverRegistry.known(json.optString("choice", ""));
        s.clockBackdrop = backdrop(json.optString("clockBackdrop", BACKDROP_GLOW));
        s.clockDimPercent = dim(json.optInt("clockDimPercent", DEFAULT_CLOCK_DIM));
        s.backgroundShowsClock = json.optBoolean("backgroundShowsClock", true);
        s.backgroundDimPercent = dim(json.optInt("backgroundDimPercent", DEFAULT_BACKGROUND_DIM));
        s.idleMinutes = idle(json.optInt("idleMinutes", ANDROID_DECIDES));
        return s;
    }

    JSONObject json() {
        try {
            return new JSONObject().put("choice", choice).put("clockBackdrop", clockBackdrop)
                    .put("clockDimPercent", clockDimPercent).put("backgroundShowsClock", backgroundShowsClock)
                    .put("backgroundDimPercent", backgroundDimPercent).put("idleMinutes", idleMinutes);
        } catch (JSONException e) { throw new IllegalStateException(e); }
    }

    ScreensaverSettings copy() { return parse(json()); }

    /** Homeboard runs its own Home idle timer only when a time is chosen. */
    boolean homeTimer() { return idleMinutes != ANDROID_DECIDES; }
}
