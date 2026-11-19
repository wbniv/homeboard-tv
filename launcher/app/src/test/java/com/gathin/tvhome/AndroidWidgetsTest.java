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

}
