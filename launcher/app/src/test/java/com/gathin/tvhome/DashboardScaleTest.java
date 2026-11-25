package com.gathin.tvhome;

import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class DashboardScaleTest {
    private Object normalized(Object value) throws Exception {
        if (value instanceof JSONObject) {
            JSONObject object=(JSONObject)value;
            java.util.Map<String,Object> result=new java.util.TreeMap<>();
            for (java.util.Iterator<String> keys=object.keys();keys.hasNext();) {
                String key=keys.next();result.put(key,normalized(object.get(key)));
            }
            return result;
        }
        if (value instanceof org.json.JSONArray) {
            org.json.JSONArray array=(org.json.JSONArray)value;
            java.util.List<Object> result=new java.util.ArrayList<>();
            for (int i=0;i<array.length();i++)result.add(normalized(array.get(i)));
            return result;
        }
        return value;
    }
    @Test public void halfScaleQuadruplesCapacityWithoutChangingWidgetDataOrPlacements() throws Exception {
        HomeLayout layout = HomeLayout.firstRun();
        JSONObject before = new JSONObject(layout.encode());
        layout.setDashboardScale(50);
        assertEquals(16, layout.homeColumns); assertEquals(8, layout.homeRows);
        assertEquals(4 * 8 * 4, layout.homeColumns * layout.homeRows);
        JSONObject after = new JSONObject(layout.encode());
        for (String key : List.of("widgets", "panelSpans", "homePositions", "rainSettings", "calendarSettings", "androidWidgets", "favorites"))
            assertEquals(key, before.get(key).toString(), after.get(key).toString());
        HomeLayout loaded = HomeLayout.parse(layout.encode());
        assertEquals(50, loaded.dashboardScale); assertEquals(16, loaded.homeColumns);
        loaded.setDashboardScale(100);
        assertEquals(normalized(before), normalized(new JSONObject(loaded.encode())));
    }
    @Test public void autoCountsRemainAutoAndFitGrowsAtEachScale() {
        HomeLayout layout = new HomeLayout(); layout.setDashboardScale(50);
        assertEquals(0, layout.homeColumns); assertEquals(0, layout.homeRows);
        assertEquals(24, DashboardScale.cells(12,100,50));
        assertEquals(11, DashboardScale.cells(8,100,75));
        assertEquals(8, DashboardScale.cells(4,100,50));
        assertEquals(2000, DashboardScale.logicalPixels(1000,50));
        assertEquals(1000, DashboardScale.logicalPixels(1000,100));
        assertEquals(24, DashboardScale.cells(24,100,50));
        layout.homeColumns=4;layout.homeRows=2;layout.setDashboardScale(100);
        assertEquals(4,layout.homeColumns);assertEquals(2,layout.homeRows);
    }
    @Test public void automaticWidgetSizesStayAtTheirOriginalCellSpanAndPreviewIsIsolated() throws Exception {
        HomeLayout saved=new HomeLayout();saved.homeColumns=8;saved.homeRows=4;
        saved.addPanel(HomeLayout.AIR);
        HomeLayout.Cell original=saved.cells(saved.widgets,8,4).get(0);
        HomeLayout draft=HomeLayout.parse(saved.encode());draft.setDashboardScale(50,8,4);
        HomeLayout.Cell smaller=draft.cells(draft.widgets,16,8).get(0);
        assertEquals(original.width,smaller.width);assertEquals(original.height,smaller.height);
        assertEquals(100,saved.dashboardScale);assertEquals(8,saved.homeColumns);
        assertNull(saved.panelSpan(HomeLayout.AIR));
        assertEquals(original.width,HomeLayout.parse(draft.encode()).panelSpan(HomeLayout.AIR).width);
    }
    @Test public void layoutUndoRestoresExactScaleGridAndWidgetConfiguration() throws Exception {
        HomeLayout layout = HomeLayout.firstRun();
        String before = layout.encode();
        layout.setDashboardScale(50);
        layout.setPanelSpan(HomeLayout.RAIN,16,1);
        layout.homePositions.put(HomeLayout.AIR,new HomeLayout.HomePosition(0,5,12));
        assertEquals(16,HomeLayout.parse(layout.encode()).panelSpan(HomeLayout.RAIN).width);
        HomeLayout undone = HomeLayout.restore(before,layout.headerClock);
        assertEquals(normalized(new JSONObject(before)), normalized(new JSONObject(undone.encode()))); assertEquals(100,undone.dashboardScale);
    }
    @Test public void unsupportedScaleCannotProduceZeroOrUnboundedWorkspace() throws Exception {
        for (int invalid : new int[]{0,-1,25,101,Integer.MAX_VALUE}) {
            HomeLayout layout=HomeLayout.parse(new JSONObject(HomeLayout.firstRun().encode()).put("dashboardScale",invalid).toString());
            assertEquals(100,layout.dashboardScale);
            assertEquals(1f,DashboardScale.factor(invalid),0f);
        }
    }
    @Test public void aScaledGridAndFullWidthWidgetSurvivePhoneEditing() throws Exception {
        HomeLayout layout=HomeLayout.firstRun();layout.seedDefaultFavorites=false;
        layout.setDashboardScale(50);layout.setPanelSpan(HomeLayout.RAIN,16,1);
        JSONObject air=new JSONObject();
        JSONObject draft=PhoneConfiguration.snapshot(layout,air,16,8);
        PhoneConfiguration.Result edited=PhoneConfiguration.validate(draft,layout,air,Set.of());
        assertEquals(50,edited.layout.dashboardScale);assertEquals(16,edited.layout.homeColumns);
        assertEquals(8,edited.layout.homeRows);assertEquals(16,edited.layout.panelSpan(HomeLayout.RAIN).width);
        assertEquals(layout.widgets,edited.layout.widgets);
    }
}
