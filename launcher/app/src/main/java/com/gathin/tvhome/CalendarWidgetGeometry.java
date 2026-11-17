package com.gathin.tvhome;

/** Date-card packing in dp; short widgets use a single horizontal strip. */
final class CalendarWidgetGeometry {
    final boolean compact;
    final int columns, rows;
    final float top, cellWidth, cellHeight, cardSize;

    CalendarWidgetGeometry(float width, float height) {
        compact = height < 190;
        top = compact ? 24 : 44;
        float availableWidth = Math.max(1, width - 12);
        float availableHeight = Math.max(1, height - top - (compact ? 20 : 34));
        float pitch = compact ? Math.max(1, Math.min(96, availableHeight - 6)) + 6 : 102;
        columns = Math.max(1, (int) (availableWidth / pitch));
        rows = compact ? 1 : Math.max(1, (int) (availableHeight / 104));
        cellWidth = availableWidth / columns;
        cellHeight = availableHeight / rows;
        cardSize = Math.max(1, Math.min(cellWidth - 6, cellHeight - 6));
    }
    int pageSize() { return columns * rows; }
    float x(int offset) { return 6 + (offset % columns) * cellWidth + (cellWidth - cardSize) / 2; }
    float y(int offset) { return top + (offset / columns) * cellHeight; }
}
