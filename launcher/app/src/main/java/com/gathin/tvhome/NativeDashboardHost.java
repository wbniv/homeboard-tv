package com.gathin.tvhome;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** Android 11+ embedded hardware hierarchy, bounded to a single UHD dashboard. */
@TargetApi(30)
final class NativeDashboardHost implements SurfaceHolder.Callback {
    private final NativeDashboardActivity activity;
    private final Runnable fallback;
    private final Resources resources;
    private final InputRoot root;
    private final List<WeakReference<Dialog>> dialogs = new ArrayList<>();
    private SurfaceView surface;
    private SurfaceControlViewHost host;
    private boolean closed, failed;
    private int surfaceWidth, surfaceHeight;
    private static final int WIDTH = 3840, HEIGHT = 2160;
    NativeDashboardHost(NativeDashboardActivity activity, Resources base, Runnable fallback) {
        this.activity = activity; this.fallback = fallback;
        DisplayMetrics metrics = new DisplayMetrics(); metrics.setTo(base.getDisplayMetrics());
        float ratio = (float)WIDTH / Math.max(1, metrics.widthPixels);
        metrics.densityDpi = Math.max(1, Math.round(metrics.densityDpi * ratio));
        metrics.density = metrics.densityDpi / 160f; metrics.scaledDensity *= ratio;
        metrics.widthPixels = WIDTH; metrics.heightPixels = HEIGHT;
        Configuration configuration = new Configuration(base.getConfiguration()); configuration.densityDpi = metrics.densityDpi;
        resources = new Resources(base.getAssets(), metrics, configuration);
        root = new InputRoot();
    }
    Resources resources() { return resources; }
    FrameLayout root() { return root; }
    int[] surfaceSize() { return new int[]{surfaceWidth, surfaceHeight}; }
    View attach(View dashboard) {
        root.addView(dashboard, new FrameLayout.LayoutParams(-1, -1));
        surface = new SurfaceView(activity.getBaseContext()); surface.setZOrderOnTop(true);
        surface.getHolder().addCallback(this); surface.getHolder().setFixedSize(WIDTH, HEIGHT);
        surface.postDelayed(() -> { if (!closed && !failed && host == null && surface.isShown()) fail(new IllegalStateException("Native surface did not attach")); }, 5000);
        return surface;
    }
    public void surfaceCreated(SurfaceHolder holder) { }
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceWidth = width; surfaceHeight = height;
        if (closed || failed || host != null) return;
        try {
            if (width != WIDTH || height != HEIGHT || surface.getHostToken() == null)
                throw new IllegalStateException("Native surface size/token unavailable");
            host = new SurfaceControlViewHost(activity, activity.getWindowManager().getDefaultDisplay(), surface.getHostToken());
            host.setView(root, WIDTH, HEIGHT);
            // SurfaceView owns the transferred package and releases it itself.
            surface.setChildSurfacePackage(host.getSurfacePackage());
            root.post(() -> Log.i("HomeboardNative", "dashboard=" + root.getWidth() + "x" + root.getHeight()
                    + " surface=" + surfaceWidth + "x" + surfaceHeight + " hardware=" + root.isHardwareAccelerated()));
        } catch (RuntimeException | OutOfMemoryError failure) { fail(failure); }
    }
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceWidth = surfaceHeight = 0;
        release();
    }
    private void release() { if (host != null) { host.release(); host = null; } }
    private void fail(Throwable error) {
        if (failed || closed) return;
        failed = true;
        Log.e("HomeboardNative", "Native dashboard failed; restoring ordinary rendering", error);
        fallback.run();
    }
    void close() { closed = true; release(); dialogs.clear(); }
    void dialog(Dialog dialog) { dialogs.add(new WeakReference<>(dialog)); }
    Dialog activeDialog() {
        for (int i = dialogs.size()-1; i >= 0; i--) {
            Dialog dialog = dialogs.get(i).get();
            if (dialog == null) dialogs.remove(i);
            else if (dialog.isShowing()) return dialog;
        }
        return null;
    }
    boolean dispatchLocal(KeyEvent event) { return root.dispatchLocal(event); }
    private final class InputRoot extends FrameLayout {
        InputRoot() { super(activity); }
        @Override public boolean dispatchKeyEvent(KeyEvent event) { return activity.dispatchKeyEvent(event); }
        boolean dispatchLocal(KeyEvent event) {
            if (super.dispatchKeyEvent(event)) return true;
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int direction;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP: direction = View.FOCUS_UP; break;
                case KeyEvent.KEYCODE_DPAD_DOWN: direction = View.FOCUS_DOWN; break;
                case KeyEvent.KEYCODE_DPAD_LEFT: direction = View.FOCUS_LEFT; break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: direction = View.FOCUS_RIGHT; break;
                default: return false;
            }
            View next = FocusFinder.getInstance().findNextFocus(this, findFocus(), direction);
            return next != null && next.requestFocus(direction);
        }
    }
}
