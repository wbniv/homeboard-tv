package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

/** Real production views; private preferences. No external store or app is launched. */
public class ProviderDirectoryProbeActivity extends MainActivity {
    private final Set<String> temporary=new HashSet<>();
    private Map<String,?> original;
    private boolean rejectLaunch;
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.startsWith("air-quality-widget-")||name.startsWith("rain-widget-")){
            name="directory-probe-"+name;if(temporary.add(name))super.getSharedPreferences(name,mode).edit().clear().apply();
        }return super.getSharedPreferences(name,mode);
    }
    @Override public void startActivity(Intent intent){
        if(rejectLaunch)throw new ActivityNotFoundException("Probe unavailable store");
        super.startActivity(intent);
    }
    private Object get(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}
    private void call(String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(this,args);}
    private void call(String name)throws Exception{call(name,new Class<?>[]{});}
    private boolean contains(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(contains(((ViewGroup)v).getChildAt(i),text))return true;return false;}
    private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View result=find(((ViewGroup)v).getChildAt(i),label);if(result!=null)return result;}return null;}
    private void require(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    @Override protected void onCreate(Bundle state){
        original=new LinkedHashMap<>(super.getSharedPreferences("widget-home",MODE_PRIVATE).getAll());
        super.onCreate(state);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        new Handler().postDelayed(()->runProof(),8000);
    }
    private void runProof(){JSONObject report=new JSONObject();try{
        String target=getPackageManager().getActivityInfo(getIntent().getComponent(),PackageManager.GET_META_DATA).metaData.getString("screen");
        call("providerDirectory");
        View overlay=(View)get("overlay");require(contains(overlay,"Overflight"),"First page missing");require(overlay.findFocus()!=null,"No remote focus");
        View next=find(overlay,getString(R.string.hb_next)+" ›");require(next!=null,"No paging wedge");next.performClick();
        require(contains((View)get("overlay"),"Daily Photos"),"Next page did not change cards");
        ProviderCatalog.Entry weather=ProviderCatalog.ENTRIES.get(7);
        call("drawProviderEntry",new Class<?>[]{ProviderCatalog.Entry.class},weather);
        require(!contains((View)get("overlay"),getString(R.string.hb_provider_open_play)),"Excluded provider offered store");
        require(contains((View)get("overlay"),getString(weather.detail)),"Missing exclusion explanation");
        ProviderCatalog.Entry movie=ProviderCatalog.ENTRIES.get(2);
        if(getPackageManager().getLaunchIntentForPackage(movie.packageName)==null){
            call("drawProviderEntry",new Class<?>[]{ProviderCatalog.Entry.class},movie);
            View play=find((View)get("overlay"),getString(R.string.hb_provider_open_play));
            require(play!=null,"Missing store route");rejectLaunch=true;play.performClick();rejectLaunch=false;
            require(get("returningToProvider")==null,"Failed store left pending return");
            require(contains((View)get("overlay"),movie.url),"Failed store lost phone fallback");
        }
        require(((HomeLayout)get("layout")).wallpaper.solid(),"Browsing selected a background");
        require(original.equals(super.getSharedPreferences("widget-home",MODE_PRIVATE).getAll()),"Household settings changed");
        set("providerDirectoryPage",0);set("providerDirectoryFocus",0);
        if(target.startsWith("page")){int page=Integer.parseInt(target.substring(4));set("providerDirectoryPage",page);set("providerDirectoryFocus",page*3);call("drawProviderDirectory");}
        else if(target.equals("dashboard")){call("dismissMenu");set("activeMenu",null);call("render",new Class<?>[]{int.class},0);}
        else {ProviderCatalog.Entry entry=ProviderCatalog.ENTRIES.stream().filter(e->e.id.equals(target)).findFirst().get();call("drawProviderEntry",new Class<?>[]{ProviderCatalog.Entry.class},entry);}
        report.put("status","pass").put("screen",target).put("householdPreferencesUnchanged",true)
            .put("checks","Paging, remote focus, exclusion, unavailable store fallback, no activation, original preferences unchanged")
            .put("notRun","Actual Google Play round trip; cross-app coordinator support required");
    }catch(Exception failure){try{report.put("status","fail").put("error",failure.toString());}catch(Exception ignored){}TextView error=new TextView(this);error.setText(report.toString());setContentView(error);}
    try{Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),report.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}
    }
}
