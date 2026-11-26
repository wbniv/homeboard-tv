package com.gathin.tvhome;

/** Home workspace scale; menus and expanded presentations keep their own dimensions. */
final class DashboardScale {
    private DashboardScale() { }
    static int valid(int percent) { return percent == 50 || percent == 75 ? percent : 100; }
    // Stored values describe the existing renderer transform, not the user's
    // native-workspace reference. Keep saved layouts unchanged when relabelling.
    static int nativePercent(int rendererPercent, boolean uhdWorkspace) {
        return valid(rendererPercent) * (uhdWorkspace ? 2 : 1);
    }
    static int defaultRendererPercent(boolean uhdWorkspace) { return uhdWorkspace ? 50 : 100; }
    static int[] choices(boolean uhdWorkspace) {
        return uhdWorkspace ? new int[]{50,75,100} : new int[]{100,75,50};
    }
    static int cells(int count, int from, int to) {
        return count == 0 ? 0 : Math.max(1, Math.min(24, Math.round(count * valid(from) / (float) valid(to))));
    }
    static float factor(int percent) { return valid(percent) / 100f; }
    static int logicalPixels(int pixels, int percent) {
        return Math.round(pixels / factor(percent));
    }
}
