package com.gathin.tvhome;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HomeTargetTest {
    private static final String SELF = "net.biohack.homeboard";
    private static final String LAUNCHERX = "com.google.android.apps.tv.launcherx";
    private static final HomeTarget.Candidate HOMEBOARD = new HomeTarget.Candidate(SELF, 0, false);
    private static final HomeTarget.Candidate GOOGLE_TV = new HomeTarget.Candidate(LAUNCHERX, 2, true);
    private static final HomeTarget.Candidate FALLBACK_HOME = new HomeTarget.Candidate("com.android.tv.settings", -1000, true);

    private static HomeTarget.Result choose(String resolved, HomeTarget.Candidate... all) {
        return HomeTarget.choose(SELF, resolved, Arrays.asList(all));
    }

    @Test public void googleTvIsTheTargetWhenItIsAndroidsChoice() {
        HomeTarget.Result r = choose(LAUNCHERX, GOOGLE_TV, HOMEBOARD, FALLBACK_HOME);
        assertEquals(LAUNCHERX, r.target);
        assertFalse(r.homeboardIsHome);
        assertFalse(r.roleHonoured);
    }

    @Test public void googleTvStaysTheTargetEvenWhenHomeboardHoldsTheRole() {
        // The role is recorded on Homeboard, and launcherx still takes the key: Homeboard is not really Home.
        HomeTarget.Result r = choose(SELF, GOOGLE_TV, HOMEBOARD);
        assertEquals(LAUNCHERX, r.target);
        assertFalse(r.homeboardIsHome);
    }

    @Test public void googleTvIsFoundByNameEvenWithoutAPriority() {
        HomeTarget.Result r = choose("android", new HomeTarget.Candidate(LAUNCHERX, 0, false), HOMEBOARD);
        assertEquals(LAUNCHERX, r.target);
    }

    @Test public void anUnknownSystemLauncherWithPriorityOutranksTheRole() {
        HomeTarget.Candidate vendor = new HomeTarget.Candidate("com.vendor.tvlauncher", 1, true);
        HomeTarget.Result r = choose(SELF, vendor, HOMEBOARD);
        assertEquals("com.vendor.tvlauncher", r.target);
        assertFalse(r.roleHonoured);
    }

    @Test public void theKnownLauncherWinsOverAnotherOutrankingOne() {
        HomeTarget.Candidate vendor = new HomeTarget.Candidate("com.vendor.tvlauncher", 5, true);
        assertEquals(LAUNCHERX, choose("android", vendor, GOOGLE_TV, HOMEBOARD).target);
    }

    @Test public void homeboardIsHomeWhenNothingOutranksIt() {
        HomeTarget.Candidate stock = new HomeTarget.Candidate("com.vendor.launcher", 0, true);
        HomeTarget.Result r = choose(SELF, stock, HOMEBOARD, FALLBACK_HOME);
        assertNull(r.target);
        assertTrue(r.homeboardIsHome);
        assertTrue(r.roleHonoured);
    }

    @Test public void aPlainStockLauncherAsAndroidsChoiceIsTheTargetAndTheRoleWouldWork() {
        HomeTarget.Candidate stock = new HomeTarget.Candidate("com.vendor.launcher", 0, true);
        HomeTarget.Result r = choose("com.vendor.launcher", stock, HOMEBOARD);
        assertEquals("com.vendor.launcher", r.target);
        assertTrue(r.roleHonoured);
    }

    @Test public void theChooserHasNoSingleTarget() {
        HomeTarget.Candidate stock = new HomeTarget.Candidate("com.vendor.launcher", 0, true);
        HomeTarget.Result r = choose("android", stock, HOMEBOARD);
        assertNull(r.target);
        assertFalse(r.homeboardIsHome);
        assertTrue(r.roleHonoured);
    }

    @Test public void settingsFallbackHomeAndTheChooserAreNeverTargets() {
        HomeTarget.Result r = choose("com.android.tv.settings", FALLBACK_HOME, new HomeTarget.Candidate("android", 0, true), HOMEBOARD);
        assertNull(r.target);
    }

    @Test public void theOtherHomeboardBuildIsAnOrdinaryLauncher() {
        HomeTarget.Candidate debug = new HomeTarget.Candidate(SELF + ".debug", 0, false);
        HomeTarget.Result r = choose(SELF, debug, HOMEBOARD);
        assertTrue(r.homeboardIsHome);
    }

    @Test public void nothingInstalledMeansNothingToDo() {
        HomeTarget.Result r = HomeTarget.choose(SELF, null, Collections.emptyList());
        assertNull(r.target);
        assertFalse(r.homeboardIsHome);
    }

    @Test public void aPackageWithSeveralHomeActivitiesCountsOnceAtItsStrongest() {
        HomeTarget.Candidate weak = new HomeTarget.Candidate("com.vendor.tvlauncher", 0, true);
        HomeTarget.Candidate strong = new HomeTarget.Candidate("com.vendor.tvlauncher", 3, true);
        assertEquals("com.vendor.tvlauncher", choose(SELF, weak, strong, HOMEBOARD).target);
    }
}
