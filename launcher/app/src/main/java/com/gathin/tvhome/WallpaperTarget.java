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
    /**
     * Animated pictures decode every frame on the CPU at their source size, so cost follows the source's pixels:
     * a 3840 x 2160 source ran 5x slow on Cast2 and 6.7x on Cast1 at the 1080p target. Above this many source
     * pixels only the first frame is shown, as a still. Provisional until the animated probe matrix sets it
     * (docs/plans/2026-11-28-animated-wallpapers-from-large-sources.md).
     */
    static final long ANIMATED_MAX_SOURCE_PIXELS = 1920L * 1080;
    /** The largest decode size for an animated picture; never the 4K bound. Provisional, as above. */
    static final int ANIMATED_WIDTH = 1920, ANIMATED_HEIGHT = 1080;

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

    /** Whether an animated source is small enough to play; otherwise its first frame is shown as a still. */
    static boolean animates(int sourceWidth, int sourceHeight) {
        return (long) Math.max(1, sourceWidth) * Math.max(1, sourceHeight) <= ANIMATED_MAX_SOURCE_PIXELS;
    }

    /** The decode size of an animated source that plays: fit inside the animated cap and the 1080p metric bound. */
    static int[] animatedSize(int sourceWidth, int sourceHeight, int[] hdBound) {
        return size(sourceWidth, sourceHeight, Math.min(ANIMATED_WIDTH, hdBound[0]), Math.min(ANIMATED_HEIGHT, hdBound[1]));
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
