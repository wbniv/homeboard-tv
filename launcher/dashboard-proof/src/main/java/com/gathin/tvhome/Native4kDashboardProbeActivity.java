package com.gathin.tvhome;

import android.app.Presentation;
import android.content.res.Resources;
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
import android.view.ViewGroup;
import android.widget.TextView;

/** The production dashboard hierarchy attached to a native-size Presentation window. */
public final class Native4kDashboardProbeActivity extends MainActivity implements SurfaceHolder.Callback {
    @Override protected boolean automaticNativeDashboard() { return false; }
    private Resources nativeResources;
    private VirtualDisplay display;
    private Presentation presentation;
    private View dashboard;
    private SurfaceView surface;
    private int width, height;
    private int observedWidth, observedHeight;
    @Override protected int[] dashboardRenderingSurface() { return new int[]{observedWidth, observedHeight}; }
    private boolean failed, destroyed;
    private final java.util.List<java.lang.ref.WeakReference<android.app.Dialog>> dialogs = new java.util.ArrayList<>();

    @Override public Resources getResources() {
        return nativeResources != null ? nativeResources : super.getResources();
    }
    @Override protected void onCreate(Bundle state) {
        ProofStartingState.prepare(this, state);
        Resources physicalResources = super.getResources();
        Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
        width = Math.max(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        height = Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        int logicalWidth = Math.max(1, physicalResources.getDisplayMetrics().widthPixels);
        int density = Math.max(1, Math.round(physicalResources.getDisplayMetrics().densityDpi * (float)width/logicalWidth));
        try {
            display = getSystemService(DisplayManager.class).createVirtualDisplay("Homeboard native dashboard proof",
                    width, height, density, null, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION);
            if (display == null) throw new IllegalStateException("Private display unavailable");
            nativeResources = createDisplayContext(display.getDisplay()).getResources();
            Log.i("Homeboard4kDashboard", "output=" + width + "x" + height + " logical="
                    + logicalWidth + "x" + physicalResources.getDisplayMetrics().heightPixels + " density=" + density);
        } catch (RuntimeException | OutOfMemoryError error) {
            failed = true;
            Log.e("Homeboard4kDashboard", "native display failed; ordinary dashboard", error);
        }
        super.onCreate(state);
    }
    @Override public void setContentView(View content) {
        dashboard = content;
        if (display == null) { super.setContentView(content); return; }
        // This is the physical Activity window; its buffer gets a separate native size.
        surface = new SurfaceView(getBaseContext());
        super.setContentView(surface);
        surface.getHolder().addCallback(this);
        surface.getHolder().setFixedSize(width, height);
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        observedWidth = w; observedHeight = h;
        if (display == null || destroyed || failed) return;
        try {
            if (w != width || h != height) throw new IllegalStateException("Unexpected surface size " + w + "x" + h);
            display.setSurface(holder.getSurface());
            if (presentation != null) return;
            presentation = new Presentation(this, display.getDisplay());
            presentation.getWindow().getDecorView().setSystemUiVisibility(5894 | 512 | 1024);
            if (dashboard.getParent() instanceof ViewGroup) ((ViewGroup)dashboard.getParent()).removeView(dashboard);
            presentation.setContentView(dashboard);
            presentation.show();
            dashboard.post(() -> Log.i("Homeboard4kDashboard", "dashboard=" + dashboard.getWidth() + "x" + dashboard.getHeight()
                    + " surface=" + w + "x" + h + " density=" + getResources().getDisplayMetrics().densityDpi
                    + " hardware=" + dashboard.isHardwareAccelerated()));
        } catch (RuntimeException | OutOfMemoryError error) {
            failed = true;
            Log.e("Homeboard4kDashboard", "native presentation failed", error);
            if (presentation != null) { presentation.dismiss(); presentation = null; }
            TextView failure = new TextView(getBaseContext());
            failure.setText("Native dashboard test failed. Press Menu to exit.");
            failure.setTextColor(android.graphics.Color.WHITE); failure.setBackgroundColor(android.graphics.Color.BLACK);
            super.setContentView(failure);
        }
    }
    @Override protected android.content.Context dashboardDialogContext() {
        return presentation != null ? presentation.getContext() : this;
    }
    @Override protected void configureDashboardDialog(android.app.Dialog dialog) {
        if (presentation == null) return;
        dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION);
        dialogs.add(new java.lang.ref.WeakReference<>(dialog));
    }
    private android.app.Dialog activeDialog() {
        for (int i=dialogs.size()-1;i>=0;i--) {
            android.app.Dialog dialog = dialogs.get(i).get();
            if (dialog == null) dialogs.remove(i);
            else if (dialog.isShowing()) return dialog;
        }
        return null;
    }
    @Override protected boolean dispatchDashboardWindowKey(KeyEvent event) {
        if (presentation == null) return super.dispatchDashboardWindowKey(event);
        boolean handled = presentation.getWindow().superDispatchKeyEvent(event);
        return handled || moveFocus(presentation.getWindow().getDecorView(), event);
    }
    @Override public View getCurrentFocus() {
        android.app.Dialog dialog = activeDialog();
        if (dialog != null) return dialog.getCurrentFocus();
        return presentation != null ? presentation.getWindow().getCurrentFocus() : super.getCurrentFocus();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_UP) finish();
            return true;
        }
        android.app.Dialog dialog = activeDialog();
        if (dialog == null) return super.dispatchKeyEvent(event);
        return dialog.dispatchKeyEvent(event) || moveFocus(dialog.getWindow().getDecorView(), event);
    }
    private boolean moveFocus(View root, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        int direction;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP: direction = View.FOCUS_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: direction = View.FOCUS_DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: direction = View.FOCUS_LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: direction = View.FOCUS_RIGHT; break;
            default: return false;
        }
        View focused = root.findFocus();
        View next = focused != null ? focused.focusSearch(direction) : null;
        return next != null && next.requestFocus();
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (presentation == null || surface.getWidth() == 0 || surface.getHeight() == 0) return super.dispatchTouchEvent(event);
        MotionEvent mapped = MotionEvent.obtain(event);
        Matrix matrix = new Matrix(); matrix.setScale((float)width/surface.getWidth(), (float)height/surface.getHeight());
        mapped.transform(matrix);
        try { android.app.Dialog dialog = activeDialog(); return dialog != null ? dialog.dispatchTouchEvent(mapped) : presentation.getWindow().superDispatchTouchEvent(mapped); } finally { mapped.recycle(); }
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        observedWidth = observedHeight = 0;
        if (presentation != null) { presentation.dismiss(); presentation = null; }
        if (display != null) display.setSurface(null);
    }
    @Override protected void onDestroy() {
        destroyed = true;
        super.onDestroy();
        if (presentation != null) { presentation.dismiss(); presentation = null; }
        if (display != null) { display.release(); display = null; }
        nativeResources = null;
    }
}
