package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.gathin.tvhome.FakeScreensaverGateway.AERIAL;
import static com.gathin.tvhome.FakeScreensaverGateway.OWN;
import static com.gathin.tvhome.ScreensaverRules.Missing;
import static org.junit.Assert.*;

/**
 * The saved screensaver app is no longer installed (docs/plans/2026-11-26-screensaver-browsing.md, edge states):
 * not managed, the choice is kept and shown as an unavailable row while Homeboard's default plays; managed, today's
 * fall-back is unchanged.
 */
public class ScreensaverMissingAppTest {
    private static final String CLOCK = ScreensaverRegistry.CLOCK;
    private static final String WF_CLOCKS = "org.worldfoundry.game.clocks/org.worldfoundry.screensaver.ClocksDreamService";

    private static List<ScreensaverRegistry.Entry> installed() {
        return ScreensaverRegistry.entries(Arrays.asList(
                new ScreensaverGateway.InstalledDream(AERIAL, "Aerial Views", null),
                new ScreensaverGateway.InstalledDream("com.z/com.z.Zoo", "Zoo", null)), OWN);
    }

    @Test public void notManagedKeepsTheChoiceAndSaysSoOncePerDisappearance() {
        assertEquals(Missing.KEEP_AND_NOTICE, ScreensaverRules.missing(WF_CLOCKS, false, false, null));
        String noticed = ScreensaverRules.noticedAfter(Missing.KEEP_AND_NOTICE, WF_CLOCKS);
        assertEquals(WF_CLOCKS, noticed);
        assertEquals(Missing.KEEP, ScreensaverRules.missing(WF_CLOCKS, false, false, noticed));   // the next resume: silent
        assertEquals(WF_CLOCKS, ScreensaverRules.noticedAfter(Missing.KEEP, WF_CLOCKS));
        // Reinstalled: nothing to do, and the marker clears so a later loss is announced again.
        assertEquals(Missing.NONE, ScreensaverRules.missing(WF_CLOCKS, true, false, noticed));
        assertNull(ScreensaverRules.noticedAfter(Missing.NONE, WF_CLOCKS));
        assertEquals(Missing.KEEP_AND_NOTICE, ScreensaverRules.missing(WF_CLOCKS, false, false, null));
        // A different app gone is a new disappearance.
        assertEquals(Missing.KEEP_AND_NOTICE, ScreensaverRules.missing(AERIAL, false, false, WF_CLOCKS));
    }

    @Test public void managedFallsBackAsBefore() {
        assertEquals(Missing.FALL_BACK, ScreensaverRules.missing(WF_CLOCKS, false, true, null));
        assertEquals(Missing.FALL_BACK, ScreensaverRules.missing(WF_CLOCKS, false, true, WF_CLOCKS));
        assertNull(ScreensaverRules.noticedAfter(Missing.FALL_BACK, WF_CLOCKS));
    }

    @Test public void builtInsAndInstalledAppsNeedNothing() {
        assertEquals(Missing.NONE, ScreensaverRules.missing(CLOCK, false, false, null));
        assertEquals(Missing.NONE, ScreensaverRules.missing(CLOCK, true, true, null));
        assertEquals(Missing.NONE, ScreensaverRules.missing(AERIAL, true, false, null));
    }

    @Test public void theStartRulePlaysTheDefaultWhileTheAppIsGone() {
        assertEquals(CLOCK, ScreensaverRules.playable(WF_CLOCKS, false));
        assertEquals(WF_CLOCKS, ScreensaverRules.playable(WF_CLOCKS, true));
        assertEquals(ScreensaverRegistry.HOME_BACKGROUND, ScreensaverRules.playable(ScreensaverRegistry.HOME_BACKGROUND, false));
        // Show now through the rule, never Android's start-now for a component that is not there.
        assertEquals(ScreensaverRules.Player.HOMEBOARD, ScreensaverRules.remotePlayer(ScreensaverRules.playable(WF_CLOCKS, false), false));
    }

    @Test public void theSavedMissingAppIsAnUnavailableRowUnderItsSavedName() {
        List<ScreensaverRegistry.Entry> entries = ScreensaverRegistry.withSaved(installed(), WF_CLOCKS, "WF Clocks");
        assertEquals(installed().size() + 1, entries.size());
        ScreensaverRegistry.Entry row = ScreensaverRegistry.resolve(WF_CLOCKS, entries);
        assertEquals(WF_CLOCKS, row.id);   // focus can open on it
        assertTrue(row.missing());
        assertTrue(row.app());
        assertFalse(row.playsOnHome());
        assertEquals("WF Clocks", row.name);
        assertEquals(R.string.hb_ss_not_installed, row.summary);
        assertNull(row.scene);
        assertNull(row.settings);
        // Placed among the apps by name: Aerial Views, WF Clocks, Zoo.
        assertEquals(Arrays.asList(CLOCK, ScreensaverRegistry.HOME_BACKGROUND, AERIAL, WF_CLOCKS, "com.z/com.z.Zoo"),
                Arrays.asList(entries.get(0).id, entries.get(1).id, entries.get(2).id, entries.get(3).id, entries.get(4).id));
        // Not counted as installed, so the managed fall-back and the label lookup still see it as gone.
        assertFalse(ScreensaverRegistry.installed(entries).contains(WF_CLOCKS));
        assertNull(ScreensaverRegistry.appLabel(WF_CLOCKS, entries));
        // Home idle plays Homeboard's default for it.
        assertEquals(ScreensaverRules.Player.HOMEBOARD, ScreensaverRules.homePlayer(row));
    }

    @Test public void withoutASavedNameThePackageIsShown() {
        ScreensaverRegistry.Entry row = ScreensaverRegistry.resolve(WF_CLOCKS, ScreensaverRegistry.withSaved(installed(), WF_CLOCKS, ""));
        assertEquals("org.worldfoundry.game.clocks", row.name);
    }

    @Test public void nothingIsAddedForABuiltInOrAnInstalledChoice() {
        List<ScreensaverRegistry.Entry> base = installed();
        assertSame(base, ScreensaverRegistry.withSaved(base, CLOCK, ""));
        assertSame(base, ScreensaverRegistry.withSaved(base, AERIAL, "Aerial Views"));
    }

    @Test public void theUnavailableRowHasNoActionsAndKeepsItsBadge() {
        List<ScreensaverRegistry.Entry> entries = ScreensaverRegistry.withSaved(installed(), WF_CLOCKS, "WF Clocks");
        ScreensaverRegistry.Entry row = ScreensaverRegistry.resolve(WF_CLOCKS, entries);
        assertEquals(Collections.emptyList(), ScreensaverPicker.actions(row, false, true));
        assertEquals(Collections.emptyList(), ScreensaverPicker.actions(row, false, false));
        Map<String, ScreensaverPicker.Badge> same = ScreensaverPicker.badges(WF_CLOCKS, null);
        assertEquals(ScreensaverPicker.Badge.IN_USE, same.get(WF_CLOCKS));
        Map<String, ScreensaverPicker.Badge> split = ScreensaverPicker.badges(WF_CLOCKS, AERIAL);
        assertEquals(ScreensaverPicker.Badge.ON_HOME, split.get(WF_CLOCKS));
    }
}
