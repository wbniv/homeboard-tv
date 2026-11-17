package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/** Real Home resize and rendering, with an isolated layout and unchanged household settings. */
public final class CalendarResizeProbeActivity extends MainActivity {
    private final Handler runner = new Handler();
    private TextView report;
    private boolean failed;
    @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        if ("widget-home".equals(name)) name = "calendar-one-row-review-" + name;
        return super.getSharedPreferences(name, mode);
    }
    @Override protected void onCreate(Bundle state) {
        HomeLayout seed = new HomeLayout(); seed.homeColumns = 12; seed.homeRows = 3;
        seed.favoritesContainer = true; seed.reducedMotion = true;
        seed.addPanel(HomeLayout.CALENDAR); seed.setPanelSpan(HomeLayout.CALENDAR, 6, 2);
        getSharedPreferences("widget-home", 0).edit().clear().putString("layout", seed.encode()).putBoolean("single-screen-default-v1",true)
                .putString("nav_focus", HomeLayout.CALENDAR)
                .putString("nav_home_focus", HomeLayout.CALENDAR).apply();
        super.onCreate(state);
        report = new TextView(this); report.setTextColor(0xffffffff); report.setBackgroundColor(0xff102333); report.setTextSize(12);
        addContentView(report, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        step(2_000, () -> { call("startResize", new Class<?>[]{String.class}, HomeLayout.CALENDAR); press(KeyEvent.KEYCODE_DPAD_UP); check("Up reaches one row", 6, 1); });
        step(6_000, () -> { press(KeyEvent.KEYCODE_DPAD_CENTER); persistedOneRow(); check("Saved 6x1", 6, 1); });
        step(10_000, () -> { calendar().turnPage(1); check("Next date page", 6, 1); });
        step(14_000, () -> { call("startResize", new Class<?>[]{String.class}, HomeLayout.CALENDAR); press(KeyEvent.KEYCODE_DPAD_DOWN); press(KeyEvent.KEYCODE_BACK); persistedOneRow(); check("Cancel restores one row", 6, 1); });
        step(18_000, () -> size(2, 1, "Minimum 2x1"));
        step(23_000, () -> size(12, 1, "Wide 12x1"));
        step(28_000, () -> size(6, 2, "Normal 6x2"));
        step(33_000, () -> { size(6, 1, "Final 6x1"); call("saveWithoutUndo", new Class<?>[]{}); persistedOneRow(); });
    }
    private interface Stage { void run() throws Exception; }
    private void step(long delay, Stage stage) { runner.postDelayed(() -> {
        if (failed) return;
        try { stage.run(); } catch (Exception error) { failed = true; report.setText("FAIL · " + error); }
    }, delay); }
    private void press(int key) {
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
    }
    private Object field(String name) throws Exception {
        Field field = MainActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(this);
    }
    private Object call(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = MainActivity.class.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(this, args);
    }
    private HomeLayout layout() throws Exception { return (HomeLayout) field("layout"); }
    @SuppressWarnings("unchecked") private CalendarWidgetView calendar() throws Exception {
        return ((Map<String, CalendarWidgetView>) field("calendarWidgets")).get(HomeLayout.CALENDAR);
    }
    private void persistedOneRow() throws Exception {
        HomeLayout saved = HomeLayout.parse(getSharedPreferences("widget-home", 0).getString("layout", ""));
        if (saved.panelSpan(HomeLayout.CALENDAR).height != 1) throw new IllegalStateException("Saved height is not one row");
    }
    private void size(int width, int height, String stage) throws Exception {
        layout().setPanelSpan(HomeLayout.CALENDAR, width, height);
        call("render", new Class<?>[]{int.class}, 0); check(stage, width, height);
    }
    private void check(String stage, int width, int height) throws Exception {
        HomeLayout.Cell cell = layout().cells(layout().widgets, 12, 3).get(0);
        if (cell.width != width || cell.height != height) throw new IllegalStateException("Incorrect calendar span");
        report.setText("PASS · " + stage + " · isolated layout · " + calendar().pages() + " date pages");
    }
    @Override protected void onDestroy() {
        runner.removeCallbacksAndMessages(null); super.onDestroy();
        getSharedPreferences("widget-home", 0).edit().clear().apply();
    }
}
