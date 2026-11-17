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
    @Override public void onCreate(Bundle b){super.onCreate(b);
        report=new TextView(this);report.setTextSize(14);report.setTextColor(0xffffffff);report.setBackgroundColor(0xdd172c3e);report.setText("PHONE PROBE · preparing");
        addContentView(report,new android.widget.FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        probe.postDelayed(()->{try{
            editor=new PhoneEditor(this);java.lang.reflect.Field field=MainActivity.class.getDeclaredField("phoneEditor");field.setAccessible(true);field.set(this,editor);editor.open();
            if(editor.server==null)throw new IllegalStateException("Listener did not start");origin=editor.server.url();io.execute(this::check);
        }catch(Exception e){fail(e);}},2500);
    }
    private void stage(String text){probe.post(()->{report.setText(text);report.bringToFront();android.util.Log.i("HomeboardPhoneProbe",text);});}
    private void check(){
        try{
            Thread.sleep(1500);JSONObject pairing=post("/api/pair",new JSONObject().put("invitation",""),false);credential=pairing.getString("token");
            probe.postDelayed(()->{dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER));dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER));},600);
            for(int i=0;i<20;i++){if("approved".equals(post("/api/status",new JSONObject(),true).getString("status")))break;Thread.sleep(300);}
            JSONObject baseline=post("/api/state",new JSONObject(),true),draft=new JSONObject(baseline.getJSONObject("draft").toString());
            stage("PHONE · actual Android HTTP and TV approval passed");
            for(int i=0;i<draft.getJSONArray("widgets").length();i++){
                JSONObject w=draft.getJSONArray("widgets").getJSONObject(i),s=w.getJSONObject("settings");
                switch(w.getString("type")){
                    case "rain":s.put("label","Phone verification");break;
                    case "calendar":s.put("holidayLookaheadDays",7);break;
                    case "air":s.put("source","airbkk").put("station","119");break;
                }
            }
            JSONArray favorites=draft.getJSONArray("favorites");if(favorites.length()>1){Object first=favorites.get(0);favorites.put(0,favorites.get(1));favorites.put(1,first);}
            Thread.sleep(2000);
            String operation=PhoneEditorSession.random();JSONObject request=new JSONObject().put("revision",baseline.getLong("revision")).put("operation",operation).put("draft",draft);
            applied=post("/api/apply",request,true);
            JSONObject repeated=post("/api/apply",request,true);require(applied.getLong("revision")==repeated.getLong("revision"),"Duplicate Apply changed revision");
            require(!canonical(applied.getJSONObject("draft")).equals(canonical(baseline.getJSONObject("draft"))),"Apply made no changes");
            stage("PHONE · mixed Rain / Air / Calendar / Favorites Apply and retry passed");Thread.sleep(4500);
            post("/api/preview",new JSONObject().put("revision",applied.getLong("revision")).put("draft",applied.getJSONObject("draft")),true);
            stage("PHONE · native temporary preview active");Thread.sleep(4500);post("/api/cancel-preview",new JSONObject(),true);
            JSONObject restored=post("/api/undo",new JSONObject().put("revision",applied.getLong("revision")).put("operation",PhoneEditorSession.random()),true);applied=null;
            require(canonical(restored.getJSONObject("draft")).equals(canonical(baseline.getJSONObject("draft"))),"Composite Undo did not restore all settings");
            post("/api/stop",new JSONObject(),true);
            stage("PHONE_PASS · Android HTTP, TV approval, mixed Apply, idempotent retry, preview, composite Undo, Stop · household restored");
        }catch(Exception failure){
            if(applied!=null){try{post("/api/cancel-preview",new JSONObject(),true);post("/api/undo",new JSONObject().put("revision",applied.getLong("revision")).put("operation",PhoneEditorSession.random()),true);}catch(Exception restore){stage("PHONE_FAIL · "+failure.getClass().getSimpleName()+"; cleanup "+restore.getClass().getSimpleName());return;}}
            fail(failure);
        }
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
    @Override protected void onDestroy(){probe.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
