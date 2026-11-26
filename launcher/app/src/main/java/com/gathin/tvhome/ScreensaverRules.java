package com.gathin.tvhome;

/**
 * Which timer is in charge and what the Screensaver screens say, from the plan's state table
 * (docs/plans/2026-11-20-screensavers.md, "Who starts it, and when"). Pure, so every row is unit tested.
 *
 * On Home only (not managed): Homeboard's Home timer shows the choice if a time is chosen; Android is untouched.
 * Homeboard everywhere / Android app everywhere (managed): Android starts the choice in every app at its own
 * timeout and Homeboard's Home timer is off, so exactly one timer is in charge.
 */
final class ScreensaverRules {
    enum Mode { HOME_ONLY, EVERYWHERE_HOMEBOARD, EVERYWHERE_APP }

    /** Which state of the Use everywhere screen to show. */
    enum Screen { SETUP, ASK_ONCE, IN_USE }

    /** What "Use" does for an entry. */
    enum Use {
        /** Saved in Homeboard and played on Home; Android untouched (Stage 1). */
        ON_HOME,
        /** Opens Use everywhere: the one-time setup or the asked-once question. */
        EVERYWHERE_SCREEN,
        /** Managed: applied in every app at once, with Undo and no question. */
        APPLY_NOW
    }

    enum HomeLine { NO_TIMER, AFTER_MINUTES, ANDROID_STARTS }

    enum OtherLine { UNCHANGED, THE_SAME, UNKNOWN }

    /** timeoutMinutes: the timeout could not be read. */
    static final int UNKNOWN = -1;
    /** timeoutMinutes: Android never times out (screen_off_timeout at or past Integer.MAX_VALUE). */
    static final int NEVER = Integer.MAX_VALUE;

    private ScreensaverRules() { }

    /** Android screensaver apps are stored by their flattened component, which always holds a '/'; built-in ids never do. */
    static boolean isApp(String id) { return id != null && id.indexOf('/') > 0; }

    static Mode mode(boolean managed, String choice) {
        if (!managed) return Mode.HOME_ONLY;
        return isApp(choice) ? Mode.EVERYWHERE_APP : Mode.EVERYWHERE_HOMEBOARD;
    }

    /** The minutes Homeboard's own Home timer runs with: the person's choice on Home only, otherwise none. */
    static int homeIdleMinutes(boolean managed, String choice, int idleMinutes) {
        return mode(managed, choice) == Mode.HOME_ONLY ? idleMinutes : ScreensaverSettings.ANDROID_DECIDES;
    }

    static boolean homeTimerRuns(boolean managed, String choice, int idleMinutes) {
        return homeIdleMinutes(managed, choice, idleMinutes) != ScreensaverSettings.ANDROID_DECIDES;
    }

    static Screen everywhereScreen(boolean managed, boolean mayWrite) {
        if (managed) return Screen.IN_USE;
        return mayWrite ? Screen.ASK_ONCE : Screen.SETUP;
    }

    static Use use(boolean managed, String entryId) {
        if (managed) return Use.APPLY_NOW;
        return isApp(entryId) ? Use.EVERYWHERE_SCREEN : Use.ON_HOME;
    }

    static HomeLine homeLine(boolean managed, String choice, int idleMinutes) {
        if (mode(managed, choice) != Mode.HOME_ONLY) return HomeLine.ANDROID_STARTS;
        return idleMinutes == ScreensaverSettings.ANDROID_DECIDES ? HomeLine.NO_TIMER : HomeLine.AFTER_MINUTES;
    }

    static OtherLine otherLine(boolean managed, boolean componentReadable) {
        if (!componentReadable) return OtherLine.UNKNOWN;
        return managed ? OtherLine.THE_SAME : OtherLine.UNCHANGED;
    }

    /**
     * Whether the Screensaver screen says that Android's own screensaver replaces Homeboard's at Android's idle time
     * (Q7, docs/testing/2026-11-26-screensaver-q7.md). It does on any TV Homeboard does not manage, because Android runs
     * its own screensaver and idle time in every app and Homeboard holds no wake lock. When Android is set to Homeboard's
     * own screensaver (managed), Android plays Homeboard's choice anyway, so the hand-over is invisible and the note is
     * left out. A setting Homeboard cannot read does not hide it: Android still takes over, only its choice is unknown.
     */
    static boolean androidFollows(boolean managed, ScreensaverGateway.Reading component, String ownComponent) {
        if (managed) return false;
        return !(component.readable && ownComponent.equals(first(component.value)));
    }

    /**
     * The remote's screensaver action. A Homeboard entry is chosen: Homeboard plays it at once (Show now), whether
     * or not it manages the TV. An Android screensaver app is chosen, which happens only through Use everywhere:
     * Android's own "start now" shortcut, since Android is set to that app and Homeboard cannot run it.
     */
    static boolean remoteStartsHomeboard(String choice) { return !isApp(choice); }

    /** The component Android must name for choice: the app itself, or this install's ScreensaverDream. */
    static String componentFor(String choice, String ownComponent) { return isApp(choice) ? choice : ownComponent; }

    /** Android is set to start choice: managed, readable and naming it first. */
    static boolean inUseEverywhere(boolean managed, ScreensaverGateway.Reading component, String choice, String ownComponent) {
        return managed && component.readable && componentFor(choice, ownComponent).equals(first(component.value));
    }

    /** The first component of a raw screensaver_components list, canonical; null when unset or empty. */
    static String first(String raw) {
        if (raw == null) return null;
        int comma = raw.indexOf(',');
        String first = (comma < 0 ? raw : raw.substring(0, comma)).trim();
        return first.isEmpty() ? null : canonical(first);
    }

    /**
     * A flattened component in its long form, "package/package.Class" for "package/.Class", so a value written by
     * Android (short form) and one written over adb (either form) compare equal. Homeboard's ids are long form.
     */
    static String canonical(String component) {
        if (component == null) return null;
        int slash = component.indexOf('/');
        if (slash <= 0 || slash + 1 >= component.length() || component.charAt(slash + 1) != '.') return component;
        return component.substring(0, slash + 1) + component.substring(0, slash) + component.substring(slash + 1);
    }

    /** Whole minutes, rounded; UNKNOWN when unreadable or not a number, NEVER for Android's "never". */
    static int timeoutMinutes(ScreensaverGateway.Reading timeout) {
        if (timeout == null || !timeout.readable || timeout.value == null) return UNKNOWN;
        try {
            long ms = Long.parseLong(timeout.value.trim());
            if (ms <= 0) return UNKNOWN;
            if (ms >= Integer.MAX_VALUE) return NEVER;
            return (int) Math.max(1, Math.round(ms / 60_000.0));
        } catch (NumberFormatException invalid) { return UNKNOWN; }
    }
}
