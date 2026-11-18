package com.gathin.tvhome;

import android.os.*;
import android.view.*;
import android.widget.TextView;

/** Exercise real planner buttons and nested Back against isolated header settings. */
public final class WorldClockMeetingProbeActivity extends WorldClockHeaderProbeActivity {
    private final Handler checks=new Handler(Looper.getMainLooper());
    @Override protected void onCreate(Bundle state){super.onCreate(state);if(state==null){
        checks.postDelayed(()->step(()->{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod("expandWidget",String.class,Bundle.class);method.setAccessible(true);method.invoke(this,HomeLayout.HEADER_CLOCK,null);}),2000);
        checks.postDelayed(()->step(()->{button(getString(R.string.hb_wc_compare)).performClick();require(content().state().getBoolean("comparing"),"Compare time did not start");}),5000);
        checks.postDelayed(()->step(()->{for(int expected:new int[]{60,90,15,30}){button(getString(R.string.hb_wc_duration,content().state().getInt("duration"))).performClick();require(content().state().getInt("duration")==expected,"Duration did not change");}button(getString(R.string.hb_wc_map)).performClick();require(content().state().getBoolean("map"),"Map missing");}),9000);
        checks.postDelayed(()->step(()->{button(getString(R.string.hb_wc_now)).performClick();require(!content().state().getBoolean("comparing"),"Now did not restore live");button(getString(R.string.hb_wc_compare)).performClick();expansion().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));require(!content().state().getBoolean("comparing"),"Back did not exit comparison first");android.util.Log.i("HomeboardMeetingProbe","MEETING_PASS · real Compare, 60/90/15/30 durations, Map, Now and nested Back");}),13000);
    }}
    private interface Check{void run()throws Exception;}
    private void step(Check check){try{check.run();}catch(Exception e){android.util.Log.e("HomeboardMeetingProbe","MEETING_FAIL",e);}}
    private WidgetExpansion expansion()throws Exception{java.lang.reflect.Field field=MainActivity.class.getDeclaredField("expandedWidget");field.setAccessible(true);return (WidgetExpansion)field.get(this);}
    private WidgetExpansion.Content content()throws Exception{java.lang.reflect.Field field=WidgetExpansion.class.getDeclaredField("content");field.setAccessible(true);return (WidgetExpansion.Content)field.get(expansion());}
    private View button(String text)throws Exception{View result=find(expansion().getWindow().getDecorView(),text);if(result==null)throw new IllegalStateException("Missing button: "+text);return result;}
    private View find(View view,String text){if(view instanceof android.widget.Button&&((TextView)view).getText().toString().equals(text))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
    private void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    @Override protected void onPause(){checks.removeCallbacksAndMessages(null);super.onPause();}
}
