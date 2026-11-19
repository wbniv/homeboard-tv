package com.gathin.tvhome;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Debug-only native rendering/restore acceptance; preserves household settings and timer runtime. */
public final class TimerProbeActivity extends MainActivity {
    private final Handler steps=new Handler(Looper.getMainLooper());private final Set<String> temporary=new HashSet<>();private Map<String,?> household;private String runtime;private boolean seeded;
    @Override public SharedPreferences getSharedPreferences(String name,int mode){if(name.equals("widget-home")||name.equals("kitchen-timers")||name.startsWith("rain-")||name.startsWith("air-quality-widget-")){name="timer-proof-"+name;if(temporary.add(name))super.getSharedPreferences(name,mode).edit().clear().commit();}return super.getSharedPreferences(name,mode);}
    @Override protected void onCreate(Bundle saved){
        household=new LinkedHashMap<>(super.getSharedPreferences("widget-home",0).getAll());runtime=getSharedPreferences("kitchen-timers",0).getString("runtime",null);
        try{
            // Refuse instead of replacing a household's active timers, including finished unacknowledged ones.
            KitchenTimers prior=KitchenTimerStore.load(this);if(prior.items().length()!=0)throw new IllegalStateException("Timer proof requires an empty timer collection; existing data preserved");
            KitchenTimers sample=KitchenTimers.empty();long now=SystemClock.elapsedRealtime(),wall=System.currentTimeMillis();int boot=KitchenTimerStore.boot(this);
            for(int i=0;i<3;i++){JSONObject create=new JSONObject().put("operation",UUID.randomUUID().toString()).put("revision",sample.revision()).put("action","create").put("kind","timer").put("name",new String[]{"Rice / ข้าว","Vegetables / ผัก","Sauce / ซอส"}[i]).put("duration",new long[]{754000,200000,300000}[i]);sample.command(create,now,wall,boot);}
            sample.items().getJSONObject(1).put("state","done").put("remaining",0);sample.items().getJSONObject(2).put("state","paused");
            // These render fixtures never arm an actual alarm or play sound.
            for(int i=0;i<3;i++)sample.items().getJSONObject(i).put("state",i==1?"done":"paused");
            getSharedPreferences("kitchen-timers",0).edit().putString("runtime",sample.data.toString()).commit();seeded=true;
            HomeLayout seed=new HomeLayout();seed.homeColumns=8;seed.homeRows=4;seed.favoritesContainer=true;seed.addPanel(HomeLayout.TIMER);seed.setPanelSpan(HomeLayout.TIMER,2,1);seed.homePositions.put(HomeLayout.TIMER,new HomeLayout.HomePosition(1,0,0));
            getSharedPreferences("widget-home",0).edit().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true).putBoolean("favorites-container-v1",true).putInt("nav_home_page",1).putString("nav_focus",HomeLayout.TIMER).putString("nav_home_focus",HomeLayout.TIMER).putBoolean("nav_all_apps",false).commit();
        }catch(Exception e){throw new IllegalStateException(e);}super.onCreate(saved);
        step(3000,()->{set("homePage",1);set("focused",HomeLayout.TIMER);call("render",new Class[]{int.class},0);capture("dashboard-2x1");step(1500,()->{call("activate",new Class[]{String.class},HomeLayout.TIMER);step(2200,this::expandedCheck);});});
    }
    private void expandedCheck()throws Exception{
        WidgetExpansion expanded=(WidgetExpansion)field("expandedWidget");if(expanded==null)throw new AssertionError("Short OK did not expand");View decor=expanded.getWindow().getDecorView();String text=text(decor);if(!text.contains("Rice")||!text.contains("Vegetables"))throw new AssertionError("Expanded timer names missing");capture("expanded");View restore=find(decor,getString(R.string.hb_fs_restore));if(restore==null)throw new AssertionError("Missing Restore dashboard");
        step(1800,()->{restore.performClick();step(800,()->{if((int)field("homePage")!=1||!HomeLayout.TIMER.equals(field("focused")))throw new AssertionError("Page/focus restore failed");capture("restored");HomeLayout layout=(HomeLayout)field("layout");layout.setPanelSpan(HomeLayout.TIMER,1,1);call("render",new Class[]{int.class},0);step(1200,()->{capture("dashboard-1x1");layout.setPanelSpan(HomeLayout.TIMER,4,2);call("render",new Class[]{int.class},0);step(1000,()->{capture("dashboard-4x2");if(!household.equals(super.getSharedPreferences("widget-home",0).getAll()))throw new AssertionError("Household layout changed");call("activate",new Class[]{String.class},HomeLayout.TIMER);report("pass","Native sizes, expanded data, Restore page and focus. Audio/exact delivery requires a separate device scenario.");});});});});
    }
    private void cleanup(){if(!seeded)return;seeded=false;SharedPreferences.Editor editor=getSharedPreferences("kitchen-timers",0).edit();if(runtime==null)editor.remove("runtime");else editor.putString("runtime",runtime);editor.commit();KitchenTimerStore.reconcile(this);}
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}
    private Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(this,args);}
    private interface Action{void run()throws Exception;}
    private void step(long delay,Action action){steps.postDelayed(()->{try{action.run();}catch(Throwable e){report("failed",e.toString());cleanup();}},delay);}
    private String text(View v){StringBuilder s=new StringBuilder();if(v instanceof TextView)s.append(((TextView)v).getText());if(v instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)v).getChildCount();i++)s.append('\n').append(text(((android.view.ViewGroup)v).getChildAt(i)));return s.toString();}
    private View find(View v,String label){if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return v;if(v instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)v).getChildCount();i++){View found=find(((android.view.ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}return null;}
    private void capture(String name)throws Exception{View v=getWindow().getDecorView();WidgetExpansion e=(WidgetExpansion)field("expandedWidget");if(e!=null)v=e.getWindow().getDecorView();android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(v.getWidth(),v.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(bitmap));File dir=new File(getExternalFilesDir(null),"timer-proof");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
    private void report(String status,String message){try{File dir=new File(getExternalFilesDir(null),"timer-proof");dir.mkdirs();Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),new JSONObject().put("status",status).put("message",message).put("homeboardVersion",getPackageManager().getPackageInfo(getPackageName(),0).versionName).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception ignored){}}
    @Override protected void onDestroy(){steps.removeCallbacksAndMessages(null);cleanup();super.onDestroy();}
}
