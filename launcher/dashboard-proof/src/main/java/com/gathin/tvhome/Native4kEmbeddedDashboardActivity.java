package com.gathin.tvhome;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

/** Comparison: direct embedded native hierarchy, without virtual-display composition. */
public final class Native4kEmbeddedDashboardActivity extends MainActivity implements SurfaceHolder.Callback {
    private Resources nativeResources;
    private SurfaceControlViewHost viewHost;
    private SurfaceView surface;
    private View dashboard;
    private InputRoot embeddedRoot;
    private int width, height;
    private final java.util.List<java.lang.ref.WeakReference<android.app.Dialog>> dialogs = new java.util.ArrayList<>();
    @Override public Resources getResources() { return nativeResources != null ? nativeResources : super.getResources(); }
    @Override protected void onCreate(Bundle state) {
        ProofStartingState.prepare(this, state);
        Resources base = super.getResources();
        Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
        width = Math.max(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        height = Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        DisplayMetrics metrics = new DisplayMetrics(); metrics.setTo(base.getDisplayMetrics());
        float ratio = (float)width / Math.max(1, metrics.widthPixels);
        metrics.densityDpi = Math.max(1, Math.round(metrics.densityDpi*ratio));
        metrics.density = metrics.densityDpi / 160f;
        metrics.scaledDensity *= ratio;
        metrics.widthPixels = width; metrics.heightPixels = height;
        Configuration config = new Configuration(base.getConfiguration()); config.densityDpi = metrics.densityDpi;
        nativeResources = new Resources(base.getAssets(), metrics, config);
        Log.i("Homeboard4kEmbedded", "output=" + width + "x" + height + " density=" + metrics.densityDpi);
        super.onCreate(state);
    }
    @Override public void setContentView(View content) {
        dashboard = content;
        surface = new SurfaceView(getBaseContext()); surface.setZOrderOnTop(true);
        super.setContentView(surface);
        surface.getHolder().addCallback(this); surface.getHolder().setFixedSize(width, height);
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (viewHost != null) return;
        try {
            if (surface.getHostToken() == null) throw new IllegalStateException("No host input token");
            embeddedRoot = new InputRoot();
            embeddedRoot.addView(dashboard, new FrameLayout.LayoutParams(-1,-1));
            viewHost = new SurfaceControlViewHost(this, getWindowManager().getDefaultDisplay(), surface.getHostToken());
            viewHost.setView(embeddedRoot, width, height);
            surface.setChildSurfacePackage(viewHost.getSurfacePackage());
            dashboard.post(() -> Log.i("Homeboard4kEmbedded", "dashboard=" + dashboard.getWidth() + "x" + dashboard.getHeight()
                    + " surface=" + w + "x" + h + " hardware=" + dashboard.isHardwareAccelerated()));
        } catch (RuntimeException | OutOfMemoryError error) {
            Log.e("Homeboard4kEmbedded", "embedded hierarchy failed", error);
            finish();
        }
    }
    @Override protected FrameLayout dashboardOverlayRoot() { return embeddedRoot; }
    @Override protected void configureDashboardDialog(android.app.Dialog dialog) { dialogs.add(new java.lang.ref.WeakReference<>(dialog)); }
    private android.app.Dialog activeDialog() {
        for (int i=dialogs.size()-1;i>=0;i--) {
            android.app.Dialog dialog = dialogs.get(i).get();
            if (dialog == null) dialogs.remove(i);
            else if (dialog.isShowing()) return dialog;
        }
        return null;
    }
    @Override protected boolean dispatchDashboardWindowKey(KeyEvent event) {
        return embeddedRoot != null ? embeddedRoot.dispatchLocal(event) : super.dispatchDashboardWindowKey(event);
    }
    @Override public View getCurrentFocus() { return embeddedRoot != null ? embeddedRoot.findFocus() : super.getCurrentFocus(); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_UP) finish();
            return true;
        }
        android.app.Dialog dialog = activeDialog();
        return dialog != null ? dialog.dispatchKeyEvent(event) : super.dispatchKeyEvent(event);
    }
    private boolean dispatchHosted(KeyEvent event) { return dispatchKeyEvent(event); }
    private final class InputRoot extends FrameLayout {
        InputRoot() { super(Native4kEmbeddedDashboardActivity.this); }
        @Override public boolean dispatchKeyEvent(KeyEvent event) { return dispatchHosted(event); }
        boolean dispatchLocal(KeyEvent event) { return super.dispatchKeyEvent(event); }
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { /* SurfaceView reparents the package on return. */ }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (viewHost != null) { viewHost.release(); viewHost = null; }
        nativeResources = null;
    }
}
