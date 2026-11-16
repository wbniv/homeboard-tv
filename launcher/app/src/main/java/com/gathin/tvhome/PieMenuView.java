package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

/** Four remote-operated compass choices around a stable center hub. */
final class PieMenuView extends ViewGroup {
    interface Listener {
        void onArm(int arm);
        void onCenter();
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final PaletteColors palette;
    private final MenuIconView[] icons = new MenuIconView[4];
    private final String[] labels = new String[4];
    private final boolean[] current = new boolean[4];
    private final RectF oval = new RectF();
    private final Path wedge = new Path();
    private String title = "";
    private String breadcrumb = "";
    private String centerAction = "Back";
    private String pageHint = "";
    private boolean emptyState;
    private int highlighted = -1;
    private Listener listener;

    PieMenuView(Context context, PaletteColors palette) {
        super(context);
        this.palette = palette;
        density = getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        labelPaint.setColor(palette.text);
        labelPaint.setTextSize(dp(18));
        labelPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        smallPaint.setColor(palette.muted);
        smallPaint.setTextSize(dp(11));
        for (int i = 0; i < icons.length; i++) {
            icons[i] = new MenuIconView(context, MenuIconView.Kind.APP, palette);
            icons[i].setFocusable(false);
            addView(icons[i]);
        }
    }

    void setContent(String title, String breadcrumb, String centerAction, String pageHint,
                    String[] choices, MenuIconView.Kind[] kinds, boolean[] selected, Listener listener) {
        this.title = title == null ? "" : title;
        this.breadcrumb = breadcrumb == null ? "" : breadcrumb;
        this.centerAction = centerAction == null ? "Back" : centerAction;
        this.pageHint = pageHint == null ? "" : pageHint;
        this.listener = listener;
        emptyState = true;
        StringBuilder description = new StringBuilder(this.title).append(". ");
        String[] compass = {"Up", "Right", "Down", "Left"};
        for (int i = 0; i < 4; i++) {
            labels[i] = choices != null && i < choices.length ? choices[i] : "";
            if (!labels[i].isEmpty()) emptyState = false;
            icons[i].setVisibility(labels[i].isEmpty() ? INVISIBLE : VISIBLE);
            current[i] = selected != null && i < selected.length && selected[i];
            if (kinds != null && i < kinds.length && kinds[i] != null)
                icons[i] = replaceIcon(i, kinds[i]);
            if (!labels[i].isEmpty()) description.append(compass[i]).append(": ").append(labels[i])
                    .append(current[i] ? ", current" : "").append(". ");
        }
        description.append("Center: ").append(this.centerAction).append(". Back returns.");
        setContentDescription(description.toString());
        invalidate(); requestLayout();
    }

    private MenuIconView replaceIcon(int index, MenuIconView.Kind kind) {
        if (icons[index] != null) removeView(icons[index]);
        icons[index] = new MenuIconView(getContext(), kind, palette);
        icons[index].setFocusable(false);
        addView(icons[index]);
        return icons[index];
    }

    void setHighlighted(int arm) {
        highlighted = arm;
        invalidate();
    }

    private int dp(float value) { return Math.round(value * density); }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int iconSize = dp(34);
        for (MenuIconView icon : icons) icon.measure(MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float outer = PieMenuGeometry.outerRadius(Math.min(getWidth(), getHeight()), dp(12));
        float inner = PieMenuGeometry.innerRadius(outer, dp(104));
        float mid = (outer + inner) / 2f;
        float iconRadius = mid - dp(25);
        float sideIconRadius = PieMenuGeometry.sideIconRadius(outer, inner, dp(30), dp(160));
        int iconSize = dp(34);
        float[][] positions = {
                {cx, cy - iconRadius}, {cx + sideIconRadius, cy},
                {cx, cy + iconRadius}, {cx - sideIconRadius, cy}
        };
        for (int i = 0; i < icons.length; i++) {
            float ix = positions[i][0], iy = positions[i][1];
            icons[i].layout(Math.round(ix - iconSize / 2f), Math.round(iy - iconSize / 2f),
                    Math.round(ix + iconSize / 2f), Math.round(iy + iconSize / 2f));
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float outer = PieMenuGeometry.outerRadius(Math.min(getWidth(), getHeight()), dp(12));
        float inner = PieMenuGeometry.innerRadius(outer, dp(104));
        oval.set(cx - outer, cy - outer, cx + outer, cy + outer);
        RectF innerOval = new RectF(cx - inner, cy - inner, cx + inner, cy + inner);
        int[] starts = {-135, -45, 45, 135};
        for (int i = 0; i < 4; i++) {
            if (labels[i] == null || labels[i].isEmpty()) continue;
            wedge.reset();
            wedge.arcTo(oval, starts[i] + 4, 82);
            float end = starts[i] + 86;
            double endRad = Math.toRadians(end);
            wedge.lineTo(cx + inner * (float) Math.cos(endRad), cy + inner * (float) Math.sin(endRad));
            wedge.arcTo(innerOval, end, -82);
            wedge.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(UiBackgroundDrawable.colorWithOpacity(
                    i == highlighted ? palette.focus : palette.raised, WallpaperSettings.PIE_BACKGROUND_OPACITY));
            canvas.drawPath(wedge, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(i == highlighted ? 2.5f : 1.25f));
            paint.setColor(i == highlighted ? palette.text : palette.edge);
            canvas.drawPath(wedge, paint);
            drawArmLabel(canvas, i, labels[i], current[i], cx, cy, outer, inner);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(UiBackgroundDrawable.colorWithOpacity(palette.background, WallpaperSettings.PIE_BACKGROUND_OPACITY));
        canvas.drawCircle(cx, cy, inner - dp(3), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(palette.edge);
        canvas.drawCircle(cx, cy, inner - dp(3), paint);
        paint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(palette.text);
        labelPaint.setTextSize(dp(emptyState ? 17 : title.length() > 20 ? 15 : 18));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(ellipsize(title, dp(170), labelPaint), cx, cy - dp(emptyState ? 37 : 20), labelPaint);
        smallPaint.setTextAlign(Paint.Align.CENTER);
        smallPaint.setColor(emptyState ? palette.text : palette.muted);
        if (emptyState) smallPaint.setTextSize(dp(14));
        canvas.drawText(ellipsize(breadcrumb, dp(172), smallPaint), cx, cy + dp(emptyState ? -8 : 1), smallPaint);
        labelPaint.setTextSize(dp(13));
        labelPaint.setColor(palette.focus);
        canvas.drawText(ellipsize(centerAction, dp(168), labelPaint), cx, cy + dp(emptyState ? 23 : 28), labelPaint);
        if (!pageHint.isEmpty()) {
            smallPaint.setColor(palette.muted);
            if (emptyState) smallPaint.setTextSize(dp(12));
            canvas.drawText(ellipsize(pageHint, dp(176), smallPaint), cx, cy + dp(emptyState ? 48 : 47), smallPaint);
        }
    }

    private void drawArmLabel(Canvas canvas, int arm, String label, boolean isCurrent,
                              float cx, float cy, float outer, float inner) {
        float mid = (outer + inner) / 2f;
        float labelRadius = mid + dp(33);
        float sideIconRadius = PieMenuGeometry.sideIconRadius(outer, inner, dp(30), dp(160));
        float iconSize = dp(34);
        float sideLabelStart = PieMenuGeometry.sideLabelStart(sideIconRadius, iconSize, dp(12));
        float sideLabelMax = PieMenuGeometry.sideLabelWidth(outer, sideIconRadius, iconSize, dp(12), dp(24));
        float verticalLabelMax = PieMenuGeometry.verticalLabelWidth(outer, labelRadius, dp(205), dp(20));
        labelPaint.setColor(palette.text);
        labelPaint.setTextSize(dp(label.length() > 17 ? 14 : 17));
        String shortLabel = ellipsize(label, arm == 1 || arm == 3 ? sideLabelMax : verticalLabelMax, labelPaint);
        float x, y;
        if (arm == 0) { x = cx; y = cy - labelRadius; labelPaint.setTextAlign(Paint.Align.CENTER); }
        else if (arm == 1) { x = cx + sideLabelStart; y = cy + dp(6); labelPaint.setTextAlign(Paint.Align.LEFT); }
        else if (arm == 2) { x = cx; y = cy + labelRadius + dp(7); labelPaint.setTextAlign(Paint.Align.CENTER); }
        else { x = cx - sideLabelStart; y = cy + dp(6); labelPaint.setTextAlign(Paint.Align.RIGHT); }
        canvas.drawText(shortLabel, x, y, labelPaint);
        if (isCurrent) {
            smallPaint.setColor(palette.focus);
            String marker = "Current";
            if (arm == 0) canvas.drawText(marker, x, y + dp(15), smallPaint);
            else if (arm == 2) canvas.drawText(marker, x, y + dp(15), smallPaint);
            else canvas.drawText(marker, x, y + dp(15), smallPaint);
        }
    }

    private String ellipsize(String value, float maxWidth, Paint p) {
        if (value == null || p.measureText(value) <= maxWidth) return value == null ? "" : value;
        String dots = "…";
        int end = value.length();
        while (end > 1 && p.measureText(value.substring(0, end) + dots) > maxWidth) end--;
        return value.substring(0, Math.max(1, end)) + dots;
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
            float dx = event.getX() - getWidth() / 2f;
            float dy = event.getY() - getHeight() / 2f;
            if (Math.hypot(dx, dy) < dp(100)) {
                if (listener != null) listener.onCenter();
            } else {
                double angle = Math.toDegrees(Math.atan2(dy, dx));
                int arm = angle >= -135 && angle < -45 ? 0 : angle >= -45 && angle < 45 ? 1
                        : angle >= 45 && angle < 135 ? 2 : 3;
                if (listener != null && labels[arm] != null && !labels[arm].isEmpty()) listener.onArm(arm);
            }
            return true;
        }
        return true;
    }
}
