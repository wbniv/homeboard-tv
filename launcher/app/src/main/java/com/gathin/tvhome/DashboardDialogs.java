package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

/** Keeps dashboard dialogs on the same rendering display as their owning content. */
final class DashboardDialogs {
    private DashboardDialogs() { }
    static int width(Activity activity) {
        return activity instanceof MainActivity ? ((MainActivity)activity).dashboardWidthPixels()
                : activity.getResources().getDisplayMetrics().widthPixels;
    }
    static int height(Activity activity) {
        return activity instanceof MainActivity ? ((MainActivity)activity).dashboardHeightPixels()
                : activity.getResources().getDisplayMetrics().heightPixels;
    }
    static Context context(Activity activity) {
        return activity instanceof MainActivity ? ((MainActivity)activity).dashboardDialogContext() : activity;
    }
    static void configure(Activity activity, Dialog dialog) {
        if (activity instanceof MainActivity) ((MainActivity)activity).configureDashboardDialog(dialog);
    }
}
