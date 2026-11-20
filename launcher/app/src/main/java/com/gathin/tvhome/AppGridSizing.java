package com.gathin.tvhome;

/** Minimum readable cell pitch in dp, including artwork focus, label and inter-cell gaps. */
final class AppGridSizing {
    static final int SIDE_MARGIN = 32, HOME_TOP = 88, APPS_TOP = 80, BOTTOM_MARGIN = 52;
    static final int COLUMN_GAP = 8, ROW_GAP = 6, TILE_PADDING = 4;
    /** The Favorites panel's inner padding on each side, in dp. */
    static final int PANEL_SIDE_PADDING = 10;
    private AppGridSizing() { }

    static int columnPitch(int labelHeight, boolean banners) {
        return Math.max(banners ? 144 : 80, Math.max(0, labelHeight) * (banners ? 6 : 4));
    }
    static int rowPitch(int labelHeight, boolean banners) {
        int artwork = banners ? 54 : AppAppearance.ICON_DIAMETER_DP;
        int focus = (int) Math.ceil(artwork * AppAppearance.FOCUS_SCALE);
        return focus + Math.max(0, labelHeight) + (banners ? 6 + 16 : 8) + TILE_PADDING + ROW_GAP;
    }
    static int columns(int availableWidth, int labelHeight, boolean banners) {
        return Math.max(1, availableWidth / columnPitch(labelHeight, banners));
    }
    static int rows(int availableHeight, int labelHeight, boolean banners) {
        return Math.max(1, availableHeight / rowPitch(labelHeight, banners));
    }
    /** Columns of the Favorites panel: the All Apps pitch over the panel's width less its side padding. */
    static int panelColumns(int panelWidthDp, int labelHeight, boolean banners) {
        return columns(panelWidthDp - PANEL_SIDE_PADDING * 2, labelHeight, banners);
    }
    /**
     * Width of one Favorites ribbon cell: the viewport divided evenly among as many whole cells as fit at
     * {@code pitch}. A fixed pitch left a sliced icon, and its label, at one end of the panel.
     */
    static int ribbonCell(int viewport, int pitch) {
        return Math.max(1, viewport / Math.max(1, viewport / Math.max(1, pitch)));
    }
    static int homeColumns(int availableWidth, int labelHeight, boolean banners) {
        int cards = columns(availableWidth, labelHeight, banners);
        return Math.max(4, Math.min(12, banners ? cards * 2 : cards));
    }
}
