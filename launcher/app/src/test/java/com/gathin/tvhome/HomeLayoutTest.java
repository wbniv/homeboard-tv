package com.gathin.tvhome;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class HomeLayoutTest {
    private static final String A = "app:one/one.Main", B = "app:two/two.Main", C = "app:three/three.Main";
    @Test public void explicitEmptyLayoutSurvivesRoundTripAndNewApps() throws Exception {
        HomeLayout s = HomeLayout.firstRun(); s.widgets.clear();
        HomeLayout loaded = HomeLayout.parse(s.encode());
        assertTrue(loaded.widgets.isEmpty());
        assertEquals(Arrays.asList(A, B), loaded.visibleApps(Arrays.asList(A, B)));
        assertTrue(loaded.widgets.isEmpty());
    }
    @Test public void favoriteIsUniqueAndDeleteIsSurfaceSpecific() throws Exception {
        HomeLayout s = new HomeLayout(); s.favorite(A); s.favorite(A);
        s.delete(A, false);
        assertEquals(Collections.singletonList(A), s.widgets);
        assertTrue(s.visibleApps(Collections.singletonList(A)).isEmpty());
        s.hidden.remove(A);
        assertEquals(Collections.singletonList(A), s.visibleApps(Collections.singletonList(A)));
        s.delete(A, true);
        assertTrue(s.widgets.isEmpty());
        assertEquals(Collections.singletonList(A), s.visibleApps(Collections.singletonList(A)));
    }
    @Test public void movePreviewDoesNotMutateSavedStateUntilCommitted() {
        HomeLayout s = new HomeLayout(); s.widgets.addAll(Arrays.asList(A, B, C));
        ArrayList<String> preview = new ArrayList<>(s.widgets); HomeLayout.move(preview, 0, 2);
        assertEquals(Arrays.asList(B, C, A), preview);
        assertEquals(Arrays.asList(A, B, C), s.widgets);
        HomeLayout.move(preview, -1, 0); assertEquals(Arrays.asList(B, C, A), preview);
    }
    @Test public void independentOrderAndHiddenTilesSurviveRoundTrip() throws Exception {
        HomeLayout s = new HomeLayout(); s.widgets.addAll(Arrays.asList(B, A));
        s.order.addAll(Arrays.asList(A, B)); s.hidden.add(A);
        HomeLayout loaded = HomeLayout.parse(s.encode());
        assertEquals(Arrays.asList(B, A), loaded.widgets);
        assertEquals(Arrays.asList(B, C), loaded.visibleApps(Arrays.asList(C, B, A)));
        loaded.hidden.remove(A);
        assertEquals(Arrays.asList(A, B, C), loaded.visibleApps(Arrays.asList(C, B, A)));
    }
    @Test public void removalCleansOnlyMatchingPackageAndMissingActivityDoesNotRemoveFavorites() {
        HomeLayout s = new HomeLayout(); s.widgets.addAll(Arrays.asList(A, B)); s.order.add(A); s.hidden.add(A);
        s.visibleApps(Collections.singletonList(B)); assertEquals(2, s.widgets.size());
        s.packageRemoved("one");
        assertEquals(Collections.singletonList(B), s.widgets); assertTrue(s.order.isEmpty()); assertTrue(s.hidden.isEmpty());
    }
    @Test public void duplicateLauncherActivitiesCollapseToChosenAppTile() {
        String second = "app:one/one.TicketToRideActivity";
        HomeLayout s = new HomeLayout();
        s.widgets.addAll(Arrays.asList(A, second, B));
        s.order.addAll(Arrays.asList(second, A, B));
        s.hidden.add(A);
        assertTrue(s.canonicalizeAppPackage("one", second));
        assertEquals(Arrays.asList(second, B), s.widgets);
        assertEquals(Arrays.asList(second, B), s.order);
        assertTrue(s.hidden.contains(second));
        assertFalse(s.canonicalizeAppPackage("one", second));
    }
    @Test public void responsiveGridPacksFavoritesAroundTwoRowWidget() {
        HomeLayout s = new HomeLayout();
        s.widgets.add(HomeLayout.AIR);
        for (int i = 0; i < 32; i++) s.widgets.add("app:package/app.Main" + i);
        List<HomeLayout.Cell> cells = s.cells(s.widgets, 10, 4);
        assertEquals(2, s.pageCount(s.widgets, 10, 4));
        HomeLayout.Cell air = cells.get(0);
        assertEquals(0, air.page); assertEquals(0, air.row); assertEquals(0, air.column);
        assertEquals(5, air.width); assertEquals(2, air.height);
        HomeLayout.Cell firstFavorite = cells.get(1);
        assertEquals(0, firstFavorite.page); assertEquals(0, firstFavorite.row);
        assertEquals(5, firstFavorite.column); assertEquals(1, firstFavorite.width);
        assertEquals(1, s.pageOf("app:package/app.Main31", s.widgets, 10, 4));
        s.setPanelSize(HomeLayout.AIR, HomeLayout.PANEL_SMALL);
        assertEquals(1, s.pageCount(s.widgets, 10, 4));
        assertEquals(3, s.cells(s.widgets, 10, 4).get(0).width);
        assertEquals(2, s.cells(s.widgets, 10, 4).get(0).height);
    }
    @Test public void gridDensityPersists() throws Exception {
        HomeLayout s = new HomeLayout(); s.homeColumns = 12; s.homeRows = 10;
        HomeLayout loaded = HomeLayout.parse(s.encode());
        assertEquals(12, loaded.homeColumns); assertEquals(10, loaded.homeRows);
    }
    @Test public void appearanceAndMotionPersist() throws Exception {
        HomeLayout s = new HomeLayout(); s.palette = "forest"; s.reducedMotion = true;
        s.setPanelSize(HomeLayout.AIR, HomeLayout.PANEL_SMALL); s.defaultPanelSize = HomeLayout.PANEL_LARGE; s.appBanners = false;
        HomeLayout copy = HomeLayout.parse(s.encode());
        assertFalse(copy.appBanners); assertFalse(HomeLayout.firstRun().appBanners);
        assertTrue(copy.reducedMotion); assertEquals("forest", copy.palette);
        assertEquals(HomeLayout.PANEL_SMALL, copy.panelSize(HomeLayout.AIR));
        assertEquals(HomeLayout.PANEL_LARGE, copy.defaultPanelSize);
        assertEquals(0, copy.homeColumns); assertEquals(0, copy.homeRows);
    }

    @Test public void newPanelsUseDefaultAndCanBeResizedIndividually() {
        HomeLayout s = new HomeLayout(); s.defaultPanelSize = HomeLayout.PANEL_LARGE;
        s.addPanel("widget:weather"); s.defaultPanelSize = HomeLayout.PANEL_SMALL;
        s.addPanel("widget:calendar"); s.setPanelSize("widget:weather", HomeLayout.PANEL_MEDIUM);
        assertEquals(HomeLayout.PANEL_MEDIUM, s.panelSize("widget:weather"));
        assertEquals(HomeLayout.PANEL_SMALL, s.panelSize("widget:calendar"));
        assertEquals(5, s.cells(Arrays.asList("widget:weather"), 10, 4).get(0).width);
        assertEquals(2, s.cells(Arrays.asList("widget:weather"), 10, 4).get(0).height);
    }

    @Test public void changingDefaultDoesNotResizeExistingPanel() throws Exception {
        HomeLayout existing = HomeLayout.firstRun();
        existing.defaultPanelSize = HomeLayout.PANEL_LARGE;
        HomeLayout saved = HomeLayout.parse(existing.encode());
        assertEquals(HomeLayout.PANEL_MEDIUM, saved.panelSize(HomeLayout.AIR));
        saved.addPanel("widget:calendar");
        assertEquals(HomeLayout.PANEL_LARGE, saved.panelSize("widget:calendar"));
    }

    @Test public void exactPanelDimensionsPersistAndOverridePreset() throws Exception {
        HomeLayout layout = HomeLayout.firstRun();
        layout.setPanelSpan(HomeLayout.AIR, 4, 3);
        HomeLayout restored = HomeLayout.parse(layout.encode());
        HomeLayout.Cell cell = restored.cells(restored.widgets, 10, 5).get(0);
        assertEquals(4, cell.width);
        assertEquals(3, cell.height);
        restored.setPanelSpan(HomeLayout.AIR, 1, 1);
        cell = restored.cells(restored.widgets, 10, 5).get(0);
        assertEquals(1, cell.width);
        assertEquals(1, cell.height);
    }

    @Test public void multipleAirWidgetsKeepIndependentSizeAndPlacement() throws Exception {
        HomeLayout layout = HomeLayout.firstRun();
        layout.addPanel("widget:air:2");
        layout.setPanelSize("widget:air:2", HomeLayout.PANEL_SMALL);
        layout.setPanelSpan("widget:air:2", 2, 1);
        layout.homePositions.put("widget:air:2", new HomeLayout.HomePosition(0, 3, 7));
        HomeLayout restored = HomeLayout.parse(layout.encode());
        assertTrue(restored.widgets.contains("widget:air:2"));
        assertEquals(HomeLayout.PANEL_SMALL, restored.panelSize("widget:air:2"));
        HomeLayout.Cell second = null;
        for (HomeLayout.Cell cell : restored.cells(restored.widgets, 10, 5))
            if ("widget:air:2".equals(cell.key)) second = cell;
        assertNotNull(second); assertEquals(7, second.column); assertEquals(3, second.row);
        assertEquals(2, second.width); assertEquals(1, second.height);
    }

    @Test public void dashboardTilesCanOccupyAnyGridCellAndPersistGaps() throws Exception {
        HomeLayout layout = new HomeLayout(); layout.widgets.addAll(Arrays.asList(A, B, C));
        layout.homePositions.put(A, new HomeLayout.HomePosition(0, 1, 3));
        layout.homePositions.put(B, new HomeLayout.HomePosition(0, 0, 0));
        HomeLayout restored = HomeLayout.parse(layout.encode());
        List<HomeLayout.Cell> cells = restored.cells(restored.widgets, 4, 2);
        assertCell(cells, A, 0, 1, 3);
        assertCell(cells, B, 0, 0, 0);
        assertCell(cells, C, 0, 0, 1);
    }

    @Test public void occupiedMoveTargetKeepsMovingTileThereAndDisplacesOtherTiles() {
        HomeLayout layout = new HomeLayout(); layout.widgets.addAll(Arrays.asList(A, B, C));
        java.util.LinkedHashMap<String, HomeLayout.HomePosition> positions = new java.util.LinkedHashMap<>();
        positions.put(A, new HomeLayout.HomePosition(0, 0, 1));
        positions.put(B, new HomeLayout.HomePosition(0, 0, 1));
        positions.put(C, new HomeLayout.HomePosition(0, 0, 2));
        List<HomeLayout.Cell> cells = layout.cells(layout.widgets, 4, 2, positions);
        assertCell(cells, A, 0, 0, 1);
        assertCell(cells, B, 0, 0, 2);
        assertCell(cells, C, 0, 0, 3);
    }

    private void assertCell(List<HomeLayout.Cell> cells, String key, int page, int row, int column) {
        for (HomeLayout.Cell cell : cells) if (key.equals(cell.key)) {
            assertEquals(page, cell.page); assertEquals(row, cell.row); assertEquals(column, cell.column); return;
        }
        fail("Missing cell for " + key);
    }

    @Test public void deletingPanelRemovesItsExactDimensions() {
        HomeLayout layout = HomeLayout.firstRun();
        layout.setPanelSpan(HomeLayout.AIR, 3, 1);
        layout.delete(HomeLayout.AIR, true);
        assertNull(layout.panelSpan(HomeLayout.AIR));
    }

    @Test public void oldCompactWidthMigratesToSmallPanelSize() throws Exception {
        HomeLayout migrated = HomeLayout.parse("{\"widgets\":[\"widget:air\"],\"order\":[],\"hidden\":[],\"airWidth\":1}");
        assertEquals(HomeLayout.PANEL_SMALL, migrated.panelSize(HomeLayout.AIR));
    }

    @Test public void homeAppSizeDefaultsAndValidatesAtParsingBoundary() throws Exception {
        String empty = "{\"widgets\":[],\"order\":[],\"hidden\":[]}";
        assertEquals(HomeLayout.APP_SMALL, HomeLayout.parse(empty).homeAppSize);
        org.json.JSONObject saved = new org.json.JSONObject(empty).put("homeAppSize", "wide");
        assertEquals(HomeLayout.APP_SMALL, HomeLayout.parse(saved.toString()).homeAppSize);
        saved.put("homeAppSize", "banner");
        assertEquals(HomeLayout.APP_BANNER, HomeLayout.parse(saved.toString()).homeAppSize);
    }

    @Test public void homeAndAllAppsArtworkRemainIndependentAcrossRoundTrips() throws Exception {
        for (String size : new String[]{HomeLayout.APP_SMALL, HomeLayout.APP_BANNER}) {
            for (boolean allAppsBanners : new boolean[]{false, true}) {
                HomeLayout layout = new HomeLayout(); layout.favorite(A);
                layout.homeAppSize = size; layout.appBanners = allAppsBanners;
                HomeLayout restored = HomeLayout.parse(layout.encode());
                assertEquals(HomeLayout.APP_BANNER.equals(size), restored.appUsesBanner(true));
                assertEquals(allAppsBanners, restored.appUsesBanner(false));
                List<HomeLayout.Cell> automatic = restored.cells(restored.widgets, 4, 2);
                restored.homePositions.put(A, new HomeLayout.HomePosition(0, 1, 0));
                List<HomeLayout.Cell> positioned = restored.cells(restored.widgets, 4, 2);
                int expected = HomeLayout.APP_BANNER.equals(size) ? 2 : 1;
                assertEquals(expected, automatic.get(0).width); assertEquals(1, automatic.get(0).height);
                assertEquals(expected, positioned.get(0).width); assertEquals(1, positioned.get(0).height);
            }
        }
    }

    @Test public void resizingFavoritesKeepsWidgetsFixedEvenWhenAppsPrecedeThem() {
        HomeLayout layout = new HomeLayout();
        layout.widgets.addAll(Arrays.asList(A, HomeLayout.AIR, B, C));
        layout.setPanelSpan(HomeLayout.AIR, 2, 2);
        layout.homePositions.put(A, new HomeLayout.HomePosition(0, 0, 0));
        layout.homePositions.put(HomeLayout.AIR, new HomeLayout.HomePosition(0, 0, 1));
        layout.homePositions.put(B, new HomeLayout.HomePosition(0, 0, 3));
        layout.homeColumns = 4; layout.homeRows = 2;
        assertTrue(layout.setHomeAppSize(HomeLayout.APP_BANNER, 4, 2, 4, 2));
        List<HomeLayout.Cell> cells = layout.cells(layout.widgets, 4, 2);
        assertCell(cells, HomeLayout.AIR, 0, 0, 1);
        assertEquals(2, cells.get(1).width); assertEquals(2, cells.get(1).height);
        assertEquals(Arrays.asList(A, HomeLayout.AIR, B, C), layout.widgets);
        assertEquals(4, layout.homeColumns); assertEquals(2, layout.homeRows);
        assertNonoverlapping(cells, 4, 2);
    }

    @Test public void bannerReflowOverflowsAndSnapshotUndoRestoresExactSmallLayout() throws Exception {
        HomeLayout layout = new HomeLayout();
        for (int i = 0; i < 8; i++) layout.favorite("app:test/" + i);
        layout.homePositions.put("app:test/0", new HomeLayout.HomePosition(0, 0, 3));
        String before = layout.encode();
        List<HomeLayout.Cell> original = layout.cells(layout.widgets, 4, 2);
        assertEquals(1, layout.pageCount(layout.widgets, 4, 2));
        assertTrue(layout.setHomeAppSize(HomeLayout.APP_BANNER, 4, 2, 4, 2));
        assertTrue(layout.pageCount(layout.widgets, 4, 2) > 1);
        assertNonoverlapping(layout.cells(layout.widgets, 4, 2), 4, 2);
        HomeLayout undo = HomeLayout.parse(before);
        assertEquals(HomeLayout.APP_SMALL, undo.homeAppSize);
        assertEquals(1, undo.pageCount(undo.widgets, 4, 2));
        List<HomeLayout.Cell> restored = undo.cells(undo.widgets, 4, 2);
        for (int i = 0; i < original.size(); i++) {
            HomeLayout.Cell cell = original.get(i);
            assertCell(restored, cell.key, cell.page, cell.row, cell.column);
        }
    }

    @Test public void shrinkingKeepsBannerOriginsAndReselectingDoesNotReflow() {
        HomeLayout layout = new HomeLayout(); layout.widgets.addAll(Arrays.asList(A, B, C));
        layout.setHomeAppSize(HomeLayout.APP_BANNER, 4, 2, 4, 2);
        List<HomeLayout.Cell> banners = layout.cells(layout.widgets, 4, 2);
        layout.setHomeAppSize(HomeLayout.APP_SMALL, 4, 2, 4, 2);
        for (HomeLayout.Cell cell : banners)
            assertCell(layout.cells(layout.widgets, 4, 2), cell.key, cell.page, cell.row, cell.column);
        String before = layout.encode();
        assertFalse(layout.setHomeAppSize(HomeLayout.APP_SMALL, 4, 2, 4, 2));
        assertEquals(before, layout.encode());
    }

    @Test public void emptyHomeAndNewFavoritesUseTheSelectedSize() {
        HomeLayout layout = new HomeLayout();
        layout.setHomeAppSize(HomeLayout.APP_BANNER, 4, 2, 4, 2);
        assertTrue(layout.widgets.isEmpty()); assertTrue(layout.homePositions.isEmpty());
        layout.favorite(A); assertEquals(2, layout.cells(layout.widgets, 4, 2).get(0).width);
        layout.delete(A, true); layout.favorite(A);
        assertEquals(HomeLayout.APP_BANNER, layout.homeAppSize);
        assertEquals(2, layout.cells(layout.widgets, 4, 2).get(0).width);
    }

    @Test public void wideMovePreviewClaimsItsWholeSpanWithoutMutatingSavedPositions() {
        HomeLayout layout = new HomeLayout(); layout.widgets.addAll(Arrays.asList(A, B, C));
        layout.setHomeAppSize(HomeLayout.APP_BANNER, 4, 2, 4, 2);
        String saved = layout.encode();
        java.util.Map<String, HomeLayout.HomePosition> preview = new java.util.LinkedHashMap<>();
        preview.put(A, new HomeLayout.HomePosition(0, 0, 2));
        for (java.util.Map.Entry<String, HomeLayout.HomePosition> entry : layout.homePositions.entrySet())
            if (!entry.getKey().equals(A)) preview.put(entry.getKey(), entry.getValue());
        List<HomeLayout.Cell> cells = layout.cells(layout.widgets, 4, 2, preview);
        assertCell(cells, A, 0, 0, 2); assertEquals(2, cells.get(0).width);
        assertNonoverlapping(cells, 4, 2); assertEquals(saved, layout.encode());
    }

    @Test public void bannerReflowRespectsEveryGridDensityAndKeepsWidgetConfiguration() {
        for (int columns : new int[]{4, 6, 8, 10, 12}) for (int rows : new int[]{2, 4, 7}) {
            HomeLayout layout = HomeLayout.firstRun(); layout.setPanelSpan(HomeLayout.AIR, 2, 2);
            layout.setPanelSize(HomeLayout.AIR, HomeLayout.PANEL_SMALL);
            for (int i = 0; i < 32; i++) layout.favorite("app:density/" + i);
            HomeLayout.Cell widget = layout.cells(layout.widgets, columns, rows).get(0);
            layout.setHomeAppSize(HomeLayout.APP_BANNER, columns, rows, columns, rows);
            List<HomeLayout.Cell> cells = layout.cells(layout.widgets, columns, rows);
            assertCell(cells, HomeLayout.AIR, widget.page, widget.row, widget.column);
            assertEquals(2, cells.get(0).width); assertEquals(2, cells.get(0).height);
            assertEquals(HomeLayout.PANEL_SMALL, layout.panelSize(HomeLayout.AIR));
            for (HomeLayout.Cell cell : cells) if (cell.key.startsWith("app:")) {
                assertEquals(2, cell.width); assertEquals(1, cell.height);
            }
            assertNonoverlapping(cells, columns, rows);
        }
    }

    private void assertNonoverlapping(List<HomeLayout.Cell> cells, int columns, int rows) {
        java.util.Set<String> occupied = new java.util.HashSet<>();
        for (HomeLayout.Cell cell : cells) {
            assertTrue(cell.column >= 0 && cell.column + cell.width <= columns);
            assertTrue(cell.row >= 0 && cell.row + cell.height <= rows);
            for (int y = cell.row; y < cell.row + cell.height; y++)
                for (int x = cell.column; x < cell.column + cell.width; x++)
                    assertTrue("Overlapping " + cell.key, occupied.add(cell.page + ":" + y + ":" + x));
        }
    }

    @Test public void switchingStyleResolvesAgainstItsOwnGridAndUndoRestoresTheOriginalGridLayout() throws Exception {
        HomeLayout layout = HomeLayout.firstRun();
        layout.addPanel("widget:air:2"); layout.favorite(A); layout.favorite(B);
        layout.setPanelSpan(HomeLayout.AIR, 2, 1);
        layout.setPanelSpan("widget:air:2", 2, 1);
        layout.homePositions.put(HomeLayout.AIR, new HomeLayout.HomePosition(0, 3, 9));
        layout.homePositions.put("widget:air:2", new HomeLayout.HomePosition(0, 2, 2));
        layout.homePositions.put(A, new HomeLayout.HomePosition(0, 0, 10));
        layout.homePositions.put(B, new HomeLayout.HomePosition(0, 3, 0));
        String before = layout.encode();
        assertTrue(layout.setHomeAppSize(HomeLayout.APP_BANNER, 11, 4, 12, 3));
        List<HomeLayout.Cell> banners = layout.cells(layout.widgets, 12, 3);
        assertCell(banners, "widget:air:2", 0, 2, 2); // Still fits, so it stays fixed.
        assertCell(banners, HomeLayout.AIR, 1, 0, 9); // Fourth row moves to the next page.
        assertNonoverlapping(banners, 12, 3);
        assertEquals(2, layout.panelSpan(HomeLayout.AIR).width);
        HomeLayout restored = HomeLayout.parse(before);
        assertEquals(HomeLayout.APP_SMALL, restored.homeAppSize);
        List<HomeLayout.Cell> icons = restored.cells(restored.widgets, 11, 4);
        assertCell(icons, HomeLayout.AIR, 0, 3, 9);
        assertCell(icons, A, 0, 0, 10);
        assertCell(icons, B, 0, 3, 0);
        assertNonoverlapping(icons, 11, 4);
    }
}
