package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppAppearanceTest {
    @Test public void haloAndRingLeaveRoomOutsideEnlargedArtworkAtEveryDensity() {
        for (float density : new float[]{1f, 1.5f, 2f, 3f}) {
            float diameter = 48 * density;
            float artworkRadius = diameter * AppAppearance.FOCUS_SCALE / 2;
            float ringInner = AppAppearance.ringOuterRadius(diameter) - AppAppearance.ringWidth(diameter);
            assertTrue("Ring must not cover the enlarged artwork", ringInner > artworkRadius);
            assertTrue("Halo must not be clipped to the rim", AppAppearance.focusExtent(diameter) / 2f
                    > AppAppearance.ringOuterRadius(diameter));
        }
    }

    @Test public void theRingFitsTheAllAppsCellThatTheFavoritesPanelShares() {
        for (int label : new int[]{19, 24, 32, 40})
            assertTrue("Ring outer diameter must fit the cell",
                    AppAppearance.ringOuterRadius(AppAppearance.ICON_DIAMETER_DP) * 2 <= AppGridSizing.columnPitch(label, false));
    }

    @Test public void denseGridAndLargeTextKeepTheFocusedArtworkAndLabelInsideTheCell() {
        for (int width : new int[]{70, 96, 160}) {
            for (int height : new int[]{64, 96, 180}) {
                for (int labelHeight : new int[]{20, 38, 48}) {
                    int diameter = AppAppearance.fitIcon(96, width, height, labelHeight, 8);
                    assertTrue(diameter >= 0 && diameter <= 96);
                    assertTrue(diameter * AppAppearance.FOCUS_SCALE <= width);
                    if (height >= labelHeight + 8)
                        assertTrue(diameter * AppAppearance.FOCUS_SCALE + labelHeight + 8 <= height);
                    else assertEquals(0, diameter);
                }
            }
        }
    }

    @Test public void unmeasuredCellCannotAllocateAVisibleIcon() {
        assertEquals(0, AppAppearance.fitIcon(96, 0, 0, 20, 8));
        assertEquals(0, AppAppearance.fitIcon(96, 100, 18, 20, 8));
    }

    @Test public void bannersKeepAspectLabelAndFocusClearanceOnDenseAndLargeTextGrids() {
        for (int width : new int[]{0, 90, 180, 400}) for (int height : new int[]{0, 64, 96, 220})
            for (int labelHeight : new int[]{20, 38, 48}) {
                int[] size = AppAppearance.fitBanner(width, height, labelHeight, 6, 8);
                assertTrue(size[0] >= 0 && size[1] >= 0);
                assertEquals(size[0] * 9f / 16f, size[1], 1f);
                assertTrue(size[0] * AppAppearance.FOCUS_SCALE <= Math.max(0, width - 16));
                assertTrue(size[1] * AppAppearance.FOCUS_SCALE <= Math.max(0, height - labelHeight - 6 - 16));
            }
    }

    @Test public void theFocusRimReadsOnALighterPanelButKeepsTheReferenceGrayOnADarkOne() {
        // Regression: the reference gray (about 1.4:1 on the Favorites panel) was nearly invisible.
        int panel = 0xff242a36;
        assertTrue(AppAppearance.contrast(AppAppearance.RIM, panel) < 3f);
        int rim = AppAppearance.rimOver(panel, 3f);
        assertTrue(AppAppearance.contrast(rim, panel) >= 3f);
        assertTrue(AppAppearance.luminance(rim) > AppAppearance.luminance(AppAppearance.RIM));
        // Already readable: unchanged, so All Apps keeps the measured colour.
        assertEquals(AppAppearance.RIM, AppAppearance.rimOver(0xff0e0f10, 1.5f));
        // Never overshoots white, and always terminates, even where 3:1 is unreachable.
        assertTrue(AppAppearance.rimOver(0xffffffff, 3f) != 0);
    }

    @Test public void favoriteIconsSitAGoogleTvGapApartButKeepAReadableLabel() {
        // Regression: Favorites kept 84 px icons on a 176 px pitch (a 92 px gap); Google TV leaves 30 px of 152.
        for (int label : new int[]{19, 24, 32}) {
            int pitch = AppAppearance.favoritePitchDp(label);
            float ratio = pitch / (float) AppAppearance.FAVORITE_ICON_DP;
            assertTrue("never narrower than a readable label",
                    pitch >= Math.round(AppAppearance.favoriteLabelHeightDp(label) * 3.4f));
            // As on Google TV, the ring may be wider than the pitch, but must not reach the neighbouring icon.
            assertTrue("the ring stays clear of the next icon",
                    AppAppearance.ringOuterRadius(AppAppearance.FAVORITE_ICON_DP) < pitch - AppAppearance.FAVORITE_ICON_DP / 2f);
            if (label <= 24) assertTrue("close to Google TV's 1.2 x (was 2.1 x): " + ratio, ratio <= 1.4f);
        }
        assertEquals(182f / 152f, AppAppearance.GTV_PITCH_RATIO, 0.001f);
    }

    @Test public void theHaloTakesTheIconsColourNotItsWhitestPixels() {
        int orange = 0xffff8000, white = 0xffffffff, clear = 0x00000000;
        assertEquals(orange, AppAppearance.dominantColor(new int[]{orange, orange, orange}, 0xff123456));
        // A white disc with a small red logo glows red, as the reference does for YouTube.
        int[] youtube = new int[100];
        java.util.Arrays.fill(youtube, white);
        for (int i = 0; i < 12; i++) youtube[i] = 0xffff0000;
        int halo = AppAppearance.dominantColor(youtube, 0);
        assertTrue("red leads: " + Integer.toHexString(halo), (halo >> 16 & 0xff) == 0xff && (halo >> 8 & 0xff) < 120);
        // Transparent pixels are ignored, and an empty icon falls back.
        assertEquals(orange, AppAppearance.dominantColor(new int[]{clear, orange, clear}, 0));
        assertEquals(0xff123456, AppAppearance.dominantColor(new int[]{clear, clear}, 0xff123456));
    }
}
