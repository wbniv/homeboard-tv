package com.gathin.tvhome;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gathin.tvhome.FakeScreensaverGateway.AERIAL;
import static com.gathin.tvhome.FakeScreensaverGateway.OWN;
import static com.gathin.tvhome.ScreensaverPicker.*;
import static org.junit.Assert.*;

/** The chooser's navigation and state table (docs/plans/2026-11-26-screensaver-browsing.md). */
public class ScreensaverPickerTest {
    private static final String CLOCK = ScreensaverRegistry.CLOCK, BACKGROUND = ScreensaverRegistry.HOME_BACKGROUND;
    private static final String WF_PACKAGE = "org.worldfoundry.game.clocks";
    private static final String WF_CLOCKS = WF_PACKAGE + "/org.worldfoundry.screensaver.ClocksDreamService";
    private static final String AMBIENT = FakeScreensaverGateway.AMBIENT;

    private static ScreensaverGateway.AppScreen wfScreen() {
        return ScreensaverRegistry.appScreen(WF_PACKAGE, "org.worldfoundry.screensaver.ScreensaverPreviewActivity", null, null);
    }

    private static List<ScreensaverRegistry.Entry> catalogue() {
        return ScreensaverRegistry.entries(Arrays.asList(
                new ScreensaverGateway.InstalledDream(AERIAL, "Aerial Views", "com.neilturner.aerialviews/com.neilturner.aerialviews.Settings"),
                new ScreensaverGateway.InstalledDream(AMBIENT, "Ambient Mode", null),
                new ScreensaverGateway.InstalledDream(WF_CLOCKS, "WF Clocks", null, wfScreen())), OWN);
    }

    private static ScreensaverRegistry.Entry entry(String id) {
        for (ScreensaverRegistry.Entry e : catalogue()) if (e.id.equals(id)) return e;
        throw new AssertionError(id);
    }

    @Test public void aFreshOpeningStartsAtTheSavedChoiceAndForgetsTheLastVisit() {
        State state = new State();
        state.browsed = AERIAL; state.anchor = AERIAL; state.anchorOffset = 40; state.action = "act:options"; state.returning = true;
        state.reset(CLOCK, 0, true);
        assertEquals(CLOCK, state.browsed);
        assertNull(state.anchor);
        assertEquals(0, state.anchorOffset);
        assertNull(state.action);
        assertNull(state.takeReturnFocus());
        assertTrue(state.fromHomeSettings);
    }

    @Test public void aReturnFocusesTheInitiatingActionOnceThenTheRow() {
        State state = new State();
        state.reset(CLOCK, 0, false);
        state.used("act:preview");
        assertEquals("act:preview", state.takeReturnFocus());
        assertNull(state.takeReturnFocus());   // the next plain redraw focuses the browsed row
        assertEquals("act:preview", state.action);   // still known for a later reopen
    }

    @Test public void aCopyKeepsEveryField() {
        State state = new State();
        state.reset(AERIAL, 2, true);
        state.anchor = BACKGROUND; state.anchorOffset = -12; state.used("act:settings");
        State copy = state.copy();
        assertEquals(AERIAL, copy.browsed); assertEquals(2, copy.browsedIndex);
        assertEquals(BACKGROUND, copy.anchor); assertEquals(-12, copy.anchorOffset);
        assertEquals("act:settings", copy.action); assertTrue(copy.returning); assertTrue(copy.fromHomeSettings);
    }

    @Test public void theBrowsedRowFollowsItsIdAcrossARefresh() {
        List<ScreensaverRegistry.Entry> entries = catalogue();
        int wf = indexOf(WF_CLOCKS, entries);
        assertEquals(wf, browsedRow(WF_CLOCKS, 0, entries));   // by identity, not by the old index
        // A new app installed before it shifts it; it is still found.
        List<ScreensaverRegistry.Entry> more = ScreensaverRegistry.entries(Arrays.asList(
                new ScreensaverGateway.InstalledDream("com.a/com.a.Abacus", "Abacus", null),
                new ScreensaverGateway.InstalledDream(AERIAL, "Aerial Views", null),
                new ScreensaverGateway.InstalledDream(AMBIENT, "Ambient Mode", null),
                new ScreensaverGateway.InstalledDream(WF_CLOCKS, "WF Clocks", null, wfScreen())), OWN);
        assertEquals(wf + 1, browsedRow(WF_CLOCKS, wf, more));
    }

    @Test public void aRemovedEntryGivesTheNearestRemainingRowNotTheFirst() {
        List<ScreensaverRegistry.Entry> entries = catalogue();
        int wf = indexOf(WF_CLOCKS, entries);
        List<ScreensaverRegistry.Entry> without = ScreensaverRegistry.entries(Arrays.asList(
                new ScreensaverGateway.InstalledDream(AERIAL, "Aerial Views", null),
                new ScreensaverGateway.InstalledDream(AMBIENT, "Ambient Mode", null)), OWN);
        assertEquals(without.size() - 1, browsedRow(WF_CLOCKS, wf, without));   // it was last: now the new last
        assertEquals(2, browsedRow("gone/gone.Saver", 2, without));
        assertEquals(-1, browsedRow(CLOCK, 0, Collections.<ScreensaverRegistry.Entry>emptyList()));
    }

    @Test public void upAndDownStopAtTheEndsWithoutWrapping() {
        assertEquals(Move.HEADER, move(0, 5, -1));   // Up on the first row: the header's Back
        assertEquals(Move.STAY, move(4, 5, 1));      // Down on the last row: stay
        assertEquals(Move.ROW, move(2, 5, 1));
        assertEquals(Move.ROW, move(2, 5, -1));
        assertEquals(Move.STAY, move(0, 1, 1));      // one row only
        assertEquals(Move.HEADER, move(0, 1, -1));
        assertEquals(3, target(2, 5, 1));
        assertEquals(1, target(2, 5, -1));
        assertEquals(4, target(4, 5, 1));
        assertEquals(0, target(0, 5, -1));
    }

    @Test public void holdingDownWalksEveryRowAndStopsOnTheLast() {
        int size = 40, at = 0, steps = 0;
        while (move(at, size, 1) == Move.ROW) { at = target(at, size, 1); steps++; }
        assertEquals(size - 1, at);
        assertEquals(size - 1, steps);
    }

    @Test public void aBuiltInOffersUsePreviewAndOptions() {
        assertEquals(Arrays.asList(Action.USE, Action.PREVIEW, Action.OPTIONS), actions(entry(BACKGROUND), false, false));
        assertEquals(Arrays.asList(Action.PREVIEW, Action.OPTIONS), actions(entry(CLOCK), false, true));   // in use: no Use
        assertEquals(Arrays.asList(Action.USE, Action.PREVIEW, Action.OPTIONS), actions(entry(BACKGROUND), true, false));
    }

    @Test public void anAppWithItsOwnPreviewScreenOffersUseAndItsRealPreview() {
        assertEquals(Arrays.asList(Action.USE, Action.PREVIEW), actions(entry(WF_CLOCKS), false, false));   // no settingsActivity yet
        assertEquals(Collections.singletonList(Action.PREVIEW), actions(entry(WF_CLOCKS), false, true));
    }

    @Test public void anAndroidOnlyAppHidesPreviewAndOffersUseEverywhereAndItsSettings() {
        assertEquals(Arrays.asList(Action.EVERYWHERE, Action.SETTINGS), actions(entry(AERIAL), false, false));
        assertEquals(Collections.singletonList(Action.EVERYWHERE), actions(entry(AMBIENT), false, false));   // settings-less
        // Managed: Use applies everywhere at once.
        assertEquals(Arrays.asList(Action.USE, Action.SETTINGS), actions(entry(AERIAL), true, false));
        for (ScreensaverRegistry.Entry e : catalogue())
            if (e.app() && !e.playsOnHome()) assertFalse(e.id, actions(e, false, false).contains(Action.PREVIEW));
    }

    @Test public void afterUseFocusGoesToTheFirstActionLeftNotTheVanishedUse() {
        // Cast1, 2026-11-27: after Use on WF Clocks focus fell to the row, while a built-in kept Preview.
        assertEquals("act:preview", focusAfterUse(entry(BACKGROUND), false));
        assertEquals("act:preview", focusAfterUse(entry(WF_CLOCKS), false));
        assertEquals("act:settings", focusAfterUse(entry(AERIAL), true));   // managed: Use applied everywhere
        assertNull(focusAfterUse(entry(AMBIENT), true));                    // nothing left: its row
    }

    @Test public void actionTagsMatchTheControlsTheyFocus() {
        assertEquals("act:use", Action.USE.tag);
        assertEquals("act:use", Action.EVERYWHERE.tag);
        assertEquals("act:preview", Action.PREVIEW.tag);
        assertEquals("act:options", Action.OPTIONS.tag);
        assertEquals("act:settings", Action.SETTINGS.tag);
    }

    @Test public void oneInUseBadgeWhenHomeAndOtherAppsAgree() {
        Map<String, Badge> same = badges(CLOCK, CLOCK);
        assertEquals(1, same.size());
        assertEquals(Badge.IN_USE, same.get(CLOCK));
        Map<String, Badge> unknown = badges(CLOCK, null);   // Android's setting unreadable or not listed
        assertEquals(Badge.IN_USE, unknown.get(CLOCK));
        assertEquals(1, unknown.size());
    }

    @Test public void scopedBadgesWhenHomeAndOtherAppsDiffer() {
        Map<String, Badge> split = badges(CLOCK, WF_CLOCKS);
        assertEquals(Badge.ON_HOME, split.get(CLOCK));
        assertEquals(Badge.IN_OTHER_APPS, split.get(WF_CLOCKS));
        assertFalse(split.containsValue(Badge.IN_USE));
    }

    @Test public void whatOtherAppsUseAsAListEntry() {
        List<ScreensaverRegistry.Entry> entries = catalogue();
        // Android set to Homeboard's own screensaver plays Home's built-in choice, or the default slot for an app choice.
        assertEquals(BACKGROUND, otherAppsEntry(ScreensaverGateway.Reading.of(OWN), OWN, BACKGROUND, entries));
        assertEquals(CLOCK, otherAppsEntry(ScreensaverGateway.Reading.of(OWN), OWN, WF_CLOCKS, entries));
        assertEquals(WF_CLOCKS, otherAppsEntry(ScreensaverGateway.Reading.of(WF_CLOCKS), OWN, CLOCK, entries));
        assertEquals(WF_CLOCKS, otherAppsEntry(ScreensaverGateway.Reading.of(WF_CLOCKS + "," + AMBIENT), OWN, CLOCK, entries));   // first of a list
        assertNull(otherAppsEntry(ScreensaverGateway.Reading.UNREADABLE, OWN, CLOCK, entries));
        assertNull(otherAppsEntry(ScreensaverGateway.Reading.of(null), OWN, CLOCK, entries));
        assertNull(otherAppsEntry(ScreensaverGateway.Reading.of("com.x/com.x.NotListed"), OWN, CLOCK, entries));
    }

    @Test public void changeOpensWhenItStartsAloneForAnAppChoice() {
        assertEquals(OptionsView.WHEN_ONLY, changeOpens(entry(WF_CLOCKS)));
        assertEquals(OptionsView.WHEN_ONLY, changeOpens(entry(AERIAL)));
        assertEquals(OptionsView.ENTRY_OPTIONS, changeOpens(entry(CLOCK)));
        assertEquals(OptionsView.ENTRY_OPTIONS, changeOpens(entry(BACKGROUND)));
        assertEquals(EnumSet.of(Group.WHEN), groups(OptionsView.WHEN_ONLY));   // no built-in look rows, no Preview
        assertEquals(EnumSet.of(Group.LOOK, Group.PREVIEW, Group.WHEN), groups(OptionsView.ENTRY_OPTIONS));
    }

    @Test public void theScrollThumbIsHiddenWhenEverythingFits() {
        assertNull(thumb(0, 500, 500, 400, 24));   // two built-ins alone
        assertNull(thumb(0, 500, 300, 400, 24));
        int[] top = thumb(0, 500, 2000, 400, 24);
        assertEquals(0, top[0]); assertEquals(100, top[1]);
        int[] end = thumb(1500, 500, 2000, 400, 24);
        assertEquals(300, end[0]); assertEquals(100, end[1]);
        int[] tiny = thumb(0, 10, 100000, 400, 24);
        assertEquals(24, tiny[1]);   // never thinner than the minimum
    }

    @Test public void stableIdsDifferPerEntryAndRepeat() {
        Set<Long> ids = new HashSet<>();
        List<String> all = new ArrayList<>();
        for (ScreensaverRegistry.Entry e : catalogue()) all.add(e.id);
        for (String id : all) assertTrue(id, ids.add(stableId(id)));
        assertEquals(stableId(WF_CLOCKS), stableId(new String(WF_CLOCKS)));
    }

    // Round 2 layout defects (docs/testing/2026-11-26-screensaver-list-cast1.md).

    @Test public void theBadgeSitsBesideTheNameOnlyWhenTheWholeNameFits() {
        assertTrue(badgeInline(400, 120, 100, 16));          // "Clock" + "On Home" at 1.0x
        assertFalse(badgeInline(400, 300, 220, 16));         // "Ambient Mode" + "Needs Use everywhere" at 1.3x: badge below
        assertTrue(badgeInline(236, 120, 100, 16));          // exactly fits
        assertFalse(badgeInline(235, 120, 100, 16));
        assertFalse(badgeInline(300, 200, 400, 16));         // a badge wider than the row: below, name keeps the width
    }

    @Test public void wrappedControlsMoveWholeToTheNextLine() {
        assertTrue(staysOnLine(0, 500, 900, false, 0, true));        // the first child of a line always stays
        assertTrue(staysOnLine(200, 500, 300, false, 0, false));     // fits whole
        assertFalse(staysOnLine(260, 500, 300, false, 0, false));    // a button never shrinks: Options moves down
        assertTrue(staysOnLine(140, 500, 600, true, 200, false));    // wrapping text keeps the line while 9 ems fit
        assertFalse(staysOnLine(400, 500, 600, true, 200, false));   // "Andr / oid's": too little left, a new line
    }

    @Test public void theIllustrationIsShortForShortPanesAndLargeText() {
        assertFalse(compactSample(420, 1.0f));   // Cast1 at 1.0x
        assertTrue(compactSample(250, 1.0f));    // the Pixel in landscape
        assertTrue(compactSample(420, 1.5f));
        assertTrue(compactSample(420, 2.0f));
        assertFalse(compactSample(420, 1.3f));
    }

    @Test public void onlyATapInspectsARowNeverASwipe() {
        assertTrue(isTap(3, 4, 16, 0, true));
        assertFalse(isTap(0, 40, 16, 0, true));    // moved past the slop
        assertFalse(isTap(0, 2, 16, 60, true));    // the list scrolled under the finger
        assertFalse(isTap(0, 0, 16, 0, false));    // still settling from a fling
        assertTrue(isTap(16, 0, 16, 0, true));     // on the slop edge
    }

    // Round 3 at 2.0x: actions wrapped onto two lines (Use, Preview / Options).
    private static final int[] TWO_LINES = {0, 0, 1}, TWO_LINE_CENTERS = {100, 320, 90};

    @Test public void rightAndLeftFollowReadingOrderAcrossWrappedLines() {
        assertEquals(1, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 0, Dir.RIGHT));    // Use → Preview
        assertEquals(2, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 1, Dir.RIGHT));    // Preview → Options, not header Back
        assertEquals(2, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 2, Dir.RIGHT));    // the last control stays
        assertEquals(1, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 2, Dir.LEFT));     // Options → Preview
        assertEquals(0, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 1, Dir.LEFT));
        assertEquals(-1, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 0, Dir.LEFT));    // first: back to the browsed row
    }

    @Test public void upAndDownMoveToTheNearestControlOnTheNextLine() {
        assertEquals(2, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 0, Dir.DOWN));     // Use → Options
        assertEquals(2, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 1, Dir.DOWN));     // Preview → Options (only one there)
        assertEquals(0, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 2, Dir.UP));       // Options → Use (nearest centre)
        assertEquals(-1, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 0, Dir.UP));      // first line: the header, as before
        assertEquals(-1, actionTarget(TWO_LINES, TWO_LINE_CENTERS, 2, Dir.DOWN));    // last line: the controls below
        int[] three = {0, 1, 1, 1}, centers = {200, 40, 210, 400};
        assertEquals(2, actionTarget(three, centers, 0, Dir.DOWN));
        assertEquals(0, actionTarget(three, centers, 3, Dir.UP));
    }

    @Test public void oneLineOfActionsBehavesAsBefore() {
        int[] one = {0, 0, 0}, centers = {80, 240, 400};
        assertEquals(1, actionTarget(one, centers, 0, Dir.RIGHT));
        assertEquals(2, actionTarget(one, centers, 2, Dir.RIGHT));
        assertEquals(-1, actionTarget(one, centers, 1, Dir.UP));
        assertEquals(-1, actionTarget(one, centers, 1, Dir.DOWN));
        assertEquals(-1, actionTarget(one, centers, 5, Dir.RIGHT));   // not one of the actions
    }
}
