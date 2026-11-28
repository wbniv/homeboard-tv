package com.gathin.tvhome;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class AndroidWidgetsTest {
    static final String KEY="widget:android:12345678-1234-1234-1234-123456789abc";
    private AndroidWidgetSpec spec(int id){return new AndroidWidgetSpec(id,"net.example/.Widget","Agenda",240,100,180,60,600,400,1);}
    private HomeLayout layout()throws Exception{HomeLayout l=new HomeLayout();l.homeColumns=8;l.homeRows=4;l.addPanel(KEY);l.setPanelSpan(KEY,3,2);l.androidWidgets.put(KEY,spec(41).json());return l;}
    @Test public void savedIdentityAndBoundsRoundTripWithoutChangingOtherWidgets()throws Exception{
        HomeLayout l=layout();l.addPanel(HomeLayout.TIMER);HomeLayout restored=HomeLayout.parse(l.encode());
        assertEquals(41,restored.androidWidget(KEY).id);assertEquals("Agenda",restored.androidWidget(KEY).label);assertTrue(restored.widgets.contains(HomeLayout.TIMER));
        assertTrue(restored.androidWidget(KEY).fits(180,100));assertFalse(restored.androidWidget(KEY).fits(179,100));assertFalse(restored.androidWidget(KEY).fits(240,99));
        assertTrue(spec(1).legalResize(3,2,4,2));assertFalse(spec(1).legalResize(3,2,3,3));
    }
    @Test public void deletionRetainsIdThroughAllUndoRepresentationsUntilEviction()throws Exception{
        HomeLayout original=layout(),removed=HomeLayout.parse(original.encode());removed.delete(KEY,true);
        assertFalse(removed.androidWidgets.has(KEY));
        for(String kind:List.of("layout","phone","home-app-size")){
            Object state=kind.equals("layout")?original.encode():new JSONObject().put("layout",kind.equals("phone")?new JSONObject(original.encode()):original.encode()).toString();
            JSONArray undo=new JSONArray().put(new JSONObject().put("kind",kind).put("state",state));
            assertEquals(Set.of(41),AndroidWidgetReferences.collect(removed.encode(),undo.toString(),""));
        }
        assertEquals(Set.of(),AndroidWidgetReferences.collect(removed.encode(),"[]",""));
        assertEquals(Set.of(42),AndroidWidgetReferences.collect(removed.encode(),"[]","{\"id\":42}"));
    }
    @Test public void corruptOrFutureRetainedStateStopsGarbageCollection()throws Exception{
        HomeLayout l=layout();JSONObject broken=new JSONObject(l.encode()).put("androidWidgets",new JSONObject());
        assertThrows(JSONException.class,()->AndroidWidgetReferences.collect(broken.toString(),"[]",""));
        assertThrows(JSONException.class,()->AndroidWidgetReferences.collect(l.encode(),"[{\"kind\":\"future\"}]",""));
        assertThrows(HomeLayout.NewerSchema.class,()->HomeLayout.parse(new JSONObject(l.encode()).put("schemaVersion",HomeLayout.SCHEMA_VERSION+1).toString()));
    }
    @Test public void browserCanArrangeAndRemoveButCannotForgeAndroidIdentityOrIds()throws Exception{
        HomeLayout l=layout();JSONObject draft=PhoneConfiguration.snapshot(l,new JSONObject(),8,4);JSONObject w=draft.getJSONArray("widgets").getJSONObject(0);
        w.put("page",1).put("width",4).put("presentation",new JSONObject().put("id",999));
        HomeLayout accepted=PhoneConfiguration.validate(draft,l,new JSONObject(),Set.of()).layout;
        assertEquals(41,accepted.androidWidget(KEY).id);assertEquals(4,accepted.panelSpan(KEY).width);
        JSONObject forged=new JSONObject(draft.toString());forged.getJSONArray("widgets").getJSONObject(0).put("key","widget:android:ffffffff-1234-1234-1234-123456789abc");
        assertThrows(PhoneConfiguration.Invalid.class,()->PhoneConfiguration.validate(forged,l,new JSONObject(),Set.of()));
        JSONObject injected=new JSONObject(draft.toString());injected.getJSONArray("widgets").getJSONObject(0).getJSONObject("settings").put("id",999);
        assertThrows(PhoneConfiguration.Invalid.class,()->PhoneConfiguration.validate(injected,l,new JSONObject(),Set.of()));
        w.put("height",3);assertThrows(PhoneConfiguration.Invalid.class,()->PhoneConfiguration.validate(draft,l,new JSONObject(),Set.of()));w.put("height",2);
        draft.put("widgets",new JSONArray());HomeLayout removed=PhoneConfiguration.validate(draft,l,new JSONObject(),Set.of()).layout;assertFalse(removed.androidWidgets.has(KEY));
    }
    @Test public void unknownTileSurvivesLayoutAndPhoneRoundTrip()throws Exception{
        HomeLayout l=layout();l.addPanel("widget:future:7");JSONObject snapshot=PhoneConfiguration.snapshot(l,new JSONObject(),8,4);
        HomeLayout saved=PhoneConfiguration.validate(snapshot,l,new JSONObject(),Set.of()).layout;
        assertTrue(saved.widgets.contains("widget:future:7"));assertEquals(l.androidWidgets.toString(),saved.androidWidgets.toString());
    }
    @Test public void staleActivityResultsCannotCancelTheCurrentPendingOperation()throws Exception{
        JSONObject pending=new JSONObject().put("id",52).put("phase","binding");
        assertFalse(AndroidWidgetPending.matchingResult(pending,7411,51));assertTrue(AndroidWidgetPending.matchingResult(pending,7411,52));
        assertFalse(AndroidWidgetPending.matchingResult(pending,7412,52));pending.put("phase","configuration");
        assertTrue(AndroidWidgetPending.matchingResult(pending,7412,-1));assertFalse(AndroidWidgetPending.matchingResult(pending,7411,52));
    }
    @Test public void failedPendingCancellationPreservesOwnershipInMemory()throws Exception{
        Map<String,String> values=new HashMap<>();String original="{\"id\":52,\"phase\":\"binding\"}";values.put(AndroidWidgetPending.KEY,original);
        android.content.SharedPreferences prefs=(android.content.SharedPreferences)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{android.content.SharedPreferences.class},(proxy,method,args)->{
            if(method.getName().equals("getString"))return values.getOrDefault(args[0],(String)args[1]);
            if(method.getName().equals("edit"))return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{android.content.SharedPreferences.Editor.class},(editor,action,parameters)->{
                switch(action.getName()){
                    case "putString":values.put((String)parameters[0],(String)parameters[1]);return editor;
                    case "remove":values.remove(parameters[0]);return editor;
                    case "commit":return false;
                    default:throw new UnsupportedOperationException(action.getName());
                }
            });throw new UnsupportedOperationException(method.getName());
        });
        assertThrows(IllegalStateException.class,()->AndroidWidgetPending.write(prefs,null));assertEquals(original,values.get(AndroidWidgetPending.KEY));
        assertThrows(IllegalStateException.class,()->AndroidWidgetPending.write(prefs,new JSONObject().put("id",53)));assertEquals(original,values.get(AndroidWidgetPending.KEY));
    }

    /** A tile whose content area collapsed must still say Too small: in the label row, since a placeholder inside a zero-height view is never seen (Cast2, 27 November). */
    @Test public void collapsedContentAreaPutsTooSmallInTheLabelRow(){
        AndroidWidgetSpec s=spec(1);
        assertEquals(AndroidWidgetGeometry.Presentation.TOO_SMALL_IN_LABEL,AndroidWidgetGeometry.presentation(s,300,0));
        assertEquals(AndroidWidgetGeometry.Presentation.TOO_SMALL_IN_LABEL,AndroidWidgetGeometry.presentation(s,0,200));
        assertEquals(AndroidWidgetGeometry.Presentation.TOO_SMALL,AndroidWidgetGeometry.presentation(s,179,200));
        assertEquals(AndroidWidgetGeometry.Presentation.LIVE,AndroidWidgetGeometry.presentation(s,180,100));
        // 24 rows over 452 dp leave 18 dp per cell: a 2-row span has no content height below the 50 dp chrome.
        assertEquals(0,AndroidWidgetGeometry.content(452,2,24,AppGridSizing.ROW_GAP,AndroidWidgetGeometry.CHROME_HEIGHT,0));
    }
    /**
     * Cast2's native 4K renderer gives Homeboard density 4 while AppWidgetManager reports a
     * 240×100 dp provider as 480×200 px at the display's density 2. Reading those pixels with
     * the renderer's density chose a 3×3 span and handed the provider 168×78 dp (27 November).
     */
    @Test public void providerMinimumUsesTheDensityItWasReportedIn(){
        int grid=960-2*AppGridSizing.SIDE_MARGIN,height=540-AppGridSizing.HOME_TOP-AppGridSizing.BOTTOM_MARGIN,pad=16;
        int minW=AndroidWidgetGeometry.providerDp(480,2f),minH=AndroidWidgetGeometry.providerDp(200,2f);
        assertEquals(240,minW);assertEquals(100,minH);
        int spanW=AndroidWidgetGeometry.minimumSpan(grid,12,AppGridSizing.COLUMN_GAP,AndroidWidgetGeometry.CHROME_WIDTH,pad,minW);
        int spanH=AndroidWidgetGeometry.minimumSpan(height,8,AppGridSizing.ROW_GAP,AndroidWidgetGeometry.CHROME_HEIGHT,pad,minH);
        assertEquals(4,spanW);assertEquals(4,spanH);
        assertTrue(AndroidWidgetGeometry.content(grid,spanW,12,AppGridSizing.COLUMN_GAP,AndroidWidgetGeometry.CHROME_WIDTH,pad)>=240);
        assertTrue(AndroidWidgetGeometry.content(height,spanH,8,AppGridSizing.ROW_GAP,AndroidWidgetGeometry.CHROME_HEIGHT,pad)>=100);
        // The defect: the renderer's density halves the minimum and a 3-column span (168 dp) is accepted.
        int wrong=AndroidWidgetGeometry.providerDp(480,4f);
        assertEquals(3,AndroidWidgetGeometry.minimumSpan(grid,12,AppGridSizing.COLUMN_GAP,AndroidWidgetGeometry.CHROME_WIDTH,pad,wrong));
        assertEquals(168,AndroidWidgetGeometry.content(grid,3,12,AppGridSizing.COLUMN_GAP,AndroidWidgetGeometry.CHROME_WIDTH,pad));
    }
    /** Deleting an Android widget said "Removed app shortcut from Home" (Cast2, 28 November): the banner names the provider's widget. */
    @Test public void removalBannerNamesTheWidgetNotAnAppShortcut()throws Exception{
        HomeLayout l=layout();
        assertEquals("Agenda",AndroidWidgetSpec.removalLabel(l,KEY,"Android widget"));
        assertEquals("Android widget",AndroidWidgetSpec.removalLabel(l,"widget:android:00000000-0000-0000-0000-000000000000","Android widget"));
        assertEquals("Android widget",AndroidWidgetSpec.removalLabel(null,KEY,"Android widget"));
    }
    /** HB_WIDGET_STATS lines: exact JSON, first update always, then at most one per widget per 60 s. */
    @Test public void widgetUpdateStatsFormatAndRateLimit(){
        long[] now={1_000};WidgetUpdateStats stats=new WidgetUpdateStats(()->now[0]);
        assertEquals("{\"event\":\"widget_updates\",\"id\":7,\"updates\":1}",stats.record(7));
        now[0]+=10_000;assertNull(stats.record(7));
        assertEquals("{\"event\":\"widget_updates\",\"id\":9,\"updates\":1}",stats.record(9));
        now[0]+=49_999;assertNull(stats.record(7));
        now[0]+=1;assertEquals("{\"event\":\"widget_updates\",\"id\":7,\"updates\":4}",stats.record(7));
        now[0]+=59_999;assertNull(stats.record(7));
        now[0]+=1;assertEquals(WidgetUpdateStats.line(7,6),stats.record(7));
    }
}
