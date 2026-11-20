package com.gathin.tvhome;

import java.util.ArrayDeque;

/**
 * The Home button setting's rules, free of Android so they are unit tested (docs/plans/2026-11-20-home-button.md).
 * The screen always shows what is true, not only what was asked: "wanted" is the person's choice, "running" is
 * whether Android actually has Homeboard's helper bound.
 */
final class HomeButtonRules {
    private HomeButtonRules() { }

    /** Which state of Home settings → Home button to show. */
    enum Screen {
        /** Android already opens Homeboard for Home and nothing outranks it. */
        NOT_NEEDED,
        /** No single launcher to send back (Android asks which app opens Home): choosing the Home app is the route. */
        CHOOSER,
        /** The setting is off: Home opens the TV's own launcher. */
        OFF,
        /** On, Android was just asked to start the helper, and it has not connected yet. */
        STARTING,
        /** On, and the helper is bound: a short press of Home opens Homeboard. */
        WORKING,
        /** On, but the helper is not running, and Homeboard can do something about it or Android lists it already. */
        NOT_RUNNING,
        /** On, the helper is off, and Homeboard has no permission to turn it on: the one-time setup or Android's screen. */
        SETUP
    }

    /**
     * @param home what HomeTarget found
     * @param wanted the person turned the setting on
     * @param listed Homeboard's helper is on Android's enabled list with the accessibility switch not off
     * @param running the helper is bound right now
     * @param mayWrite Homeboard holds WRITE_SECURE_SETTINGS (the one-time setup)
     * @param waiting Homeboard asked Android to start the helper moments ago and is waiting for it to connect
     */
    static Screen screen(HomeTarget.Result home, boolean wanted, boolean listed, boolean running, boolean mayWrite, boolean waiting) {
        if (home.target == null) return home.homeboardIsHome ? Screen.NOT_NEEDED : Screen.CHOOSER;
        if (!wanted) return Screen.OFF;
        if (running) return Screen.WORKING;
        if (waiting) return Screen.STARTING;
        if (!listed && !mayWrite) return Screen.SETUP;
        return Screen.NOT_RUNNING;
    }

    /** What the Home settings entry says after "Home button · ". */
    enum Summary { HOMEBOARD, OTHER_LAUNCHER, ATTENTION }

    static Summary summary(Screen screen) {
        switch (screen) {
            case WORKING: case NOT_NEEDED: return Summary.HOMEBOARD;
            case OFF: case CHOOSER: return Summary.OTHER_LAUNCHER;
            default: return Summary.ATTENTION;
        }
    }

    /** The helper sends a launcher back only while the person wants it and there is one to send back. */
    static String redirectTarget(boolean wanted, HomeTarget.Result home) {
        return wanted && home.target != null ? home.target : "";
    }

    /**
     * Whether the helper filters remote keys. Filtering any key stops held keys repeating on Google TV, in every
     * app, so the redirect, which needs only window changes, does not ask for it: keys are filtered when key_filter
     * is on and either nothing is being redirected (the helper is there for remote buttons) or a remote-button
     * feature is configured.
     */
    static boolean filterKeys(boolean keyFilter, boolean redirecting, boolean keyFeatures) {
        return keyFilter && (!redirecting || keyFeatures);
    }

    /**
     * Whether Home's resume, boot or an update puts the helper back without asking: only in the clear cases, where
     * Android took it off the list (a force stop does) or turned the accessibility switch off. A helper still listed
     * but not bound may simply be starting, so that case waits for the person's "Start it again".
     * @param enabledSwitch Settings.Secure accessibility_enabled; -1 when unreadable or unset
     */
    static boolean repairAutomatically(boolean wanted, HomeTarget.Result home, boolean inList, int enabledSwitch, boolean mayWrite, boolean running) {
        return wanted && home.target != null && mayWrite && !running && (!inList || enabledSwitch == 0);
    }

    /** The one notice on Home when Android dropped the helper and Homeboard cannot put it back itself. */
    static boolean droppedNotice(boolean wanted, HomeTarget.Result home, boolean inList, boolean mayWrite, boolean running) {
        return wanted && home.target != null && !inList && !mayWrite && !running;
    }

    /**
     * No lock-out: the helper sends the launcher's home screen back to Homeboard, so if Homeboard fails to stay up
     * the two could bounce forever. More than LIMIT redirects within WINDOW_MS with no key pressed in between stops
     * the redirects until the next key, and the TV's own launcher stays usable.
     */
    static final class RedirectGuard {
        static final int LIMIT = 3;
        static final long WINDOW_MS = 10_000L;
        private final ArrayDeque<Long> recent = new ArrayDeque<>();

        /** A redirect would happen at now (monotonic ms): true to do it. */
        boolean allow(long now) {
            while (!recent.isEmpty() && now - recent.peekFirst() >= WINDOW_MS) recent.pollFirst();
            if (recent.size() >= LIMIT) return false;
            recent.addLast(now);
            return true;
        }

        /** A key press is a person at the remote: the count starts again. */
        void keyPressed() { recent.clear(); }
    }
}
