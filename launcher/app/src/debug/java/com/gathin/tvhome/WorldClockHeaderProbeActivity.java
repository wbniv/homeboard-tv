package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.os.Bundle;

/** Header acceptance runs in isolated preferences, including the empty-Home variant. */
public class WorldClockHeaderProbeActivity extends MainActivity {
    protected boolean empty(){return false;}
    protected WorldClockSettings clocks(){return new WorldClockSettings();}
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("rain-cache-")||name.startsWith("air-quality-widget-"))name="header-review-"+name;
        return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state){
        if(state==null){HomeLayout seed=HomeLayout.firstRun();seed.headerClock=clocks();if(empty()){seed.widgets.clear();seed.homePositions.clear();seed.panelSpans.clear();}
            getSharedPreferences("widget-home",0).edit().clear().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true)
                .putString("nav_focus",HomeLayout.HEADER_CLOCK).putString("nav_home_focus",HomeLayout.HEADER_CLOCK).putBoolean("nav_all_apps",false).apply();}
        super.onCreate(state);
    }
}
