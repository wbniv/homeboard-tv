package com.gathin.tvhome;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Home settings → Home button: the person's choice, and the Android side of it (docs/plans/2026-11-20-home-button.md).
 *
 * On Google TV the Home role does not move the Home key, so the route is HomeKeyService, Homeboard's accessibility
 * helper: with the setting on, a short press of Home opens Homeboard and the TV's own launcher's home screen is sent
 * back here, while that launcher stays installed and enabled and a held press still opens its quick settings.
 *
 * The choice is saved here, not in the layout. When Homeboard holds WRITE_SECURE_SETTINGS (the one-time setup the
 * screensaver's Use everywhere also uses), turning it on adds only Homeboard's helper to Android's enabled list and
 * turning it off removes only that entry, and only if Homeboard added it. Every call is caught: a refusal is reported,
 * never thrown. Nothing here disables, hides or changes another package.
 */
final class HomeButton {
    private static final String PREFS = "home-button";
    private static final String WANTED = "wanted", ADDED = "added-helper", SWITCH_BEFORE = "switch-before";

    /** What set() did on Android's side. The screen still reads the result back through state(). */
    enum Outcome { SAVED, WROTE, REFUSED, SAVE_FAILED }

    private HomeButton() { }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean wanted(Context context) {
        return prefs(context).getBoolean(WANTED, false);
    }

    /** This install's helper, as Android writes it in the enabled list (debug and release packages differ). */
    static String component(Context context) {
        return new ComponentName(context, HomeKeyService.class).flattenToShortString();
    }

    /** What Home opens on this TV, from PackageManager. Needs the HOME query in the manifest's queries. */
    static HomeTarget.Result home(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        String resolved = null;
        List<HomeTarget.Candidate> candidates = new ArrayList<>();
        try {
            ResolveInfo chosen = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            if (chosen != null && chosen.activityInfo != null) resolved = chosen.activityInfo.packageName;
            for (ResolveInfo ri : pm.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)) {
                if (ri.activityInfo == null) continue;
                ApplicationInfo app = ri.activityInfo.applicationInfo;
                boolean system = app != null && (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                candidates.add(new HomeTarget.Candidate(ri.activityInfo.packageName, ri.priority, system));
            }
        } catch (RuntimeException unavailable) {
            // PackageManager failed: no candidates, so no redirect.
        }
        return HomeTarget.choose(context.getPackageName(), resolved, candidates);
    }

    /** The launcher HomeKeyService sends back here; empty when the setting is off or there is none. */
    static String redirectTarget(Context context) {
        return HomeButtonRules.redirectTarget(wanted(context), home(context));
    }

    static String rawList(Context context) {
        try { return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES); }
        catch (RuntimeException unreadable) { return null; }
    }

    /** Settings.Secure accessibility_enabled, or -1 when unset or unreadable. */
    static int enabledSwitch(Context context) {
        try { return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, -1); }
        catch (RuntimeException unreadable) { return -1; }
    }

    static boolean inList(Context context) {
        return AccessibilityList.contains(rawList(context), component(context));
    }

    static boolean running() {
        return HomeKeyService.running != null;
    }

    static boolean mayWrite(Context context) {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    static HomeButtonRules.Screen screen(Context context, HomeTarget.Result home, boolean waiting) {
        boolean listed = inList(context) && enabledSwitch(context) != 0;
        return HomeButtonRules.screen(home, wanted(context), listed, running(), mayWrite(context), waiting);
    }

    /**
     * Saves the choice, then turns the helper on or off when Homeboard may, and tells a running helper to reload.
     * Turning off removes the helper only if this setting added it: one the household turned on for other remote
     * buttons stays on, and only stops redirecting.
     */
    static Outcome set(Context context, boolean on) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.edit().putBoolean(WANTED, on).commit()) return Outcome.SAVE_FAILED;
        Outcome outcome = Outcome.SAVED;
        if (mayWrite(context)) {
            try {
                ContentResolver cr = context.getContentResolver();
                String raw = rawList(context), mine = component(context);
                int before = enabledSwitch(context);
                if (on && !AccessibilityList.contains(raw, mine)) {
                    if (Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, AccessibilityList.with(raw, mine))) {
                        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                        prefs.edit().putBoolean(ADDED, true).putInt(SWITCH_BEFORE, before).commit();
                        outcome = Outcome.WROTE;
                    } else outcome = Outcome.REFUSED;
                } else if (on && before == 0) {
                    outcome = Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1) ? Outcome.WROTE : Outcome.REFUSED;
                } else if (!on && prefs.getBoolean(ADDED, false)) {
                    String rest = AccessibilityList.without(raw, mine);
                    if (Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, rest)) {
                        // Put the switch back only when nothing else is left and it was off before Homeboard turned it on.
                        if (rest.isEmpty() && prefs.getInt(SWITCH_BEFORE, -1) == 0)
                            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0);
                        prefs.edit().remove(ADDED).remove(SWITCH_BEFORE).commit();
                        outcome = Outcome.WROTE;
                    } else outcome = Outcome.REFUSED;
                }
            } catch (RuntimeException refused) {
                outcome = Outcome.REFUSED;
            }
        }
        HomeKeyService.reload();
        return outcome;
    }

    /** "Start it again": the add, remove, add cycle that also clears a crashed mark. Needs the one-time setup. */
    static void restart(Context context) {
        ServiceRepair.cycle(context, component(context));
    }

    /** At Home's resume, boot and after an update: puts the helper back in the clear cases (HomeButtonRules.repairAutomatically). */
    static void repair(Context context) {
        try {
            if (!wanted(context)) return;
            HomeTarget.Result home = home(context);
            if (HomeButtonRules.repairAutomatically(true, home, inList(context), enabledSwitch(context), mayWrite(context), running()))
                restart(context);
        } catch (RuntimeException ignored) {
            // The settings are the system's; a refusal leaves the helper as it was.
        }
    }

    /**
     * The household turned the helper off in Android's own settings (HomeKeyService.onUnbind outside standby). The
     * setting follows, so Home does not put it back against their choice; Home settings then shows it off.
     */
    static void helperTurnedOff(Context context) {
        prefs(context).edit().putBoolean(WANTED, false).remove(ADDED).remove(SWITCH_BEFORE).apply();
    }
}
