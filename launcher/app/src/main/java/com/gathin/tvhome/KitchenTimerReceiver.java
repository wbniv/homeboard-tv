package com.gathin.tvhome;
import android.content.*;
/** Exact delivery, same-boot APK replacement, reboot and wall-clock corrections. */
public final class KitchenTimerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){KitchenTimerStore.reconcile(context);}
}
