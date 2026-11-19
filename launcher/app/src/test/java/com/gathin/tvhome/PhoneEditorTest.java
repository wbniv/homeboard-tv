package com.gathin.tvhome;

import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class PhoneEditorTest {
    @Test public void connectionNeedsApprovalAndReplacementRevokesPreviousPhone(){
        AtomicLong time=new AtomicLong();PhoneEditorSession s=new PhoneEditorSession(time::get);
        String invitation=s.invitation(),a=s.request(invitation);assertFalse(s.authorized(a,false));assertEquals("pending",s.status(a));
        s.approve(a,true);assertTrue(s.authorized(a,false));assertNotEquals(invitation,s.invitation());
        time.addAndGet(10_001);String b=s.request("");assertTrue(s.authorized(a,false));s.approve(b,true);
        assertFalse(s.authorized(a,false));assertTrue(s.authorized(b,false));s.close();assertFalse(s.authorized(b,false));
    }
    @Test public void expiryDenialRateLimitAndStatusPollingNeverExtendSession(){
        AtomicLong time=new AtomicLong();PhoneEditorSession s=new PhoneEditorSession(time::get);
        String a=s.request("");assertThrows(IllegalArgumentException.class,()->s.request(""));s.approve(a,false);assertFalse(s.authorized(a,false));
        time.addAndGet(10_001);String b=s.request("");time.addAndGet(PhoneEditorSession.PENDING);s.approve(b,true);assertFalse(s.authorized(b,false));
        String c=s.request("");s.approve(c,true);long expiry=time.get()+PhoneEditorSession.IDLE;
        time.set(expiry-1);assertEquals("approved",s.status(c));time.set(expiry);assertEquals("expired",s.status(c));assertFalse(s.running());
    }
    private JSONObject air()throws Exception{return new JSONObject().put(HomeLayout.AIR,PhoneConfiguration.defaultAir());}
    private JSONObject draft(HomeLayout layout)throws Exception{return PhoneConfiguration.snapshot(layout,air(),8,4);}
    @Test public void allWidgetsRoundTripAndMixedChangesKeepUnrelatedSettings()throws Exception {
        HomeLayout original=HomeLayout.firstRun();original.palette="forest";original.uiOpacityPercent=42;original.addFavorite("app:old/Main");
        JSONObject draft=draft(original);draft.getJSONArray("widgets").getJSONObject(0).getJSONObject("settings").put("hours",48).put("label","Garden");
        for(int i=0;i<draft.getJSONArray("widgets").length();i++){JSONObject w=draft.getJSONArray("widgets").getJSONObject(i);if(w.getString("type").equals("air"))w.getJSONObject("settings").put("source","airbkk").put("station","119");}
        PhoneConfiguration.Result result=PhoneConfiguration.validate(draft,original,air(),Set.of());
        assertEquals(48,result.layout.rainSettings.get(HomeLayout.RAIN).hours);assertEquals("119",result.air.getJSONObject(HomeLayout.AIR).getString("station"));
        assertEquals(original.favorites,result.layout.favorites);assertEquals("forest",result.layout.palette);assertEquals(42,result.layout.uiOpacityPercent);
        assertEquals(24,original.rainSettings.get(HomeLayout.RAIN).hours);
    }
    @Test public void rejectsOverlapInvalidRangesUnknownPreferencesAndNewUnavailableApps()throws Exception {
        HomeLayout before=HomeLayout.firstRun();JSONObject draft=draft(before);
        draft.getJSONArray("widgets").getJSONObject(1).put("row",0);
        assertThrows(PhoneConfiguration.Invalid.class,()->PhoneConfiguration.validate(draft,before,air(),Set.of()));
        JSONObject bad=draft(before);bad.getJSONArray("widgets").getJSONObject(0).getJSONObject("settings").put("hours",13);
        assertThrows(PhoneConfiguration.Invalid.class,()->PhoneConfiguration.validate(bad,before,air(),Set.of()));
        JSONObject unknown=draft(before).put("password","no");assertThrows(PhoneConfiguration.Invalid.class,()->PhoneConfiguration.validate(unknown,before,air(),Set.of()));
        JSONObject apps=draft(before).put("favorites",new JSONArray().put("app:new/Main"));assertThrows(PhoneConfiguration.Invalid.class,()->PhoneConfiguration.validate(apps,before,air(),Set.of()));
    }
    @Test public void allowsEmptyHomeAndValidNewInstancesWithoutMutatingOriginal()throws Exception {
        HomeLayout before=HomeLayout.firstRun();JSONObject empty=draft(before).put("widgets",new JSONArray());
        assertTrue(PhoneConfiguration.validate(empty,before,air(),Set.of()).layout.widgets.isEmpty());
        JSONObject newOne=draft(before);JSONObject rain=new JSONObject(newOne.getJSONArray("widgets").getJSONObject(0).toString()).put("key",HomeLayout.RAIN+":2").put("page",1);
        newOne.getJSONArray("widgets").put(rain);assertEquals(5,PhoneConfiguration.validate(newOne,before,air(),Set.of()).layout.widgets.size());assertEquals(4,before.widgets.size());
    }
    @Test public void serverRequiresExactOriginAndApprovedCredentialAndBoundsBody()throws Exception {
        AtomicLong time=new AtomicLong();PhoneEditorSession session=new PhoneEditorSession(time::get);
        PhoneEditorServer server=new PhoneEditorServer("127.0.0.1",0,new PhoneEditorServer.Host(){
            public JSONObject command(String path,JSONObject body,String credential)throws Exception{return new JSONObject().put("private","configuration");}
            public void pairing(String request,String code){}
        },session,Map.of("/","Editor".getBytes(StandardCharsets.UTF_8)));
        server.open();
        try {
            String url=server.url();assertEquals(403,post(url,"/api/state","{}","",url));
            assertEquals(403,post(url,"/api/state","{}","", "http://attacker.invalid"));
            String token=session.request("");assertEquals(403,post(url,"/api/state","{}",token,url));session.approve(token,true);
            assertEquals(200,post(url,"/api/state","{}",token,url));assertEquals(200,post(url,"/api/icon","{}",token,url));assertEquals(200,post(url,"/api/touch","{}",token,url));assertEquals(413,post(url,"/api/state"," ".repeat(524289),token,url));
            session.close();assertEquals(403,post(url,"/api/state","{}",token,url));
        }finally{server.stop();}
    }
    @Test public void shortcutsPublishEscapedPerTVMetadataAndRejectAnotherTVBeforeApproval()throws Exception {
        AtomicLong time=new AtomicLong();PhoneEditorSession session=new PhoneEditorSession(time::get);java.util.concurrent.atomic.AtomicInteger prompts=new java.util.concurrent.atomic.AtomicInteger();
        PhoneEditorServer server=new PhoneEditorServer("127.0.0.1",0,new PhoneEditorServer.Host(){
            public JSONObject command(String path,JSONObject body,String credential){return new JSONObject();}
            public void pairing(String request,String code){prompts.incrementAndGet();}
            public String id(){return "cast-one-id";}
            public String name(){return "Cast1";}
        },session,Map.of("/","<title>Homeboard · Your dashboard</title><meta name=\"apple-mobile-web-app-title\" content=\"Homeboard\">".getBytes(StandardCharsets.UTF_8),"/tv-icon.png",new byte[]{1,2,3}));
        server.open();try{
            String url=server.url(),name="<TV> & \"Family\"",query=java.net.URLEncoder.encode(name,"UTF-8");
            String html=get(url+"/?tv=cast-one-id&name="+query);assertTrue(html.contains("&lt;TV&gt; &amp; &quot;Family&quot;"));assertFalse(html.contains("<TV>"));
            JSONObject manifest=new JSONObject(get(url+"/shortcut.webmanifest?name="+query));assertEquals(name,manifest.getString("name"));assertEquals("/?tv=cast-one-id",manifest.getString("id"));assertFalse(manifest.toString().contains(session.invitation()));assertTrue(manifest.getString("start_url").startsWith("/?tv=cast-one-id&name="));
            assertEquals(422,post(url,"/api/pair","{\"tvId\":\"cast-two-id\"}","",url));assertEquals(0,prompts.get());
            assertEquals(200,post(url,"/api/pair","{\"tvId\":\"cast-one-id\"}","",url));assertEquals(1,prompts.get());
        }finally{server.stop();}
    }
    private String get(String url)throws Exception {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new URL(url).openStream(),StandardCharsets.UTF_8))){return reader.lines().collect(java.util.stream.Collectors.joining("\n"));}
    }
    @Test public void recreatedWidgetHasNewIdentityWhileOrdinaryEditsKeepIdentity()throws Exception {
        HomeLayout original=HomeLayout.firstRun();JSONObject generations=PhoneConfigurationStore.nextInstances(new HomeLayout(),original,new JSONObject());
        assertEquals(1,generations.getLong(HomeLayout.RAIN));
        HomeLayout edited=HomeLayout.parse(original.encode());edited.rainSettings.get(HomeLayout.RAIN).hours=48;
        JSONObject unchanged=PhoneConfigurationStore.nextInstances(original,edited,generations);assertEquals(1,unchanged.getLong(HomeLayout.RAIN));
        HomeLayout removed=HomeLayout.parse(edited.encode());removed.widgets.remove(HomeLayout.RAIN);
        JSONObject tombstones=PhoneConfigurationStore.nextInstances(edited,removed,unchanged);
        JSONObject recreated=PhoneConfigurationStore.nextInstances(removed,original,tombstones);
        assertEquals(2,recreated.getLong(HomeLayout.RAIN));assertEquals(1,recreated.getLong(HomeLayout.CALENDAR));
        assertEquals(1,generations.getLong(HomeLayout.RAIN));
    }
    @Test public void firstDashboardSaveCreatesWidgetIdentitiesWithoutParsingMissingLayout()throws Exception {
        Map<String,String> staged=new HashMap<>();
        android.content.SharedPreferences prefs=(android.content.SharedPreferences)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{android.content.SharedPreferences.class},(proxy,method,args)->{
            if(method.getName().equals("getString"))return args[1];
            throw new UnsupportedOperationException(method.getName());
        });
        android.content.SharedPreferences.Editor edit=(android.content.SharedPreferences.Editor)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{android.content.SharedPreferences.Editor.class},(proxy,method,args)->{
            if(method.getName().equals("putString")){staged.put((String)args[0],(String)args[1]);return proxy;}
            throw new UnsupportedOperationException(method.getName());
        });
        HomeLayout dashboard=HomeLayout.firstRun();
        assertSame(edit,PhoneConfigurationStore.trackInstances(prefs,edit,dashboard));
        JSONObject generations=new JSONObject(staged.get("widget-instance-generations"));
        assertEquals(dashboard.widgets.size(),generations.length());
        for(String key:dashboard.widgets)assertEquals(1,generations.getLong(key));
    }
    @Test public void failedDiskCommitCannotLeaveAnAcknowledgedOperationInMemory()throws Exception {
        Map<String,Object> values=new HashMap<>();HomeLayout before=HomeLayout.firstRun();values.put("layout",before.encode());values.put(PhoneConfigurationStore.REVISION,7L);
        android.content.SharedPreferences prefs=(android.content.SharedPreferences)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{android.content.SharedPreferences.class},(proxy,method,args)->{
            switch(method.getName()){
                case "getAll":return new HashMap<>(values);
                case "getString":case "getLong":return values.getOrDefault(args[0],args[1]);
                case "edit":{
                    Map<String,Object> staged=new HashMap<>();Set<String> removed=new HashSet<>();
                    return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{android.content.SharedPreferences.Editor.class},(editor,action,parameters)->{
                        switch(action.getName()){
                            case "putString":case "putLong":staged.put((String)parameters[0],parameters[1]);return editor;
                            case "remove":removed.add((String)parameters[0]);return editor;
                            case "commit":for(String key:removed)values.remove(key);values.putAll(staged);return false;
                            default:throw new UnsupportedOperationException(action.getName());
                        }
                    });
                }
                default:throw new UnsupportedOperationException(method.getName());
            }
        });
        HomeLayout next=HomeLayout.parse(before.encode());next.rainSettings.get(HomeLayout.RAIN).hours=48;
        assertThrows(IllegalStateException.class,()->PhoneConfigurationStore.commit(prefs,next,new JSONObject(),new JSONArray(),"failed-operation","committed"));
        assertEquals(before.encode(),values.get("layout"));assertEquals(7L,values.get(PhoneConfigurationStore.REVISION));
        assertEquals("",PhoneConfigurationStore.result(prefs,"failed-operation"));assertFalse(values.containsKey("widget-instance-generations"));
    }
    private int post(String url,String path,String body,String token,String origin)throws Exception {
        URL target=new URL(url);byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        try(Socket socket=new Socket(target.getHost(),target.getPort())){
            socket.setSoTimeout(3000);OutputStream out=socket.getOutputStream();
            byte[] headers=("POST "+path+" HTTP/1.1\r\nHost: "+target.getAuthority()+"\r\nOrigin: "+origin+"\r\nContent-Type: application/json\r\nX-Homeboard-Session: "+token+"\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            // An oversized Content-Length must be rejected before reading a body.
            // Sending its half-megabyte payload races the valid early close (EPIPE).
            ByteArrayOutputStream request=new ByteArrayOutputStream();request.write(headers);
            if(bytes.length<=524288)request.write(bytes);out.write(request.toByteArray());out.flush();BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            return Integer.parseInt(in.readLine().split(" ")[1]);
        }
    }
}
