package com.gathin.tvhome;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper;

/** Real external-provider baseline. No provider preferences or household layout are written. */
public final class ProviderCompatibilityProbeActivity extends Activity {
    private final Handler main = new Handler();
    private final JSONArray events = new JSONArray();
    private final JSONObject result = new JSONObject();
    private final Map<String, Map<String, ?>> household = new LinkedHashMap<>();
    private WallpaperSurface surface;
    private ProjectivyWallpaperHost host;
    private TextView report;
    private long started;
    private String target, phase = "initial", lastStatus = "", currentUriHash = "";
    private int rendered, deliveries, rotations;
    private boolean initial, resumed, refreshed, hiddenReleased, finished;
    private Bitmap motionFrame;
    private String motionHash;

    private void put(JSONObject object, String key, Object value) {
        try { object.put(key,value); } catch (org.json.JSONException invalid) { throw new IllegalStateException(invalid); }
    }
    private void event(String name, String detail) {
        JSONObject entry = new JSONObject(); put(entry,"ms",SystemClock.elapsedRealtime()-started);
        put(entry,"phase",phase);put(entry,"event",name);put(entry,"detail",detail);events.put(entry);
        android.util.Log.i("ProviderCompatibility",target+" "+name+" "+detail);
        persist();show();
    }
    private void persist() {
        put(result,"events",events);
        try {
            File folder=getExternalFilesDir(null);
            if(folder==null)folder=getFilesDir();
            try(FileOutputStream out=new FileOutputStream(new File(folder,"wf.log"))) {
                out.write(result.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch(Exception error) { android.util.Log.e("ProviderCompatibility","Cannot write report",error); }
    }
    private void show() {
        report.setText("PROVIDER COMPATIBILITY · "+target+"\n"+phase+" · "+lastStatus
                +"\nRendered "+rendered+" · timer deliveries "+rotations
                +"\nInitial "+initial+" · resume "+resumed+" · refresh "+refreshed
                +(finished?"\n"+result.optString("baseline")+" · full compatibility remains partial":""));
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);started=SystemClock.elapsedRealtime();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff102333);
        surface=new WallpaperSurface(this);surface.options(0,false);surface.visible(true);
        root.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        report=new TextView(this);report.setTextColor(-1);report.setTextSize(18);report.setPadding(20,12,20,12);
        report.setBackgroundColor(0xdd102333);FrameLayout.LayoutParams text=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        root.addView(report,text);setContentView(root);
        try {
            target=getPackageManager().getActivityInfo(getIntent().getComponent(),PackageManager.GET_META_DATA)
                    .metaData.getString("providerPackage");
            put(result,"schema",1);put(result,"providerPackage",target);put(result,"hostPackage",getPackageName());
            put(result,"homeboardVersion",getPackageManager().getPackageInfo(getPackageName(),0).versionName);
            put(result,"configuration","installed provider preferences unchanged");
            for(String name:new String[]{"widget-home","app-language"})
                household.put(name,new LinkedHashMap<>(getSharedPreferences(name,MODE_PRIVATE).getAll()));
            ProjectivyProviderRegistry.Provider provider=new ProjectivyProviderRegistry(this).discover().stream()
                    .filter(p->p.component.getPackageName().equals(target)).findFirst().orElse(null);
            if(provider==null){finishResult("blocked","Provider not installed or not discoverable");return;}
            put(result,"providerVersion",getPackageManager().getPackageInfo(target,0).versionName);
            put(result,"component",provider.component.flattenToString());put(result,"uuid",provider.uuid);
            put(result,"eventMask",provider.events);put(result,"cacheMillis",provider.cacheMillis);
            put(result,"settingsActivityAvailable",new ProjectivyProviderRegistry(this).settings(provider)!=null);
            put(result,"unsupportedEvents",WallpaperSettings.missingEvents(provider.events));
            event("discovery",provider.readiness());
            if(!provider.usable()){finishResult("fail",provider.problem);return;}
            host=new ProjectivyWallpaperHost(this,new ProjectivyWallpaperHost.Listener(){
                @Override public void wallpaper(Wallpaper item){
                    if(finished)return;
                    deliveries++;if(phase.equals("rotation"))rotations++;
                    String identity=hash(item.getUri());currentUriHash=identity;
                    event("descriptor","type="+item.getType()+" mode="+item.getDisplayMode()+" uriHash="+identity);
                    surface.show(item,(ready,status)->{
                        if(finished)return;
                        lastStatus=status;
                        if(ready){
                            rendered++;if(phase.equals("initial")||phase.equals("rotation"))initial=true;
                            if(phase.equals("resume"))resumed=true;if(phase.equals("refresh"))refreshed=true;
                            event("render-ready","type="+item.getType()+" phase="+phase);
                            if(item.getType()==4){
                                main.postDelayed(()->sampleMotion(identity,false),1500);
                                main.postDelayed(()->sampleMotion(identity,true),4500);
                            }
                        } else {event("media-failed",status);host.mediaFailed(status);}
                    });
                }
                @Override public void status(String status){if(!finished){lastStatus=status;event("host-status",status);}}
                @Override public void fallback(){surface.solid();}
            });
            WallpaperSettings settings=new WallpaperSettings();settings.component=provider.component.flattenToString();
            settings.uuid=provider.uuid;settings.rotationMinutes=1;settings.dimPercent=0;
            host.select(settings);host.visible(true);
            host.appFocused("Homeboard TV",getPackageName(),0xffeeeeee,0xff102333);
            main.postDelayed(()->{
                if(!initial){finishResult("blocked","No usable media with installed/default configuration: "+lastStatus);}
                else {phase="rotation";event("waiting-for-timer","one-minute production timer");}
            },55_000);
            main.postDelayed(()->{
                if(finished)return;phase="hidden";host.visible(false);surface.visible(false);
                hiddenReleased=field(surface,"player")==null&&field(surface,"preparing")==null;
                event("hidden-release",Boolean.toString(hiddenReleased));
            },125_000);
            main.postDelayed(()->{
                if(finished)return;phase="resume";surface.visible(true);host.visible(true);
                host.appFocused("Homeboard TV",getPackageName(),0xffeeeeee,0xff102333);event("resume-requested","");
            },128_000);
            main.postDelayed(()->{if(!finished){phase="refresh";event("refresh-requested","");host.refresh();}},145_000);
            main.postDelayed(()->finishResult(initial&&resumed&&refreshed&&hiddenReleased
                    &&((provider.events&1)==0||rotations>0)?"pass":"partial","Baseline complete; see individual cases"),165_000);
        } catch(Exception error){finishResult("fail",error.getClass().getSimpleName()+": "+error.getMessage());}
    }
    private Object field(Object object,String name){
        try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
        catch(Exception error){return "inspection unavailable";}
    }
    private static String hash(String value){
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder text=new StringBuilder();for(byte b:digest)text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return text.toString();
        }catch(Exception impossible){throw new IllegalStateException(impossible);}
    }
    private TextureView texture(View view){
        if(view instanceof TextureView)return (TextureView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            TextureView found=texture(((ViewGroup)view).getChildAt(i));if(found!=null)return found;
        }return null;
    }
    private void sampleMotion(String identity,boolean second){
        if(finished||!identity.equals(currentUriHash))return;
        TextureView video=texture(surface);if(video==null||!video.isAvailable())return;
        Bitmap sample=video.getBitmap(160,90);if(sample==null)return;
        if(!second){if(motionFrame!=null)motionFrame.recycle();motionFrame=sample;motionHash=identity;return;}
        if(motionFrame!=null&&identity.equals(motionHash)){
            long delta=0;for(int y=0;y<90;y++)for(int x=0;x<160;x++){
                int a=motionFrame.getPixel(x,y),b=sample.getPixel(x,y);
                delta+=Math.abs((a&255)-(b&255))+Math.abs(((a>>8)&255)-((b>>8)&255))+Math.abs(((a>>16)&255)-((b>>16)&255));
            }
            put(result,"videoFrameDifference",delta);event("video-motion-sample",Long.toString(delta));
            motionFrame.recycle();motionFrame=null;
        }sample.recycle();
    }
    private void finishResult(String baseline,String reason){
        if(finished)return;finished=true;phase="finished";
        boolean unchanged=true;for(Map.Entry<String,Map<String,?>> entry:household.entrySet())
            unchanged&=entry.getValue().equals(getSharedPreferences(entry.getKey(),MODE_PRIVATE).getAll());
        put(result,"householdPreferencesUnchanged",unchanged);put(result,"baseline",unchanged?baseline:"fail");
        put(result,"compatibility","partial");put(result,"reason",reason);
        put(result,"initialRendered",initial);put(result,"resumeRendered",resumed);put(result,"refreshRendered",refreshed);
        put(result,"hiddenMotionReleased",hiddenReleased);put(result,"timerDeliveries",rotations);
        put(result,"rendered",rendered);put(result,"deliveries",deliveries);
        put(result,"notRun",new JSONArray(java.util.Arrays.asList("provider settings editing","actual cache expiry",
                "external action launch","network fault injection","full Home preview/apply/undo","long video soak")));
        lastStatus=reason;event("RESULT",result.optString("baseline"));
        main.removeCallbacksAndMessages(null);
    }
    @Override protected void onPause(){
        if(!finished)finishResult("blocked","Activity paused before completion");
        if(host!=null)host.visible(false);if(surface!=null)surface.visible(false);super.onPause();
    }
    @Override protected void onDestroy(){
        main.removeCallbacksAndMessages(null);if(host!=null)host.close();if(surface!=null)surface.close();
        if(motionFrame!=null)motionFrame.recycle();super.onDestroy();
    }
}
