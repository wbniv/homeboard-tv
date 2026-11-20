package com.gathin.tvhome;

import org.junit.Test;

import java.util.HashSet;
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
}
