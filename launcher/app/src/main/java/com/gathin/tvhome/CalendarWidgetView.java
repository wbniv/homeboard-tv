package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import java.time.Instant;
import java.util.List;

/** One remote-focusable calendar widget, containing event-only date cards. */
final class CalendarWidgetView extends View {
    private final CalendarSnapshot snapshot;
    private final CalendarWidgetSettings settings;
    private final PaletteColors palette;
    private final int cardBackground;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.text.TextPaint textPaint = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CalendarSnapshot.Display display;
    private boolean active;
    private int page, pageSize = 1, fillOpacity = WallpaperSettings.DEFAULT_WIDGET_OPACITY;
    private final Runnable tick = new Runnable() {
        @Override public void run() { if (active) { refresh(); handler.postDelayed(this, 30_000L); } }
    };
    CalendarWidgetView(Context context, CalendarSnapshot snapshot, CalendarWidgetSettings settings, PaletteColors palette) {
        super(context); this.snapshot = snapshot; this.settings = settings; this.palette = palette;
        cardBackground = PaletteColors.mix(palette.surface, palette.text, 0.22f);
        refresh();
    }
    void start() { stop(); active = true; tick.run(); }
    void stop() { active = false; handler.removeCallbacks(tick); }
    void setFillOpacity(int percent) { fillOpacity = Math.max(0, Math.min(100, percent)); invalidate(); }
    int pages() { return Math.max(1, (display.days.size() + pageSize - 1) / pageSize); }
    void turnPage(int direction) { page = Math.floorMod(page + direction, pages()); invalidate(); }
    java.time.LocalDate firstVisibleDate() { return display.days.isEmpty()?null:display.days.get(Math.min(display.days.size()-1,page*pageSize)).date; }
    private String header() {
        String today = CalendarLabels.date(getContext(), java.time.LocalDate.now());
        return getContext().getString(R.string.hb_cal_today, today) + " · "
                + getContext().getString(R.string.hb_cal_days, settings.holidayLookaheadDays);
    }
    private void refresh() {
        display = snapshot.display(settings, Instant.now()); page = Math.min(page, pages() - 1);
        StringBuilder description = new StringBuilder(header());
        for (CalendarSnapshot.Day day : display.days) {
            description.append(". ").append(CalendarLabels.date(getContext(), day.date));
            for (CalendarSnapshot.Event event : day.events) description.append(". ").append(CalendarLabels.event(getContext(), event, settings, Instant.now()));
            description.append(". ").append(String.join(", ", day.countries()));
        }
        description.append(". ").append(status()); setContentDescription(description); invalidate();
    }
    private String status() {
        if (settings.enabled.isEmpty()) return getContext().getString(R.string.hb_cal_no_sources);
        if (!display.incomplete.isEmpty()) return getContext().getString(R.string.hb_cal_incomplete);
        if (!display.offline.isEmpty()) return getContext().getString(R.string.hb_cal_offline);
        return "";
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float width = getWidth() / density, height = getHeight() / density;
        if (width < 1 || height < 1) return;
        int outer = canvas.save(); canvas.scale(density, density);
        paint.setStyle(Paint.Style.FILL); paint.setColor(palette.surface); paint.setAlpha(255 * fillOpacity / 100);
        canvas.drawRoundRect(2, 2, width - 2, height - 2, 12, 12, paint); paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(isFocused() ? 2 : 1);
        paint.setColor(isFocused() ? palette.text : palette.edge);
        canvas.drawRoundRect(2, 2, width - 2, height - 2, 12, 12, paint); paint.setStyle(Paint.Style.FILL);
        CalendarWidgetGeometry grid = new CalendarWidgetGeometry(width, height);
        pageSize = grid.pageSize(); page = Math.min(page, pages() - 1);
        String pageLabel = pages() > 1 ? getContext().getString(R.string.hb_cal_page, page + 1, pages()) : "";
        if (grid.compact) {
            textPaint.setTextSize(10); textPaint.setTypeface(Typeface.DEFAULT);
            float pageWidth = pageLabel.isEmpty() ? 0 : Math.min((width - 20) / 2, textPaint.measureText(pageLabel) + 8);
            text(canvas, header(), 10, 17, 11, palette.text, width - 20 - pageWidth, true);
            if (pageWidth > 0) text(canvas, pageLabel, width - 10 - pageWidth, 17, 10, palette.muted, pageWidth, false);
        } else {
            text(canvas, header(), 10, 22, 13, palette.text, width - 20, true);
            int[] weekLabels = {R.string.hb_cal_week_this, R.string.hb_cal_week_next, R.string.hb_cal_week_both};
            text(canvas, getContext().getString(R.string.hb_cal_windows, getContext().getString(weekLabels[settings.flowerWeeks.ordinal()]),
                    getContext().getString(weekLabels[settings.buddhistWeeks.ordinal()])), 10, 38, 10, palette.muted, width - 20, false);
        }
        if (display.days.isEmpty()) {
            String empty = !display.incomplete.isEmpty() ? getContext().getString(R.string.hb_cal_incomplete)
                    : getContext().getString(settings.enabled.isEmpty() ? R.string.hb_cal_no_sources : R.string.hb_cal_empty);
            text(canvas, empty, 10, Math.max(40, height / 2), 13, palette.muted, width - 20, false);
        } else {
            int first = page * pageSize, last = Math.min(display.days.size(), first + pageSize);
            for (int index = first; index < last; index++) {
                int offset = index - first; int saved = canvas.save();
                canvas.translate(grid.x(offset), grid.y(offset));
                canvas.scale(grid.cardSize / 200, grid.cardSize / 200);
                card(canvas, display.days.get(index), grid.compact, grid.cardSize); canvas.restoreToCount(saved);
            }
        }
        if (!grid.compact && pages() > 1) {
            text(canvas, status(), 10, height - 25, 10, palette.muted, width - 20, false);
            text(canvas, getContext().getString(R.string.hb_cal_page, page + 1, pages()), 10, height - 10, 10, palette.muted, width - 20, false);
        } else text(canvas, status(), 10, height - 10, 11, palette.muted, width - 20, false);
        canvas.restoreToCount(outer);
    }
    void card(Canvas canvas, CalendarSnapshot.Day day, boolean compact, float dpSize) {
        paint.setColor(cardBackground); paint.setAlpha(255 * fillOpacity / 100);
        canvas.drawRoundRect(1, 1, 199, 199, 12, 12, paint); paint.setAlpha(255);
        Path clip = new Path(); clip.addRoundRect(new RectF(1, 1, 199, 199), 12, 12, Path.Direction.CW);
        int saved = canvas.save(); canvas.clipPath(clip);
        List<String> countries = day.countries();
        if (countries.size() == 1) { band(canvas, countries.get(0), 0, 0, 200); band(canvas, countries.get(0), 193, 0, 200); }
        else if (countries.size() == 2) { band(canvas, countries.get(0), 0, 0, 200); band(canvas, countries.get(1), 193, 0, 200); }
        else {
            for (int i = 0; i < countries.size(); i++) {
                boolean top = i < 2; int count = top ? 2 : countries.size() - 2; int slot = top ? i : i - 2;
                band(canvas, countries.get(i), top ? 0 : 193, slot * 200f / count, 200f / count);
            }
        }
        canvas.restoreToCount(saved);
        String weekday = day.date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, DisplayTime.locale(getContext()));
        float scale = dpSize / 200;
        if (!compact || dpSize >= 64)
            text(canvas, weekday, 13, 34, compact ? Math.max(16, 10 / scale) : 16, palette.text, 174, false);
        text(canvas, String.valueOf(day.date.getDayOfMonth()), 10, 130, 87, palette.text, 130, true);
        boolean flowers = day.events.stream().anyMatch(e -> e.kind == CalendarSnapshot.Kind.MAKE || e.kind == CalendarSnapshot.Kind.SELL);
        boolean phra = day.events.stream().anyMatch(e -> e.kind == CalendarSnapshot.Kind.WAN_PHRA);
        if (flowers) { int icon = canvas.save(); canvas.translate(164, 77); canvas.scale(.8f, .8f); flower(canvas); canvas.restoreToCount(icon); }
        if (phra) { int icon = canvas.save(); canvas.translate(164, flowers ? 126 : 91); canvas.scale(.8f, .8f); buddha(canvas); canvas.restoreToCount(icon); }
        Instant now = Instant.now();
        if(day.events.isEmpty())return;
        if (compact) {
            if (dpSize >= 64) text(canvas, CalendarLabels.event(getContext(), day.events.get(0), settings, now),
                    13, 183, Math.max(15, 10 / scale), palette.text, 174, false);
        } else {
            text(canvas, CalendarLabels.event(getContext(), day.events.get(0), settings, now), 13, 164, 15, palette.text, 174, false);
            String second = day.events.size() > 1 ? CalendarLabels.event(getContext(), day.events.get(1), settings, now) : String.join(" / ", countries);
            text(canvas, second, 13, 185, 13, palette.text, 174, false);
        }
    }
    private void band(Canvas canvas, String country, float y, float x, float width) {
        int[] colors; int[] weights;
        switch (country) {
            case "TH": colors = new int[]{0xffa51931, Color.WHITE, 0xff2d2a4a, Color.WHITE, 0xffa51931}; weights = new int[]{1,1,2,1,1}; break;
            case "ES": colors = new int[]{0xffaa151b, 0xfff1bf00, 0xffaa151b}; weights = new int[]{1,2,1}; break;
            case "US": colors = new int[]{0xffb22234, Color.WHITE, 0xff3c3b6e}; weights = new int[]{1,1,1}; break;
            default: colors = new int[]{0xffde2910, 0xffffde00, 0xffde2910}; weights = new int[]{1,2,1}; break;
        }
        int total = 0; for (int w : weights) total += w;
        for (int i = 0; i < colors.length; i++) { float segment = width * weights[i] / total;
            paint.setColor(colors[i]); canvas.drawRect(x, y, x + segment + .1f, y + 7, paint); x += segment; }
    }
    private void text(Canvas canvas, String value, float x, float baseline, float size, int color, float width, boolean bold) {
        textPaint.setTextSize(size); textPaint.setColor(color); textPaint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        canvas.drawText(TextUtils.ellipsize(value, textPaint, Math.max(0, width), TextUtils.TruncateAt.END).toString(), x, baseline, textPaint);
    }
    private void flower(Canvas canvas) {
        paint.setColor(0xffeab2cf);
        for (int i = 0; i < 6; i++) {
            int saved = canvas.save(); canvas.rotate(i * 60);
            canvas.drawOval(new RectF(-9, -28, 9, -4), paint); canvas.restoreToCount(saved);
        }
        paint.setColor(0xffedc968); canvas.drawCircle(0, 0, 9, paint);
    }
    private void buddha(Canvas canvas) {
        paint.setColor(0xffedc968);
        canvas.drawOval(new RectF(-4, -30, 4, -17), paint); canvas.drawCircle(0, -14, 9, paint);
        Path body = new Path(); body.moveTo(-7, -2);
        body.cubicTo(-20, 8, -17, 15, -14, 18); body.lineTo(-28, 24);
        body.cubicTo(-36, 33, -15, 34, 0, 31); body.cubicTo(15, 34, 36, 33, 28, 24);
        body.lineTo(14, 18); body.cubicTo(17, 15, 20, 8, 7, -2); body.close(); canvas.drawPath(body, paint);
    }
    @Override protected void onFocusChanged(boolean gain, int direction, android.graphics.Rect previous) {
        super.onFocusChanged(gain, direction, previous); invalidate();
    }
    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
}
