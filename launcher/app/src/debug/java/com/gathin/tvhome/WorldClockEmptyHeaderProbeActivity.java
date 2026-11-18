package com.gathin.tvhome;

import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

/** Enter empty Home after startup, exercising real navigation without changing household settings. */
public final class WorldClockEmptyHeaderProbeActivity extends WorldClockHeaderProbeActivity {
    private final Handler checks=new Handler(Looper.getMainLooper());
    private boolean failed;
    private TextView report;
    @Override protected void onCreate(Bundle state){super.onCreate(state);report=new TextView(this);report.setTextColor(0xffffffff);report.setTextSize(14);addContentView(report,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));if(state==null){
        step(15000,()->{HomeLayout layout=(HomeLayout)field("layout");for(String key:new ArrayList<>(layout.widgets))layout.delete(key,true);call("saveWithoutUndo",new Class<?>[]{});set("focused",HomeLayout.RAIN);call("render",new Class<?>[]{int.class},0);require("".equals(field("focused")),"Empty Home did not retain surface navigation");pass("Empty Home retains surface focus");});
        step(18000,()->{press(KeyEvent.KEYCODE_DPAD_RIGHT);require((boolean)field("allApps"),"One Right did not open All Apps");pass("One Right opens All Apps from empty Home");});
        step(21000,()->{press(KeyEvent.KEYCODE_BACK);require(!(boolean)field("allApps")&&((HomeLayout)field("layout")).widgets.isEmpty(),"Back refilled empty Home");press(KeyEvent.KEYCODE_DPAD_UP);press(KeyEvent.KEYCODE_DPAD_LEFT);require(HomeLayout.HEADER_CLOCK.equals(field("focused")),"Header is unreachable from empty Home");pass("Empty Home remains empty; header reachable");});
        step(24000,()->{press(KeyEvent.KEYCODE_DPAD_CENTER);require(field("expandedWidget")!=null,"Empty Home header did not expand");pass("Empty Home opens full planner");});
        step(28000,()->{WidgetExpansion expanded=(WidgetExpansion)field("expandedWidget");expanded.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));require(field("expandedWidget")==null&&HomeLayout.HEADER_CLOCK.equals(field("focused")),"Planner Back lost header focus");pass("EMPTY_PASS · one-Right All Apps, Back, empty grid, header access and full-screen restoration");});
    }}
    private interface Check{void run()throws Exception;}
    private void step(long delay,Check check){checks.postDelayed(()->{if(failed)return;try{check.run();}catch(Exception error){failed=true;report.setText("EMPTY_FAIL · "+error);android.util.Log.e("HomeboardEmptyProbe","EMPTY_FAIL",error);}},delay);}
    private void pass(String text){report.setText(text);android.util.Log.i("HomeboardEmptyProbe",text);}
    private void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private Object field(String name)throws Exception{java.lang.reflect.Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(this);}
    private void set(String name,Object value)throws Exception{java.lang.reflect.Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(this,value);}
    private void call(String name,Class<?>[] types,Object...args)throws Exception{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(this,args);}
    private void press(int key){dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));}
    @Override protected void onPause(){checks.removeCallbacksAndMessages(null);super.onPause();}
}
