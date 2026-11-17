package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/** Real Android provider + Rain controller with isolated layout and weather preferences. */
public final class RainAutoProbeActivity extends MainActivity {
    private final Handler runner = new Handler();
    private TextView report;
    private String stage = "Automatic · 4×3";
    @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        if (name.equals("widget-home") || name.startsWith("rain-cache-") || name.startsWith("air-quality-widget-")) name = "rain-auto-review-" + name;
        return super.getSharedPreferences(name, mode);
    }
    @Override protected void onCreate(Bundle state) {
        HomeLayout seed = new HomeLayout(); seed.addPanel(HomeLayout.RAIN);
        getSharedPreferences("widget-home", 0).edit().clear().putString("layout", seed.encode()).putBoolean("single-screen-default-v1",true).apply();
        super.onCreate(state);
        report = new TextView(this); report.setTextSize(15); report.setTextColor(0xffffffff); report.setBackgroundColor(0xff102333);
        addContentView(report, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        runner.postDelayed(this::report, 17_000);
        runner.postDelayed(() -> size(1, 1, "Automatic · 1×1"), 23_000);
        runner.postDelayed(() -> size(12, 12, "Automatic · Fill Home"), 30_000);
        runner.postDelayed(() -> {
            try {
                RainSettings fixed = controller().settings().copy(); fixed.automatic = false;
                fixed.latitude = 40.416; fixed.longitude = -3.704; fixed.name = "Madrid test place";
                controller().changed(fixed); stage = "Fixed override · Madrid"; report();
            } catch (Exception e) { fail(e); }
        }, 37_000);
        runner.postDelayed(() -> {
            try { RainSettings automatic = controller().settings().copy(); automatic.automatic = true; controller().changed(automatic); stage = "Back to automatic"; report(); }
            catch (Exception e) { fail(e); }
        }, 45_000);
        runner.postDelayed(() -> size(4, 3, "Restored · 4×3"), 53_000);
        runner.postDelayed(this::report, 61_000);
    }
    private Object field(String name) throws Exception { Field f = MainActivity.class.getDeclaredField(name); f.setAccessible(true); return f.get(this); }
    @SuppressWarnings("unchecked") private RainWidgetController controller() throws Exception { return ((Map<String, RainWidgetController>) field("rainWidgets")).get(HomeLayout.RAIN); }
    private void size(int columns, int rows, String title) {
        try {
            ((HomeLayout) field("layout")).setPanelSpan(HomeLayout.RAIN, columns, rows);
            Method render = MainActivity.class.getDeclaredMethod("render", int.class); render.setAccessible(true); render.invoke(this, 0);
            stage = title; report();
        } catch (Exception e) { fail(e); }
    }
    private void report() {
        try {
            RainWidgetController c = controller(); RainSettings s = c.settings();
            report.setText(stage + " · " + (s.automatic ? "DEVICE" : "FIXED") + " · " + (s.located() ? "fix available" : "no fix")
                    + " · " + (c.forecast == null ? c.status() : "forecast loaded · " + c.status()) + " · isolated settings");
        } catch (Exception e) { fail(e); }
    }
    private void fail(Exception e) { report.setText("FAIL · " + e); }
    @Override protected void onDestroy() { runner.removeCallbacksAndMessages(null); super.onDestroy(); }
}
