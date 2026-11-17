package com.gathin.tvhome;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DefaultDashboardTest {
    @Test public void firstRunHasExactlyFourPanelsAndNoInventedFavorites() {
        HomeLayout layout = HomeLayout.firstRun();
        assertEquals(Arrays.asList(HomeLayout.RAIN, HomeLayout.CALENDAR, HomeLayout.AIR, HomeLayout.FAVORITES), layout.widgets);
        assertTrue(layout.favoritesContainer); assertTrue(layout.favorites.isEmpty());
        assertTrue(layout.rainSettings.get(HomeLayout.RAIN).automatic);
        assertEquals(24, layout.rainSettings.get(HomeLayout.RAIN).hours);
        assertEquals(6, layout.rainSettings.get(HomeLayout.RAIN).days);
        assertEquals(4, layout.panelSpan(HomeLayout.RAIN).width);
        assertEquals(2, layout.panelSpan(HomeLayout.RAIN).height);
    }
    @Test public void defaultPlacementFitsWithoutOverlapAndKeepsExtras() throws Exception {
        for (int columns : new int[]{4, 6, 8, 10, 12}) for (int rows : new int[]{2, 3, 4, 5, 7}) {
            HomeLayout layout = new HomeLayout(); layout.addPanel("widget:air:2");
            layout.homePositions.put("widget:air:2", new HomeLayout.HomePosition(0,0,0));
            layout.arrangeDefaultDashboard(HomeLayout.RAIN, HomeLayout.AIR, columns, rows);
            Set<String> occupied = new HashSet<>();
            for (HomeLayout.Cell cell : layout.cells(layout.widgets, columns, rows)) {
                assertTrue(cell.row >= 0 && cell.row+cell.height <= rows);
                assertTrue(cell.column >= 0 && cell.column+cell.width <= columns);
                for (int y=cell.row;y<cell.row+cell.height;y++) for (int x=cell.column;x<cell.column+cell.width;x++)
                    assertTrue(occupied.add(cell.page+":"+y+":"+x));
                if (!cell.key.equals("widget:air:2")) assertEquals(rows>=4?0:
                        cell.key.equals(HomeLayout.RAIN)||cell.key.equals(HomeLayout.FAVORITES)?0:1, cell.page);
                else assertTrue(cell.page>0);
            }
            String before = layout.encode();
            layout.arrangeDefaultDashboard(HomeLayout.RAIN, HomeLayout.AIR, columns, rows);
            assertEquals(before, layout.encode());
        }
    }
    @Test public void presetReusesSelectedInstancesAndPreservesSettingsAndOtherWidgets() throws Exception {
        HomeLayout layout = HomeLayout.firstRun(); layout.addPanel(HomeLayout.RAIN+":2"); layout.addPanel("widget:air:2");
        layout.addFavorite("app:example/Main"); layout.order.add("app:other/Main"); layout.hidden.add("app:hidden/Main");
        layout.palette="forest"; layout.uiOpacityPercent=42;
        RainSettings rain=layout.rainSettings.get(HomeLayout.RAIN+":2"); rain.automatic=false;rain.latitude=40;rain.longitude=-3;rain.label="Home";rain.hours=48;
        String before=layout.encode();
        layout.arrangeDefaultDashboard(HomeLayout.RAIN+":2","widget:air:2",8,4);
        assertSame(rain,layout.rainSettings.get(HomeLayout.RAIN+":2")); assertEquals("Home",rain.label); assertEquals(48,rain.hours);
        assertEquals(6,layout.widgets.size()); assertTrue(layout.widgets.contains(HomeLayout.RAIN)); assertTrue(layout.widgets.contains(HomeLayout.AIR));
        assertEquals(Collections.singletonList("app:example/Main"),layout.favorites);
        assertEquals("forest",layout.palette); assertEquals(42,layout.uiOpacityPercent);
        assertEquals(Collections.singletonList("app:other/Main"),layout.order); assertTrue(layout.hidden.contains("app:hidden/Main"));
        HomeLayout undo=HomeLayout.parse(before);
        assertEquals(Arrays.asList(HomeLayout.RAIN,HomeLayout.CALENDAR,HomeLayout.AIR,HomeLayout.FAVORITES,HomeLayout.RAIN+":2","widget:air:2"),undo.widgets);
        assertFalse(undo.homePositions.containsKey(HomeLayout.RAIN+":2"));
        assertEquals(0,undo.homePositions.get(HomeLayout.RAIN).column);
        assertEquals("Home",undo.rainSettings.get(HomeLayout.RAIN+":2").label);
        assertEquals(48,undo.rainSettings.get(HomeLayout.RAIN+":2").hours);
    }
    @Test public void parsingASavedEmptyDashboardNeverRestoresDefaults() throws Exception {
        HomeLayout empty=new HomeLayout();
        assertTrue(HomeLayout.parse(empty.encode()).widgets.isEmpty());
        HomeLayout cleared=HomeLayout.firstRun();for(String key:new ArrayList<>(cleared.widgets))cleared.delete(key,true);
        assertTrue(HomeLayout.parse(cleared.encode()).widgets.isEmpty());
    }
    @Test public void invalidPresetArgumentsLeaveTheOriginalUntouched() {
        HomeLayout original=HomeLayout.firstRun();String before=original.encode();
        assertThrows(IllegalArgumentException.class,()->original.arrangeDefaultDashboard("app:wrong",HomeLayout.AIR,8,4));
        assertThrows(IllegalArgumentException.class,()->original.arrangeDefaultDashboard(HomeLayout.RAIN,HomeLayout.AIR,3,4));
        assertEquals(before,original.encode());
    }
    @Test public void blankPagesUseTheNormalFinitePageSequence() {
        assertEquals(HomePageNavigation.ALL_APPS,HomePageNavigation.next(0,1,1));
        assertEquals(0,HomePageNavigation.next(0,1,-1));
        assertEquals(1,HomePageNavigation.next(0,3,1));
        assertEquals(2,HomePageNavigation.next(1,3,1));
        assertEquals(HomePageNavigation.ALL_APPS,HomePageNavigation.next(2,3,1));
        assertEquals(0,HomePageNavigation.next(1,3,-1));
        assertEquals(HomePageNavigation.ALL_APPS,HomePageNavigation.next(9,2,1));
        assertEquals(HomePageNavigation.ALL_APPS,HomePageNavigation.next(0,0,1));
    }
}
