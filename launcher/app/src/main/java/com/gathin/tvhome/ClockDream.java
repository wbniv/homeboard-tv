package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.dreams.DreamService;
import android.util.Log;
import android.widget.FrameLayout;

/**
 * Homeboard's Android screensaver: plays the screensaver chosen in Home settings → Screensaver through the
 * same ScreensaverScene contract as ScreensaverActivity. Its component name stays com.gathin.tvhome/.ClockDream
 * so a TV whose Android setting already names it keeps working. A ringing timer or alarm ends it.
 */
public class ClockDream extends DreamService {

    private ScreensaverScene scene;
    private String entry;
    private boolean receiving, started;
    private final BroadcastReceiver alarm = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { finish(); }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppLanguage.wrap(base));   // the theme's locale, as ScreensaverActivity
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        HomeLayout layout = ScreensaverActivity.savedLayout(this);
        entry = ScreensaverRegistry.known(layout.screensaver.choice);
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(0xFF000000);
        setContentView(container);
        scene = ScreensaverRegistry.entry(entry).scene.create(this, container, layout);
        ClockFormat.apply(getWindow().getDecorView(), Config.load(this).clockFormat);
        androidx.core.content.ContextCompat.registerReceiver(this, alarm, new IntentFilter(ScreensaverActivity.ACTION_END),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
        receiving = true;
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        scene.start();
        if (!started) Log.i(ScreensaverActivity.TAG, "screensaver started mode=dream entry=" + entry);
        started = true;
    }

    @Override
    public void onDreamingStopped() {
        scene.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (receiving) { unregisterReceiver(alarm); receiving = false; }
        if (scene != null) { scene.release(); scene = null; }
        if (started) Log.i(ScreensaverActivity.TAG, "screensaver stopped mode=dream entry=" + entry);
        started = false;
        super.onDetachedFromWindow();
    }
}
