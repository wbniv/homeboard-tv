package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper;

/** Draw the real Home and All Apps at each opacity; never save a layout or opacity draft. */
public final class UiBackgroundProbeActivity extends MainActivity {
    private final Handler probe = new Handler();
    private final Bitmap[] screens = new Bitmap[6];
    private final int[] levels = {0, 75, 100};
    private int stage, wallpaperPixel;
    private FrameLayout actualRoot;
    private Method apply;
    private String originalLayout;

    private Object field(String name) throws Exception {
        Field f = MainActivity.class.getDeclaredField(name); f.setAccessible(true); return f.get(this);
    }
    private void field(String name, Object value) throws Exception {
        Field f = MainActivity.class.getDeclaredField(name); f.setAccessible(true); f.set(this, value);
    }
    @Override protected void onCreate(Bundle state) {
        originalLayout = getSharedPreferences("widget-home", MODE_PRIVATE).getString("layout", null);
        super.onCreate(state);
        probe.postDelayed(() -> {
            try {
                actualRoot = (FrameLayout) field("root");
                apply = MainActivity.class.getDeclaredMethod("applyUiBackgroundOpacity", int.class); apply.setAccessible(true);
                ((ProjectivyWallpaperHost) field("wallpaperHost")).visible(false);
                startSurface(false);
            } catch (Exception error) { failed(error); }
        }, 12_000);
    }
    private void startSurface(boolean apps) throws Exception {
        field("allApps", apps);
        Method render = MainActivity.class.getDeclaredMethod("render", int.class); render.setAccessible(true); render.invoke(this, 0);
        WallpaperSurface wallpaper = (WallpaperSurface) field("wallpaperSurface");
        wallpaper.options(0, false);
        wallpaper.show(new Wallpaper("android.resource://" + getPackageName() + "/raw/test_image",
                0, 1, "Offline opacity image", null, null, null), (ready, status) -> {
            if (!ready) failed(new IllegalStateException(status));
            else runStage();
        });
    }
    private void runStage() {
        try { apply.invoke(this, levels[stage % 3]); }
        catch (Exception error) { failed(error); return; }
        probe.postDelayed(() -> {
            try {
                int percent = levels[stage % 3];
                View surface = (View) field("surface");
                require(surface.getAlpha() == 1f, "dashboard content faded");
                assertForeground(surface, percent);
                Bitmap full = Bitmap.createBitmap(actualRoot.getWidth(), actualRoot.getHeight(), Bitmap.Config.ARGB_8888);
                actualRoot.draw(new Canvas(full));
                int pixel = full.getPixel(full.getWidth() - 2, full.getHeight() - 2);
                int palette = (Integer) field("background");
                if (percent == 0) { wallpaperPixel = pixel; require(pixel != palette, "wallpaper is covered at zero"); }
                else if (percent == 100) require(pixel == palette, "wallpaper visible at 100");
                else {
                    int mixed = PaletteColors.mix(wallpaperPixel, palette, .75f);
                    require(Math.abs(Color.red(mixed) - Color.red(pixel)) <= 2
                            && Math.abs(Color.green(mixed) - Color.green(pixel)) <= 2
                            && Math.abs(Color.blue(mixed) - Color.blue(pixel)) <= 2, "backdrop is not a 75% blend");
                }
                screens[stage] = Bitmap.createScaledBitmap(full, 600, 338, true); full.recycle();
                stage++;
                if (stage == 3) startSurface(true);
                else if (stage < 6) runStage();
                else finishProbe();
            } catch (Exception error) { failed(error); }
        }, 700);
    }
    private void assertForeground(View view, int percent) {
        if (view instanceof TextView || view instanceof ImageView) require(view.getAlpha() == 1f, "label or artwork faded");
        if (view.getBackground() instanceof UiBackgroundDrawable)
            require((((UiBackgroundDrawable) view.getBackground()).getColor().getDefaultColor() >>> 24)
                    == Math.round(255 * percent / 100f), "panel fill differs from draft");
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) assertForeground(group.getChildAt(i), percent);
        }
    }
    private void finishProbe() throws Exception {
        // An opaque focus border must survive a zero-opacity fill.
        UiBackgroundDrawable border = new UiBackgroundDrawable(Color.RED); border.setStroke(6, Color.WHITE);
        border.setFillOpacity(0); border.setBounds(0, 0, 100, 100);
        Bitmap pixels = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888); border.draw(new Canvas(pixels));
        require(pixels.getPixel(50, 2) == Color.WHITE && Color.alpha(pixels.getPixel(50, 50)) == 0, "focus border faded"); pixels.recycle();
        // Include the inactive focused state: changing focus must not bring an opaque fill back.
        UiBackgroundDrawable.States states = new UiBackgroundDrawable.States();
        UiBackgroundDrawable selected = new UiBackgroundDrawable(Color.RED);
        states.addState(new int[]{android.R.attr.state_focused}, selected);
        states.addState(new int[]{}, new UiBackgroundDrawable(Color.BLUE));
        states.setFillOpacity(0); states.setState(new int[]{android.R.attr.state_focused});
        require((selected.getColor().getDefaultColor() >>> 24) == 0, "hidden focus fill ignored draft");
        require(java.util.Objects.equals(originalLayout, getSharedPreferences("widget-home", MODE_PRIVATE).getString("layout", null)), "probe changed saved layout");
        apply.invoke(this, ((HomeLayout) field("layout")).uiOpacityPercent);
        LinearLayout deck = new LinearLayout(this); deck.setOrientation(LinearLayout.VERTICAL); deck.setBackgroundColor(0xff101b2b);
        TextView title = label("ALL CHECKS PASSED · background fills only\n0%: wallpaper visible · 75%: blend · 100%: wallpaper covered\nText/artwork/focus stay opaque · saved settings unchanged"); deck.addView(title);
        for (int row = 0; row < 2; row++) {
            TextView heading = label(row == 0 ? "HOME: 0%                                  75%                                  100%" : "ALL APPS: 0%                              75%                                  100%");
            deck.addView(heading);
            LinearLayout strip = new LinearLayout(this);
            for (int col = 0; col < 3; col++) {
                ImageView shot = new ImageView(this); shot.setImageBitmap(screens[row * 3 + col]); shot.setScaleType(ImageView.ScaleType.FIT_CENTER);
                strip.addView(shot, new LinearLayout.LayoutParams(0, -1, 1));
            }
            deck.addView(strip, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        actualRoot.addView(deck, new FrameLayout.LayoutParams(-1, -1));
    }
    private TextView label(String value) {
        TextView text = new TextView(this); text.setText(value); text.setTextColor(Color.WHITE); text.setTextSize(17);
        text.setGravity(Gravity.CENTER); text.setPadding(8, 4, 8, 4); return text;
    }
    private void failed(Exception error) {
        TextView failure = label("FAIL: " + error); failure.setBackgroundColor(Color.RED);
        addContentView(failure, new ViewGroup.LayoutParams(-1, -2));
    }
    private void require(boolean valid, String message) { if (!valid) throw new IllegalStateException(message); }
    @Override protected void onDestroy() {
        probe.removeCallbacksAndMessages(null); super.onDestroy();
        for (Bitmap screen : screens) if (screen != null) screen.recycle();
    }
}
