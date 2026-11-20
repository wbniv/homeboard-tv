package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ScreensaverRegistryTest {
    @Test public void everyBuiltInHasLabelDescriptionOptionsAndScene() {
        assertFalse(ScreensaverRegistry.BUILT_IN.isEmpty());
        for (ScreensaverRegistry.Entry entry : ScreensaverRegistry.BUILT_IN) {
            assertFalse(entry.id.isEmpty());
            assertNotEquals(0, entry.label);
            assertNotEquals(0, entry.summary);
            assertNotEquals(0, entry.description);
            assertFalse(entry.glyph.isEmpty());
            assertFalse(entry.id, entry.options.isEmpty());
            assertNotNull(entry.scene);
        }
    }

    @Test public void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (ScreensaverRegistry.Entry entry : ScreensaverRegistry.BUILT_IN) assertTrue(entry.id, ids.add(entry.id));
    }

    @Test public void clockAndHomeBackgroundAreTheStageOneEntriesWithClockInTheDefaultSlot() {
        assertEquals(2, ScreensaverRegistry.BUILT_IN.size());
        assertEquals(ScreensaverRegistry.CLOCK, ScreensaverRegistry.fallback().id);
        assertEquals(ScreensaverRegistry.HOME_BACKGROUND, ScreensaverRegistry.BUILT_IN.get(1).id);
        assertEquals(new ScreensaverSettings().choice, ScreensaverRegistry.fallback().id);
    }

    @Test public void optionsMatchThePlan() {
        ScreensaverRegistry.Entry clock = ScreensaverRegistry.find(ScreensaverRegistry.CLOCK);
        assertTrue(clock.options.contains(ScreensaverRegistry.Option.BACKDROP));
        assertTrue(clock.options.contains(ScreensaverRegistry.Option.BACKDROP_DIM));
        assertFalse(clock.options.contains(ScreensaverRegistry.Option.SHOW_CLOCK));
        ScreensaverRegistry.Entry background = ScreensaverRegistry.find(ScreensaverRegistry.HOME_BACKGROUND);
        assertTrue(background.options.contains(ScreensaverRegistry.Option.SHOW_CLOCK));
        assertTrue(background.options.contains(ScreensaverRegistry.Option.DIM));
        assertFalse(background.options.contains(ScreensaverRegistry.Option.BACKDROP));
    }

    @Test public void unknownIdsResolveToTheDefaultSlot() {
        assertNull(ScreensaverRegistry.find("missing"));
        assertEquals(ScreensaverRegistry.CLOCK, ScreensaverRegistry.known("missing"));
        assertEquals(ScreensaverRegistry.CLOCK, ScreensaverRegistry.entry(null).id);
        assertEquals(0, ScreensaverRegistry.indexOf("missing"));
        assertEquals(1, ScreensaverRegistry.indexOf(ScreensaverRegistry.HOME_BACKGROUND));
        assertEquals(ScreensaverRegistry.HOME_BACKGROUND, ScreensaverRegistry.known(ScreensaverRegistry.HOME_BACKGROUND));
    }

    private static ScreensaverGateway.InstalledDream dream(String component, String label, String settings) {
        return new ScreensaverGateway.InstalledDream(component, label, settings);
    }

    @Test public void installedScreensaverAppsFollowTheBuiltInsByLabelWithoutHomeboardsOwn() {
        List<ScreensaverRegistry.Entry> entries = ScreensaverRegistry.entries(Arrays.asList(
                dream("com.z/com.z.Zoo", "zoo", null),
                dream(FakeScreensaverGateway.OWN, "Homeboard", null),
                dream(FakeScreensaverGateway.AERIAL, "Aerial Views", "com.neilturner.aerialviews/com.neilturner.aerialviews.Settings"),
                dream(FakeScreensaverGateway.AERIAL, "Aerial Views", null),   // listed twice by a vendor
                dream("not-a-component", "Broken", null),
                null), FakeScreensaverGateway.OWN);
        assertEquals(Arrays.asList(ScreensaverRegistry.CLOCK, ScreensaverRegistry.HOME_BACKGROUND, FakeScreensaverGateway.AERIAL, "com.z/com.z.Zoo"),
                Arrays.asList(entries.get(0).id, entries.get(1).id, entries.get(2).id, entries.get(3).id));
        assertEquals(4, entries.size());
        ScreensaverRegistry.Entry aerial = entries.get(2);
        assertTrue(aerial.app());
        assertEquals(ScreensaverRegistry.Kind.ANDROID_APP, aerial.kind);
        assertEquals("Aerial Views", aerial.name);
        assertNull(aerial.scene);   // no Preview, no Home idle: Android runs it
        assertTrue(aerial.options.isEmpty());
        assertEquals("com.neilturner.aerialviews/com.neilturner.aerialviews.Settings", aerial.settings);
        assertEquals(R.string.hb_ss_app_summary, aerial.summary);
        assertNull(entries.get(3).settings);
        assertFalse(entries.get(0).app());
    }

    @Test public void anUninstalledAppResolvesToTheDefaultSlotOnRead() {
        List<ScreensaverRegistry.Entry> entries = ScreensaverRegistry.entries(Arrays.asList(dream(FakeScreensaverGateway.AERIAL, "Aerial Views", null)), FakeScreensaverGateway.OWN);
        assertEquals(FakeScreensaverGateway.AERIAL, ScreensaverRegistry.resolve(FakeScreensaverGateway.AERIAL, entries).id);
        assertEquals(2, ScreensaverRegistry.indexOf(FakeScreensaverGateway.AERIAL, entries));
        List<ScreensaverRegistry.Entry> afterUninstall = ScreensaverRegistry.entries(Arrays.<ScreensaverGateway.InstalledDream>asList(), FakeScreensaverGateway.OWN);
        assertEquals(ScreensaverRegistry.CLOCK, ScreensaverRegistry.resolve(FakeScreensaverGateway.AERIAL, afterUninstall).id);
        assertEquals(0, ScreensaverRegistry.indexOf(FakeScreensaverGateway.AERIAL, afterUninstall));
        assertEquals(new HashSet<>(Arrays.asList(FakeScreensaverGateway.AERIAL)), ScreensaverRegistry.installed(entries));
        assertEquals("Aerial Views", ScreensaverRegistry.appLabel(FakeScreensaverGateway.AERIAL, entries));
        assertNull(ScreensaverRegistry.appLabel(FakeScreensaverGateway.AERIAL, afterUninstall));
        // The saved choice keeps an app id; only a non-component falls to the default at parse time.
        assertEquals(FakeScreensaverGateway.AERIAL, ScreensaverRegistry.stored(FakeScreensaverGateway.AERIAL));
        assertEquals(ScreensaverRegistry.CLOCK, ScreensaverRegistry.stored("plasma"));
    }

    @Test public void aDeclaredSettingsActivityIsReadAsAndroidReadsIt() {
        assertEquals("com.a/com.a.Settings", ScreensaverRegistry.settingsComponent("com.a", "com.a.Settings"));
        assertEquals("com.a/com.a.ui.Settings", ScreensaverRegistry.settingsComponent("com.a", ".ui.Settings"));
        assertEquals("com.b/.Settings", ScreensaverRegistry.settingsComponent("com.a", "com.b/.Settings"));
        assertNull(ScreensaverRegistry.settingsComponent("com.a", null));
        assertNull(ScreensaverRegistry.settingsComponent("com.a", "  "));
        assertNull(ScreensaverRegistry.settingsComponent("com.a", "com.b/"));
    }
}
