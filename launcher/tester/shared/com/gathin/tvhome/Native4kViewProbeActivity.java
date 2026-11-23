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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
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
    private DisplayManager displayManager;
    private Display.Mode requestedMode;
    private SurfaceView testSurface;
    private int presentationWidth, presentationHeight;
    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int id) { }
        @Override public void onDisplayRemoved(int id) { }
        @Override public void onDisplayChanged(int id) {
            if (physicalDisplay == null || id != physicalDisplay.getDisplayId()) return;
            if (updateOutputSize() && testSurface != null) {
                release();
                testSurface.getHolder().setFixedSize(width, height);
            }
            updateStats();
        }
    };
    private int width, height, density, logicalWidth, logicalHeight, surfaceWidth, surfaceHeight;
    private boolean showingModes;
    private ScrollView modeScroll;
    private Display.Mode[] modes = new Display.Mode[0];
    private TextView stats;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 512 | 1024);
        physicalDisplay = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        logicalWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels);
        logicalHeight = Math.max(1, getResources().getDisplayMetrics().heightPixels);
        // Keep the 1920 × 1080 content layout stable across TV and phone densities.
        density = 320;
        requestedMode = chooseHighestMode();
        if (requestedMode != null) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.preferredDisplayModeId = requestedMode.getModeId();
            getWindow().setAttributes(params);
        }
        updateOutputSize();
        testSurface = new SurfaceView(this);
        setContentView(testSurface);
        testSurface.getHolder().addCallback(this);
        testSurface.getHolder().setFixedSize(width, height);
        displayManager.registerDisplayListener(displayListener, null);
        Log.i(TAG, "logical=" + logicalWidth + "x" + logicalHeight
                + " target=" + width + "x" + height + " density=" + density
                + " requestedMode=" + (requestedMode == null ? "none" : requestedMode.getModeId()));
    }

    private Display.Mode chooseHighestMode() {
        Display.Mode[] available = physicalDisplay.getSupportedModes();
        Display.Mode current = physicalDisplay.getMode();
        int[] widths = new int[available.length], heights = new int[available.length];
        float[] rates = new float[available.length];
        int currentIndex = -1;
        for (int i = 0; i < available.length; i++) {
            widths[i] = available[i].getPhysicalWidth(); heights[i] = available[i].getPhysicalHeight();
            rates[i] = available[i].getRefreshRate();
            if (available[i].getModeId() == current.getModeId()) currentIndex = i;
        }
        int best = ResolutionModeChooser.bestIndex(widths, heights, rates, currentIndex);
        return best < 0 ? null : available[best];
    }

    private boolean updateOutputSize() {
        Display.Mode mode = physicalDisplay.getMode();
        int rotation = physicalDisplay.getRotation();
        int newWidth = ResolutionModeChooser.orientedWidth(mode.getPhysicalWidth(), mode.getPhysicalHeight(), rotation);
        int newHeight = ResolutionModeChooser.orientedHeight(mode.getPhysicalWidth(), mode.getPhysicalHeight(), rotation);
        boolean changed = width != newWidth || height != newHeight;
        width = Math.max(1, newWidth); height = Math.max(1, newHeight);
        return changed;
    }

    @Override public void surfaceCreated(SurfaceHolder holder) { }

    private void createPresentation(SurfaceHolder holder, int surfaceWidth, int surfaceHeight) {
        try {
            virtualDisplay = displayManager.createVirtualDisplay("Homeboard 4K views probe", surfaceWidth, surfaceHeight, density,
                    holder.getSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                            | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
            if (virtualDisplay == null) throw new IllegalStateException("Virtual display unavailable");
            presentationWidth = surfaceWidth; presentationHeight = surfaceHeight;
            presentation = new Presentation(this, virtualDisplay.getDisplay());
            showMain();
            presentation.show();
        } catch (RuntimeException | OutOfMemoryError error) {
            Log.e(TAG, "virtual display or presentation failed", error);
            release();
            TextView failure = new TextView(this);
            failure.setTextColor(Color.WHITE); failure.setBackgroundColor(Color.BLACK);
            failure.setText("Could not create the full-resolution test surface. Try a lower display mode.");
            setContentView(failure);
        }
    }

    private LinearLayout panel() {
        LinearLayout layout = new LinearLayout(presentation.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(28), dp(20), dp(28), dp(20));
        layout.setBackgroundColor(Color.argb(80, 16, 28, 41));
        return layout;
    }
    /** Fill the surface with size references and anchor the actual UI to 1920 × 1080 pixels. */
    private void setContentWithResolutionBackground(View content) {
        FrameLayout root = new FrameLayout(presentation.getContext());
        root.addView(new ResolutionBackground(presentation.getContext()),
                new FrameLayout.LayoutParams(-1, -1));
        root.addView(content, new FrameLayout.LayoutParams(1920, 1080, Gravity.TOP | Gravity.LEFT));
        presentation.setContentView(root);
        root.post(() -> Log.i(TAG, "resolution background=" + root.getWidth() + "x" + root.getHeight()
                + " content=" + content.getWidth() + "x" + content.getHeight()));
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
        layout.addView(text("Content: 1920 × 1080 pixels · Background: resolution sizes", 12));
        layout.addView(text("Back exits · Buffer pixels may be scaled or filtered by the TV", 12));
        setContentWithResolutionBackground(layout);
        button.requestFocus(); clock.start(); updateStats();
        layout.post(() -> {
            updateStats();
            Log.i(TAG, "presentation view=" + layout.getWidth() + "x" + layout.getHeight()
                    + " clock=" + clock.getWidth() + "x" + clock.getHeight()
                    + " density=" + layout.getResources().getDisplayMetrics().densityDpi
                    + " hardware=" + layout.isHardwareAccelerated());
        });
    }
    private String orientedModeSize(Display.Mode mode) {
        int rotation = physicalDisplay.getRotation();
        return ResolutionModeChooser.orientedWidth(mode.getPhysicalWidth(), mode.getPhysicalHeight(), rotation)
                + " × " + ResolutionModeChooser.orientedHeight(mode.getPhysicalWidth(), mode.getPhysicalHeight(), rotation);
    }
    private void updateStats() {
        if (stats == null || showingModes) return;
        Display.Mode mode = physicalDisplay.getMode();
        String surface = surfaceWidth > 0 ? surfaceWidth + " × " + surfaceHeight : "waiting for surface";
        stats.setText(String.format(Locale.US,
                "Android UI %d × %d   ·   Requested %s   ·   Output %s @ %.2f Hz   ·   Surface %s",
                logicalWidth, logicalHeight, requestedMode == null ? "unavailable" : orientedModeSize(requestedMode),
                orientedModeSize(mode), mode.getRefreshRate(), surface));
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
            entries.addView(text(String.format(Locale.US, "%s   ·   %.2f Hz   ·   mode %d",
                    orientedModeSize(mode), mode.getRefreshRate(), mode.getModeId()), 15));
        }
        modeScroll.addView(entries);
        layout.addView(modeScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button back = new Button(presentation.getContext()); back.setText("Back to tester");
        back.setOnClickListener(v -> showMain());
        layout.addView(back);
        setContentWithResolutionBackground(layout);
        back.requestFocus();
        Log.i(TAG, "modes=" + modes.length);
    }

    /** Rectangles share a top-left origin and use actual rendering-buffer pixels. */
    private static final class ResolutionBackground extends View {
        private static final int[] WIDTHS = {7680, 3840, 2560, 1920, 1280, 960};
        private static final int[] HEIGHTS = {4320, 2160, 1440, 1080, 720, 540};
        private static final int[] COLORS = {0xff573b42, 0xff263752, 0xff424260, 0xff365448, 0xff6a5639, 0xff52606b};
        private static final String[] LABELS = {"8K 7680 × 4320", "UHD 3840 × 2160", "QHD 2560 × 1440",
                "Full HD 1920 × 1080", "HD 1280 × 720", "960 × 540"};
        private final Paint paint = new Paint();
        ResolutionBackground(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int i = 0; i < WIDTHS.length; i++) {
                paint.setColor(COLORS[i]);
                canvas.drawRect(0, 0, WIDTHS[i], HEIGHTS[i], paint);
            }
            paint.setColor(Color.WHITE);
            paint.setAntiAlias(true);
            paint.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
            for (int i = 0; i < WIDTHS.length; i++) {
                if (WIDTHS[i] <= getWidth() && HEIGHTS[i] <= getHeight()) {
                    float x = Math.max(8, WIDTHS[i] - paint.measureText(LABELS[i]) - 12);
                    canvas.drawText(LABELS[i], x, HEIGHTS[i] - 12, paint);
                }
            }
        }
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
        surfaceWidth = w; surfaceHeight = h;
        if (virtualDisplay == null || presentationWidth != w || presentationHeight != h) {
            release();
            createPresentation(holder, w, h);
        }
        updateStats(); Log.i(TAG, "surface=" + w + "x" + h);
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { release(); }
    @Override protected void onDestroy() { if (displayManager != null) displayManager.unregisterDisplayListener(displayListener); release(); super.onDestroy(); }
    private void release() {
        stats = null; showingModes = false; modeScroll = null; presentationWidth = 0; presentationHeight = 0;
        if (presentation != null) { presentation.dismiss(); presentation = null; }
        if (virtualDisplay != null) { virtualDisplay.release(); virtualDisplay = null; }
    }
}
