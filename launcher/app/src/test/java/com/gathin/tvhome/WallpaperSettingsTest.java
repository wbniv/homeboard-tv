package com.gathin.tvhome;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class WallpaperSettingsTest {
    @Test public void firstRunOpacityAndBackgroundAreIndependent() throws Exception {
        HomeLayout layout = HomeLayout.firstRun();
        assertEquals(65, layout.uiOpacityPercent); assertTrue(layout.wallpaper.solid());
        assertEquals(35, layout.wallpaper.dimPercent); assertEquals(15, layout.wallpaper.rotationMinutes);
        layout.uiOpacityPercent = 0; layout.wallpaper.dimPercent = 100;
        HomeLayout restored = HomeLayout.parse(layout.encode());
        assertEquals(0, restored.uiOpacityPercent); assertEquals(100, restored.wallpaper.dimPercent);
        assertEquals(95, WallpaperSettings.PIE_BACKGROUND_OPACITY);
    }
    @Test public void newDefaultDoesNotOverwriteExplicitSavedOpacity() throws Exception {
        JSONObject saved=new JSONObject(HomeLayout.firstRun().encode());
        saved.put("uiOpacityPercent",75);assertEquals(75,HomeLayout.parse(saved.toString()).uiOpacityPercent);
        saved.remove("uiOpacityPercent");assertEquals(65,HomeLayout.parse(saved.toString()).uiOpacityPercent);
    }
    @Test public void savedSelectionAndOpacityRoundTripWithoutAlteringWidgets() throws Exception {
        HomeLayout layout = HomeLayout.firstRun();
        layout.wallpaper.component = "example/.Provider"; layout.wallpaper.uuid = "25423441-01d0-4cd3-8c84-4ec946fe08c7";
        layout.wallpaper.rotationMinutes = 60; layout.uiOpacityPercent = 40;
        HomeLayout restored = HomeLayout.parse(layout.encode());
        assertEquals(layout.wallpaper.identity(), restored.wallpaper.identity());
        assertEquals(60, restored.wallpaper.rotationMinutes); assertEquals(40, restored.uiOpacityPercent);
        assertEquals(layout.widgets, restored.widgets); assertEquals(layout.palette, restored.palette);
    }
    @Test public void malformedPercentAndTimersAreBounded() throws Exception {
        JSONObject saved = new JSONObject(HomeLayout.firstRun().encode());
        saved.put("uiOpacityPercent", -5);
        saved.put("wallpaper", new JSONObject().put("rotationMinutes", Integer.MAX_VALUE).put("dimPercent", -1));
        HomeLayout restored = HomeLayout.parse(saved.toString());
        assertEquals(0, restored.uiOpacityPercent); assertEquals(1440, restored.wallpaper.rotationMinutes);
        assertEquals(0, restored.wallpaper.dimPercent);
        saved.put("uiOpacityPercent", 110); assertEquals(100, HomeLayout.parse(saved.toString()).uiOpacityPercent);
    }
    @Test public void opacityDraftCannotMutateSavedSelectionAndResetIsSixtyFive() {
        WallpaperSettings saved = new WallpaperSettings(); saved.component = "example/.Provider";
        WallpaperSettings draft = saved.copy(); draft.component = "other/.Provider"; draft.dimPercent = 90;
        assertEquals("example/.Provider", saved.component); assertEquals(35, saved.dimPercent);
        assertEquals(100, WallpaperSettings.opacityStep(100, 0));
        assertEquals(0, WallpaperSettings.opacityStep(0, 2));
        assertEquals(65, WallpaperSettings.opacityStep(0, 3));
        assertEquals(100, WallpaperSettings.opacityStep(75, 1));
    }
    @Test public void savingOpacityPreservesHomeFavoriteStyle() throws Exception {
        JSONObject current = new JSONObject(HomeLayout.firstRun().encode()).put("homeAppSize", "banner");
        HomeLayout branch = HomeLayout.parse(current.toString()); branch.uiOpacityPercent = 70;
        JSONObject saved = new JSONObject(branch.encode());
        assertEquals("banner", saved.getString("homeAppSize")); assertEquals(70, saved.getInt("uiOpacityPercent"));
    }
    @Test public void eventReadinessDoesNotInventSources() {
        assertTrue(WallpaperSettings.usableEvents(1 | 2));
        assertFalse(WallpaperSettings.usableEvents(2 | 8));
        assertEquals("now playing, program focus", WallpaperSettings.missingEvents(2 | 8));
        assertEquals("", WallpaperSettings.missingEvents(1 | 4 | 16));
    }
}
