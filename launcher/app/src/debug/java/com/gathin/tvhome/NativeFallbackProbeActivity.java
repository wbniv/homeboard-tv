package com.gathin.tvhome;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONObject;

/** Inject an attachment failure, then observe actual recreation and retained layout. */
public final class NativeFallbackProbeActivity extends MainActivity {
    private static boolean injected;
    private static String originalLayout;
    private final Handler probe = new Handler(Looper.getMainLooper());
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (originalLayout == null) originalLayout = getSharedPreferences("widget-home", 0).getString("layout", "");
    }
    @Override protected void onResume() {
        super.onResume();
        probe.postDelayed(() -> {
            try {
                if (!injected) {
                    int[] surface = dashboardRenderingSurface();
                    if (surface == null || surface[0] != 3840 || surface[1] != 2160)
                        throw new IllegalStateException("Native surface was not active before injection");
                    java.lang.reflect.Field field = NativeDashboardActivity.class.getDeclaredField("nativeHost");
                    field.setAccessible(true);
                    Object host = field.get(this);
                    java.lang.reflect.Method fail = NativeDashboardHost.class.getDeclaredMethod("fail", Throwable.class);
                    fail.setAccessible(true); injected = true;
                    fail.invoke(host, new IllegalStateException("Debug acceptance attachment failure"));
                } else {
                    if (dashboardRenderingSurface() != null) throw new IllegalStateException("Fallback retried native rendering");
                    if (!originalLayout.equals(getSharedPreferences("widget-home", 0).getString("layout", "")))
                        throw new IllegalStateException("Fallback changed saved widget layout");
                    JSONObject result = new JSONObject().put("status", "pass").put("nativeBefore", "3840x2160")
                            .put("ordinaryWidth", getResources().getDisplayMetrics().widthPixels)
                            .put("ordinaryHeight", getResources().getDisplayMetrics().heightPixels)
                            .put("layoutUnchanged", true).put("failure", "injected attachment failure, not actual OOM");
                    report(result);
                }
            } catch (Exception failure) {
                try { report(new JSONObject().put("status", "fail").put("detail", failure.toString())); }
                catch (Exception ignored) { Log.e("HomeboardFallbackProbe", "Report failed", failure); }
            }
        }, 2500);
    }
    private void report(JSONObject result) throws Exception {
        Log.i("HomeboardFallbackProbe", result.toString());
        Files.write(new File(getExternalFilesDir(null), "wf.log").toPath(), result.toString().getBytes(StandardCharsets.UTF_8));
    }
    @Override protected void onPause() { probe.removeCallbacksAndMessages(null); super.onPause(); }
}
