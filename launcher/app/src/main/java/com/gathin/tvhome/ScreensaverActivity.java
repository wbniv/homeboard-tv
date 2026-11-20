package com.gathin.tvhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Homeboard's own screensaver screen: Home idle (MainActivity's timer), Preview (from the Screensaver screen)
 * and Show now (the remote's screensaver action where Android has no "start now" shortcut). It plays the chosen
 * ScreensaverRegistry entry, or the previewed one, through the same ScreensaverScene contract as ScreensaverDream.
 * It never starts over live TV or an app, which matters on TVs whose Live TV window does not keep the screen on.
 *
 * Idle and Show now: any key ends it, and that key does not also act on Home. Preview: OK uses the entry
 * (RESULT_OK), Back cancels and nothing is saved. A ringing timer or alarm (KitchenTimerSound) always ends it.
 * Logs "screensaver started" and "screensaver stopped" under TAG so device checks wait on an event.
 */
public class ScreensaverActivity extends Activity {
    static final String TAG = "HomeboardScreensaver";
    static final String EXTRA_MODE = "mode", EXTRA_ENTRY = "entry";
    static final String MODE_IDLE = "idle", MODE_PREVIEW = "preview", MODE_NOW = "now";
    /** Sent within the package by KitchenTimerSound when a timer or alarm starts ringing. */
    static final String ACTION_END = "com.gathin.tvhome.action.END_SCREENSAVER";

    private ScreensaverScene scene;
    private String mode, entry;
    private boolean started;
    private String stopReason = "left";
    private final BroadcastReceiver alarm = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { end("alarm"); }
    };

    /** The saved Home layout, or defaults when there is none or it cannot be read; the screensaver never writes it. */
    static HomeLayout savedLayout(Context context) {
        String encoded = context.getSharedPreferences("widget-home", MODE_PRIVATE).getString("layout", null);
        if (encoded != null) try { return HomeLayout.parse(encoded); } catch (Exception ignored) { }
        return new HomeLayout();
    }

    static Intent intent(Context context, String mode, String entry) {
        Intent intent = new Intent(context, ScreensaverActivity.class).putExtra(EXTRA_MODE, mode);
        return entry == null ? intent : intent.putExtra(EXTRA_ENTRY, entry);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppLanguage.wrap(base));   // locale
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!MODE_IDLE.equals(mode) && !MODE_PREVIEW.equals(mode)) mode = MODE_NOW;
        HomeLayout layout = savedLayout(this);
        String requested = getIntent().getStringExtra(EXTRA_ENTRY);
        entry = ScreensaverRegistry.known(requested != null ? requested : layout.screensaver.choice);
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(0xFF000000);
        setContentView(container);
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        scene = ScreensaverRegistry.entry(entry).scene.create(this, container, layout);
        ClockFormat.apply(decor, Config.load(this).clockFormat);   // the time format follows Home
        if (MODE_PREVIEW.equals(mode)) {
            TextView hint = new TextView(this);
            hint.setText(getString(R.string.hb_ss_preview_hint));
            hint.setTextSize(18); hint.setTextColor(0xE6FFFFFF); hint.setShadowLayer(6, 0, 0, 0xFF000000);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.bottomMargin = Math.round(36 * getResources().getDisplayMetrics().density);
            container.addView(hint, lp);
            hint.animate().alpha(0f).setStartDelay(5000).setDuration(1200).start();   // a deliberate fade, not a wait
        }
        androidx.core.content.ContextCompat.registerReceiver(this, alarm, new IntentFilter(ACTION_END),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scene.start();
        if (!started) {
            started = true;
            Log.i(TAG, "screensaver started mode=" + mode + " entry=" + entry);
        }
    }

    @Override
    protected void onPause() {
        scene.stop();
        super.onPause();
        finish();   // never left behind in the back stack; a preview left this way saves nothing
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(alarm);
        scene.release();
        if (started) Log.i(TAG, "screensaver stopped mode=" + mode + " entry=" + entry + " reason=" + stopReason);
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // The key that ends the screensaver must not also act on Home, so every key is consumed here.
        if (event.getAction() != KeyEvent.ACTION_UP) return true;
        int key = event.getKeyCode();
        if (!MODE_PREVIEW.equals(mode)) { end("key"); return true; }
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_ENTRY, entry));
            end("use");
        } else if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
            end("cancel");
        }
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (!MODE_PREVIEW.equals(mode)) end("input");
        return true;
    }

    private void end(String reason) {
        if (isFinishing()) return;
        stopReason = reason;
        finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
