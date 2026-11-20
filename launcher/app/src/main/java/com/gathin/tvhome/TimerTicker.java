package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * One timer tick for the whole process, on the main looper. Views listen while visible and
 * receive a {@link TimerTickerSchedule.Snapshot}; the stored timers are parsed once per
 * revision, and a callback is posted only when something shown changes
 * ({@link TimerIndicatorRules#nextWake}). The decisions live in {@link TimerTickerSchedule}.
 */
final class TimerTicker {
    interface Listener extends TimerTickerSchedule.Listener {}

    private static TimerTicker instance;
    private final TimerTickerSchedule schedule;

    private TimerTicker(Context app) {
        Handler handler = new Handler(Looper.getMainLooper());
        schedule = new TimerTickerSchedule(
                new TimerTickerSchedule.Clock() {
                    public long elapsed() { return SystemClock.elapsedRealtime(); }
                    public long wall() { return System.currentTimeMillis(); }
                },
                new TimerTickerSchedule.Poster() {
                    public void postDelayed(Runnable r, long delayMs) { handler.postDelayed(r, delayMs); }
                    public void cancel(Runnable r) { handler.removeCallbacks(r); }
                },
                () -> KitchenTimerStore.snapshot(app));
        KitchenTimerStore.listen(schedule::changed);
    }

    /** Main thread only. */
    static TimerTicker get(Context c) {
        if (instance == null) instance = new TimerTicker(c.getApplicationContext());
        return instance;
    }

    void listen(Listener l) { schedule.listen(l); }
    void unlisten(Listener l) { schedule.unlisten(l); }
    TimerTickerSchedule.Snapshot current() { return schedule.current(); }
    /** Re-read on the next callback, for state outside the revision (alarm access after Settings). */
    void refresh() { schedule.refresh(); }
}
