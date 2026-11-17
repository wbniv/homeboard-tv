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
            assertEquals(200,post(url,"/api/state","{}",token,url));assertEquals(413,post(url,"/api/state"," ".repeat(65537),token,url));
            session.close();assertEquals(403,post(url,"/api/state","{}",token,url));
        }finally{server.stop();}
    }
    private int post(String url,String path,String body,String token,String origin)throws Exception {
        URL target=new URL(url);byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        try(Socket socket=new Socket(target.getHost(),target.getPort())){
            socket.setSoTimeout(3000);OutputStream out=socket.getOutputStream();
            out.write(("POST "+path+" HTTP/1.1\r\nHost: "+target.getAuthority()+"\r\nOrigin: "+origin+"\r\nContent-Type: application/json\r\nX-Homeboard-Session: "+token+"\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(bytes);out.flush();BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            return Integer.parseInt(in.readLine().split(" ")[1]);
        }
    }
}
