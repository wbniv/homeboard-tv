package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Real Home rendering with separate preferences and a generic public location. Never seeds Will's Home. */
public final class RainProbeActivity extends MainActivity {
    private final Handler runner=new Handler();
    private TextView report;
    private String addressStatus="Address lookup pending";
    private String current="Live rain · Bangkok example";
    private final int[][] spans={{1,1},{2,2},{4,2},{2,3},{4,3},{6,3},{12,12}};
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("rain-cache-"))name="rain-review-"+name;
        return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state){
        HomeLayout seed=new HomeLayout();seed.addPanel(HomeLayout.RAIN);
        RainSettings settings=seed.rainSettings.get(HomeLayout.RAIN);settings.automatic=false;settings.latitude=13.7563;settings.longitude=100.5018;settings.name="Bangkok example";
        getSharedPreferences("widget-home",0).edit().clear().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true).apply();
        super.onCreate(state);
        report=new TextView(this);report.setTextColor(Color.WHITE);report.setBackgroundColor(0xff102333);report.setTextSize(15);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);addContentView(report,p);updateReport();
        CompletableFuture.supplyAsync(()->{
            if(!Geocoder.isPresent())return "Address backend unavailable";
            try{
                java.util.List<android.location.Address> found=new Geocoder(this).getFromLocationName("1600 Amphitheatre Parkway, Mountain View, CA",1);
                return found!=null&&!found.isEmpty()&&found.get(0).hasLatitude()?"Address backend resolved public street address":"Address backend returned no results";
            }catch(Exception e){return "Address backend lookup failed";}
        }).thenAccept(result->runner.post(()->{addressStatus=result;updateReport();}));
        for(int i=0;i<spans.length;i++){final int index=i;runner.postDelayed(()->size(index),12_000+i*7_000L);}
        runner.postDelayed(()->{try{RainWidgetController c=controller();c.details();current="Hourly details · six hours per page";updateReport();}catch(Exception e){fail(e);}},63_000);
        runner.postDelayed(()->{try{RainWidgetController c=controller();Method m=RainWidgetController.class.getDeclaredMethod("openDetails");m.setAccessible(true);Field f=RainWidgetController.class.getDeclaredField("expanded");f.setAccessible(true);f.set(c,true);m.invoke(c);}catch(Exception e){fail(e);}},70_000);
    }
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    @SuppressWarnings("unchecked") private RainWidgetController controller()throws Exception{return ((Map<String,RainWidgetController>)field("rainWidgets")).get(HomeLayout.RAIN);}
    private void size(int i){try{
        HomeLayout layout=(HomeLayout)field("layout");layout.setPanelSpan(HomeLayout.RAIN,spans[i][0],spans[i][1]);
        Method render=MainActivity.class.getDeclaredMethod("render",int.class);render.setAccessible(true);render.invoke(this,0);
        RainWidgetController c=controller();current=(i==6?"Fill Home":spans[i][0]+"×"+spans[i][1])+" · "+(c.forecast==null?c.status():"LIVE forecast parsed");
        updateReport();
    }catch(Exception e){fail(e);}}
    private void fail(Exception e){current="FAIL: "+e.getClass().getSimpleName();updateReport();}
    private void updateReport(){if(report!=null)report.setText("Rain review · "+current+" · "+addressStatus+" · isolated preferences");}
    @Override protected void onDestroy(){runner.removeCallbacksAndMessages(null);super.onDestroy();getSharedPreferences("widget-home",0).edit().clear().apply();}
}
