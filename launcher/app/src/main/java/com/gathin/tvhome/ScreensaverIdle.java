package com.gathin.tvhome;

/**
 * The Home idle timer's decision, kept free of Android so it is unit tested. MainActivity asks it after every
 * input, on resume, on window focus and when its delay comes due, and posts one delayed callback for the answer;
 * nothing polls.
 */
final class ScreensaverIdle {
    /** NONE: run no timer. WAIT: ask again after delayMs. START: show the screensaver now. */
    enum Kind { NONE, WAIT, START }

    static final class Decision {
        final Kind kind;
        final long delayMs;
        private Decision(Kind kind, long delayMs) { this.kind = kind; this.delayMs = delayMs; }
        static final Decision NONE = new Decision(Kind.NONE, -1);
        static final Decision START = new Decision(Kind.START, 0);
        static Decision waitFor(long delayMs) { return new Decision(Kind.WAIT, delayMs); }
    }

    private ScreensaverIdle() { }

    /**
     * @param now and lastInput in the same monotonic clock (SystemClock.uptimeMillis on the TV)
     * @param idleMinutes "Start on Home after"; ScreensaverSettings.ANDROID_DECIDES runs no Homeboard timer
     * @param homeForeground Home's window is resumed and focused
     * @param busy a menu, overlay, editor, expanded widget, move or resize is up: the person is using Home
     */
    static Decision decide(long now, long lastInput, int idleMinutes, boolean homeForeground, boolean busy) {
        if (idleMinutes <= ScreensaverSettings.ANDROID_DECIDES || !homeForeground || busy) return Decision.NONE;
        long due = lastInput + idleMinutes * 60_000L;
        return now >= due ? Decision.START : Decision.waitFor(due - now);
    }
}
