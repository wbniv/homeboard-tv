package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.view.ViewGroup;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Production menu and remote controls against an independent provider, isolated Home preferences. */
public final class ProviderControlsProbeActivity extends MainActivity {
    private final Handler runner=new Handler();
    private final Set<String> temporary=new HashSet<>();
    private final List<String> passed=new ArrayList<>();
    private Map<String,?> original;
    private String target,language;
    private boolean finished;
    private int round;
    private long deadline;
    private TextView output;
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("air-quality-widget-")||name.startsWith("rain-widget-")){
            name="provider-controls-"+name;if(temporary.add(name))super.getSharedPreferences(name,mode).edit().clear().apply();
        }return super.getSharedPreferences(name,mode);
    }
    @Override protected void onCreate(Bundle state){
        original=new LinkedHashMap<>(super.getSharedPreferences("widget-home",MODE_PRIVATE).getAll());
        language=AppLanguage.mode(this);
        HomeLayout seed=HomeLayout.firstRun();seed.widgets.clear();seed.palette="forest";seed.uiOpacityPercent=100;
        getSharedPreferences("widget-home",MODE_PRIVATE).edit().putString("layout",seed.encode())
                .putBoolean("single-screen-default-v1",true).putString("undo_history","[]").apply();
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try{target=getPackageManager().getActivityInfo(getIntent().getComponent(),PackageManager.GET_META_DATA)
                .metaData.getString("providerPackage");}
        catch(Exception error){end("fail","No provider metadata");return;}
        runner.postDelayed(()->attempt(()->startRound()),10_000);
    }
    private interface Action{void run()throws Exception;}
    private void attempt(Action action){if(finished)return;try{action.run();}catch(Exception error){end("fail",error.toString());}}
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}
    private Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(this,args);}
    private void call(String name)throws Exception{call(name,new Class<?>[]{});}
    private HomeLayout layout()throws Exception{return (HomeLayout)field("layout");}
    private void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private void key(int code){dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}
    private void pass(String name){passed.add((round==0?"Home: ":"All Apps: ")+name);android.util.Log.i("ProviderControls",passed.get(passed.size()-1));}
    private void select(int index)throws Exception{
        Object menu=field("activeMenu");require(menu!=null,"No active menu");
        Field size=menu.getClass().getDeclaredField("pageSize");size.setAccessible(true);
        call("selectMenuChoice",new Class<?>[]{menu.getClass(),int.class,int.class},menu,index/(Integer)size.get(menu),index);
    }
    private void openPreview()throws Exception{
        call("providerList");List<ProjectivyProviderRegistry.Provider> providers=new ProjectivyProviderRegistry(this).discover();
        int selected=-1;for(int i=0;i<providers.size();i++)if(providers.get(i).component.getPackageName().equals(target))selected=i;
        require(selected>=0,"Provider not discoverable");select(selected);key(KeyEvent.KEYCODE_DPAD_UP);
        deadline=SystemClock.uptimeMillis()+40_000;runner.postDelayed(()->attempt(this::waitPreview),250);
    }
    private void startRound()throws Exception{
        set("allApps",round==1);call("render",new Class<?>[]{int.class},0);openPreview();
    }
    private boolean applying;
    private void waitPreview()throws Exception{
        if(!(Boolean)field("wallpaperReady")){
            if(SystemClock.uptimeMillis()>=deadline){end("blocked","Provider preview did not become ready: "+field("wallpaperStatus"));return;}
            runner.postDelayed(()->attempt(this::waitPreview),250);return;
        }
        require(layout().wallpaper.solid(),"Preview saved a provider early");
        require(((View)field("surface")).getVisibility()==View.INVISIBLE,"Dashboard covers preview");
        if(!applying){
            for(int opacity:new int[]{0,75,100}){
                call("applyUiBackgroundOpacity",new Class<?>[]{int.class},opacity);
                require(((View)field("surface")).getVisibility()==View.INVISIBLE,"Opacity exposed dashboard over preview");
                require(layout().uiOpacityPercent==100,"Preview changed saved widget opacity");
            }
            pass("Preview ready and visible at 0/75/100% widget opacity");
            key(KeyEvent.KEYCODE_BACK);
            require(field("wallpaperDraft")==null&&layout().wallpaper.solid(),"Back saved or retained preview draft");
            require(((View)field("surface")).getVisibility()==View.VISIBLE,"Back did not restore dashboard");
            pass("Back cancels preview and restores dashboard");
            for(int i=0;i<15&&field("overlay")!=null;i++)key(KeyEvent.KEYCODE_BACK);
            applying=true;openPreview();return;
        }
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        require(layout().wallpaper.component.startsWith(target+"/"),"Apply did not save selected provider");
        require(field("overlay")==null&&((View)field("surface")).getVisibility()==View.VISIBLE,"Apply did not return to dashboard");
        JSONArray history=new JSONArray(getSharedPreferences("widget-home",0).getString("undo_history","[]"));
        require(history.length()==1,"Apply did not create exactly one Undo entry");
        pass("Apply saves selected provider as one Undo entry");
        WallpaperSurface wallpaper=(WallpaperSurface)field("wallpaperSurface");
        Field dim=WallpaperSurface.class.getDeclaredField("dim");dim.setAccessible(true);
        float before=((View)dim.get(wallpaper)).getAlpha();
        call("applyUiBackgroundOpacity",new Class<?>[]{int.class},0);
        require(before==((View)dim.get(wallpaper)).getAlpha(),"Widget opacity altered wallpaper dimming");
        call("applyUiBackgroundOpacity",new Class<?>[]{int.class},100);pass("Wallpaper dimming independent of widget opacity");
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK));
        runner.postDelayed(()->attempt(()->{
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));
            require(layout().wallpaper.solid(),"Hold Back did not undo provider Apply");
            require(new JSONArray(getSharedPreferences("widget-home",0).getString("undo_history","[]")).length()==0,"Undo retained change");
            require((Boolean)field("allApps")== (round==1),"Hold release navigated unexpectedly");
            pass("Hold Back undoes provider Apply without navigating");
            applying=false;round++;
            if(round==2)end("pass","Home and All Apps provider controls passed");else startRound();
        }),1200);
    }
    private void end(String status,String reason){
        if(finished)return;finished=true;runner.removeCallbacksAndMessages(null);
        boolean unchanged=original.equals(super.getSharedPreferences("widget-home",0).getAll())&&language.equals(AppLanguage.mode(this));
        try{
            JSONObject report=new JSONObject().put("schema",1).put("providerPackage",target)
                    .put("baseline",unchanged?status:"fail").put("compatibility","partial")
                    .put("householdPreferencesUnchanged",unchanged).put("reason",reason).put("passed",new JSONArray(passed));
            File dir=getExternalFilesDir(null);if(dir==null)dir=getFilesDir();
            try(FileOutputStream out=new FileOutputStream(new File(dir,"wf.log"))){out.write(report.toString().getBytes(StandardCharsets.UTF_8));}
            output=new TextView(this);output.setTextColor(-1);output.setTextSize(18);output.setPadding(20,15,20,15);
            output.setBackgroundColor(0xee102333);output.setText(status.toUpperCase(java.util.Locale.ROOT)+" · "+target+"\n"+reason+"\n"+String.join("\n",passed)+"\nHousehold settings unchanged: "+unchanged);
            addContentView(output,new ViewGroup.LayoutParams(-1,-2));
        }catch(Exception error){android.util.Log.e("ProviderControls","Report failure",error);}
    }
    @Override protected void onDestroy(){runner.removeCallbacksAndMessages(null);super.onDestroy();
        for(String name:temporary)super.getSharedPreferences(name,0).edit().clear().apply();}
}
