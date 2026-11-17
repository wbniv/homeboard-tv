package com.gathin.tvhome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Reproduce the shipped two-row preview/header traps using isolated household settings. */
public final class DashboardProbeActivity extends MainActivity {
    private final Handler runner=new Handler();
    private TextView report;
    private boolean failed;
    @Override public SharedPreferences getSharedPreferences(String name,int mode) {
        if(name.equals("widget-home")||name.startsWith("rain-cache-")||name.startsWith("air-quality-widget-"))name="dashboard-review-"+name;
        return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state) {
        HomeLayout seed=HomeLayout.firstRun();seed.homeColumns=8;seed.homeRows=2;seed.reducedMotion=true;
        seed.rainSettings.get(HomeLayout.RAIN).label="Retained location";
        seed.rainSettings.get(HomeLayout.RAIN).hours=12;
        seed.homePositions.put(HomeLayout.CALENDAR,new HomeLayout.HomePosition(1,0,0));
        for(ResolveInfo app:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),0)) {
            if(!app.activityInfo.packageName.equals(getPackageName())&&seed.favorites.size()<5)
                seed.addFavorite("app:"+new android.content.ComponentName(app.activityInfo.packageName,app.activityInfo.name).flattenToString());
        }
        getSharedPreferences("widget-home",0).edit().clear().putString("layout",seed.encode()).apply();
        super.onCreate(state);
        report=new TextView(this);report.setTextSize(14);report.setTextColor(0xffffffff);report.setBackgroundColor(0xff102333);
        addContentView(report,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        step(3_000,()->{singleScreen();require(layout().rainSettings.get(HomeLayout.RAIN).hours==12,"Repair reset Rain settings");require(getSharedPreferences("widget-home",0).getBoolean("single-screen-default-v1",false),"Repair not remembered");pass("Two-row upgrade repaired once; all four on page 1");});
        step(8_000,()->{set("focused","control:homeboard-settings");call("restoreFocus",new Class<?>[]{});press(KeyEvent.KEYCODE_DPAD_RIGHT);press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps"),"Populated settings row trapped Right");pass("Populated settings row opens All Apps");});
        step(13_000,()->{press(KeyEvent.KEYCODE_BACK);for(String key:new ArrayList<>(layout().widgets)) {
            set("focused",key);call("restoreFocus",new Class<?>[]{});
            for(int i=0;i<20&&!(boolean)field("allApps");i++)press(KeyEvent.KEYCODE_DPAD_RIGHT);
            require((boolean)field("allApps"),"Right trapped from "+key);press(KeyEvent.KEYCODE_BACK);
        }pass("Right reaches All Apps from every widget and Favorites");});
        step(19_000,()->{call("showUndoBanner",new Class<?>[]{String.class},"Navigation test");((TextView)field("undoButton")).requestFocus();press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps"),"Undo trapped Right");press(KeyEvent.KEYCODE_BACK);((Runnable)field("hideUndoBanner")).run();pass("Undo control allows All Apps");});
        step(25_000,()->{layout().homePositions.put(HomeLayout.CALENDAR,new HomeLayout.HomePosition(2,0,0));call("saveWithoutUndo",new Class<?>[]{});call("widgetsAndRestore",new Class<?>[]{});press(KeyEvent.KEYCODE_DPAD_DOWN);singleScreen();require(field("firstPageBefore")==null,"Default left a confirmation preview");require(HomeLayout.parse(getSharedPreferences("widget-home",0).getString("layout","")).pageCount(layout().widgets,8,2)==1,"Default not saved immediately");pass("Default applies immediately on one screen");});
        step(32_000,()->{call("firstPagePreview",new Class<?>[]{String.class},HomeLayout.RAIN);require(field("firstPageBefore")!=null,"Legacy preview not started");for(int i=0;i<10&&!(boolean)field("allApps");i++)press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps")&&field("firstPageBefore")==null,"Final preview edge trapped Right");press(KeyEvent.KEYCODE_BACK);singleScreen();pass("Right exits old preview and restores saved dashboard");});
        step(40_000,()->{call("undoThrough",new Class<?>[]{int.class},0);require(layout().homePositions.get(HomeLayout.CALENDAR).page==2,"Undo did not restore original placement");call("chooseDefaultDashboard",new Class<?>[]{});singleScreen();pass("Immediate arrangement remains Undoable");});
        step(47_000,()->{for(String key:new ArrayList<>(layout().widgets))layout().delete(key,true);call("saveWithoutUndo",new Class<?>[]{});set("focused",HomeLayout.RAIN);set("homePage",0);call("render",new Class<?>[]{int.class},0);press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps"),"Empty Home trapped Right");press(KeyEvent.KEYCODE_BACK);require(layout().widgets.isEmpty(),"Empty Home refilled");pass("Empty Home remains navigable");});
        step(54_000,()->{layout().homeRows=4;call("chooseDefaultDashboard",new Class<?>[]{});singleScreen();pass("Four-row grid also keeps every widget on page 1");});
        step(61_000,()->{layout().homeRows=2;call("chooseDefaultDashboard",new Class<?>[]{});singleScreen();pass("PASS: two-row single-screen default; all Right exits verified");});
    }
    private void singleScreen()throws Exception{HomeLayout s=layout();int c=(int)call("homeGridColumns",new Class<?>[]{}),r=(int)call("homeGridRows",new Class<?>[]{});require(s.widgets.size()==4,"Missing default widgets");for(HomeLayout.Cell cell:s.cells(s.widgets,c,r))require(cell.page==0,"Widget left the first screen: "+cell.key);require((int)field("homePage")==0,"Not showing first Home page");}
    private interface Stage {void run()throws Exception;}
    private void step(long delay,Stage stage){runner.postDelayed(()->{if(failed)return;try{stage.run();}catch(Exception error){failed=true;report.setText("FAIL · "+error);}},delay);}
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}
    private Object call(String name,Class<?>[] types,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(this,args);}
    private HomeLayout layout()throws Exception{return (HomeLayout)field("layout");}
    private void press(int key){dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));}
    private void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private void pass(String message){report.setText("PASS · "+message+" · isolated settings");}
    @Override protected void onPause(){runner.removeCallbacksAndMessages(null);super.onPause();}
    @Override protected void onDestroy(){runner.removeCallbacksAndMessages(null);super.onDestroy();}
}
