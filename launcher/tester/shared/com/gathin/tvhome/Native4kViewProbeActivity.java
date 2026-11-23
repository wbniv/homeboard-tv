package com.gathin.tvhome;

import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/** Debug-only proof of normal Android views on a private display-backed surface. */
public final class Native4kViewProbeActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "Homeboard4kViews";
    private VirtualDisplay virtualDisplay;
    private Presentation presentation;
    private Display physicalDisplay;
    private int width, height, density, logicalWidth, logicalHeight, surfaceWidth, surfaceHeight;
    private boolean showingModes;
    private ScrollView modeScroll;
    private Display.Mode[] modes = new Display.Mode[0];
    private TextView stats;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 512 | 1024);
        physicalDisplay = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Display.Mode mode = physicalDisplay.getMode();
        width = mode.getPhysicalWidth(); height = mode.getPhysicalHeight();
        double scale = Math.min(1, Math.min(3840.0 / width, 2160.0 / height));
        width = Math.max(1, (int) (width * scale)); height = Math.max(1, (int) (height * scale));
        logicalWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels);
        logicalHeight = Math.max(1, getResources().getDisplayMetrics().heightPixels);
        density = Math.max(1, Math.round(getResources().getDisplayMetrics().densityDpi * (float) width / logicalWidth));
        SurfaceView surface = new SurfaceView(this);
        setContentView(surface);
        surface.getHolder().addCallback(this);
        surface.getHolder().setFixedSize(width, height);
        Log.i(TAG, "logical=" + logicalWidth + "x" + logicalHeight
                + " target=" + width + "x" + height + " density=" + density);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        try {
            DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
            virtualDisplay = manager.createVirtualDisplay("Homeboard 4K views probe", width, height, density,
                    holder.getSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                            | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
            if (virtualDisplay == null) { Log.e(TAG, "virtual display unavailable"); return; }
            presentation = new Presentation(this, virtualDisplay.getDisplay());
            showMain();
            presentation.show();
        } catch (RuntimeException error) { Log.e(TAG, "virtual display or presentation failed", error); release(); }
    }

    private LinearLayout panel() {
        LinearLayout layout = new LinearLayout(presentation.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(28), dp(20), dp(28), dp(20));
        layout.setBackgroundColor(Color.rgb(16, 28, 41));
        return layout;
    }
    private TextView text(String value, float size) {
        TextView view = new TextView(presentation.getContext());
        view.setText(value); view.setTextColor(Color.WHITE); view.setTextSize(size);
        return view;
    }
    private void showMain() {
        showingModes = false;
        modeScroll = null;
        LinearLayout layout = panel();
        layout.addView(text("Homeboard 4K Tester", 21));
        stats = text("Reading display information…", 14);
        layout.addView(stats);
        WorldClockWidgetView clock = new WorldClockWidgetView(presentation.getContext(),
                new WorldClockSettings(), PaletteColors.from(Color.rgb(16, 28, 41)), 65);
        clock.setFocusable(false);
        layout.addView(clock, new LinearLayout.LayoutParams(-1, dp(65)));
        Button button = new Button(presentation.getContext()); button.setText("Show supported TV modes");
        button.setOnClickListener(v -> { modes = physicalDisplay.getSupportedModes(); showModes(); });
        layout.addView(button);
        layout.addView(new PixelLines(presentation.getContext()), new LinearLayout.LayoutParams(-1, dp(225)));
        layout.addView(text("Back exits · Lines are buffer pixels · TV output can still scale or filter them", 12));
        presentation.setContentView(layout);
        button.requestFocus(); clock.start(); updateStats();
        layout.post(() -> {
            updateStats();
            Log.i(TAG, "presentation view=" + layout.getWidth() + "x" + layout.getHeight()
                    + " clock=" + clock.getWidth() + "x" + clock.getHeight()
                    + " density=" + layout.getResources().getDisplayMetrics().densityDpi
                    + " hardware=" + layout.isHardwareAccelerated());
        });
    }
    private void updateStats() {
        if (stats == null || showingModes) return;
        Display.Mode mode = physicalDisplay.getMode();
        String surface = surfaceWidth > 0 ? surfaceWidth + " × " + surfaceHeight : "waiting for surface";
        stats.setText(String.format(Locale.US,
                "Android UI %d × %d   ·   Test surface %s   ·   Current output %d × %d @ %.2f Hz",
                logicalWidth, logicalHeight, surface, mode.getPhysicalWidth(), mode.getPhysicalHeight(), mode.getRefreshRate()));
    }
    private void showModes() {
        showingModes = true; stats = null;
        LinearLayout layout = panel();
        layout.addView(text("Supported TV modes", 21));
        layout.addView(text("Reported by Android for this display. Viewing this list does not change the mode.", 13));
        modeScroll = new ScrollView(presentation.getContext());
        modeScroll.setFillViewport(true);
        LinearLayout entries = new LinearLayout(presentation.getContext());
        entries.setOrientation(LinearLayout.VERTICAL);
        if (modes.length == 0) entries.addView(text("No display modes reported.", 15));
        for (Display.Mode mode : modes) {
            entries.addView(text(String.format(Locale.US, "%d × %d   ·   %.2f Hz   ·   mode %d",
                    mode.getPhysicalWidth(), mode.getPhysicalHeight(), mode.getRefreshRate(), mode.getModeId()), 15));
        }
        modeScroll.addView(entries);
        layout.addView(modeScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button back = new Button(presentation.getContext()); back.setText("Back to tester");
        back.setOnClickListener(v -> showMain());
        layout.addView(back);
        presentation.setContentView(layout);
        back.requestFocus();
        Log.i(TAG, "modes=" + modes.length);
    }

    /** A pixel here is one pixel of the virtual display's rendering buffer. */
    private static final class PixelLines extends View {
        private final Paint paint = new Paint();
        PixelLines(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int density = getResources().getDisplayMetrics().densityDpi;
            int pad = Math.round(5 * density / 160f), textSize = Math.round(13 * density / 160f);
            int panel = (getWidth() - pad * 4) / 3;
            int labelHeight = textSize + pad, available = (getHeight() - 2 * labelHeight - 3 * pad) / 2;
            if (panel <= 0 || available <= 0) return;
            paint.setTextSize(textSize);
            for (int row = 0; row < 2; row++) {
                int labelTop = pad + row * (labelHeight + available + pad);
                int top = labelTop + labelHeight, bottom = top + available;
                for (int i = 0; i < 3; i++) {
                    int line = 1 << i, left = pad + i * (panel + pad), right = left + panel;
                    paint.setAntiAlias(true); paint.setColor(Color.WHITE);
                    canvas.drawText((row == 0 ? "Vertical " : "Horizontal ") + line + " px", left, labelTop + textSize, paint);
                    paint.setAntiAlias(false); paint.setColor(Color.BLACK); canvas.drawRect(left, top, right, bottom, paint);
                    paint.setColor(Color.WHITE);
                    if (row == 0) for (int x = left; x < right; x += line * 2)
                        canvas.drawRect(x, top, Math.min(x + line, right), bottom, paint);
                    else for (int y = top; y < bottom; y += line * 2)
                        canvas.drawRect(left, y, right, Math.min(y + line, bottom), paint);
                }
            }
        }
    }
    private int dp(float n) { return Math.round(n * density / 160f); }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (presentation == null || presentation.getWindow() == null) return super.dispatchTouchEvent(event);
        View decor = presentation.getWindow().getDecorView();
        View host = getWindow().getDecorView();
        if (host.getWidth() == 0 || host.getHeight() == 0) return super.dispatchTouchEvent(event);
        MotionEvent mapped = MotionEvent.obtain(event);
        Matrix transform = new Matrix();
        transform.setScale((float) decor.getWidth() / host.getWidth(), (float) decor.getHeight() / host.getHeight());
        mapped.transform(transform);
        boolean handled = decor.dispatchTouchEvent(mapped);
        mapped.recycle();
        return handled || super.dispatchTouchEvent(event);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_BACK) {
            if (showingModes) { if (event.getAction() == KeyEvent.ACTION_UP) showMain(); return true; }
            return super.dispatchKeyEvent(event);
        }
        if (presentation == null) return super.dispatchKeyEvent(event);
        View focused = presentation.getWindow().getDecorView().findFocus();
        boolean direction = key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_UP
                || key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT;
        if (showingModes && modeScroll != null && (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) modeScroll.smoothScrollBy(0, key == KeyEvent.KEYCODE_DPAD_DOWN ? dp(80) : -dp(80));
            return true;
        }
        if (direction) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && focused != null) {
                View next;
                switch (key) {
                    case KeyEvent.KEYCODE_DPAD_DOWN: next = focused.focusSearch(View.FOCUS_DOWN); break;
                    case KeyEvent.KEYCODE_DPAD_UP: next = focused.focusSearch(View.FOCUS_UP); break;
                    case KeyEvent.KEYCODE_DPAD_LEFT: next = focused.focusSearch(View.FOCUS_LEFT); break;
                    default: next = focused.focusSearch(View.FOCUS_RIGHT); break;
                }
                boolean moved = next != null && next.requestFocus();
                Log.i(TAG, "focus key=" + key + " moved=" + moved);
            }
            return true;
        }
        if ((key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) && focused != null) {
            focused.dispatchKeyEvent(event); return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        surfaceWidth = w; surfaceHeight = h; updateStats(); Log.i(TAG, "surface=" + w + "x" + h);
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { release(); }
    @Override protected void onDestroy() { release(); super.onDestroy(); }
    private void release() {
        stats = null; showingModes = false; modeScroll = null;
        if (presentation != null) { presentation.dismiss(); presentation = null; }
        if (virtualDisplay != null) { virtualDisplay.release(); virtualDisplay = null; }
    }
}
