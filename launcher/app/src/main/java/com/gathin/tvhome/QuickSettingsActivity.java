package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

/** Four-way Quick Settings pie opened by the remote's Settings button. */
public final class QuickSettingsActivity extends Activity {
    private PieMenuView pie;
    private PaletteColors palette;
    private final String[] labels = {"TV settings", "Picture", "Network", "Apps"};
    private final MenuIconView.Kind[] icons = {MenuIconView.Kind.SETTINGS, MenuIconView.Kind.APPEARANCE,
            MenuIconView.Kind.LOCATION, MenuIconView.Kind.APP};

    @Override protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(AppLanguage.wrap(base));
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        int background = MainActivity.paletteColorForId(savedPalette());
        palette = PaletteColors.from(background);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.argb(148, 0, 0, 0));
        root.setOnClickListener(v -> finish());
        pie = new PieMenuView(this, palette);
        pie.setContent("Quick settings", "DEVICE", "Homeboard", "Directions choose · Back returns",
                labels, icons, new boolean[4], new PieMenuView.Listener() {
                    @Override public void onArm(int arm) { openChoice(arm); }
                    @Override public void onCenter() { openHomeboardSettings(); }
                });
        float density = getResources().getDisplayMetrics().density;
        int availableHeight = getResources().getDisplayMetrics().heightPixels - Math.round(130 * density);
        int availableWidth = getResources().getDisplayMetrics().widthPixels - Math.round(96 * density);
        int diameter = Math.max(Math.round(380 * density), Math.min(availableHeight, availableWidth));
        diameter = Math.min(diameter, Math.round(650 * density));
        FrameLayout.LayoutParams pieLp = new FrameLayout.LayoutParams(diameter, diameter,
                Gravity.TOP | Gravity.RIGHT);
        pieLp.topMargin = Math.round(48 * density);
        pieLp.rightMargin = Math.round(48 * density);
        root.addView(pie, pieLp);
        setContentView(root);
        pie.requestFocus();
    }

    private String savedPalette() {
        SharedPreferences preferences = getSharedPreferences("widget-home", MODE_PRIVATE);
        String encoded = preferences.getString("layout", null);
        if (encoded == null) return "midnight";
        try { return HomeLayout.parse(encoded).palette; }
        catch (Exception ignored) { return "midnight"; }
    }

    private void openChoice(int choice) {
        Intent target;
        switch (choice) {
            case 0: target = SystemSettings.intent(this, Settings.ACTION_SETTINGS); break;
            case 1: target = SystemSettings.displayIntent(this); break;
            case 2: target = SystemSettings.intent(this, Settings.ACTION_WIFI_SETTINGS); break;
            case 3: target = SystemSettings.intent(this, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS); break;
            default: return;
        }
        try { startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (RuntimeException ignored) { /* Keep the quick menu open if the TV has no matching page. */ return; }
        finish();
    }

    private void openHomeboardSettings() {
        startActivity(new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_UP) finish();
            return true;
        }
        int arm = key == KeyEvent.KEYCODE_DPAD_UP ? 0 : key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1
                : key == KeyEvent.KEYCODE_DPAD_DOWN ? 2 : key == KeyEvent.KEYCODE_DPAD_LEFT ? 3 : -1;
        if (arm >= 0) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) pie.setHighlighted(arm);
            else if (!event.isCanceled()) openChoice(arm);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_UP) openHomeboardSettings();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }
}
