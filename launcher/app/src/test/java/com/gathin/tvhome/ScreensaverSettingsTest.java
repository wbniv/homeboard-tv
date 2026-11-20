package com.gathin.tvhome;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScreensaverSettingsTest {
    @Test public void defaultsAreClockOnGlowWithNoHomeTimer() {
        for (ScreensaverSettings s : new ScreensaverSettings[]{new ScreensaverSettings(), ScreensaverSettings.parse(null),
                ScreensaverSettings.parse(new JSONObject()), HomeLayout.firstRun().screensaver}) {
            assertEquals(ScreensaverRegistry.CLOCK, s.choice);
            assertEquals(ScreensaverSettings.BACKDROP_GLOW, s.clockBackdrop);
            assertEquals(ScreensaverSettings.ANDROID_DECIDES, s.idleMinutes);
            assertFalse(s.homeTimer());
            assertTrue(s.backgroundShowsClock);
            assertEquals(35, s.clockDimPercent);
            assertEquals(20, s.backgroundDimPercent);
        }
    }

    @Test public void roundTripsThroughItsJsonAndTheLayout() throws Exception {
        ScreensaverSettings s = new ScreensaverSettings();
        s.choice = ScreensaverRegistry.HOME_BACKGROUND; s.clockBackdrop = ScreensaverSettings.BACKDROP_BLACK;
        s.clockDimPercent = 50; s.backgroundShowsClock = false; s.backgroundDimPercent = 10; s.idleMinutes = 5;
        ScreensaverSettings copy = s.copy();
        assertEquals(s.json().toString(), copy.json().toString());
        assertTrue(copy.homeTimer());

        HomeLayout layout = HomeLayout.firstRun();
        layout.screensaver = s;
        HomeLayout restored = HomeLayout.parse(layout.encode());
        assertEquals(s.json().toString(), restored.screensaver.json().toString());
        // Separate from Home's background: the wallpaper's dim is untouched.
        assertEquals(layout.wallpaper.dimPercent, restored.wallpaper.dimPercent);
    }

    @Test public void layoutsSavedBeforeScreensaversReadAsDefaults() throws Exception {
        JSONObject saved = new JSONObject(HomeLayout.firstRun().encode());
        saved.remove("screensaver");
        HomeLayout restored = HomeLayout.parse(saved.toString());
        assertEquals(ScreensaverRegistry.CLOCK, restored.screensaver.choice);
        assertEquals(ScreensaverSettings.ANDROID_DECIDES, restored.screensaver.idleMinutes);
    }

    @Test public void unknownChoiceAndBackdropFallBackToClockAndGlow() throws Exception {
        ScreensaverSettings s = ScreensaverSettings.parse(new JSONObject()
                .put("choice", "plasma").put("clockBackdrop", "plasma"));
        assertEquals(ScreensaverRegistry.CLOCK, s.choice);
        assertEquals(ScreensaverSettings.BACKDROP_GLOW, s.clockBackdrop);
        assertEquals(ScreensaverRegistry.CLOCK, ScreensaverSettings.parse(new JSONObject().put("choice", "")).choice);
    }

    @Test public void anAndroidAppChoiceIsKeptAndCheckedForInstallationOnRead() throws Exception {
        ScreensaverSettings s = ScreensaverSettings.parse(new JSONObject()
                .put("choice", "com.example/com.example.UninstalledDream").put("choiceName", "Example"));
        assertEquals("com.example/com.example.UninstalledDream", s.choice);
        assertEquals("Example", s.choiceName);
        assertEquals(s.json().toString(), s.copy().json().toString());
        // Not installed: the list resolves it to the default slot, and Homeboard itself can only play built-ins.
        assertEquals(ScreensaverRegistry.CLOCK, ScreensaverRegistry.resolve(s.choice, ScreensaverRegistry.BUILT_IN).id);
        assertEquals(ScreensaverRegistry.CLOCK, ScreensaverRegistry.known(s.choice));
        // A label is kept only beside an app choice.
        assertEquals("", ScreensaverSettings.parse(new JSONObject().put("choice", "clock").put("choiceName", "x")).choiceName);
    }

    @Test public void idleMinutesSnapToTheAllowedSet() throws Exception {
        for (int allowed : ScreensaverSettings.IDLE_MINUTES) assertEquals(allowed, ScreensaverSettings.idle(allowed));
        assertEquals(ScreensaverSettings.ANDROID_DECIDES, ScreensaverSettings.idle(-3));
        assertEquals(ScreensaverSettings.ANDROID_DECIDES, ScreensaverSettings.idle(Integer.MIN_VALUE));
        assertEquals(2, ScreensaverSettings.idle(3));      // nearest allowed value
        assertEquals(5, ScreensaverSettings.idle(4));
        assertEquals(10, ScreensaverSettings.idle(12));
        assertEquals(15, ScreensaverSettings.idle(13));
        assertEquals(30, ScreensaverSettings.idle(1440));
        assertEquals(30, ScreensaverSettings.idle(Integer.MAX_VALUE));
        assertEquals(30, ScreensaverSettings.parse(new JSONObject().put("idleMinutes", 600)).idleMinutes);
        assertEquals(0, ScreensaverSettings.parse(new JSONObject().put("idleMinutes", -1)).idleMinutes);
    }

    @Test public void dimIsClampedToZeroToNinety() throws Exception {
        ScreensaverSettings s = ScreensaverSettings.parse(new JSONObject().put("clockDimPercent", 140).put("backgroundDimPercent", -20));
        assertEquals(90, s.clockDimPercent);
        assertEquals(0, s.backgroundDimPercent);
        assertEquals(90, ScreensaverSettings.dim(95));
    }
}
