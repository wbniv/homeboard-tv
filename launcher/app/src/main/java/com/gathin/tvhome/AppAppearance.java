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
    /** Measured on Cast2's Google TV home at 1080p: 152 px icons on a 182 px centre pitch (a 30 px gap). */
    static final float GTV_PITCH_RATIO = 182f / 152f;
    /** Favorites circular icon, in dp. Large enough that the Google TV pitch ratio leaves a Google TV-sized gap. */
    static final int FAVORITE_ICON_DP = 64;
    private AppAppearance() { }

    /**
     * Favorites circular pitch in dp: the Google TV ratio to the icon, but never narrower than a readable label
     * (3.4 label heights; All Apps uses 4).
     */
    static int favoritePitchDp(int labelHeightDp) {
        return Math.max(Math.round(FAVORITE_ICON_DP * GTV_PITCH_RATIO), Math.round(Math.max(0, labelHeightDp) * 3.4f));
    }

    /**
     * The colour a focused icon's halo takes: the average of its opaque pixels, weighted toward saturated ones so a
     * white disc with a small red logo glows red, not gray. {@code fallback} when nothing is opaque.
     */
    static int dominantColor(int[] argb, int fallback) {
        double r = 0, g = 0, b = 0, total = 0;
        for (int px : argb) {
            if ((px >>> 24) < 0xc0) continue;
            int pr = px >> 16 & 0xff, pg = px >> 8 & 0xff, pb = px & 0xff;
            double weight = 1 + (Math.max(pr, Math.max(pg, pb)) - Math.min(pr, Math.min(pg, pb))) / 16.0;
            r += pr * weight; g += pg * weight; b += pb * weight; total += weight;
        }
        if (total == 0) return fallback;
        return 0xff000000 | (int) Math.round(r / total) << 16 | (int) Math.round(g / total) << 8 | (int) Math.round(b / total);
    }

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
