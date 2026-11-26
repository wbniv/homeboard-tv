package com.gathin.tvhome;

import org.junit.Test;

import static com.gathin.tvhome.FakeScreensaverGateway.AERIAL;
import static com.gathin.tvhome.FakeScreensaverGateway.AMBIENT;
import static com.gathin.tvhome.FakeScreensaverGateway.OWN;
import static com.gathin.tvhome.ScreensaverRules.*;
import static org.junit.Assert.*;

/** Every row of the plan's "Who starts it, and when" table and every transition of its state diagram. */
public class ScreensaverRulesTest {
    private static final String CLOCK = ScreensaverRegistry.CLOCK, BACKGROUND = ScreensaverRegistry.HOME_BACKGROUND;
    private static final int OFF = ScreensaverSettings.ANDROID_DECIDES;

    @Test public void theThreeStates() {
        assertEquals(Mode.HOME_ONLY, mode(false, CLOCK));
        assertEquals(Mode.HOME_ONLY, mode(false, AERIAL));   // after Restore the saved choice may still name an app
        assertEquals(Mode.EVERYWHERE_HOMEBOARD, mode(true, CLOCK));
        assertEquals(Mode.EVERYWHERE_HOMEBOARD, mode(true, BACKGROUND));
        assertEquals(Mode.EVERYWHERE_APP, mode(true, AERIAL));
    }

    @Test public void onHomeOnlyHomeboardsTimerRunsOnlyWhenATimeIsChosen() {
        assertTrue(homeTimerRuns(false, CLOCK, 5));
        assertEquals(5, homeIdleMinutes(false, CLOCK, 5));
        assertFalse(homeTimerRuns(false, CLOCK, OFF));   // "Android decides": no Homeboard timer, today's behaviour
        assertEquals(HomeLine.AFTER_MINUTES, homeLine(false, CLOCK, 5));
        assertEquals(HomeLine.NO_TIMER, homeLine(false, CLOCK, OFF));
        assertEquals(OtherLine.UNCHANGED, otherLine(false, true));
    }

    @Test public void everywhereOnlyAndroidsTimerIsInCharge() {
        for (String choice : new String[]{CLOCK, BACKGROUND, AERIAL})
            for (int minutes : ScreensaverSettings.IDLE_MINUTES) {
                assertFalse(choice + " " + minutes, homeTimerRuns(true, choice, minutes));
                assertEquals(OFF, homeIdleMinutes(true, choice, minutes));
                assertEquals(HomeLine.ANDROID_STARTS, homeLine(true, choice, minutes));
            }
        assertEquals(OtherLine.THE_SAME, otherLine(true, true));
    }

    @Test public void anUnreadableSettingIsShownAsUnknownInEveryState() {
        assertEquals(OtherLine.UNKNOWN, otherLine(false, false));
        assertEquals(OtherLine.UNKNOWN, otherLine(true, false));
    }

    @Test public void androidsScreensaverFollowsHomeboardsOnATvHomeboardDoesNotManage() {
        // Q7 (Cast1, 2026-11-26): Homeboard's plays first, then Android's selected one at Android's idle time.
        assertTrue(androidFollows(false, ScreensaverGateway.Reading.of(AMBIENT), OWN));
        assertTrue(androidFollows(false, ScreensaverGateway.Reading.of(AERIAL), OWN));
        assertTrue(androidFollows(false, ScreensaverGateway.Reading.of(null), OWN));          // Android's default
        assertTrue(androidFollows(false, ScreensaverGateway.Reading.UNREADABLE, OWN));        // still true, only its choice is unknown
    }

    @Test public void theNoteIsLeftOutWhenTheHandOverCannotBeSeen() {
        // Managed ("Use everywhere"): Android plays the choice in every app, with Homeboard's own timer off.
        assertFalse(androidFollows(true, ScreensaverGateway.Reading.of(OWN), OWN));
        assertFalse(androidFollows(true, ScreensaverGateway.Reading.of(AERIAL), OWN));
        assertFalse(androidFollows(true, ScreensaverGateway.Reading.UNREADABLE, OWN));
        // Not managed, but Android already names Homeboard's own screensaver: it plays Homeboard's choice anyway.
        assertFalse(androidFollows(false, ScreensaverGateway.Reading.of(OWN), OWN));
        assertFalse(androidFollows(false, ScreensaverGateway.Reading.of(OWN + "," + AMBIENT), OWN));
    }

    @Test public void theMainPaneSaysAndroidFollowsOnlyWhileHomeboardsHomeTimerIsSet() {
        ScreensaverGateway.Reading ambient = ScreensaverGateway.Reading.of(AMBIENT);
        // AFTER_MINUTES: Homeboard plays on Home first, then Android's takes over.
        for (int minutes : ScreensaverSettings.IDLE_MINUTES) {
            if (minutes == OFF) continue;
            assertEquals(HomeLine.AFTER_MINUTES, homeLine(false, CLOCK, minutes));
            assertTrue("" + minutes, androidFollowsOnPane(false, CLOCK, minutes, ambient, OWN));
        }
        // NO_TIMER: Homeboard's screensaver never plays on Home, so there is nothing to be replaced.
        assertEquals(HomeLine.NO_TIMER, homeLine(false, CLOCK, OFF));
        assertFalse(androidFollowsOnPane(false, CLOCK, OFF, ambient, OWN));
        // ANDROID_STARTS: managed, whatever the minutes were.
        for (int minutes : ScreensaverSettings.IDLE_MINUTES) {
            assertEquals(HomeLine.ANDROID_STARTS, homeLine(true, CLOCK, minutes));
            assertFalse("" + minutes, androidFollowsOnPane(true, CLOCK, minutes, ambient, OWN));
        }
        // A timer is set but Android already names Homeboard's own screensaver: nothing visible changes.
        assertFalse(androidFollowsOnPane(false, CLOCK, 5, ScreensaverGateway.Reading.of(OWN), OWN));
        // The Options box does not depend on the timer: it explains what a timer will do.
        assertTrue(androidFollows(false, ambient, OWN));
    }

    @Test public void useEverywhereAsksOnceThenGoesStraightToInUse() {
        assertEquals(Screen.SETUP, everywhereScreen(false, false));
        assertEquals(Screen.ASK_ONCE, everywhereScreen(false, true));
        assertEquals(Screen.IN_USE, everywhereScreen(true, true));
        // Managed and the grant later removed: still in use (Restore stays reachable); a write reports not permitted.
        assertEquals(Screen.IN_USE, everywhereScreen(true, false));
    }

    @Test public void transitionsFromHomeOnly() {
        // HomeOnly → EverywhereHomeboard / EverywhereApp: Use everywhere, through setup or the question.
        assertEquals(Use.ON_HOME, use(false, CLOCK, false));
        assertEquals(Use.EVERYWHERE_SCREEN, use(false, AERIAL, false));
    }

    @Test public void transitionsBetweenTheEverywhereStatesNeedNoQuestion() {
        // EverywhereHomeboard → EverywhereApp and back: every later choice applies at once with Undo.
        assertEquals(Use.APPLY_NOW, use(true, AERIAL, false));
        assertEquals(Use.APPLY_NOW, use(true, CLOCK, false));
        assertEquals(Use.APPLY_NOW, use(true, BACKGROUND, false));
    }

    @Test public void restoreReturnsToHomeOnly() {
        // Everywhere* → HomeOnly: Restore clears the managed flag, and the next Use is on Home again.
        assertEquals(Mode.HOME_ONLY, mode(false, CLOCK));
        assertEquals(Use.ON_HOME, use(false, BACKGROUND, false));
        assertEquals(Screen.ASK_ONCE, everywhereScreen(false, true));
    }

    @Test public void theComponentAndroidMustName() {
        assertEquals(OWN, componentFor(CLOCK, OWN));
        assertEquals(OWN, componentFor(BACKGROUND, OWN));
        assertEquals(AERIAL, componentFor(AERIAL, OWN));
        ScreensaverGateway.Reading own = ScreensaverGateway.Reading.of(OWN);
        assertTrue(inUseEverywhere(true, own, CLOCK, OWN));
        assertFalse(inUseEverywhere(false, own, CLOCK, OWN));
        assertFalse(inUseEverywhere(true, own, AERIAL, OWN));
        assertFalse(inUseEverywhere(true, ScreensaverGateway.Reading.UNREADABLE, CLOCK, OWN));
        assertTrue(inUseEverywhere(true, ScreensaverGateway.Reading.of(AERIAL + "," + AMBIENT), AERIAL, OWN));
    }

    @Test public void theRemoteStartsHomeboardsOwnEntryAndAndroidsShortcutForAnApp() {
        assertEquals(Player.HOMEBOARD, remotePlayer(CLOCK, false));
        assertEquals(Player.HOMEBOARD, remotePlayer(BACKGROUND, false));
        assertEquals(Player.ANDROID, remotePlayer(AERIAL, false));
    }

    // Stage 5: an app that declares its own preview screen (docs/plans/2026-11-26-screensaver-companion-contract.md).
    private static final String AQUARIUM = "org.worldfoundry.game.aquarium/org.worldfoundry.screensaver.AquariumDream";

    private static ScreensaverRegistry.Entry app(String component, ScreensaverGateway.AppScreen screen) {
        return ScreensaverRegistry.app(new ScreensaverGateway.InstalledDream(component, "App", null, screen));
    }

    private static ScreensaverGateway.AppScreen aquariumScreen() {
        return ScreensaverRegistry.appScreen("org.worldfoundry.game.aquarium", "org.worldfoundry.screensaver.ScreensaverPreviewActivity",
                "org.worldfoundry.screensaver.DREAM", "aquarium");
    }

    @Test public void anAppWithItsOwnPreviewScreenIsUsedOnHomeBeforeTheQuestion() {
        assertEquals(Use.ON_HOME, use(false, AQUARIUM, true));
        assertEquals(Use.EVERYWHERE_SCREEN, use(false, AQUARIUM, false));   // without the metadata: as before Stage 5
        // Once Homeboard manages the TV, Use everywhere works as today for every app.
        assertEquals(Use.APPLY_NOW, use(true, AQUARIUM, true));
        assertEquals(Use.APPLY_NOW, use(true, AQUARIUM, false));
        // A built-in never needs the flag.
        assertEquals(Use.ON_HOME, use(false, CLOCK, true));
    }

    @Test public void homeIdlePlaysAnAppsOwnScreenAndHomeboardsForEverythingElse() {
        assertEquals(Player.APP_SCREEN, homePlayer(app(AQUARIUM, aquariumScreen())));
        assertEquals(Player.HOMEBOARD, homePlayer(app(AERIAL, null)));   // no preview screen: Homeboard's default slot
        assertEquals(Player.HOMEBOARD, homePlayer(ScreensaverRegistry.find(CLOCK)));
        assertEquals(Player.HOMEBOARD, homePlayer(ScreensaverRegistry.find(BACKGROUND)));
        assertEquals(Player.HOMEBOARD, homePlayer(null));
        // An app that was uninstalled resolves to the default slot before homePlayer sees it.
        java.util.List<ScreensaverRegistry.Entry> none = ScreensaverRegistry.entries(java.util.Collections.emptyList(), OWN);
        assertEquals(Player.HOMEBOARD, homePlayer(ScreensaverRegistry.resolve(AQUARIUM, none)));
        // On Home only the Home timer runs for an app choice; managed, Android's timer is in charge as before.
        assertTrue(homeTimerRuns(false, AQUARIUM, 5));
        assertFalse(homeTimerRuns(true, AQUARIUM, 5));
    }

    @Test public void theRemotePlaysAnAppsOwnScreenAtOnce() {
        assertEquals(Player.APP_SCREEN, remotePlayer(AQUARIUM, true));
        assertEquals(Player.ANDROID, remotePlayer(AQUARIUM, false));
        assertEquals(Player.HOMEBOARD, remotePlayer(CLOCK, true));   // a built-in id never names an app screen
        assertEquals(Player.HOMEBOARD, remotePlayer(null, false));
    }

    @Test public void componentsCompareInTheirLongForm() {
        assertEquals("com.a/com.a.Saver", canonical("com.a/.Saver"));
        assertEquals("com.a/com.b.Saver", canonical("com.a/com.b.Saver"));
        assertEquals("com.a/com.a.Saver", first("com.a/.Saver,com.b/.Other"));
        assertNull(first(null));
        assertNull(first(""));
        assertNull(first(" , x/y"));
        assertTrue(isApp(AERIAL));
        assertFalse(isApp(CLOCK));
        assertFalse(isApp(null));
        assertFalse(isApp("/x"));
    }

    @Test public void androidsTimeoutInMinutes() {
        assertEquals(10, timeoutMinutes(ScreensaverGateway.Reading.of("600000")));
        assertEquals(1, timeoutMinutes(ScreensaverGateway.Reading.of("30000")));
        assertEquals(NEVER, timeoutMinutes(ScreensaverGateway.Reading.of(String.valueOf(Integer.MAX_VALUE))));
        assertEquals(UNKNOWN, timeoutMinutes(ScreensaverGateway.Reading.of(null)));
        assertEquals(UNKNOWN, timeoutMinutes(ScreensaverGateway.Reading.of("soon")));
        assertEquals(UNKNOWN, timeoutMinutes(ScreensaverGateway.Reading.of("-1")));
        assertEquals(UNKNOWN, timeoutMinutes(ScreensaverGateway.Reading.UNREADABLE));
    }
}
