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

    @Test public void useEverywhereAsksOnceThenGoesStraightToInUse() {
        assertEquals(Screen.SETUP, everywhereScreen(false, false));
        assertEquals(Screen.ASK_ONCE, everywhereScreen(false, true));
        assertEquals(Screen.IN_USE, everywhereScreen(true, true));
        // Managed and the grant later removed: still in use (Restore stays reachable); a write reports not permitted.
        assertEquals(Screen.IN_USE, everywhereScreen(true, false));
    }

    @Test public void transitionsFromHomeOnly() {
        // HomeOnly → EverywhereHomeboard / EverywhereApp: Use everywhere, through setup or the question.
        assertEquals(Use.ON_HOME, use(false, CLOCK));
        assertEquals(Use.EVERYWHERE_SCREEN, use(false, AERIAL));
    }

    @Test public void transitionsBetweenTheEverywhereStatesNeedNoQuestion() {
        // EverywhereHomeboard → EverywhereApp and back: every later choice applies at once with Undo.
        assertEquals(Use.APPLY_NOW, use(true, AERIAL));
        assertEquals(Use.APPLY_NOW, use(true, CLOCK));
        assertEquals(Use.APPLY_NOW, use(true, BACKGROUND));
    }

    @Test public void restoreReturnsToHomeOnly() {
        // Everywhere* → HomeOnly: Restore clears the managed flag, and the next Use is on Home again.
        assertEquals(Mode.HOME_ONLY, mode(false, CLOCK));
        assertEquals(Use.ON_HOME, use(false, BACKGROUND));
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
        assertTrue(remoteStartsHomeboard(CLOCK));
        assertTrue(remoteStartsHomeboard(BACKGROUND));
        assertFalse(remoteStartsHomeboard(AERIAL));
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
