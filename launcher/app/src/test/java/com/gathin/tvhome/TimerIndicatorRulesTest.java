package com.gathin.tvhome;

import com.gathin.tvhome.TimerIndicatorRules.Entry;
import com.gathin.tvhome.TimerIndicatorRules.Page;
import com.gathin.tvhome.TimerIndicatorRules.Presence;
import com.gathin.tvhome.TimerIndicatorRules.State;
import com.gathin.tvhome.TimerIndicatorRules.Word;
import org.junit.Test;
import java.util.*;
import static com.gathin.tvhome.TimerIndicatorRules.*;
import static org.junit.Assert.*;

public class TimerIndicatorRulesTest {
    static final long E = 5_000_000L, W = 1_800_000_000_000L; // elapsed and wall "now"

    static Entry running(String name, long left) { return Entry.timer(name, name, "running", E + left, 0, 0); }
    static Entry paused(String name, long left) { return Entry.timer(name, name, "paused", 0, left, 0); }
    static Entry event(String name, long left) { return Entry.event(name, name, "running", W + left, 0); }
    static Entry ringing(String name, long soundLeft) { return Entry.timer(name, name, "done", E - 1000, 0, E + soundLeft); }
    static Entry done(String name) { return Entry.timer(name, name, "done", E - 120_000, 0, 0); }
    static Entry other(String name, String state) { return Entry.timer(name, name, state, 0, 0, 0); }
    static List<String> names(List<Entry> list) { List<String> n = new ArrayList<>(); for (Entry e : list) n.add(e.name); return n; }

    @Test public void eachStateIsShownOrHidden() {
        assertEquals(State.RUNNING, state(running("Rice", 300_000), E, W));
        assertEquals(State.PAUSED, state(paused("Rice", 300_000), E, W));
        assertEquals(State.RINGING, state(ringing("Sauce", 30_000), E, W));
        assertEquals("Decision 2: done stays after the sound until Stop or Restart", State.DONE, state(done("Sauce"), E, W));
        assertEquals(State.DONE, state(ringing("Sauce", 0), E, W)); // sound deadline reached
        assertEquals(State.HIDDEN, state(other("Eggs", "stopped"), E, W));
        assertEquals("Decision 3", State.HIDDEN, state(other("Eggs", "interrupted"), E, W));
        assertEquals("Decision 3", State.HIDDEN, state(Entry.event("x", "Call", "missed", W - 1000, 0), E, W));
    }

    @Test public void eventAppearsSixtyMinutesBeforeItIsDue() {
        assertEquals(State.HIDDEN, state(event("School", 61 * MINUTE), E, W));
        assertEquals(State.HIDDEN, state(event("School", 60 * MINUTE + 1), E, W));
        assertEquals(State.EVENT_SOON, state(event("School", 60 * MINUTE), E, W));
        assertEquals(State.EVENT_SOON, state(event("School", 59 * MINUTE), E, W));
        assertEquals(State.DONE, state(Entry.event("x", "School", "done", W - 1000, 0), E, W));
    }

    @Test public void orderRingingDoneRunningByRemainingEventsThenPaused() {
        List<Entry> all = Arrays.asList(paused("Bread", 60_000), event("School", 18 * MINUTE), running("Rice", 754_000),
                other("Old", "stopped"), done("Soup"), running("Sauce", 250_000), ringing("Eggs", 40_000),
                running("Tea", 5_000), event("Far", 3 * 60 * MINUTE), other("Lost", "interrupted"));
        assertEquals(Arrays.asList("Eggs", "Soup", "Tea", "Sauce", "Rice", "School", "Bread"), names(shown(all, E, W)));
        assertEquals("the chip takes its first entry's state", State.RINGING, chip(all, E, W));
        assertEquals(State.LAST_SECONDS, chip(Arrays.asList(running("Rice", 754_000), running("Tea", 5_000), paused("Bread", 1)), E, W));
        assertEquals(State.EVENT_SOON, chip(Arrays.asList(paused("Bread", 1), event("School", MINUTE)), E, W));
        assertEquals(State.PAUSED, chip(Collections.singletonList(paused("Bread", 1)), E, W));
        assertEquals(State.HIDDEN, chip(Arrays.asList(other("Old", "stopped"), event("Far", 2 * EVENT_LEAD)), E, W));
        assertEquals(State.HIDDEN, chip(Collections.emptyList(), E, W));
    }

    @Test public void twoNamesThenPlusN() {
        Capacity four = capacity(4, 1.0f, 500, 1920);
        assertEquals(2, four.named); assertEquals(2, four.more); assertFalse(four.timeOnly);
        Capacity one = capacity(1, 1.0f, 200, 1920);
        assertEquals(1, one.named); assertEquals(0, one.more);
        Capacity two = capacity(2, 1.0f, 500, 1920);
        assertEquals(2, two.named); assertEquals(0, two.more);
        Capacity none = capacity(0, 1.0f, 0, 1920);
        assertEquals(0, none.named); assertEquals(0, none.more); assertFalse(none.timeOnly);
    }

    @Test public void timeOnlyAtLargeTextOrWideNames() {
        Capacity large = capacity(3, 1.15f, 300, 1920);
        assertTrue(large.timeOnly); assertEquals(0, large.named); assertEquals("04:10 +2", 2, large.more);
        assertTrue(capacity(3, 1.3f, 300, 1920).timeOnly);
        assertFalse(capacity(3, 1.0f, 768, 1920).timeOnly);  // exactly 40 %
        assertTrue(capacity(3, 1.0f, 769, 1920).timeOnly);   // over 40 %
    }

    @Test public void lastSecondsThresholdMatchesClockRounding() {
        for (long left = 8_000; left <= 12_000; left += 1) {
            int shownSeconds = Integer.parseInt(KitchenTimers.clock(left).substring(3));
            State s = state(running("Tea", left), E, W);
            assertEquals("left=" + left + " shows " + KitchenTimers.clock(left), shownSeconds <= 10, s == State.LAST_SECONDS);
        }
        assertEquals("00:10", KitchenTimers.clock(LAST_SECONDS));
        assertEquals("00:11", KitchenTimers.clock(LAST_SECONDS + 1));
    }

    @Test public void pageTableAndFocus() {
        assertEquals(Presence.CHIP, presence(Page.HOME)); assertTrue(focusable(Page.HOME));
        assertEquals(Presence.CHIP, presence(Page.ALL_APPS)); assertTrue(focusable(Page.ALL_APPS));
        assertEquals(Presence.CHIP, presence(Page.FULL_SCREEN)); assertTrue(focusable(Page.FULL_SCREEN));
        assertEquals(Presence.NONE, presence(Page.TIMERS)); assertFalse(focusable(Page.TIMERS));
        assertEquals(Presence.CHIP, presence(Page.MENU)); assertFalse(focusable(Page.MENU));
        assertEquals(Presence.CHIP, presence(Page.QUICK_SETTINGS)); assertFalse(focusable(Page.QUICK_SETTINGS));
        assertEquals(Presence.NONE, presence(Page.EDITOR)); assertFalse(focusable(Page.EDITOR));
        assertEquals(Presence.LINE, presence(Page.SCREENSAVER)); assertFalse(focusable(Page.SCREENSAVER));
    }

    @Test public void downFromTheBottomRowReachesAShownChip() {
        assertTrue(downReaches(Page.HOME, true, true));
        assertTrue(downReaches(Page.ALL_APPS, true, true));
        assertTrue(downReaches(Page.FULL_SCREEN, true, true));
        assertFalse(downReaches(Page.HOME, false, true));
        assertFalse(downReaches(Page.HOME, true, false));
        for (Page p : new Page[]{Page.TIMERS, Page.MENU, Page.QUICK_SETTINGS, Page.EDITOR, Page.SCREENSAVER})
            assertFalse(p.name(), downReaches(p, true, true));
    }

    @Test public void nextWakeUp() {
        assertEquals("next displayed-second boundary", 400, nextWake(Collections.singletonList(running("Rice", 754_400)), E, W));
        assertEquals("on a boundary, a whole second", 1000, nextWake(Collections.singletonList(running("Rice", 754_000)), E, W));
        assertEquals("soonest of several", 100, nextWake(Arrays.asList(running("Rice", 754_400), running("Tea", 30_100)), E, W));
        assertEquals("last-seconds threshold", 500, nextWake(Collections.singletonList(running("Tea", 10_500)), E, W));
        assertEquals("deadline passed: reconcile now", 0, nextWake(Collections.singletonList(running("Tea", 0)), E, W));
        assertEquals("only a distant event: one wake-up at its lead window, no per-second tick",
                2 * 60 * MINUTE - EVENT_LEAD, nextWake(Collections.singletonList(event("Far", 2 * 60 * MINUTE)), E, W));
        assertEquals("event in its window: next whole-minute change", 30_000, nextWake(Collections.singletonList(event("School", 17 * MINUTE + 30_000)), E, W));
        assertEquals("ringing: when the sound ends", 40_000, nextWake(Collections.singletonList(ringing("Eggs", 40_000)), E, W));
        assertEquals("paused only: no tick", NONE, nextWake(Collections.singletonList(paused("Bread", 60_000)), E, W));
        assertEquals("done after the sound: no tick", NONE, nextWake(Arrays.asList(done("Soup"), other("Old", "stopped"), other("Lost", "interrupted")), E, W));
        assertEquals("nothing: no tick", NONE, nextWake(Collections.emptyList(), E, W));
        assertTrue(needsReconcile(Collections.singletonList(running("Tea", 0)), E, W));
        assertFalse(needsReconcile(Collections.singletonList(running("Tea", 1)), E, W));
    }

    @Test public void textPartsAreKeysAndNumbersForEnglishAndThai() {
        Locale before = Locale.getDefault();
        try {
            for (Locale l : new Locale[]{Locale.ENGLISH, Locale.forLanguageTag("th-TH-u-nu-thai")}) {
                Locale.setDefault(l);
                Text run = text(running("ซอส", 250_000), E, W);
                assertEquals(State.RUNNING, run.state); assertEquals(Label.NAME, run.label); assertEquals("ซอส", run.name);
                assertEquals(Word.NONE, run.word); assertEquals("Western digits in Thai too", "04:10", KitchenTimers.clock(run.clock));
                Text ending = text(running("Tea", 9_200), E, W);
                assertEquals(Word.ENDING, ending.word); assertEquals("00:10", KitchenTimers.clock(ending.clock));
                Text pause = text(paused("", 60_000), E, W);
                assertEquals(Word.PAUSED, pause.word); assertEquals(Label.COOKING, pause.label); assertEquals(60_000, pause.clock);
                Text ring = text(ringing("Eggs", 30_000), E, W);
                assertEquals(Word.DONE, ring.word); assertEquals(NONE, ring.clock);
                Text after = text(done("Soup"), E, W);
                assertEquals(Word.SINCE, after.word); assertEquals(W - 120_000, after.timeOfDay);
                Text soon = text(Entry.event("s", "", "running", W + 17 * MINUTE + 1, 0), E, W);
                assertEquals(Word.IN_MINUTES, soon.word); assertEquals(Label.EVENT, soon.label);
                assertEquals("in 18 min, rounded up", 18, soon.minutes); assertEquals(W + 17 * MINUTE + 1, soon.timeOfDay);
            }
        } finally {
            Locale.setDefault(before);
        }
    }
}
