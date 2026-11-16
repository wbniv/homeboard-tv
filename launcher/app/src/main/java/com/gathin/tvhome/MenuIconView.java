package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small, consistent line icons for Homeboard's remote-operated menus. */
final class MenuIconView extends View {
    enum Kind {
        APP, MOVE, FAVORITE, DELETE, UNINSTALL, INFO, SETTINGS, AIR, RESIZE, ADD,
        RESTORE, WIDGET, GRID, APPEARANCE, ARTWORK, BEHAVIOR, HOME, WALLPAPER, THEME, MOON,
        FOREST, PALETTE, CIRCLE, MOTION, BANNER, CANCEL, CONFIRM, LOCATION, CLOSE,
        NEXT, PREVIOUS, LICENSE, UNDO
    }

    private final Kind kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean selected;
    private int swatchColor = 0xFF36506B;
    private final PaletteColors palette;

    MenuIconView(Context context, Kind kind, PaletteColors palette) {
        super(context);
        this.kind = kind;
        this.palette = palette;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setSelectedChoice(boolean value) {
        selected = value;
        invalidate();
    }

    void setSwatchColor(int color) { swatchColor = color; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        canvas.save();
        canvas.translate((getWidth() - size) / 2f, (getHeight() - size) / 2f);
        canvas.scale(size / 24f, size / 24f);
        paint.setColor(selected ? palette.focus : palette.text);
        paint.setStrokeWidth(1.8f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        switch (kind) {
            case APP:
                rect(canvas, 3, 3, 9, 9); rect(canvas, 15, 3, 21, 9);
                rect(canvas, 3, 15, 9, 21); rect(canvas, 15, 15, 21, 21); break;
            case MOVE:
                line(canvas, 12, 3, 12, 21); line(canvas, 3, 12, 21, 12);
                path(canvas, 12, 3, 9, 6); path(canvas, 12, 3, 15, 6);
                path(canvas, 12, 21, 9, 18); path(canvas, 12, 21, 15, 18);
                path(canvas, 3, 12, 6, 9); path(canvas, 3, 12, 6, 15);
                path(canvas, 21, 12, 18, 9); path(canvas, 21, 12, 18, 15); break;
            case FAVORITE:
                polygon(canvas, 12, 2.5f, 14.9f, 8.7f, 21.7f, 9.5f, 16.7f, 14.2f,
                        18, 21, 12, 17.7f, 6, 21, 7.3f, 14.2f, 2.3f, 9.5f, 9.1f, 8.7f); break;
            case DELETE:
                line(canvas, 4, 6, 20, 6); line(canvas, 9, 3.5f, 15, 3.5f);
                path(canvas, 6, 7, 7, 21, 17, 21, 18, 7);
                line(canvas, 10, 10, 10, 18); line(canvas, 14, 10, 14, 18); break;
            case UNINSTALL:
                rect(canvas, 4, 4, 20, 20); line(canvas, 4, 9, 20, 9);
                line(canvas, 12, 12, 12, 18); path(canvas, 9, 15, 12, 18, 15, 15); break;
            case INFO:
                circle(canvas, 12, 12, 9); line(canvas, 12, 11, 12, 17);
                circle(canvas, 12, 7.5f, .65f); break;
            case SETTINGS:
            case BEHAVIOR:
                line(canvas, 3, 6, 21, 6); line(canvas, 3, 12, 21, 12); line(canvas, 3, 18, 21, 18);
                circle(canvas, kind == Kind.BEHAVIOR ? 8 : 15, 6, 1.6f);
                circle(canvas, kind == Kind.BEHAVIOR ? 16 : 8, 12, 1.6f);
                circle(canvas, kind == Kind.BEHAVIOR ? 11 : 16, 18, 1.6f); break;
            case AIR:
                path(canvas, 5, 16, 3.5f, 14.4f, 3.5f, 12, 5, 10.5f);
                path(canvas, 5, 10.5f, 5.5f, 6.5f, 9.5f, 5.5f, 12, 8);
                path(canvas, 12, 8, 14.5f, 5.2f, 19, 7, 19, 10);
                path(canvas, 19, 10, 22, 11, 21, 15, 18, 16);
                line(canvas, 5, 16, 18, 16); circle(canvas, 8, 20, .6f); circle(canvas, 13, 20, .6f); circle(canvas, 18, 20, .6f); break;
            case RESIZE:
                path(canvas, 4, 10, 4, 4, 10, 4); path(canvas, 4, 4, 10, 10);
                path(canvas, 20, 14, 20, 20, 14, 20); path(canvas, 20, 20, 14, 14); break;
            case ADD:
                circle(canvas, 12, 12, 9); line(canvas, 12, 7, 12, 17); line(canvas, 7, 12, 17, 12); break;
            case RESTORE:
                path(canvas, 5, 9, 7, 5, 12, 4, 18, 7, 20, 12, 18, 18, 13, 20, 8, 19);
                path(canvas, 5, 9, 5, 4, 10, 5); path(canvas, 12, 8, 12, 12, 16, 14); break;
            case UNDO:
                path(canvas, 9, 7, 5, 7, 5, 11); path(canvas, 5, 7, 9, 3);
                path(canvas, 5, 7, 14, 7, 19, 11, 19, 16, 15, 19, 10, 19); break;
            case WIDGET:
                rect(canvas, 3, 4, 21, 20); line(canvas, 3, 9, 21, 9);
                rect(canvas, 6, 12, 10, 16); rect(canvas, 13, 12, 18, 16); break;
            case GRID:
                rect(canvas, 3, 3, 9, 9); rect(canvas, 15, 3, 21, 9);
                rect(canvas, 3, 15, 9, 21); rect(canvas, 15, 15, 21, 21); break;
            case APPEARANCE:
                path(canvas, 12, 3, 5, 4, 3, 10, 4, 17, 10, 21, 17, 19, 21, 14, 20, 8, 16, 4, 12, 3);
                circle(canvas, 8, 9, .7f); circle(canvas, 12, 7, .7f); circle(canvas, 16, 9, .7f);
                circle(canvas, 8, 14, .7f); break;
            case ARTWORK:
            case WALLPAPER:
            case BANNER:
                rect(canvas, kind == Kind.BANNER ? 2 : 3, kind == Kind.BANNER ? 6 : 3,
                        kind == Kind.BANNER ? 22 : 21, kind == Kind.BANNER ? 18 : 21);
                circle(canvas, 9, 8, 1.4f);
                path(canvas, 4, 18, 9, 12, 12, 15, 15, 11, 20, 18); break;
            case HOME:
                path(canvas, 3, 11, 12, 4, 21, 11); path(canvas, 6, 10, 6, 20, 18, 20, 18, 10);
                rect(canvas, 10, 14, 14, 20); break;
            case THEME:
                path(canvas, 12, 3, 13, 9, 19, 10, 13, 12, 12, 19, 11, 13, 5, 12, 11, 10, 12, 3);
                line(canvas, 19, 3, 19, 7); line(canvas, 17, 5, 21, 5); break;
            case MOON:
                path(canvas, 17, 4, 11, 5, 8, 10, 10, 17, 17, 20, 21, 17);
                path(canvas, 17, 4, 15, 9, 16, 14, 20, 17); break;
            case FOREST:
                path(canvas, 20, 4, 9, 5, 4, 13, 5, 20, 12, 19, 20, 11, 20, 4);
                line(canvas, 5, 20, 17, 8); line(canvas, 9, 16, 8, 11); line(canvas, 13, 12, 18, 13); break;
            case PALETTE:
                paint.setStyle(Paint.Style.FILL); paint.setColor(swatchColor);
                canvas.drawCircle(12, 12, 8, paint);
                paint.setStyle(Paint.Style.STROKE); paint.setColor(palette.edge);
                canvas.drawCircle(12, 12, 8, paint); break;
            case CIRCLE:
                circle(canvas, 12, 12, 8); break;
            case MOTION:
                path(canvas, 9, 5, 18, 12, 9, 19, 9, 5); line(canvas, 3, 8, 6, 8); line(canvas, 2, 12, 5, 12); line(canvas, 3, 16, 6, 16); break;
            case CANCEL:
            case CLOSE:
                line(canvas, 5, 5, 19, 19); line(canvas, 19, 5, 5, 19); break;
            case CONFIRM:
                path(canvas, 4, 12, 10, 18, 20, 6); break;
            case LOCATION:
                path(canvas, 12, 21, 5, 12, 5, 8, 8, 4, 12, 3, 16, 4, 19, 8, 19, 12, 12, 21);
                circle(canvas, 12, 9, 2); break;
            case NEXT:
                path(canvas, 8, 4, 16, 12, 8, 20); line(canvas, 3, 12, 15, 12); break;
            case PREVIOUS:
                path(canvas, 16, 4, 8, 12, 16, 20); line(canvas, 9, 12, 21, 12); break;
            case LICENSE:
                path(canvas, 3, 5, 11, 5, 12, 7, 13, 5, 21, 5, 21, 19, 13, 19, 12, 21, 11, 19, 3, 19, 3, 5);
                line(canvas, 12, 7, 12, 21); line(canvas, 5, 9, 9, 9); line(canvas, 15, 9, 19, 9); break;
        }
        if (selected) {
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(1);
            canvas.drawCircle(20, 20, 3, paint);
            paint.setColor(palette.background);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f);
            path(canvas, 18.5f, 20, 19.5f, 21, 21.5f, 18.8f);
        }
        canvas.restore();
    }

    private void rect(Canvas c, float l, float t, float r, float b) { c.drawRoundRect(l, t, r, b, 1.2f, 1.2f, paint); }
    private void circle(Canvas c, float x, float y, float r) { c.drawCircle(x, y, r, paint); }
    private void line(Canvas c, float x1, float y1, float x2, float y2) { c.drawLine(x1, y1, x2, y2, paint); }
    private void path(Canvas c, float... points) {
        Path p = new Path(); p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) p.lineTo(points[i], points[i + 1]);
        c.drawPath(p, paint);
    }
    private void polygon(Canvas c, float... points) {
        Path p = new Path(); p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) p.lineTo(points[i], points[i + 1]);
        p.close(); c.drawPath(p, paint);
    }
}
