package com.gathin.tvhome;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Counts RemoteViews updates per hosted Android widget since process start and decides when to
 * report them for the coordinator's long-exposure sampler (tag {@link #TAG}). The first update of a
 * widget is always reported; after that at most one line per widget per {@link #INTERVAL_MS}.
 */
final class WidgetUpdateStats {
    static final String TAG = "HB_WIDGET_STATS";
    static final long INTERVAL_MS = 60_000L;
    /** One per process, so counts survive activity recreation and are "since process start". */
    static final WidgetUpdateStats PROCESS = new WidgetUpdateStats(android.os.SystemClock::elapsedRealtime);

    private final LongSupplier clock;
    private final Map<Integer, long[]> widgets = new HashMap<>(); // id -> {count, lastReportedAt}

    WidgetUpdateStats(LongSupplier clock) { this.clock = clock; }

    /** Records one update; returns the line to log now, or null while rate-limited. */
    synchronized String record(int id) {
        long now = clock.getAsLong();
        long[] w = widgets.get(id);
        if (w == null) { w = new long[]{0, Long.MIN_VALUE}; widgets.put(id, w); }
        w[0]++;
        if (w[1] != Long.MIN_VALUE && now - w[1] < INTERVAL_MS) return null;
        w[1] = now;
        return line(id, w[0]);
    }

    static String line(int id, long updates) {
        return "{\"event\":\"widget_updates\",\"id\":" + id + ",\"updates\":" + updates + "}";
    }
}
