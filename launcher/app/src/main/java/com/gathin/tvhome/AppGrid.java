package com.gathin.tvhome;

/** All Apps cell budget in dp, including the label and focus clearance. */
final class AppGrid {
    private AppGrid() { }
    static int columns(int viewportWidth, boolean banners, boolean circular) {
        return banners ? 4 : Math.max(1, (viewportWidth - 88) / (circular ? 96 : 108));
    }
    static int rows(int viewportHeight, boolean banners) {
        return banners ? 2 : Math.max(1, (viewportHeight - 132) / 96);
    }
}
