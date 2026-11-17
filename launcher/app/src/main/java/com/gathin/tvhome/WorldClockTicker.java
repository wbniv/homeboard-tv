package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;

/** Visible views only: realign after manual clock changes and stop every callback on pause. */
final class WorldClockTicker {
    private final Context context;
    private final Runnable refresh;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean running,seconds;
    private final BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent intent) { tick(); }
    };
    private final Runnable callback=this::tick;
    WorldClockTicker(Context context,Runnable refresh) { this.context=context; this.refresh=refresh; }
    void start(boolean seconds) {
        this.seconds=seconds;
        if(!running) {
            running=true;
            IntentFilter f=new IntentFilter(); f.addAction(Intent.ACTION_TIME_CHANGED); f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            f.addAction(Intent.ACTION_DATE_CHANGED); f.addAction(Intent.ACTION_LOCALE_CHANGED);
            ContextCompat.registerReceiver(context,receiver,f,ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        tick();
    }
    private void tick() {
        handler.removeCallbacks(callback); if(!running) return;
        refresh.run(); long interval=seconds?1000:60000;
        handler.postDelayed(callback,interval-Math.floorMod(System.currentTimeMillis(),interval)+15);
    }
    void stop() {
        handler.removeCallbacks(callback); if(!running) return;
        running=false; context.unregisterReceiver(receiver);
    }
}
