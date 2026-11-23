package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/** Debug-only measurement of a display-sized surface; this is not the dashboard renderer. */
public final class Native4kSurfaceProbeActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "Homeboard4kProbe";
    private int requestedWidth, requestedHeight;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 512 | 1024);
        SurfaceView surface = new SurfaceView(this);
        setContentView(surface);
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Display.Mode mode = display.getMode();
        requestedWidth = mode.getPhysicalWidth();
        requestedHeight = mode.getPhysicalHeight();
        // Bound memory use on displays above UHD; do not alter the display's mode or logical size.
        if (requestedWidth > 3840 || requestedHeight > 2160) {
            double scale = Math.min(3840.0 / requestedWidth, 2160.0 / requestedHeight);
            requestedWidth = Math.max(1, (int) (requestedWidth * scale));
            requestedHeight = Math.max(1, (int) (requestedHeight * scale));
        }
        surface.getHolder().addCallback(this);
        surface.getHolder().setFixedSize(requestedWidth, requestedHeight);
        Log.i(TAG, "logical=" + getResources().getDisplayMetrics().widthPixels + "x"
                + getResources().getDisplayMetrics().heightPixels + " mode=" + mode.getPhysicalWidth()
                + "x" + mode.getPhysicalHeight() + " requested=" + requestedWidth + "x" + requestedHeight);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) { }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Canvas canvas = null;
        try {
            canvas = Build.VERSION.SDK_INT >= 26 ? holder.lockHardwareCanvas() : holder.lockCanvas();
            if (canvas == null) { Log.e(TAG, "surface canvas unavailable"); return; }
            int w = canvas.getWidth(), h = canvas.getHeight();
            canvas.drawColor(Color.rgb(14, 25, 38));
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE); paint.setTextSize(Math.max(32, w / 32f));
            canvas.drawText("4K surface probe — not the Homeboard dashboard", w * .05f, h * .16f, paint);
            paint.setTextSize(Math.max(20, w / 60f));
            canvas.drawText("Surface " + width + " × " + height + "   Canvas " + w + " × " + h,
                    w * .05f, h * .23f, paint);
            paint.setAntiAlias(false); paint.setStrokeWidth(1);
            for (int x = 0; x < Math.min(w, 1024); x += 2) {
                canvas.drawLine(w * .05f + x, h * .34f, w * .05f + x, h * .56f, paint);
            }
            paint.setAntiAlias(true);
            canvas.drawText("One-buffer-pixel alternating lines: inspect at native output resolution", w * .05f, h * .65f, paint);
            Log.i(TAG, "surface=" + width + "x" + height + " canvas=" + w + "x" + h
                    + " hardware=" + canvas.isHardwareAccelerated());
        } catch (RuntimeException error) {
            Log.e(TAG, "surface rendering failed", error);
        } finally {
            if (canvas != null) holder.unlockCanvasAndPost(canvas);
        }
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { }
}
