package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * After a restart or an update of Homeboard, puts the Home button helper back if the person turned the setting on
 * and Android dropped the helper (HomeButton.repair; only with the one-time setup, and only in the clear cases).
 */
public final class HomeButtonReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        HomeButton.repair(context);
    }
}
