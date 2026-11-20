package com.gathin.tvhome;

import org.junit.Test;
import static com.gathin.tvhome.HomeButtonRules.Screen.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomeButtonRulesTest {
    private static final HomeTarget.Result GOOGLE_TV = new HomeTarget.Result("com.google.android.apps.tv.launcherx", false, false);
    private static final HomeTarget.Result IS_HOME = new HomeTarget.Result(null, true, true);
    private static final HomeTarget.Result CHOOSER_ONLY = new HomeTarget.Result(null, false, true);

    private static HomeButtonRules.Screen screen(HomeTarget.Result home, boolean wanted, boolean listed, boolean running, boolean mayWrite, boolean waiting) {
        return HomeButtonRules.screen(home, wanted, listed, running, mayWrite, waiting);
    }

    @Test public void offIsOffWhateverAndroidSays() {
        assertEquals(OFF, screen(GOOGLE_TV, false, false, false, false, false));
        assertEquals(OFF, screen(GOOGLE_TV, false, true, true, true, false));
    }

    @Test public void workingOnlyWhenTheHelperIsActuallyBound() {
        assertEquals(WORKING, screen(GOOGLE_TV, true, true, true, false, false));
        assertEquals(WORKING, screen(GOOGLE_TV, true, false, true, true, true));
    }

    @Test public void justAskedAndNotConnectedIsStarting() {
        assertEquals(STARTING, screen(GOOGLE_TV, true, true, false, true, true));
    }

    @Test public void onWithoutPermissionAndNotListedNeedsSetup() {
        assertEquals(SETUP, screen(GOOGLE_TV, true, false, false, false, false));
    }

    @Test public void onButNotRunningWhenHomeboardCanActOrAndroidListsIt() {
        assertEquals(NOT_RUNNING, screen(GOOGLE_TV, true, false, false, true, false));
        assertEquals(NOT_RUNNING, screen(GOOGLE_TV, true, true, false, false, false));
        assertEquals(NOT_RUNNING, screen(GOOGLE_TV, true, true, false, true, false));
    }

    @Test public void noTargetMeansNotNeededOrChooser() {
        assertEquals(NOT_NEEDED, screen(IS_HOME, true, true, true, true, false));
        assertEquals(NOT_NEEDED, screen(IS_HOME, false, false, false, false, false));
        assertEquals(CHOOSER, screen(CHOOSER_ONLY, false, false, false, false, false));
    }

    @Test public void theMenuSummarySaysWhatHomeOpens() {
        assertEquals(HomeButtonRules.Summary.HOMEBOARD, HomeButtonRules.summary(WORKING));
        assertEquals(HomeButtonRules.Summary.HOMEBOARD, HomeButtonRules.summary(NOT_NEEDED));
        assertEquals(HomeButtonRules.Summary.OTHER_LAUNCHER, HomeButtonRules.summary(OFF));
        assertEquals(HomeButtonRules.Summary.OTHER_LAUNCHER, HomeButtonRules.summary(CHOOSER));
        assertEquals(HomeButtonRules.Summary.ATTENTION, HomeButtonRules.summary(STARTING));
        assertEquals(HomeButtonRules.Summary.ATTENTION, HomeButtonRules.summary(NOT_RUNNING));
        assertEquals(HomeButtonRules.Summary.ATTENTION, HomeButtonRules.summary(SETUP));
    }

    @Test public void theHelperRedirectsOnlyWhenWantedAndThereIsATarget() {
        assertEquals(GOOGLE_TV.target, HomeButtonRules.redirectTarget(true, GOOGLE_TV));
        assertEquals("", HomeButtonRules.redirectTarget(false, GOOGLE_TV));
        assertEquals("", HomeButtonRules.redirectTarget(true, IS_HOME));
    }

    @Test public void automaticRepairOnlyInTheClearCases() {
        assertTrue(HomeButtonRules.repairAutomatically(true, GOOGLE_TV, false, 1, true, false));   // dropped from the list
        assertTrue(HomeButtonRules.repairAutomatically(true, GOOGLE_TV, true, 0, true, false));    // switch turned off
        assertFalse(HomeButtonRules.repairAutomatically(true, GOOGLE_TV, true, 1, true, false));   // listed: may be starting
        assertFalse(HomeButtonRules.repairAutomatically(true, GOOGLE_TV, true, -1, true, false));
        assertFalse(HomeButtonRules.repairAutomatically(true, GOOGLE_TV, false, 1, false, false)); // no permission
        assertFalse(HomeButtonRules.repairAutomatically(false, GOOGLE_TV, false, 1, true, false)); // not wanted
        assertFalse(HomeButtonRules.repairAutomatically(true, IS_HOME, false, 1, true, false));    // nothing to send back
        assertFalse(HomeButtonRules.repairAutomatically(true, GOOGLE_TV, false, 1, true, true));   // already running
    }

    @Test public void theDroppedNoticeIsForWhatHomeboardCannotFixItself() {
        assertTrue(HomeButtonRules.droppedNotice(true, GOOGLE_TV, false, false, false));
        assertFalse(HomeButtonRules.droppedNotice(true, GOOGLE_TV, false, true, false));
        assertFalse(HomeButtonRules.droppedNotice(true, GOOGLE_TV, true, false, false));
        assertFalse(HomeButtonRules.droppedNotice(false, GOOGLE_TV, false, false, false));
        assertFalse(HomeButtonRules.droppedNotice(true, GOOGLE_TV, false, false, true));
    }

    @Test public void theGuardStopsABounceUntilAKey() {
        HomeButtonRules.RedirectGuard guard = new HomeButtonRules.RedirectGuard();
        assertTrue(guard.allow(0));
        assertTrue(guard.allow(1_000));
        assertTrue(guard.allow(2_000));
        assertFalse(guard.allow(3_000));
        guard.keyPressed();
        assertTrue(guard.allow(3_500));
    }

    /** Cast1, 0.1.108: with the redirect on and keys filtered, holding Right for 1.5 s moved one tile, not eleven. */
    @Test public void theRedirectAloneDoesNotFilterKeys() {
        assertFalse(HomeButtonRules.filterKeys(true, true, false));
    }

    @Test public void keysAreFilteredForRemoteButtonFeaturesAndWhenNothingIsRedirected() {
        assertTrue(HomeButtonRules.filterKeys(true, true, true));
        assertTrue(HomeButtonRules.filterKeys(true, false, false));
        assertFalse(HomeButtonRules.filterKeys(false, false, true));
        assertFalse(HomeButtonRules.filterKeys(false, true, true));
    }

    @Test public void theGuardForgetsRedirectsOutsideItsWindow() {
        HomeButtonRules.RedirectGuard guard = new HomeButtonRules.RedirectGuard();
        assertTrue(guard.allow(0));
        assertTrue(guard.allow(1_000));
        assertTrue(guard.allow(2_000));
        assertTrue(guard.allow(HomeButtonRules.RedirectGuard.WINDOW_MS + 1));
        assertFalse(guard.allow(HomeButtonRules.RedirectGuard.WINDOW_MS + 2));
    }
}
