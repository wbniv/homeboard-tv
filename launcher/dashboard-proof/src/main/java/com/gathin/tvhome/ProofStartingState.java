package com.gathin.tvhome;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

/** Deterministic navigation only for explicit acceptance aliases; normal launches retain focus. */
final class ProofStartingState {
    static void prepare(Activity activity, Bundle state) {
        if (state != null) return;
        try {
            Bundle metadata = activity.getPackageManager().getActivityInfo(activity.getComponentName(), PackageManager.GET_META_DATA).metaData;
            if (metadata == null || !metadata.getBoolean("dashboard-start-home", false)) return;
            activity.getSharedPreferences("widget-home", Activity.MODE_PRIVATE).edit()
                    .putBoolean("nav_all_apps", false).putInt("nav_home_page", 0).putInt("nav_apps_page", 0)
                    .putString("nav_focus", HomeLayout.RAIN).putString("nav_home_focus", HomeLayout.RAIN)
                    .putString("nav_apps_focus", "").apply();
        } catch (PackageManager.NameNotFoundException error) {
            throw new IllegalStateException("Missing proof activity metadata", error);
        }
    }
    private ProofStartingState() { }
}
