package com.gathin.tvhome;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;
import org.json.*;

/** Exercises the real host and native/phone transactions, using its own prefs and host ID. */
public final class AndroidWidgetProbeActivity extends MainActivity {
    private final Handler steps=new Handler();private final Set<String> temporary=new HashSet<>();private Map<String,?> household;private String key,before;
    @Override int androidWidgetHostId(){return 8724;}
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("rain-")||name.startsWith("air-quality-widget-")){
            name="android-widget-proof-"+name;if(temporary.add(name))super.getSharedPreferences(name,mode).edit().clear().commit();
        }return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state){
        household=new LinkedHashMap<>(super.getSharedPreferences("widget-home",0).getAll());
        if(state==null){HomeLayout seed=new HomeLayout();seed.homeColumns=8;seed.homeRows=4;seed.favoritesContainer=true;
            getSharedPreferences("widget-home",0).edit().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true).putBoolean("favorites-container-v1",true).commit();}
        super.onCreate(state);report("running","Starting real host acceptance");
        step(2500,()->beginAndroidWidgetTest(new ComponentName("net.biohack.homeboard.widgetprovider","net.biohack.homeboard.widgetprovider.ProbeProvider")));
        step(4500,()->{
            require(!PhoneConfigurationStore.preferences(this).getString(AndroidWidgetHostController.PENDING,"").isEmpty(),"No durable pending operation");
            choose(0);key=layout().widgets.stream().filter(AndroidWidgetSpec::key).findFirst().orElseThrow(()->new IllegalStateException("No saved Android widget"));
            require(layout().androidWidget(key).id>0,"Bound widget identity missing");
        });
        step(6500,()->{
            require(text(getWindow().getDecorView()).contains("Widget bound across apps"),"Real compact RemoteViews not painted");
            JSONObject snapshot=phoneState(),draft=snapshot.getJSONObject("draft");draft.getJSONArray("widgets").getJSONObject(0).put("page",1);
            phoneApply(new JSONObject().put("revision",snapshot.getLong("revision")).put("operation","android-widget-proof-move").put("draft",draft));
            before=layout().encode();set("homePage",1);set("focused",key);set("homeFocus",key);call("render",new Class<?>[]{int.class},0);
            press(KeyEvent.KEYCODE_DPAD_CENTER);
        });
        step(9000,()->{
            require(expanded()!=null,"Short OK failed to expand");require(text(expanded().getWindow().getDecorView()).contains("Widget bound across apps"),"Expanded provider view missing");
            require(text(expanded().getWindow().getDecorView()).contains("Saved dashboard size"),"Richer provider detail missing");
            View restore=find(expanded().getWindow().getDecorView(),getString(R.string.hb_fs_restore));require(restore!=null,"Restore action missing");restore.performClick();
        });
        step(11000,()->{
            require(expanded()==null&&before.equals(layout().encode())&&Integer.valueOf(1).equals(field("homePage"))&&key.equals(field("focused")),"Restore changed page, focus or saved size");
            int id=layout().androidWidget(key).id;JSONObject snapshot=phoneState(),draft=snapshot.getJSONObject("draft");draft.put("widgets",new JSONArray());
            JSONObject removed=phoneApply(new JSONObject().put("revision",snapshot.getLong("revision")).put("operation","android-widget-proof-remove").put("draft",draft));
            require(android.appwidget.AppWidgetManager.getInstance(this).getAppWidgetInfo(id)!=null,"Remove prematurely deleted Undo binding");
            phoneUndo(new JSONObject().put("revision",removed.getLong("revision")).put("operation","android-widget-proof-undo"));require(layout().androidWidget(key).id==id,"Undo replaced original binding");
            press(KeyEvent.KEYCODE_DPAD_CENTER);
        });
        step(14000,()->{require(expanded()!=null,"Reopening after Undo failed");expanded().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK));expanded().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));});
        step(16000,()->{
            require(expanded()==null,"Back failed to restore");require(household.equals(super.getSharedPreferences("widget-home",0).getAll()),"Household settings changed");
            android.appwidget.AppWidgetHost cleanup=new android.appwidget.AppWidgetHost(this,androidWidgetHostId());for(int id:cleanup.getAppWidgetIds())cleanup.deleteAppWidgetId(id);require(cleanup.getAppWidgetIds().length==0,"Disposable host IDs remain");
            report("pass","Actual compact/expanded RemoteViews; short OK, Restore/Back, phone move/remove/Undo; original binding retained; disposable IDs released; household unchanged");
        });
    }
    private Object field(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}catch(Exception e){throw new IllegalStateException(e);}}
    private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}
    private Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(this,args);}
    private void choose(int index)throws Exception{Object menu=field("activeMenu");require(menu!=null,"Review menu missing");call("selectMenuChoice",new Class<?>[]{menu.getClass(),int.class,int.class},menu,0,index);}
    private HomeLayout layout(){return (HomeLayout)field("layout");}
    private WidgetExpansion expanded(){return (WidgetExpansion)field("expandedWidget");}
    private void press(int code){dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}
    private View find(View v,String label){if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}return null;}
    private String text(View v){String result=v instanceof TextView?String.valueOf(((TextView)v).getText()):"";if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)result+="\n"+text(((ViewGroup)v).getChildAt(i));return result;}
    private interface Stage{void run()throws Exception;}
    private void step(long ms,Stage action){steps.postDelayed(()->{try{action.run();}catch(Exception e){steps.removeCallbacksAndMessages(null);report("fail",e.toString());}},ms);}
    private void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private void report(String status,String detail){try{JSONObject report=new JSONObject().put("status",status).put("detail",detail).put("homeboardVersion",getPackageManager().getPackageInfo(getPackageName(),0).versionName);Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),report.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}
    @Override protected void onDestroy(){steps.removeCallbacksAndMessages(null);super.onDestroy();android.appwidget.AppWidgetHost cleanup=new android.appwidget.AppWidgetHost(this,androidWidgetHostId());for(int id:cleanup.getAppWidgetIds())cleanup.deleteAppWidgetId(id);}
}
