package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Real key routing, preview, persistence and Undo using isolated preferences. */
public final class DashboardProbeActivity extends MainActivity {
    private final Handler runner=new Handler();
    private TextView report;
    private boolean failed;
    @Override public SharedPreferences getSharedPreferences(String name,int mode) {
        if(name.equals("widget-home")||name.startsWith("rain-cache-")||name.startsWith("air-quality-widget-"))name="dashboard-review-"+name;
        return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state) {
        getSharedPreferences("widget-home",0).edit().clear().putString("layout",new HomeLayout().encode()).apply();
        super.onCreate(state);
        report=new TextView(this);report.setTextSize(14);report.setTextColor(0xffffffff);report.setBackgroundColor(0xff102333);
        addContentView(report,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        step(3_000,()->{require(layout().widgets.isEmpty(),"Saved empty Home refilled");pass("Saved empty Home retained");});
        step(5_000,()->{press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps"),"Empty canvas Right trapped");pass("Right opens All Apps");});
        step(9_000,()->{press(KeyEvent.KEYCODE_BACK);require(!(boolean)field("allApps"),"Back did not return Home");press(KeyEvent.KEYCODE_DPAD_UP);press(KeyEvent.KEYCODE_DPAD_RIGHT);press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps"),"Settings header Right trapped");pass("Both header buttons allow escape");});
        step(13_000,()->{
            HomeLayout blank=new HomeLayout();blank.reducedMotion=true;blank.addPanel(HomeLayout.AIR);blank.setPanelSpan(HomeLayout.AIR,2,2);blank.homePositions.put(HomeLayout.AIR,new HomeLayout.HomePosition(2,0,0));
            set("layout",blank);set("homePage",0);set("allApps",false);set("focused",HomeLayout.AIR);call("render",new Class<?>[]{int.class},0);
            press(KeyEvent.KEYCODE_DPAD_RIGHT);require((int)field("homePage")==1&&!(boolean)field("allApps"),"Blank intermediate page skipped");pass("Blank intermediate page reached");
        });
        step(19_000,()->{press(KeyEvent.KEYCODE_DPAD_RIGHT);require((int)field("homePage")==2&&!(boolean)field("allApps"),"Populated final page skipped");press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps"),"Final page did not open All Apps");pass("Normal page order reaches All Apps");});
        step(25_000,()->{HomeLayout empty=new HomeLayout();empty.reducedMotion=true;set("layout",empty);set("homePage",0);set("allApps",false);set("focused","");call("saveWithoutUndo",new Class<?>[]{});call("render",new Class<?>[]{int.class},0);call("chooseDefaultDashboard",new Class<?>[]{});require(layout().widgets.size()==4,"Preview missing widgets");pass("Four-widget preview");});
        step(33_000,()->{press(KeyEvent.KEYCODE_BACK);require(layout().widgets.isEmpty(),"Cancel changed layout");require(HomeLayout.parse(getSharedPreferences("widget-home",0).getString("layout","")).widgets.isEmpty(),"Preview persisted before apply");pass("Cancel preserves empty Home");});
        step(38_000,()->{call("chooseDefaultDashboard",new Class<?>[]{});press(KeyEvent.KEYCODE_DPAD_CENTER);require(HomeLayout.parse(getSharedPreferences("widget-home",0).getString("layout","")).widgets.size()==4,"Apply not persisted");pass("Preset applied and saved");});
        step(46_000,()->{call("undoThrough",new Class<?>[]{int.class},0);require(layout().widgets.isEmpty(),"Undo did not restore empty Home");press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps"),"Right trapped after Undo");pass("Undo restores empty Home and navigation");});
        step(53_000,()->{press(KeyEvent.KEYCODE_BACK);call("chooseDefaultDashboard",new Class<?>[]{});press(KeyEvent.KEYCODE_DPAD_CENTER);pass("Final default dashboard");});
    }
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
