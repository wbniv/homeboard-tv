package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.os.Bundle;

/** Native Home fixtures in a separate preference namespace; never changes household layouts. */
public final class WorldClockProbeActivity extends MainActivity {
    @Override public SharedPreferences getSharedPreferences(String name,int mode) {
        if(name.equals("widget-home")||name.startsWith("rain-cache-")||name.startsWith("air-quality-widget-"))name="world-clock-review-"+name;
        return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state) {
        if(state==null) {
            HomeLayout seed=HomeLayout.firstRun();seed.widgets.clear();seed.homePositions.clear();seed.panelSpans.clear();seed.homeColumns=8;seed.homeRows=4;
            WorldClockSettings clocks=new WorldClockSettings();WorldClockCatalog.studio(clocks);
            clocks.places.add(WorldClockCatalog.CITIES.get(1).place("tivissa"));clocks.places.add(WorldClockCatalog.CITIES.get(2).place("beijing"));clocks.places.add(WorldClockCatalog.CITIES.get(3).place("new-york"));
            clocks.primary="tivissa";clocks.reference=clocks.places.get(1).id;
            for(int i=0;i<3;i++) {
                String key=HomeLayout.WORLD_CLOCK+(i==0?"":":"+(i+1));seed.addPanel(key);seed.clockSettings.put(key,clocks.copy());
                seed.setPanelSpan(key,i==0?1:i==1?7:8,i==2?3:1);seed.homePositions.put(key,new HomeLayout.HomePosition(0,i==2?1:0,i==1?1:0));
            }
            String key=HomeLayout.WORLD_CLOCK+":3";
            getSharedPreferences("widget-home",0).edit().clear().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true)
                    .putString("nav_focus",key).putString("nav_home_focus",key).putBoolean("nav_all_apps",false).apply();
        }
        super.onCreate(state);
    }
}
