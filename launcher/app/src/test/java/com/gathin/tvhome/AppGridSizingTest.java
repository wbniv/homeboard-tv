package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppGridSizingTest {
    @Test public void typicalTvUsesDifferentDenseLayoutsForIconsAndBanners() {
        int width = 960 - 2 * AppGridSizing.SIDE_MARGIN;
        int homeHeight = 540 - AppGridSizing.HOME_TOP - AppGridSizing.BOTTOM_MARGIN;
        int appsHeight = 540 - AppGridSizing.APPS_TOP - AppGridSizing.BOTTOM_MARGIN;
        assertEquals(11, AppGridSizing.homeColumns(width, 20, false));
        assertEquals(4, AppGridSizing.rows(homeHeight, 20, false));
        assertEquals(12, AppGridSizing.homeColumns(width, 20, true)); // Six two-column banners.
        assertEquals(3, AppGridSizing.rows(homeHeight, 20, true));
        assertEquals(11, AppGridSizing.columns(width, 20, false));
        assertEquals(4, AppGridSizing.rows(appsHeight, 20, false));
        assertEquals(6, AppGridSizing.columns(width, 20, true));
        assertEquals(3, AppGridSizing.rows(appsHeight, 20, true));
    }

    @Test public void gridsUseEveryReadableRowAndColumnAcrossTvSizesAndTextHeights() {
        for (int width : new int[]{640, 960, 1280, 1920})
            for (int height : new int[]{360, 540, 720, 1080})
                for (int label : new int[]{19, 24, 32, 40})
                    for (boolean banners : new boolean[]{false, true}) {
                        int availableWidth = width - AppGridSizing.SIDE_MARGIN * 2;
                        int availableHeight = height - AppGridSizing.APPS_TOP - AppGridSizing.BOTTOM_MARGIN;
                        int columns = AppGridSizing.columns(availableWidth, label, banners);
                        int rows = AppGridSizing.rows(availableHeight, label, banners);
                        int columnPitch = AppGridSizing.columnPitch(label, banners);
                        int rowPitch = AppGridSizing.rowPitch(label, banners);
                        assertTrue(columns * columnPitch <= availableWidth);
                        assertTrue(rows * rowPitch <= availableHeight);
                        assertTrue((columns + 1) * columnPitch > availableWidth);
                        assertTrue((rows + 1) * rowPitch > availableHeight);
                    }
    }

    @Test public void minimumPitchesFitFullArtworkWithLabelsAndFocusedScale() {
        for (int label : new int[]{19, 24, 32, 40}) {
            int width = AppGridSizing.columnPitch(label, false) - AppGridSizing.COLUMN_GAP - AppGridSizing.TILE_PADDING;
            int height = AppGridSizing.rowPitch(label, false) - AppGridSizing.ROW_GAP - AppGridSizing.TILE_PADDING;
            assertEquals(48, AppAppearance.fitIcon(48, width, height, label, 8));
            width = AppGridSizing.columnPitch(label, true) - AppGridSizing.COLUMN_GAP - AppGridSizing.TILE_PADDING;
            height = AppGridSizing.rowPitch(label, true) - AppGridSizing.ROW_GAP - AppGridSizing.TILE_PADDING;
            int[] banner = AppAppearance.fitBanner(width, height, label, 6, 8);
            assertTrue(banner[0] >= 96); assertTrue(banner[1] >= 54);
        }
    }

    @Test public void largeTextReducesDensityAndHomePlacementColumnsStayEvenForBanners() {
        assertTrue(AppGridSizing.columns(896, 40, false) < AppGridSizing.columns(896, 20, false));
        assertTrue(AppGridSizing.rows(408, 56, true) < AppGridSizing.rows(408, 20, true));
        for (int width : new int[]{576, 896, 1216}) for (int label : new int[]{19, 32, 40}) {
            int columns = AppGridSizing.homeColumns(width, label, true);
            assertEquals(0, columns % 2); assertTrue(columns >= 4 && columns <= 12);
        }
    }
    @Test public void denseIconsFitMoreThanThirtySixSlotsAtTypicalTvSize() {
        assertTrue(AppGridSizing.columns(896, 19, false) * AppGridSizing.rows(408, 19, false) >= 36);
        assertFalse(HomeLayout.firstRun().appBanners);
    }
}
