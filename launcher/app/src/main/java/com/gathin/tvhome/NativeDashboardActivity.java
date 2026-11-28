package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

/** Keeps the launcher identity and data while choosing its rendering surface. */
public abstract class NativeDashboardActivity extends Activity {
    private static boolean nativeFailed;
    private NativeDashboardHost nativeHost;
    private final Handler renderingHandler = new Handler(Looper.getMainLooper());
    private DisplayManager displays;
    private int outputWidth, outputHeight;
    private Configuration originalConfiguration;
    private boolean recreating;
    private boolean uhdWorkspace;
    protected boolean automaticNativeDashboard() { return true; }
    protected boolean native4kWorkspace() { return uhdWorkspace; }
    @Override public Resources getResources() {
        return nativeHost != null ? nativeHost.resources() : super.getResources();
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        originalConfiguration = new Configuration(super.getResources().getConfiguration());
        displays = (DisplayManager)getSystemService(DISPLAY_SERVICE);
        Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
        outputWidth = mode.getPhysicalWidth(); outputHeight = mode.getPhysicalHeight();
        android.util.DisplayMetrics ui = super.getResources().getDisplayMetrics();
        boolean tv = getPackageManager().hasSystemFeature("android.software.leanback")
                || getPackageManager().hasSystemFeature("android.hardware.type.television");
        if (tv) for (Display.Mode supported : getWindowManager().getDefaultDisplay().getSupportedModes()) {
            if (Math.max(supported.getPhysicalWidth(), supported.getPhysicalHeight()) >= 3840
                    && Math.min(supported.getPhysicalWidth(), supported.getPhysicalHeight()) >= 2160) {
                uhdWorkspace = true; break;
            }
        }
        if (Build.VERSION.SDK_INT >= 30 && automaticNativeDashboard() && NativeDashboardPolicy.embed(Build.VERSION.SDK_INT, tv,
                outputWidth, outputHeight, ui.widthPixels, ui.heightPixels, nativeFailed)) {
            try { nativeHost = new NativeDashboardHost(this, super.getResources(), this::ordinaryFallback); }
            catch (RuntimeException | OutOfMemoryError failure) { nativeFailed = true; }
        }
    }
    @Override public void setContentView(View view) {
        if (nativeHost == null) super.setContentView(view);
        else super.setContentView(nativeHost.attach(view));
    }
    protected FrameLayout dashboardOverlayRoot() { return nativeHost != null ? nativeHost.root() : null; }
    protected void configureDashboardDialog(Dialog dialog) { if (nativeHost != null) nativeHost.dialog(dialog); }
    protected int[] dashboardRenderingSurface() { return nativeHost != null ? nativeHost.surfaceSize() : null; }
    /** True while this activity draws through the native 4K surface; media can then use 4K-sized assets. */
    boolean nativeDashboardRendering() { return nativeHost != null; }
    protected boolean dispatchDashboardWindowKey(KeyEvent event) {
        return nativeHost != null ? nativeHost.dispatchLocal(event) : super.dispatchKeyEvent(event);
    }
    protected Dialog nativeDashboardDialog() { return nativeHost != null ? nativeHost.activeDialog() : null; }
    @Override public View getCurrentFocus() { return nativeHost != null ? nativeHost.root().findFocus() : super.getCurrentFocus(); }
    private void ordinaryFallback() {
        // Do not repeat a failed allocation on subsequent Home intents in this process.
        nativeFailed = true;
        requestRenderingRecreation();
    }
    private void requestRenderingRecreation() {
        if (recreating || isFinishing() || isDestroyed()) return;
        recreating = true;
        renderingHandler.post(() -> { if (!isFinishing() && !isDestroyed()) recreate(); });
    }
    private void checkOutput() {
        Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
        if (mode.getPhysicalWidth() != outputWidth || mode.getPhysicalHeight() != outputHeight)
            requestRenderingRecreation();
    }
    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        public void onDisplayAdded(int id) { }
        public void onDisplayRemoved(int id) { }
        public void onDisplayChanged(int id) { if (id == Display.DEFAULT_DISPLAY) checkOutput(); }
    };
    @Override protected void onStart() {
        super.onStart();
        if (automaticNativeDashboard() && displays != null) displays.registerDisplayListener(displayListener, renderingHandler);
    }
    @Override protected void onResume() { super.onResume(); if (automaticNativeDashboard()) checkOutput(); }
    @Override protected void onStop() {
        if (displays != null) displays.unregisterDisplayListener(displayListener);
        super.onStop();
    }
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (automaticNativeDashboard() && (configuration.densityDpi != originalConfiguration.densityDpi
                || configuration.fontScale != originalConfiguration.fontScale
                || configuration.orientation != originalConfiguration.orientation)) requestRenderingRecreation();
    }
    @Override protected void onDestroy() {
        renderingHandler.removeCallbacksAndMessages(null);
        if (nativeHost != null) { nativeHost.close(); nativeHost = null; }
        super.onDestroy();
    }
}
