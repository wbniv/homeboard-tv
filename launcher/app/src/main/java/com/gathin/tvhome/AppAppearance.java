package com.gathin.tvhome;

/** Ratios measured in the 1920 x 1080 Google TV reference; arguments use physical pixels. */
final class AppAppearance {
    static final int ICON_DIAMETER_DP = 48;
    static final float FOCUS_SCALE = 168f / 152f;
    static final int TRANSITION_MS = 180; // Provisional until reference motion is recorded.
    static final int SELECTED_LABEL = 0xffe4f3ff;
    static final int RESTING_LABEL = 0xff94989d;
    static final float HALO_RADIUS = 0.95f;
    /** The reference launcher's focus rim, measured on a near-black wallpaper. */
    static final int RIM = 0xff434546;
    private AppAppearance() { }

    /** WCAG relative luminance of an opaque colour. */
    static float luminance(int argb) {
        float[] c = {(argb >> 16 & 0xff) / 255f, (argb >> 8 & 0xff) / 255f, (argb & 0xff) / 255f};
        for (int i = 0; i < 3; i++) c[i] = c[i] <= 0.03928f ? c[i] / 12.92f : (float) Math.pow((c[i] + 0.055f) / 1.055f, 2.4f);
        return 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2];
    }

    /** WCAG contrast ratio, 1 to 21. */
    static float contrast(int a, int b) {
        float x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05f) / (Math.min(x, y) + 0.05f);
    }

    /**
     * The reference rim, lightened only as far as needed to reach {@code minContrast} against {@code background}.
     * The reference gray reads on a near-black wallpaper but all but vanishes on a lighter panel.
     */
    static int rimOver(int background, float minContrast) {
        int color = RIM;
        for (int i = 0; i < 40 && contrast(color, background) < minContrast; i++) {
            int r = (color >> 16 & 0xff), g = (color >> 8 & 0xff), b = (color & 0xff);
            r += Math.round((255 - r) * 0.08f); g += Math.round((255 - g) * 0.08f); b += Math.round((255 - b) * 0.08f);
            color = 0xff000000 | r << 16 | g << 8 | b;
        }
        return color;
    }

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
