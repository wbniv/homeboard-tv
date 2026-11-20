package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The scheduling and re-parse decisions behind {@link TimerTicker}, with the clock, the
 * handler and the store passed in so a JVM test can drive them. Single-threaded: every call
 * comes from the main looper.
 *
 * It keeps the parsed model and reads the store again only when the store reports a new
 * revision, when a caller asks ({@link #refresh}), or when a running entry reaches its
 * deadline (the store's snapshot then reconciles, so a late alarm delivery still finishes the
 * timer). Between reads it posts at most one delayed callback, at the time
 * {@link TimerIndicatorRules#nextWake} names, and none without listeners.
 */
final class TimerTickerSchedule {
    interface Clock { long elapsed(); long wall(); }
    interface Poster { void postDelayed(Runnable r, long delayMs); void cancel(Runnable r); }
    /** Reads, reconciles and parses the stored timers: the expensive call this class rations. */
    interface Source { JSONObject read(); }
    interface Listener { void tick(Snapshot snapshot); }

    /** What listeners receive. {@code data} is the store's snapshot JSON: read it, never change it. */
    static final class Snapshot {
        final JSONObject data;
        final long revision;
        final List<TimerIndicatorRules.Entry> entries;
        final long elapsed, wall;
        Snapshot(JSONObject data, long revision, List<TimerIndicatorRules.Entry> entries, long elapsed, long wall) {
            this.data = data; this.revision = revision; this.entries = entries; this.elapsed = elapsed; this.wall = wall;
        }
    }

    private final Clock clock;
    private final Poster poster;
    private final Source source;
    private final List<Listener> listeners = new ArrayList<>();
    private final Runnable run = this::run;
    private JSONObject data;
    private long revision;
    private List<TimerIndicatorRules.Entry> entries = Collections.emptyList();
    private boolean stale = true, reading, pending;

    TimerTickerSchedule(Clock clock, Poster poster, Source source) {
        this.clock = clock; this.poster = poster; this.source = source;
    }

    /** Adds a listener and gives it the current snapshot at once. */
    void listen(Listener l) {
        if (listeners.contains(l)) return;
        listeners.add(l);
        Snapshot s = current();
        l.tick(s);
        schedule(s, false);
    }

    void unlisten(Listener l) {
        listeners.remove(l);
        if (listeners.isEmpty()) cancel();
    }

    /** The current snapshot; reads the store only if it changed since the last read. */
    Snapshot current() {
        if (stale || data == null) read();
        return snapshot();
    }

    /** The store saved a new runtime. A revision already held costs nothing. */
    void changed(long newRevision) {
        if (reading || (data != null && !stale && newRevision == revision)) return;
        stale = true;
        kick();
    }

    /** Read the store again on the next callback, for state the revision does not cover (alarm access). */
    void refresh() {
        stale = true;
        kick();
    }

    boolean pending() { return pending; }
    int listeners() { return listeners.size(); }

    private void kick() {
        if (listeners.isEmpty()) return;
        cancel();
        pending = true;
        poster.postDelayed(run, 0);
    }

    private void cancel() {
        if (pending) poster.cancel(run);
        pending = false;
    }

    private void run() {
        pending = false;
        if (listeners.isEmpty()) return;
        boolean reconcile = !stale && data != null && TimerIndicatorRules.needsReconcile(entries, clock.elapsed(), clock.wall());
        if (stale || data == null || reconcile) read();
        Snapshot s = snapshot();
        for (Listener l : new ArrayList<>(listeners)) l.tick(s);
        schedule(s, reconcile);
    }

    private void schedule(Snapshot s, boolean afterReconcile) {
        cancel();
        if (listeners.isEmpty()) return;
        long delay = TimerIndicatorRules.nextWake(s.entries, s.elapsed, s.wall);
        if (delay == TimerIndicatorRules.NONE) return; // nothing changes on its own: no tick
        // A deadline the store did not settle retries once a second, never in a tight loop.
        if (afterReconcile && delay == 0) delay = TimerIndicatorRules.SECOND;
        pending = true;
        poster.postDelayed(run, delay);
    }

    private void read() {
        reading = true;
        try {
            JSONObject next = source.read();
            data = next;
            revision = next.optLong("revision");
            entries = entries(next.optJSONArray("items"));
            stale = false;
        } finally {
            reading = false;
        }
    }

    private Snapshot snapshot() {
        return new Snapshot(data, revision, entries, clock.elapsed(), clock.wall());
    }

    /** Stored items as plain entries for {@link TimerIndicatorRules}. */
    static List<TimerIndicatorRules.Entry> entries(JSONArray items) {
        List<TimerIndicatorRules.Entry> list = new ArrayList<>();
        if (items == null) return Collections.unmodifiableList(list);
        for (int i = 0; i < items.length(); i++) {
            JSONObject t = items.optJSONObject(i);
            if (t == null) continue;
            if ("event".equals(t.optString("kind")))
                list.add(TimerIndicatorRules.Entry.event(t.optString("id"), t.optString("name"), t.optString("state"), t.optLong("at"), t.optLong("soundDeadline")));
            else
                list.add(TimerIndicatorRules.Entry.timer(t.optString("id"), t.optString("name"), t.optString("state"), t.optLong("deadline"), t.optLong("remaining"), t.optLong("soundDeadline")));
        }
        return Collections.unmodifiableList(list);
    }
}
