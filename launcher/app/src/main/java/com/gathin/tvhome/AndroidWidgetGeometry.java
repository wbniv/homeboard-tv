package com.gathin.tvhome;

/**
 * Pure tile geometry for hosted Android widgets, in dp, so the size Homeboard promises a
 * provider and the way a tile presents itself can be tested without a device.
 */
final class AndroidWidgetGeometry {
    private AndroidWidgetGeometry() { }
    /** Tile chrome around the provider view: 16 dp side padding each way; label row 26 dp plus 12 dp top and bottom padding. */
    static final int CHROME_WIDTH = 32, CHROME_HEIGHT = 50;

    enum Presentation { LIVE, TOO_SMALL, TOO_SMALL_IN_LABEL }

    /**
     * Provider bounds arrive in pixels at the density of the context that owns the
     * AppWidgetManager (the real display), not at the density the native 4K renderer gives
     * Homeboard's own views, so they are converted back with that density.
     */
    static int providerDp(int pixels, float providerDensity) {
        return pixels <= 0 ? 0 : Math.round(pixels / Math.max(0.1f, providerDensity));
    }
    /** Smallest span whose content reaches {@code minimumDp}; {@code cells + 1} when none does. */
    static int minimumSpan(int gridDp, int cells, int gapDp, int chromeDp, int paddingDp, int minimumDp) {
        int span = 1;
        while (span <= cells && content(gridDp, span, cells, gapDp, chromeDp, paddingDp) < minimumDp) span++;
        return span;
    }
    /** Content extent of a span of {@code span} cells out of {@code cells} across {@code gridDp}. */
    static int content(int gridDp, int span, int cells, int gapDp, int chromeDp, int paddingDp) {
        return Math.max(0, gridDp * span / Math.max(1, cells) - gapDp - chromeDp - paddingDp);
    }
    /**
     * How a tile shows its provider: live when the content area meets the provider's minimum,
     * a Too small placeholder in the content area otherwise, and the message in the label row
     * when the content area has no height or width left at all, where a placeholder inside it
     * would be invisible.
     */
    static Presentation presentation(AndroidWidgetSpec spec, int contentWidthDp, int contentHeightDp) {
        if (contentWidthDp <= 0 || contentHeightDp <= 0) return Presentation.TOO_SMALL_IN_LABEL;
        return spec.fits(contentWidthDp, contentHeightDp) ? Presentation.LIVE : Presentation.TOO_SMALL;
    }
}
