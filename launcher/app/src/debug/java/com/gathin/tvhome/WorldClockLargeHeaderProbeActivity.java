package com.gathin.tvhome;

public final class WorldClockLargeHeaderProbeActivity extends WorldClockHeaderProbeActivity {
    @Override protected void attachBaseContext(android.content.Context base){
        android.content.res.Configuration config=new android.content.res.Configuration(base.getResources().getConfiguration());config.fontScale=2;
        super.attachBaseContext(base.createConfigurationContext(config));
    }
    @Override protected WorldClockSettings clocks(){
        WorldClockSettings s=new WorldClockSettings();
        for(int i=0;i<7;i++)s.places.add(new WorldClockSettings.Place("extra"+i,"Family "+(i+1),"Asia/Tokyo"));
        s.primary="extra6";s.format="12";return s;
    }
}
