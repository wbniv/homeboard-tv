package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Everything the timer indicator decides, with no Android types and no clock of its own.
 * Callers pass {@code elapsed} (SystemClock.elapsedRealtime, the clock cooking deadlines use)
 * and {@code wall} (System.currentTimeMillis, the clock event alarms use).
 * See docs/plans/2026-11-20-timers-on-every-page.md, "What it shows" and "Performance".
 * Strings stay out: text parts are keys and numbers that the view formats.
 */
final class TimerIndicatorRules {
    private TimerIndicatorRules() {}

    static final long SECOND = 1000L, MINUTE = 60_000L;
    /** Last seconds: the displayed {@link KitchenTimers#clock} reads 00:10 or less (it rounds up). */
    static final long LAST_SECONDS = 10_000L;
    /** Decision 4: an event alarm appears 60 min before it is due, fixed. */
    static final long EVENT_LEAD = 60 * MINUTE;
    /** Android's "Large" font scale; at or above it the chip keeps only the time and the count. */
    static final float LARGE_FONT_SCALE = 1.15f;
    /** Two names wider than this share of the screen also drop the names. */
    static final float NAMES_MAX_SCREEN_SHARE = 0.40f;
    static final int MAX_NAMED = 2;
    /** No wake-up is needed. */
    static final long NONE = -1L;

    enum Kind { TIMER, EVENT }

    /** State of one entry, and of the chip (the state of its first, most urgent entry). */
    enum State { HIDDEN, RUNNING, LAST_SECONDS, RINGING, DONE, PAUSED, EVENT_SOON }

    /** One stored timer or event alarm, as plain values. */
    static final class Entry {
        final String id, name, state;
        final Kind kind;
        /** Cooking timers: elapsed-clock deadline while running; stored remaining otherwise. */
        final long deadline, remaining;
        /** Event alarms: wall-clock due time. */
        final long at;
        /** Elapsed-clock end of the alert sound; 0 or past when silent. */
        final long soundDeadline;

        Entry(String id, String name, Kind kind, String state, long deadline, long remaining, long at, long soundDeadline) {
            this.id = id; this.name = name == null ? "" : name; this.kind = kind; this.state = state;
            this.deadline = deadline; this.remaining = remaining; this.at = at; this.soundDeadline = soundDeadline;
        }

        static Entry timer(String id, String name, String state, long deadline, long remaining, long soundDeadline) {
            return new Entry(id, name, Kind.TIMER, state, deadline, remaining, 0, soundDeadline);
        }

        static Entry event(String id, String name, String state, long at, long soundDeadline) {
            return new Entry(id, name, Kind.EVENT, state, 0, 0, at, soundDeadline);
        }

        /** Same arithmetic as {@link KitchenTimers#remaining}. */
        long remaining(long elapsed, long wall) {
            if (kind == Kind.EVENT) return Math.max(0, at - wall);
            return "running".equals(state) ? Math.max(0, deadline - elapsed) : Math.max(0, remaining);
        }
    }

    /** The state one entry shows, or HIDDEN (stopped, interrupted, missed, distant event). */
    static State state(Entry e, long elapsed, long wall) {
        switch (e.state) {
            case "done":
                return e.soundDeadline > elapsed ? State.RINGING : State.DONE; // Decision 2
            case "paused":
                return e.kind == Kind.TIMER ? State.PAUSED : State.HIDDEN;
            case "running":
                long left = e.remaining(elapsed, wall);
                if (e.kind == Kind.EVENT) return left <= EVENT_LEAD ? State.EVENT_SOON : State.HIDDEN;
                return left <= LAST_SECONDS ? State.LAST_SECONDS : State.RUNNING;
            default:
                return State.HIDDEN; // stopped; interrupted and missed (Decision 3)
        }
    }

    private static int rank(State s) {
        switch (s) {
            case RINGING: return 0;
            case DONE: return 1;
            case LAST_SECONDS: case RUNNING: return 2;
            case EVENT_SOON: return 3;
            case PAUSED: return 4;
            default: return 5;
        }
    }

    /** Shown entries, most urgent first: ringing, done, running by remaining time, events by time, paused. */
    static List<Entry> shown(List<Entry> entries, long elapsed, long wall) {
        List<Entry> list = new ArrayList<>();
        for (Entry e : entries) if (state(e, elapsed, wall) != State.HIDDEN) list.add(e);
        list.sort(Comparator.comparingInt((Entry e) -> rank(state(e, elapsed, wall)))
                .thenComparingLong(e -> e.kind == Kind.EVENT ? e.at : e.remaining(elapsed, wall)));
        return list;
    }

    /** The chip's overall state: that of its first entry, or HIDDEN with nothing live. */
    static State chip(List<Entry> entries, long elapsed, long wall) {
        List<Entry> list = shown(entries, elapsed, wall);
        return list.isEmpty() ? State.HIDDEN : state(list.get(0), elapsed, wall);
    }

    /** How many entries the chip names, and the {@code +N} after them. */
    static final class Capacity {
        final int named, more;
        final boolean timeOnly;
        Capacity(int named, int more, boolean timeOnly) { this.named = named; this.more = more; this.timeOnly = timeOnly; }
    }

    /**
     * Two names then {@code +N}; time and count only ({@code 04:10 +2}) at Large text or when
     * the names would take more than 40 % of the screen width.
     */
    static Capacity capacity(int shown, float fontScale, float namesWidth, float screenWidth) {
        if (shown <= 0) return new Capacity(0, 0, false);
        boolean timeOnly = fontScale >= LARGE_FONT_SCALE || namesWidth > NAMES_MAX_SCREEN_SHARE * screenWidth;
        if (timeOnly) return new Capacity(0, shown - 1, true);
        int named = Math.min(MAX_NAMED, shown);
        return new Capacity(named, shown - named, false);
    }

    /** Which label a name shows: the stored name, or the default for its kind. */
    enum Label { NAME, COOKING, EVENT }

    /** Word keys the view turns into English or Thai. */
    enum Word { NONE, ENDING, PAUSED, DONE, SINCE, IN_MINUTES }

    /** Text parts for one entry. Numbers are raw; the view formats them (Western digits). */
    static final class Text {
        final State state;
        final Label label;
        final String name;
        final Word word;
        /** Remaining time for {@link KitchenTimers#clock}, or NONE. */
        final long clock;
        /** Wall-clock time of day to show (event due time, or the finish time after Done), or NONE. */
        final long timeOfDay;
        /** Whole minutes until an event, rounded up, or NONE. */
        final long minutes;
        Text(State state, Label label, String name, Word word, long clock, long timeOfDay, long minutes) {
            this.state = state; this.label = label; this.name = name; this.word = word;
            this.clock = clock; this.timeOfDay = timeOfDay; this.minutes = minutes;
        }
    }

    static Text text(Entry e, long elapsed, long wall) {
        State s = state(e, elapsed, wall);
        Label label = !e.name.isEmpty() ? Label.NAME : e.kind == Kind.EVENT ? Label.EVENT : Label.COOKING;
        long left = e.remaining(elapsed, wall);
        switch (s) {
            case RUNNING: return new Text(s, label, e.name, Word.NONE, left, NONE, NONE);
            case LAST_SECONDS: return new Text(s, label, e.name, Word.ENDING, left, NONE, NONE);
            case PAUSED: return new Text(s, label, e.name, Word.PAUSED, left, NONE, NONE);
            case RINGING: return new Text(s, label, e.name, Word.DONE, NONE, NONE, NONE);
            case DONE: return new Text(s, label, e.name, Word.SINCE, NONE, finishedAt(e, elapsed, wall), NONE);
            case EVENT_SOON: return new Text(s, label, e.name, Word.IN_MINUTES, NONE, e.at, (left + MINUTE - 1) / MINUTE);
            default: return new Text(s, label, e.name, Word.NONE, NONE, NONE, NONE);
        }
    }

    /** Wall-clock finish time of a done entry, or NONE when the store no longer says. */
    static long finishedAt(Entry e, long elapsed, long wall) {
        if (e.kind == Kind.EVENT) return e.at;
        return e.deadline > 0 && e.deadline <= elapsed ? wall - (elapsed - e.deadline) : NONE;
    }

    /** Pages of the plan's "Where it lives on each page" table. */
    enum Page { HOME, ALL_APPS, FULL_SCREEN, TIMERS, MENU, QUICK_SETTINGS, EDITOR, SCREENSAVER }

    enum Presence { CHIP, LINE, NONE }

    static Presence presence(Page page) {
        switch (page) {
            case HOME: case ALL_APPS: case FULL_SCREEN: case MENU: case QUICK_SETTINGS: return Presence.CHIP;
            case SCREENSAVER: return Presence.LINE; // Stage 4, Decision 1
            default: return Presence.NONE;           // Timers & alarms lists them; editors' keyboards cover the band
        }
    }

    /** The chip is focusable (never focused by itself) only where the page owns focus. */
    static boolean focusable(Page page) {
        return page == Page.HOME || page == Page.ALL_APPS || page == Page.FULL_SCREEN;
    }

    /** Down from the bottom row (or a full-screen view's last row) reaches a shown chip. */
    static boolean downReaches(Page page, boolean fromBottomRow, boolean chipShown) {
        return chipShown && fromBottomRow && presence(page) == Presence.CHIP && focusable(page);
    }

    /** A running entry has reached its deadline and the store has not reconciled it yet. */
    static boolean needsReconcile(List<Entry> entries, long elapsed, long wall) {
        for (Entry e : entries) if ("running".equals(e.state) && e.remaining(elapsed, wall) == 0) return true;
        return false;
    }

    /**
     * Delay in ms until something displayed changes, or NONE: the next displayed-second
     * boundary of a running cooking timer, its last-seconds threshold and deadline; an event's
     * lead-window start, then its minute boundaries and due time; the end of an alert sound.
     * A distant event sets one wake-up for its window, never a per-second tick.
     */
    static long nextWake(List<Entry> entries, long elapsed, long wall) {
        long next = Long.MAX_VALUE;
        for (Entry e : entries) {
            if ("running".equals(e.state)) {
                long left = e.remaining(elapsed, wall);
                if (left <= 0) return 0;
                if (e.kind == Kind.TIMER) {
                    next = Math.min(next, boundary(left, SECOND));
                    if (left > LAST_SECONDS) next = Math.min(next, left - LAST_SECONDS);
                } else if (left > EVENT_LEAD) {
                    next = Math.min(next, left - EVENT_LEAD);
                } else {
                    next = Math.min(next, boundary(left, MINUTE));
                }
                next = Math.min(next, left);
            } else if ("done".equals(e.state) && e.soundDeadline > elapsed) {
                next = Math.min(next, e.soundDeadline - elapsed);
            }
        }
        return next == Long.MAX_VALUE ? NONE : next;
    }

    /** Time until a rounded-up display of {@code left} in {@code unit}s drops by one. */
    private static long boundary(long left, long unit) {
        long r = left % unit;
        return r == 0 ? unit : r;
    }
}
