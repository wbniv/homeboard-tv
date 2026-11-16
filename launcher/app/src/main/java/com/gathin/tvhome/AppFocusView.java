package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** App-only focus: a soft halo behind the icon and a gray rim separated by a dark gap. */
final class AppFocusView extends View {
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gap = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float diameter;

    AppFocusView(Context context) {
        super(context);
        gap.setColor(0xff27292a);
        rim.setColor(0xff434546);
        rim.setStyle(Paint.Style.STROKE);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    void setIconDiameter(float value) {
        if (diameter == value) return;
        diameter = value;
        rim.setStrokeWidth(AppAppearance.ringWidth(value));
        updateHalo();
        invalidate();
    }

    private void updateHalo() {
        if (diameter <= 0 || getWidth() == 0 || getHeight() == 0) return;
        halo.setShader(new RadialGradient(getWidth() / 2f, getHeight() / 2f,
                diameter * AppAppearance.HALO_RADIUS,
                new int[]{0x18aeb9c0, 0x18aeb9c0, 0x0caeb9c0, 0x00aeb9c0},
                new float[]{0f, 0.58f, 0.72f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { updateHalo(); }

    @Override protected void onDraw(Canvas canvas) {
        if (diameter <= 0) return;
        float x = getWidth() / 2f, y = getHeight() / 2f;
        float outer = AppAppearance.ringOuterRadius(diameter);
        float stroke = AppAppearance.ringWidth(diameter);
        canvas.drawCircle(x, y, diameter * AppAppearance.HALO_RADIUS, halo);
        canvas.drawCircle(x, y, outer - stroke, gap);
        canvas.drawCircle(x, y, outer - stroke / 2f, rim);
    }
}
