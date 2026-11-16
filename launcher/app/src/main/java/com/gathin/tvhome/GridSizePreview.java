package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Scaled dashboard schematic: editing columns emphasizes columns; rows emphasizes rows. */
final class GridSizePreview extends View {
    private final int columns, rows;
    private final boolean editingColumns, selected;
    private final PaletteColors palette;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    GridSizePreview(Context context, int columns, int rows, boolean editingColumns, boolean selected, PaletteColors palette) {
        super(context); this.columns = Math.max(1, columns); this.rows = Math.max(1, rows);
        this.editingColumns = editingColumns; this.selected = selected; this.palette = palette;
        setContentDescription(context.getString(R.string.hb_grid_preview_dimensions, this.columns, this.rows));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float width = Math.max(0, getWidth() - 16 * density);
        float height = Math.min(width * 9 / 16, Math.max(0, getHeight() - 12 * density));
        if (height <= 0 || width <= 0) return;
        width = Math.min(width, height * 16 / 9);
        float x = (getWidth() - width) / 2, y = (getHeight() - height) / 2;
        paint.setColor(palette.surface); canvas.drawRoundRect(x, y, x + width, y + height, 5 * density, 5 * density, paint);
        float pad = 4 * density, gap = 2 * density;
        float cw = (width - 2 * pad) / columns, ch = (height - 2 * pad) / rows;
        for (int row = 0; row < rows; row++) for (int col = 0; col < columns; col++) {
            int group = editingColumns ? col : row;
            paint.setColor(group % 2 == 0 ? palette.focus : palette.edge);
            paint.setAlpha(selected ? 255 : 170);
            float left = x + pad + col * cw, top = y + pad + row * ch;
            canvas.drawRoundRect(left, top, left + Math.max(1, cw - gap), top + Math.max(1, ch - gap), density, density, paint);
        }
        paint.setAlpha(255);
    }
}
