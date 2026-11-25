package com.gathin.tvhome;

/** Home workspace scale; menus and expanded presentations keep their own dimensions. */
final class DashboardScale {
    private DashboardScale() { }
    static int valid(int percent) { return percent == 50 || percent == 75 ? percent : 100; }
    static int cells(int count, int from, int to) {
        return count == 0 ? 0 : Math.max(1, Math.min(24, Math.round(count * valid(from) / (float) valid(to))));
    }
    static float factor(int percent) { return valid(percent) / 100f; }
    static int logicalPixels(int pixels, int percent) {
        return Math.round(pixels / factor(percent));
    }
}
