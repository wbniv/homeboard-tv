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
}
