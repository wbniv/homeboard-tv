package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Device acceptance for plan docs/plans/2026-11-20-screensavers.md, Verification 7: starts a short test timer,
 * puts the screensaver up, and checks that the ringing timer ends it and opens the timer screen. Runs only when
 * no household timers exist, removes its own timer, and writes wf.log for the coordinator's evidence.
 */
public final class ScreensaverAlarmProbeActivity extends Activity {
    private static final long RING_AFTER_MS = 12_000, CHECK_AFTER_MS = 30_000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView text;
    private String before;
    private boolean started, ownsRuntime;
    private long baseline;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        text = new TextView(this);
        text.setTextColor(-1); text.setTextSize(24); text.setPadding(40, 40, 40, 40);
        text.setBackgroundColor(0xff101a23);
        setContentView(text);
    }

    @Override protected void onResume() {
        super.onResume();
        if (started) return;   // returning from the screensaver must not start another run
        started = true;
        try {
            if (!KitchenTimerStore.allowed(this)) { blocked("Alarms & reminders access is required; no test timer was started."); return; }
            if (KitchenTimerStore.load(this).items().length() != 0) { blocked("Household timers exist and are preserved; nothing was started."); return; }
            SharedPreferences p = getSharedPreferences("kitchen-timers", 0);
            before = p.getString("runtime", null);
            baseline = p.getLong("last-sound-started", 0);
            ownsRuntime = true;
            KitchenTimerStore.command(this, KitchenTimerUi.command(this, "create").put("kind", "timer")
                    .put("name", "Homeboard screensaver test").put("duration", RING_AFTER_MS));
            text.setText("Screensaver up; the test timer rings in 12 s.");
            handler.postDelayed(() -> startActivity(ScreensaverActivity.intent(this, ScreensaverActivity.MODE_NOW, null)), 1500);
            handler.postDelayed(this::finishRun, CHECK_AFTER_MS);
        } catch (Exception e) { report("fail", e.toString()); restore(); }
    }

    private void finishRun() {
        try {
            KitchenTimers model = KitchenTimerStore.load(this);
            boolean done = model.items().length() > 0 && "done".equals(model.items().getJSONObject(0).getString("state"));
            boolean sounded = getSharedPreferences("kitchen-timers", 0).getLong("last-sound-started", 0) > baseline;
            report(done && sounded ? "pass" : "fail", "timer done=" + done + ", alarm sound started=" + sounded
                    + ". The log line `screensaver stopped ... reason=alarm` and the timer-screen capture are the evidence.");
        } catch (Exception e) { report("fail", e.toString()); }
        KitchenTimerStore.silence(this);
        restore();
        finish();
    }

    private void blocked(String why) { text.setText(why); report("blocked", why); }

    private void restore() {
        if (!ownsRuntime) return;
        ownsRuntime = false;
        SharedPreferences.Editor edit = getSharedPreferences("kitchen-timers", 0).edit();
        if (before == null) edit.remove("runtime"); else edit.putString("runtime", before);
        edit.commit();
        before = null;
        KitchenTimerStore.reconcile(this);
    }

    private void report(String status, String detail) {
        try {
            Files.write(new File(getExternalFilesDir(null), "wf.log").toPath(), new JSONObject().put("status", status)
                    .put("detail", detail).put("homeboardVersion", getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
                    .toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (ownsRuntime) { KitchenTimerStore.silence(this); restore(); }
        super.onDestroy();
    }
}
