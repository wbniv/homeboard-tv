package com.gathin.tvhome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Exercises real Home with isolated preferences; only actual installed app identities are used. */
public final class FavoritesProbeActivity extends MainActivity {
    private final Handler runner=new Handler();
    private TextView report;
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("air-quality-widget-")||name.startsWith("rain-cache-"))name="favorites-review-"+name;
        return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state){
        HomeLayout seed=new HomeLayout();seed.homeColumns=8;seed.homeRows=4;
        for(ResolveInfo app:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),0)){
            if(!app.activityInfo.packageName.equals(getPackageName()))seed.favorite("app:"+new android.content.ComponentName(app.activityInfo.packageName,app.activityInfo.name).flattenToString());
        }
        seed.addPanel(HomeLayout.RAIN);RainSettings r=seed.rainSettings.get(HomeLayout.RAIN);r.automatic=false;r.latitude=13.7563;r.longitude=100.5018;r.name="Bangkok example";
        getSharedPreferences("widget-home",0).edit().clear().putString("layout",seed.encode()).apply();
        super.onCreate(state);
        report=new TextView(this);report.setTextColor(0xffffffff);report.setBackgroundColor(0xff102333);report.setTextSize(14);
        addContentView(report,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        runner.postDelayed(()->check("Page one"),7000);
        runner.postDelayed(()->{try{panel().enter();check("Apps focused");}catch(Exception e){fail(e);}},11_000);
        for(int i=0;i<6;i++){runner.postDelayed(()->{try{panel().navigate(KeyEvent.KEYCODE_DPAD_DOWN);check("Internal scroll");}catch(Exception e){fail(e);}},14_000+i*700L);}
        runner.postDelayed(()->{try{panel().header();check("Header focus");}catch(Exception e){fail(e);}},23_000);
        runner.postDelayed(()->{try{HomeLayout layout=(HomeLayout)field("layout");layout.setPanelSpan(HomeLayout.FAVORITES,1,1);call("render",new Class<?>[]{int.class},0);check("1x1 opener");}catch(Exception e){fail(e);}},28_000);
        runner.postDelayed(()->{try{call("openFavoritesBrowser",new Class<?>[]{});check("Larger browser");}catch(Exception e){fail(e);}},34_000);
        runner.postDelayed(()->{try{call("dismissMenu",new Class<?>[]{});HomeLayout layout=(HomeLayout)field("layout");layout.setPanelSpan(HomeLayout.FAVORITES,4,2);call("render",new Class<?>[]{int.class},0);check("Return to Home");}catch(Exception e){fail(e);}},42_000);
    }
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    private Object call(String name,Class<?>[] types,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(this,args);}
    private FavoritesWidgetView panel()throws Exception{return (FavoritesWidgetView)field("favoritesWidget");}
    private void check(String stage){try{
        HomeLayout layout=(HomeLayout)field("layout");
        if(layout.pageOf(HomeLayout.RAIN,layout.widgets,8,4)!=0||layout.pageOf(HomeLayout.FAVORITES,layout.widgets,8,4)!=0)throw new IllegalStateException("Requested panels left page one");
        Field f=FavoritesWidgetView.class.getDeclaredField("list");f.setAccessible(true);RecyclerView list=(RecyclerView)f.get(panel());
        if(list.isVerticalScrollBarEnabled()||list.isHorizontalScrollBarEnabled()||list.getOverScrollMode()!=android.view.View.OVER_SCROLL_NEVER)throw new IllegalStateException("Scrollbar or overscroll enabled");
        report.setText("PASS · "+stage+" · "+layout.favorites.size()+" favorites · isolated preferences · wedges only");
    }catch(Exception e){fail(e);}}
    private void fail(Exception e){report.setText("FAIL · "+e.toString());}
    @Override protected void onDestroy(){runner.removeCallbacksAndMessages(null);super.onDestroy();getSharedPreferences("widget-home",0).edit().clear().apply();}
}
