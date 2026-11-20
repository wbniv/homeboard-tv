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

    @Test public void favoriteIconsSitCloseTogetherAndTheRingFitsTheCell() {
        assertTrue("Pitch must not drift back to the old 104 dp spacing",
                AppAppearance.FAVORITE_ICON_PITCH_DP <= AppAppearance.ICON_DIAMETER_DP * 1.5f);
        assertTrue("Ring outer diameter must fit the cell",
                AppAppearance.ringOuterRadius(AppAppearance.ICON_DIAMETER_DP) * 2 <= AppAppearance.FAVORITE_ICON_PITCH_DP);
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
}
