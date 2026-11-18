package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.io.File;
import java.util.*;
import org.json.JSONObject;

/** Real Settings action and Back path, isolated preferences, no cleanup transport. */
public class CleanupInfoProbeActivity extends MainActivity {
    private final Set<String> temporary=new HashSet<>();
    protected String language(){return "en";}
    @Override protected void attachBaseContext(Context base){Configuration c=new Configuration(base.getResources().getConfiguration());c.setLocales(LocaleList.forLanguageTags(language()));super.attachBaseContext(base.createConfigurationContext(c));}
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("rain-")||name.startsWith("air-quality-widget-")||name.equals("air-expanded-cache")){
            name="cleanup-info-"+language()+"-"+name;if(temporary.add(name))super.getSharedPreferences(name,mode).edit().clear().apply();
        }return super.getSharedPreferences(name,mode);
    }
    private Object get(Object target,String name)throws Exception{Field f=target.getClass()==getClass()?MainActivity.class.getDeclaredField(name):target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private void call(String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(this);}
    private void select(Object menu,int index)throws Exception{Method m=MainActivity.class.getDeclaredMethod("selectMenuChoice",menu.getClass(),int.class,int.class);m.setAccessible(true);m.invoke(this,menu,index/((Integer)get(menu,"pageSize")),index);}
    private String text(View view){StringBuilder b=new StringBuilder();if(view instanceof TextView)b.append(((TextView)view).getText()).append('\n');if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)b.append(text(((ViewGroup)view).getChildAt(i)));return b.toString();}
    @Override protected void onCreate(Bundle state){
        Map<String,?> original=new LinkedHashMap<>(super.getSharedPreferences("widget-home",0).getAll());String mode=AppLanguage.mode(this);
        super.onCreate(state);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        new Handler().postDelayed(()->{JSONObject result=new JSONObject();try{
            call("homeSettings");Object menu=get(this,"activeMenu");String[] labels=(String[])get(menu,"labels");int index=Arrays.asList(labels).indexOf(getString(R.string.hb_cleanup_title));
            if(index<0)throw new IllegalStateException("Settings entry missing");select(menu,index);
            String content=text((View)get(this,"overlay"));if(!content.contains(getString(R.string.hb_cleanup_intro))||!content.contains("task cleanup:serve"))throw new IllegalStateException("Instructions not rendered");
            long down=android.os.SystemClock.uptimeMillis();
            dispatchKeyEvent(new android.view.KeyEvent(down,down,android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_BACK,0));
            dispatchKeyEvent(new android.view.KeyEvent(down,down+80,android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_BACK,0));
            if(!getString(R.string.hb_home_settings).equals(get(get(this,"activeMenu"),"title")))throw new IllegalStateException("Back lost Settings");
            select(get(this,"activeMenu"),index);
            if(!original.equals(super.getSharedPreferences("widget-home",0).getAll())||!mode.equals(AppLanguage.mode(this)))throw new IllegalStateException("Household state changed");
            result.put("status","pass").put("language",language()).put("checks","Actual Settings action, complete instructions, Back to Settings, unchanged household preferences and language");
        }catch(Exception e){try{result.put("status","fail").put("error",e.toString());}catch(Exception ignored){}}
        try{result.put("version",getPackageManager().getPackageInfo(getPackageName(),0).versionName);Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception ignored){}
        },7000);
    }
}
