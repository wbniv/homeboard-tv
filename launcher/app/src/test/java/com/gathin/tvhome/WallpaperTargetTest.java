package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WallpaperTargetTest {
    private static final long MIB = 1024L * 1024;

    /** The expressions WallpaperSurface used before the 4K path, kept here as the regression oracle. */
    private static int[] oldBound(int metricsWidth, int metricsHeight) {
        return new int[]{Math.min(1920, Math.max(1, metricsWidth)), Math.min(1080, Math.max(1, metricsHeight))};
    }
    private static int[] oldSize(int w, int h, int width, int height) {
        double scale = Math.min(1, Math.min((double) width / w, (double) height / h));
        return new int[]{Math.max(1, (int) (w * scale)), Math.max(1, (int) (h * scale))};
    }

    @Test public void ordinaryRenderingKeepsTheOld1080pBoundAndSizes() {
        int[][] metrics = {{1920, 1080}, {3840, 2160}, {1280, 720}, {2560, 1600}, {0, -5}, {1080, 1920}};
        int[][] sources = {{3840, 2160}, {1920, 1080}, {1280, 720}, {4032, 3024}, {65535, 2159}, {1, 1}, {2161, 3841}};
        for (int[] m : metrics) {
            int[] bound = WallpaperTarget.bound(false, m[0], m[1]);
            assertArrayEquals(oldBound(m[0], m[1]), bound);
            for (int[] s : sources)
                assertArrayEquals(oldSize(s[0], s[1], bound[0], bound[1]), WallpaperTarget.size(s[0], s[1], bound[0], bound[1]));
        }
    }

    @Test public void nativeRendererRaisesTheBoundTo2160p() {
        assertArrayEquals(new int[]{3840, 2160}, WallpaperTarget.bound(true, 3840, 2160));
        int[] bound = WallpaperTarget.bound(true, 3840, 2160);
        assertArrayEquals(new int[]{3840, 2160}, WallpaperTarget.size(3840, 2160, bound[0], bound[1]));
        assertArrayEquals(new int[]{3840, 2160}, WallpaperTarget.size(7680, 4320, bound[0], bound[1]));
        // The same 4K source on the ordinary path still lands at 1080p.
        int[] hd = WallpaperTarget.bound(false, 3840, 2160);
        assertArrayEquals(new int[]{1920, 1080}, WallpaperTarget.size(3840, 2160, hd[0], hd[1]));
    }

    @Test public void uhdNeverExceedsTheViewOrEnlargesASource() {
        assertArrayEquals(new int[]{1920, 1080}, WallpaperTarget.bound(true, 1920, 1080));
        assertArrayEquals(new int[]{3840, 2160}, WallpaperTarget.bound(true, 7680, 4320));
        int[] bound = WallpaperTarget.bound(true, 3840, 2160);
        assertArrayEquals(new int[]{1920, 1080}, WallpaperTarget.size(1920, 1080, bound[0], bound[1]));
        assertArrayEquals(new int[]{2880, 2160}, WallpaperTarget.size(5120, 3840, bound[0], bound[1]));
        assertArrayEquals(new int[]{3840, 126}, WallpaperTarget.size(65535, 2159, bound[0], bound[1]));
    }

    @Test public void bytesAreArgb8888() {
        assertEquals(33_177_600L, WallpaperTarget.bytes(3840, 2160));
        assertEquals(8_294_400L, WallpaperTarget.bytes(1920, 1080));
    }

    @Test public void admissionNeedsThreePicturesAboveThresholdPlusMargin() {
        long threshold = 216 * MIB;
        long limit = threshold + WallpaperTarget.MARGIN_BYTES + 3 * WallpaperTarget.bytes(3840, 2160);
        assertTrue(WallpaperTarget.uhdAffordable(false, limit, threshold));
        assertFalse(WallpaperTarget.uhdAffordable(false, limit - 1, threshold));
        assertTrue(WallpaperTarget.uhdAffordable(false, 625 * MIB, threshold));
        assertFalse(WallpaperTarget.uhdAffordable(false, 300 * MIB, threshold));
    }

    @Test public void lowRamDevicesNeverDecodeAt4k() {
        assertFalse(WallpaperTarget.uhdAffordable(true, 4096 * MIB, 0));
    }
}
