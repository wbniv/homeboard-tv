package com.gathin.tvhome;

/** Ratios measured in the 1920 x 1080 Google TV reference; arguments use physical pixels. */
final class AppAppearance {
    static final int ICON_DIAMETER_DP = 76;
    static final float FOCUS_SCALE = 168f / 152f;
    static final int TRANSITION_MS = 180; // Provisional until reference motion is recorded.
    static final int SELECTED_LABEL = 0xffe4f3ff;
    static final int RESTING_LABEL = 0xff94989d;
    static final float HALO_RADIUS = 0.95f;
    private AppAppearance() { }

    static float ringOuterRadius(float diameter) { return diameter * 95f / 152f; }
    static float ringWidth(float diameter) { return diameter * 5f / 152f; }
    static int focusExtent(float diameter) { return (int) Math.ceil(diameter * HALO_RADIUS * 2) + 2; }

    /** Fit the fixed label slot and focused artwork without changing the user's saved grid. */
    static int fitIcon(int preferred, int width, int height, int labelHeight, int gap) {
        int available = Math.max(0, Math.min(width, height - labelHeight - gap));
        return Math.max(0, Math.min(preferred, (int) (available / FOCUS_SCALE)));
    }

    /** 16:9 artwork, with label and rectangular focus clearance reserved before scaling. */
    static int[] fitBanner(int width, int height, int labelHeight, int gap, int clearance) {
        int availableWidth = Math.max(0, width - clearance * 2);
        int availableHeight = Math.max(0, height - labelHeight - gap - clearance * 2);
        int imageWidth = (int) (Math.min(availableWidth, availableHeight * 16f / 9f) / FOCUS_SCALE);
        return new int[]{imageWidth, imageWidth * 9 / 16};
    }
}
