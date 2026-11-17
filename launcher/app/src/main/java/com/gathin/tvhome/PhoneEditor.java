package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.*;
import android.os.SystemClock;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import org.json.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Foreground-only phone editor, lifecycle and UI bridge. */
final class PhoneEditor implements PhoneEditorServer.Host {
    private final MainActivity activity;
    PhoneEditorServer server;
    private PhoneEditorSession session;
    private String address;
    private final Runnable tick=new Runnable(){public void run(){
        if(server==null)return;
        if(!session.running()||!Objects.equals(address,address(activity))){stop();return;}
        activity.getWindow().getDecorView().postDelayed(this,5000);
    }};
    PhoneEditor(MainActivity activity){this.activity=activity;}
    boolean running(){return server!=null&&session.running();}
    void open(){
        if(running()){show();return;}
        address=address(activity);
        if(address==null){activity.phoneMessage(R.string.hb_phone_no_network);return;}
        try{
            session=new PhoneEditorSession(SystemClock::elapsedRealtime);
            Map<String,byte[]> assets=new HashMap<>();
            for(String name:List.of("index.html","editor.js","editor.css"))try(java.io.InputStream in=activity.getAssets().open("phone/"+name)){assets.put(name.equals("index.html")?"/":"/"+name,read(in));}
            server=new PhoneEditorServer(address,0,this,session,assets);server.open();
            activity.getWindow().getDecorView().postDelayed(tick,5000);show();
        }catch(Exception failure){stop();activity.phoneMessage(R.string.hb_phone_unavailable);}
    }
    void show(){
        try{
        String url=server.url()+"/#"+session.invitation();
        activity.phonePanel(activity.getString(R.string.hb_phone_title),activity.phoneName()+"\n"+server.url()+"\n"+activity.getString(R.string.hb_phone_pair_hint),qr(url),
                new String[]{activity.getString(R.string.hb_phone_dashboard),activity.getString(R.string.hb_phone_stop)},i->{if(i==1)stop();activity.phoneDismiss();});
        }catch(Exception e){stop();activity.phoneMessage(R.string.hb_phone_unavailable);}
    }
    @Override public void pairing(String request,String code){activity.runOnUiThread(()->{
        if(server==null||!session.pending())return;
        if(!activity.phoneCanPair()){session.approve(request,false);return;}
        activity.phonePanel(activity.getString(R.string.hb_phone_allow),activity.phoneName()+"\n"+code+"\n"+activity.getString(R.string.hb_phone_match),null,
                new String[]{activity.getString(R.string.hb_phone_accept),activity.getString(R.string.hb_phone_deny)},i->{session.approve(request,i==0);activity.phoneDismiss();activity.phoneConnectionChanged(i==0);});
    });}
    @Override public JSONObject command(String path,JSONObject body,String credential)throws Exception {
        if(path.equals("/api/lookup")){JSONObject result=lookup(body);if(!session.authorized(credential,false))throw new IllegalStateException("Session revoked");return result;}
        FutureTask<JSONObject> task=new FutureTask<>(()->{
            if(!running()||!session.authorized(credential,false)||!activity.phoneForeground())throw new IllegalStateException("Session stopped");
            switch(path){
                case "/api/state":return activity.phoneState();
                case "/api/apply":return activity.phoneApply(body);
                case "/api/undo":return activity.phoneUndo(body);
                case "/api/preview":return activity.phonePreview(body);
                case "/api/cancel-preview":activity.phoneCancelPreview();return new JSONObject().put("ok",true);
                case "/api/operation":return activity.phoneOperation(body);
                case "/api/stop":activity.getWindow().getDecorView().postDelayed(this::stop,250);return new JSONObject().put("ok",true);
                default:throw new IllegalArgumentException("Unknown action");
            }
        });
        activity.runOnUiThread(task);
        try{return task.get(8,TimeUnit.SECONDS);}catch(ExecutionException e){if(e.getCause() instanceof Exception)throw(Exception)e.getCause();throw e;}
        catch(TimeoutException e){task.cancel(false);throw e;}
    }
    private JSONObject lookup(JSONObject body)throws Exception {
        String query=PhoneConfiguration.bounded(body,"query",240).trim();if(query.isEmpty())throw new PhoneConfiguration.Invalid("query","Enter a place");
        JSONArray places=new JSONArray();
        if(body.optBoolean("address")){
            if(!android.location.Geocoder.isPresent())throw new PhoneConfiguration.Invalid("query","Address lookup unavailable; use a city or coordinates");
            java.util.List<android.location.Address> hits=new android.location.Geocoder(activity,Locale.getDefault()).getFromLocationName(query,5);
            if(hits!=null)for(android.location.Address a:hits)if(a.hasLatitude()&&a.hasLongitude())places.put(new JSONObject().put("name",a.getAddressLine(0)==null?query:a.getAddressLine(0)).put("latitude",a.getLatitude()).put("longitude",a.getLongitude()));
        }else{
            JSONObject result=new JSONObject(RainClient.search(query,Locale.getDefault().getLanguage()).get(7,TimeUnit.SECONDS));
            JSONArray hits=result.optJSONArray("results");
            if(hits!=null)for(int i=0;i<hits.length();i++){JSONObject p=hits.getJSONObject(i);places.put(new JSONObject().put("name",p.getString("name")+", "+p.optString("admin1")+", "+p.optString("country")).put("latitude",p.getDouble("latitude")).put("longitude",p.getDouble("longitude")));}
        }
        if(!running())throw new IllegalStateException("Session closed");return new JSONObject().put("places",places);
    }
    void stop(){
        activity.getWindow().getDecorView().removeCallbacks(tick);
        PhoneEditorServer old=server;server=null;if(old!=null)old.stop();
        activity.phoneCancelPreview();activity.phoneConnectionChanged(false);
    }
    private static byte[] read(java.io.InputStream in)throws java.io.IOException{java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();}
    static String address(Context context){
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties links=cm.getLinkProperties(cm.getActiveNetwork());if(links==null)return null;
        for(LinkAddress link:links.getLinkAddresses()){InetAddress a=link.getAddress();if(a instanceof Inet4Address&&a.isSiteLocalAddress())return a.getHostAddress();}return null;
    }
    private Bitmap qr(String value)throws Exception{
        BitMatrix bits=new MultiFormatWriter().encode(value,BarcodeFormat.QR_CODE,384,384);
        Bitmap bitmap=Bitmap.createBitmap(bits.getWidth(),bits.getHeight(),Bitmap.Config.ARGB_8888);
        for(int y=0;y<bits.getHeight();y++)for(int x=0;x<bits.getWidth();x++)bitmap.setPixel(x,y,bits.get(x,y)?0xff101820:0xffffffff);return bitmap;
    }
}
