package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Rectangular focus rim and soft halo behind banner artwork. */
final class BannerFocusView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    BannerFocusView(Context context) {
        super(context); density = getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.STROKE);
    }
    @Override protected void onDraw(Canvas canvas) {
        float inset = 8 * density, radius = 10 * density;
        for (int i = 7; i >= 2; i--) {
            paint.setStrokeWidth(i * density); paint.setColor(0x08b8d5ee);
            canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset, radius, radius, paint);
        }
        paint.setStrokeWidth(2 * density); paint.setColor(0xffb5cfe4);
        canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset, radius, radius, paint);
    }
}
