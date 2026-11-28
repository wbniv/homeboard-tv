package com.gathin.tvhome;

/**
 * The size a provider wallpaper is decoded at. Ordinary rendering keeps the 1920 x 1080 bound it has always had;
 * only the native 4K renderer raises it to 3840 x 2160, and only while the device can spare the memory
 * (docs/plans/2026-11-28-4k-wallpapers-and-video.md).
 */
final class WallpaperTarget {
    static final int HD_WIDTH = 1920, HD_HEIGHT = 1080, UHD_WIDTH = 3840, UHD_HEIGHT = 2160;
    /** Copies alive at the worst moment of a 4K change: the software decode, its hardware copy, the outgoing picture. */
    static final int PEAK_COPIES = 3;
    /** Headroom kept above Android's own low-memory threshold after that peak. */
    static final long MARGIN_BYTES = 64L * 1024 * 1024;

    private WallpaperTarget() { }

    /** The largest decode size: the view's metrics, capped at 1080p, or at 2160p for the native 4K renderer. */
    static int[] bound(boolean uhd, int metricsWidth, int metricsHeight) {
        return new int[]{Math.min(uhd ? UHD_WIDTH : HD_WIDTH, Math.max(1, metricsWidth)),
                Math.min(uhd ? UHD_HEIGHT : HD_HEIGHT, Math.max(1, metricsHeight))};
    }

    /** Fits a source inside a bound without enlarging it, keeping its aspect ratio. */
    static int[] size(int width, int height, int boundWidth, int boundHeight) {
        double scale = Math.min(1, Math.min((double) boundWidth / width, (double) boundHeight / height));
        return new int[]{Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale))};
    }

    /** ARGB_8888 bytes of a picture. */
    static long bytes(int width, int height) {
        return 4L * width * height;
    }

    /** Whether a 4K decode leaves the system above its low-memory threshold at the change's peak. */
    static boolean uhdAffordable(boolean lowRamDevice, long availableBytes, long thresholdBytes) {
        return !lowRamDevice
                && availableBytes - PEAK_COPIES * bytes(UHD_WIDTH, UHD_HEIGHT) >= thresholdBytes + MARGIN_BYTES;
    }
}
