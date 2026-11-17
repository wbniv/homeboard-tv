package com.gathin.tvhome;

import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Real Android HTTP + UI-approval integration check. Preserves household settings with composite Undo. */
public final class PhoneEditorProbeActivity extends MainActivity {
    private final Handler probe=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private TextView report;
    private PhoneEditor editor;
    private String origin,credential="";
    private JSONObject applied;
    @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){
        return super.getSharedPreferences(name.equals("widget-home")?"phone-review-widget-home":name,mode);
    }
    @Override public void onCreate(Bundle b){
        if(b==null){
            HomeLayout seed=HomeLayout.firstRun();
            try{
                HomeLayout household=HomeLayout.parse(super.getSharedPreferences("widget-home",0).getString("layout",seed.encode()));
                for(String key:household.favorites)seed.addFavorite(key);
                // Read-only capture for reproducing Will's explicitly requested new-clock preset.
                // Never copy other household settings or identifiers into the exported record.
                for(String key:household.widgets)if(HomeLayout.isWorldClockWidget(key)){
                    WorldClockSettings clock=household.clockSettings.get(key);
                    HomeLayout.PanelSpan span=household.panelSpans.get(key);
                    JSONObject snapshot=new JSONObject().put("key",key).put("settings",clock.json()).put("width",span==null?4:span.width).put("height",span==null?1:span.height);
                    String encoded=android.util.Base64.encodeToString(snapshot.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
                    for(int offset=0;offset<encoded.length();offset+=2000)android.util.Log.i("HomeboardClockPreset",key+" "+offset+" "+encoded.substring(offset,Math.min(offset+2000,encoded.length())));
                }
            }catch(JSONException ignored){}
            getSharedPreferences("widget-home",0).edit().clear().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true).apply();
        }
        super.onCreate(b);
        report=new TextView(this);report.setTextSize(14);report.setTextColor(0xffffffff);report.setBackgroundColor(0xdd172c3e);report.setText("PHONE PROBE · preparing");
        addContentView(report,new android.widget.FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        probe.postDelayed(()->{try{
            editor=new PhoneEditor(this);java.lang.reflect.Field field=MainActivity.class.getDeclaredField("phoneEditor");field.setAccessible(true);field.set(this,editor);editor.open();
            if(editor.server==null)throw new IllegalStateException("Listener did not start");origin=editor.server.url();io.execute(this::check);
        }catch(Exception e){fail(e);}},5000);
    }
    private void stage(String text){probe.post(()->{report.setText(text);report.bringToFront();android.util.Log.i("HomeboardPhoneProbe",text);});}
    private void check(){
        try{
            Thread.sleep(1500);JSONObject pairing=post("/api/pair",new JSONObject().put("invitation",""),false);credential=pairing.getString("token");
            probe.post(()->approveVisible(0));
            for(int i=0;i<20;i++){if("approved".equals(post("/api/status",new JSONObject(),true).getString("status")))break;Thread.sleep(300);}
            JSONObject baseline=post("/api/state",new JSONObject(),true),draft=new JSONObject(baseline.getJSONObject("draft").toString());
            require(!baseline.getString("tvId").isEmpty(),"TV identity missing");
            require(baseline.getJSONObject("instances").length()>0,"Widget identities missing");
            require(post("/api/touch",new JSONObject(),true).getBoolean("ok"),"Draft activity touch failed");
            JSONArray apps=baseline.getJSONArray("apps");
            if(apps.length()>0)require(post("/api/icon",new JSONObject().put("key",apps.getJSONObject(0).getString("key")),true).optString("icon").startsWith("data:image/png;base64,"),"App icon unavailable");
            stage("PHONE · actual Android HTTP and TV approval passed");
            for(int i=0;i<draft.getJSONArray("widgets").length();i++){
                JSONObject w=draft.getJSONArray("widgets").getJSONObject(i),s=w.getJSONObject("settings");
                switch(w.getString("type")){
                    case "rain":s.put("label","Phone verification");break;
                    case "calendar":s.put("holidayLookaheadDays",7);break;
                    case "air":s.put("source","airbkk").put("station","119");break;
                }
            }
            JSONObject clockDefault=baseline.getJSONObject("worldClockDefaults");
            require(clockDefault.getInt("width")==8&&clockDefault.getInt("height")==1,"Clock default size differs from captured preset");
            require(clockDefault.getJSONObject("settings").getJSONArray("places").length()==6,"Clock default places missing");
            draft.getJSONArray("widgets").put(new JSONObject().put("key",HomeLayout.WORLD_CLOCK).put("type","world_clock").put("page",1).put("column",0).put("row",0)
                    .put("width",clockDefault.getInt("width")).put("height",clockDefault.getInt("height")).put("settings",clockDefault.getJSONObject("settings")));
            JSONArray favorites=draft.getJSONArray("favorites");if(favorites.length()>1){Object first=favorites.get(0);favorites.put(0,favorites.get(1));favorites.put(1,first);}
            Thread.sleep(2000);
            String operation=PhoneEditorSession.random();JSONObject request=new JSONObject().put("revision",baseline.getLong("revision")).put("operation",operation).put("draft",draft);
            applied=post("/api/apply",request,true);
            JSONObject repeated=post("/api/apply",request,true);require(applied.getLong("revision")==repeated.getLong("revision"),"Duplicate Apply changed revision");
            require(!canonical(applied.getJSONObject("draft")).equals(canonical(baseline.getJSONObject("draft"))),"Apply made no changes");
            JSONObject addedClock=null;for(int i=0;i<applied.getJSONObject("draft").getJSONArray("widgets").length();i++){JSONObject candidate=applied.getJSONObject("draft").getJSONArray("widgets").getJSONObject(i);if(candidate.getString("type").equals("world_clock"))addedClock=candidate;}
            require(addedClock!=null&&addedClock.getInt("width")==8&&addedClock.getInt("height")==1&&canonical(addedClock.getJSONObject("settings")).equals(canonical(clockDefault.getJSONObject("settings"))),"Created clock did not retain native preset");
            stage("PHONE · mixed Rain / Air / Calendar / Favorites and new six-place Clock Apply and retry passed");Thread.sleep(4500);
            post("/api/preview",new JSONObject().put("revision",applied.getLong("revision")).put("draft",applied.getJSONObject("draft")),true);
            stage("PHONE · native temporary preview active");Thread.sleep(4500);post("/api/cancel-preview",new JSONObject(),true);
            JSONObject restored=post("/api/undo",new JSONObject().put("revision",applied.getLong("revision")).put("operation",PhoneEditorSession.random()),true);applied=null;
            require(canonical(restored.getJSONObject("draft")).equals(canonical(baseline.getJSONObject("draft"))),"Composite Undo did not restore all settings");
            post("/api/stop",new JSONObject(),true);
            stage("PHONE_PASS · Android HTTP, TV approval, mixed Apply, idempotent retry, preview, composite Undo, Stop · isolated test settings restored; household untouched");
        }catch(Exception failure){
            if(applied!=null){try{post("/api/cancel-preview",new JSONObject(),true);post("/api/undo",new JSONObject().put("revision",applied.getLong("revision")).put("operation",PhoneEditorSession.random()),true);}catch(Exception restore){stage("PHONE_FAIL · "+failure.getClass().getSimpleName()+"; cleanup "+restore.getClass().getSimpleName());return;}}
            fail(failure);
        }
    }
    private void approveVisible(int attempt){
        if(isFinishing()||editor==null||!editor.running())return;
        View allow=findAllow(getWindow().getDecorView());
        if(allow!=null&&editor.server.session.pending()){allow.performClick();return;}
        if(attempt<40)probe.postDelayed(()->approveVisible(attempt+1),150);
    }
    private View findAllow(View view){
        if(view instanceof TextView&&view.isShown()&&view.isClickable()&&getString(R.string.hb_phone_accept).contentEquals(((TextView)view).getText()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View result=findAllow(((ViewGroup)view).getChildAt(i));if(result!=null)return result;}
        return null;
    }
    private JSONObject post(String path,JSONObject body,boolean auth)throws Exception{
        URL u=new URL(origin);byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
        try(Socket socket=new Socket()){socket.connect(new InetSocketAddress(u.getHost(),u.getPort()),4000);socket.setSoTimeout(12000);
            OutputStream out=socket.getOutputStream();String headers="POST "+path+" HTTP/1.1\r\nHost: "+u.getAuthority()+"\r\nOrigin: "+origin+"\r\nContent-Type: application/json\r\nX-Homeboard-Session: "+(auth?credential:"")+"\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n";
            out.write(headers.getBytes(StandardCharsets.UTF_8));out.write(bytes);out.flush();InputStream in=socket.getInputStream();String status=line(in);int length=-1;String h;while(!(h=line(in)).isEmpty())if(h.toLowerCase(Locale.ROOT).startsWith("content-length:"))length=Integer.parseInt(h.substring(h.indexOf(':')+1).trim());
            if(length<0||length>262144)throw new IOException("Invalid response size");ByteArrayOutputStream data=new ByteArrayOutputStream();for(int i=0;i<length;i++){int b=in.read();if(b<0)throw new EOFException();data.write(b);}JSONObject result=new JSONObject(data.toString("UTF-8"));
            if(!status.contains(" 200 "))throw new IOException("HTTP "+status+" · "+result.optString("error"));return result;
        }
    }
    private static String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();for(int n=0;n<8192;n++){int c=in.read();if(c<0)throw new EOFException();if(c=='\n')return b.toString("UTF-8").replace("\r","");b.write(c);}throw new IOException("Header too long");}
    private static String canonical(Object value)throws JSONException{
        if(value instanceof JSONObject){JSONObject o=(JSONObject)value;List<String> keys=new ArrayList<>();o.keys().forEachRemaining(keys::add);Collections.sort(keys);StringBuilder b=new StringBuilder("{");for(String k:keys)b.append(JSONObject.quote(k)).append(':').append(canonical(o.get(k))).append(',');return b.append('}').toString();}
        if(value instanceof JSONArray){JSONArray a=(JSONArray)value;StringBuilder b=new StringBuilder("[");for(int i=0;i<a.length();i++)b.append(canonical(a.get(i))).append(',');return b.append(']').toString();}return value instanceof String?JSONObject.quote((String)value):String.valueOf(value);
    }
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException(text);}
    private void fail(Exception e){stage("PHONE_FAIL · "+e.getClass().getSimpleName()+" · "+e.getMessage());}
    @Override protected void onPause(){probe.removeCallbacksAndMessages(null);super.onPause();}
    @Override protected void onDestroy(){probe.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
