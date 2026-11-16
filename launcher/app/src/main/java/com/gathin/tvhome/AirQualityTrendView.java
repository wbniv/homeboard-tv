package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import java.util.List;

/** Two compact PM2.5 sparklines: the last 24 hours and the rolling 30-day window. */
final class AirQualityTrendView extends View {
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private AirQualityReading[] dayValues = new AirQualityReading[0];
    private AirQualityReading[] monthValues = new AirQualityReading[0];
    private PaletteColors palette = PaletteColors.from(0xff101010);

    AirQualityTrendView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(getContext().getString(R.string.hb_status_recent_pm2_5_trend));
        gridPaint.setStrokeWidth(dp(1));
        linePaint.setStyle(Paint.Style.STROKE); linePaint.setStrokeWidth(dp(3));
        linePaint.setStrokeCap(Paint.Cap.ROUND); linePaint.setStrokeJoin(Paint.Join.ROUND);
        dotPaint.setStyle(Paint.Style.FILL);
        labelPaint.setTextSize(dp(9)); labelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    void setPalette(PaletteColors colors) {
        palette = colors;
        gridPaint.setColor(PaletteColors.mix(colors.surface, colors.text, 0.25f));
        labelPaint.setColor(colors.muted);
        invalidate();
    }

    void setReadings(List<AirQualityReading> day, List<AirQualityReading> month) {
        dayValues = day.toArray(new AirQualityReading[0]);
        monthValues = month.toArray(new AirQualityReading[0]);
        String latest = monthValues.length == 0 ? "" : ", latest "
                + monthValues[monthValues.length - 1].pm25 + " micrograms per cubic meter";
        setContentDescription("PM2.5 24-hour sparkline, " + dayValues.length + " samples; "
                + "30-day sparkline, " + monthValues.length + " hourly 24-hour-average samples" + latest);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(36), right = getWidth() - dp(5), top = dp(3), bottom = getHeight() - dp(3);
        if (right <= left || bottom <= top) return;
        float mid = top + (bottom - top) / 2;
        labelPaint.setAlpha(220);
        canvas.drawText(getContext().getString(R.string.hb_status_24h), dp(1), top + dp(13), labelPaint);
        canvas.drawText(getContext().getString(R.string.hb_status_30d), dp(1), mid + dp(13), labelPaint);
        canvas.drawLine(left, mid, right, mid, gridPaint);
        drawSeries(canvas, dayValues, left, right, top, mid - dp(2));
        drawSeries(canvas, monthValues, left, right, mid + dp(2), bottom);
    }

    private void drawSeries(Canvas canvas, AirQualityReading[] values, float left, float right, float top, float bottom) {
        canvas.drawLine(left, bottom, right, bottom, gridPaint);
        if (values.length == 0) return;
        double min = values[0].pm25, max = values[0].pm25;
        for (AirQualityReading value : values) { min = Math.min(min, value.pm25); max = Math.max(max, value.pm25); }
        if (max - min < 1) { min = Math.max(0, min - 1); max += 1; }
        float inset = dp(4), usableTop = top + inset, usableBottom = bottom - inset;
        float previousX = 0, previousY = 0, lastX = 0, lastY = 0;
        for (int i = 0; i < values.length; i++) {
            float x = values.length == 1 ? (left + right) / 2
                    : left + (right - left) * i / (values.length - 1f);
            float y = usableBottom - (float) ((values[i].pm25 - min) / (max - min)) * (usableBottom - usableTop);
            if (i > 0) {
                linePaint.setColor(AirQualityCard.conditionColor(values[i].condition()));
                canvas.drawLine(previousX, previousY, x, y, linePaint);
            }
            previousX = x; previousY = y; lastX = x; lastY = y;
        }
        dotPaint.setColor(AirQualityCard.conditionColor(values[values.length - 1].condition()));
        canvas.drawCircle(lastX, lastY, dp(4), dotPaint);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
