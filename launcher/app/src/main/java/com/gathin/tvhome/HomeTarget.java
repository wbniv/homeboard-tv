package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the remote's Home button opens on this TV, decided from what PackageManager reports, without Android calls so
 * it is unit tested (docs/plans/2026-11-20-home-button.md). Found at runtime, so one public APK works on any TV.
 *
 * Some launchers keep Home whatever app holds Android's Home role: Google TV's launcherx does (upstream's TCL notes,
 * and Stage 0 of the control interface plan on Cast1 and Cast2). Those are named in OUTRANKING, and any other system
 * launcher whose HOME filter has a positive priority is treated the same way, since Android honours filter priority
 * only for system apps. When one is installed and enabled, it is the target: the helper sends its home screen back
 * here. Otherwise Android's own choice decides, and the role is the better route.
 */
final class HomeTarget {
    /** Launchers known to take Home regardless of the role, best first. */
    static final List<String> OUTRANKING = Collections.singletonList("com.google.android.apps.tv.launcherx");
    /** HOME activities that are not launchers: the system chooser, and Settings' FallbackHome used during boot. */
    static final Set<String> NOT_LAUNCHERS = new HashSet<>(Arrays.asList("android", "com.android.settings", "com.android.tv.settings"));

    /** One enabled HOME activity, as queryIntentActivities reports it. */
    static final class Candidate {
        final String packageName;
        final int priority;
        final boolean system;
        Candidate(String packageName, int priority, boolean system) {
            this.packageName = packageName; this.priority = priority; this.system = system;
        }
    }

    static final class Result {
        /** The launcher whose home screen the helper sends back to Homeboard; null when there is none to send back. */
        final String target;
        /** Android already opens Homeboard for Home, and nothing outranks it. */
        final boolean homeboardIsHome;
        /** No installed launcher outranks Android's Home choice, so choosing Homeboard as the Home app would work. */
        final boolean roleHonoured;
        Result(String target, boolean homeboardIsHome, boolean roleHonoured) {
            this.target = target; this.homeboardIsHome = homeboardIsHome; this.roleHonoured = roleHonoured;
        }
    }

    private HomeTarget() { }

    /**
     * @param self this install's package (debug and release differ)
     * @param resolved the package of resolveActivity(HOME, MATCH_DEFAULT_ONLY): Android's choice, "android" for the chooser, or null
     * @param candidates every enabled HOME activity
     */
    static Result choose(String self, String resolved, List<Candidate> candidates) {
        Map<String, Candidate> real = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            if (c == null || c.packageName == null || c.packageName.equals(self) || NOT_LAUNCHERS.contains(c.packageName)) continue;
            Candidate seen = real.get(c.packageName);
            // A package with several HOME activities counts once, with its strongest filter.
            if (seen == null || c.priority > seen.priority) real.put(c.packageName, c);
        }
        List<Candidate> outranking = new ArrayList<>();
        for (Candidate c : real.values()) if (OUTRANKING.contains(c.packageName) || (c.system && c.priority > 0)) outranking.add(c);
        if (!outranking.isEmpty()) {
            for (Candidate c : outranking) if (c.packageName.equals(resolved)) return new Result(resolved, false, false);
            return new Result(best(outranking).packageName, false, false);
        }
        if (self.equals(resolved)) return new Result(null, true, true);
        if (resolved != null && real.containsKey(resolved)) return new Result(resolved, false, true);
        // The chooser, or nothing: no single launcher to send back. Android asks the person, who can choose Homeboard.
        return new Result(null, false, true);
    }

    /** Known outranking launchers first, in OUTRANKING order; then higher priority; then by name, so the choice is stable. */
    private static Candidate best(List<Candidate> list) {
        Candidate best = null;
        for (Candidate c : list) if (best == null || better(c, best)) best = c;
        return best;
    }

    private static boolean better(Candidate a, Candidate b) {
        int ka = known(a), kb = known(b);
        if (ka != kb) return ka < kb;
        if (a.priority != b.priority) return a.priority > b.priority;
        return a.packageName.compareTo(b.packageName) < 0;
    }

    private static int known(Candidate c) {
        int index = OUTRANKING.indexOf(c.packageName);
        return index < 0 ? Integer.MAX_VALUE : index;
    }
}
