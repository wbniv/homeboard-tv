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
import java.time.*;
import java.util.*;
import java.lang.reflect.*;
import java.io.File;
import java.nio.file.Files;
import org.json.JSONObject;

/** Production UI with isolated preferences and locale; never changes the saved app language. */
public class DatePresentationProbeActivity extends MainActivity {
    private final Set<String> temporary=new HashSet<>();
    private Map<String,?> original;
    private String originalLanguage;
    protected String language(){return "th";}
    @Override protected void attachBaseContext(Context base){
        Configuration c=new Configuration(base.getResources().getConfiguration());c.setLocales(LocaleList.forLanguageTags(language()));
        super.attachBaseContext(base.createConfigurationContext(c));
    }
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.equals("air-expanded-cache")||name.startsWith("air-quality-widget-")||name.startsWith("rain-")){
            name="date-review-"+language()+"-"+name;
            if(temporary.add(name))super.getSharedPreferences(name,mode).edit().clear().apply();
        }return super.getSharedPreferences(name,mode);
    }
    private Object get(Object target,Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private void set(Object target,Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    private View find(View view,String label){if(view instanceof TextView&&((TextView)view).getText().toString().equals(label))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View match=find(((ViewGroup)view).getChildAt(i),label);if(match!=null)return match;}return null;}
    private void require(boolean value,String text){if(!value)throw new IllegalStateException(text);}
    private String text(View view){StringBuilder b=new StringBuilder();if(view instanceof TextView)b.append(((TextView)view).getText()).append('\n');if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)b.append(text(((ViewGroup)view).getChildAt(i)));return b.toString();}
    @Override protected void onCreate(Bundle state){
        original=new LinkedHashMap<>(super.getSharedPreferences("widget-home",0).getAll());originalLanguage=AppLanguage.mode(this);
        HomeLayout seed=HomeLayout.firstRun();seed.addFavorite("app:"+getPackageName()+"/com.gathin.tvhome.MainActivity");
        getSharedPreferences("widget-home",0).edit().putString("layout",seed.encode()).putBoolean("single-screen-default-v1",true).apply();
        super.onCreate(state);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        new Handler().postDelayed(()->{
            try{
                String screen=getPackageManager().getActivityInfo(getIntent().getComponent(),android.content.pm.PackageManager.GET_META_DATA).metaData.getString("screen");
                if(screen.equals("dashboard")){finishProof(screen);return;}
                String key=screen.equals("calendar")||screen.equals("restore")?HomeLayout.CALENDAR:screen.equals("air")?HomeLayout.AIR:screen.equals("favorites")?HomeLayout.FAVORITES:HomeLayout.HEADER_CLOCK;
                Bundle saved=new Bundle();saved.putString("selected",screen.equals("calendar")?"2026-11-21":"bangkok");saved.putString("start","2026-11-16");
                Method m=MainActivity.class.getDeclaredMethod("expandWidget",String.class,Bundle.class);m.setAccessible(true);m.invoke(this,key,saved);
                new Handler().postDelayed(()->finishProof(screen),3000);
            }catch(Exception error){report("fail","setup",error.toString());}
        },6000);
    }
    private void finishProof(String screen){try{
        boolean thai=language().equals("th");String year=thai?"2569":"2026";
        if(screen.equals("dashboard")){
            Map<?,?> calendars=(Map<?,?>)get(this,MainActivity.class,"calendarWidgets");
            require(!calendars.isEmpty(),"Missing Calendar widget");
            String today=getString(R.string.hb_cal_today,CalendarLabels.date(this,LocalDate.now()));
            for(Object widget:calendars.values())require(((View)widget).getContentDescription().toString().startsWith(today),"Calendar header lacks today's local date");
        }
        require(DisplayTime.locale(this).getLanguage().equals(language()),"Effective locale mismatch");
        require(((HomeLayout)get(this,MainActivity.class,"layout")).uiOpacityPercent==65,"Fresh widget opacity is not 65%");
        require(CalendarLabels.date(this,LocalDate.of(2026,11,18)).contains(year),"Calendar era");
        require(WorldClockLabels.date(this,Instant.parse("2026-11-18T07:05:00Z"),ZoneId.of("Asia/Bangkok")).contains(year),"Clock era");
        require(DisplayTime.duration(this,65*60000).equals(DisplayTime.STOPWATCH+(thai?"1 ชม. 5 น.":"1h 5m")),"Duration units/icon");
        require(DisplayTime.ago(this,0,65*60000).endsWith(thai?"ที่แล้ว":"ago"),"Relative wrapper");
        require(WorldClockModel.time(Instant.parse("2026-11-18T07:05:00Z"),ZoneId.of("Asia/Bangkok"),DisplayTime.locale(this),false,false).equals("14:05"),"Decorated clock face");
        WidgetExpansion dialog=(WidgetExpansion)get(this,MainActivity.class,"expandedWidget");
        if(screen.equals("air")){
            AirExpanded content=(AirExpanded)get(dialog,WidgetExpansion.class,"content");
            android.location.Location location=new android.location.Location("date-fixture");location.setLatitude(13.73);location.setLongitude(100.54);
            set(content,AirExpanded.class,"actual",location);
            StationCatalog.Station station=new StationCatalog.Station("date-fixture","Date presentation fixture","Bangkok",13.73,100.54,18.5,System.currentTimeMillis()-65*60_000L);
            set(content,AirExpanded.class,"all",new ArrayList<>(List.of(station)));
            set(content,AirExpanded.class,"loading",false);set(content,AirExpanded.class,"locating",false);
            Method update=AirExpanded.class.getDeclaredMethod("updateStations");update.setAccessible(true);update.invoke(content);
            content.active(false);set(content,AirExpanded.class,"closed",true);
        }
        if(dialog!=null&&!screen.equals("restore")){
            String ui=text(dialog.getWindow().getDecorView());require(ui.contains(year),"Rendered UI lacks "+year);
            if(screen.equals("clock"))require(ui.contains(DisplayTime.STOPWATCH),"Meeting duration lacks icon");
            if(screen.equals("favorites")||screen.equals("air"))require(ui.contains(DisplayTime.CLOCK),"Metadata lacks clock icon");
            if(screen.equals("air"))require(ui.contains(DisplayTime.STOPWATCH),"Age lacks stopwatch");
        }
        if(screen.equals("restore")){
            View restore=find(dialog.getWindow().getDecorView(),getString(R.string.hb_fs_restore));require(restore!=null,"Missing visible Restore dashboard");restore.performClick();
            new Handler().postDelayed(()->{try{
                require(get(this,MainActivity.class,"expandedWidget")==null,"Restore retained expansion");
                require(HomeLayout.CALENDAR.equals(get(this,MainActivity.class,"focused")),"Restore lost widget focus");
                require(original.equals(super.getSharedPreferences("widget-home",0).getAll()),"Household preferences changed");
                require(originalLanguage.equals(AppLanguage.mode(this)),"Saved app language changed");
                report("pass",screen,"Visible Restore dashboard returned to Calendar focus; household preferences/language unchanged");
            }catch(Exception error){report("fail",screen,error.toString());}},500);return;
        }
        require(original.equals(super.getSharedPreferences("widget-home",0).getAll()),"Household preferences changed");
        require(originalLanguage.equals(AppLanguage.mode(this)),"Saved app language changed");
        report("pass",screen,"BE/CE, units, selective icons and clean clock; household preferences/language unchanged");
    }catch(Exception error){report("fail",screen,error.toString());}}
    private void report(String status,String screen,String detail){try{
        JSONObject result=new JSONObject().put("status",status).put("language",language()).put("screen",screen).put("detail",detail).put("airData",screen.equals("air")?"synthetic date fixture":"not-applicable")
            .put("homeboardVersion",getPackageManager().getPackageInfo(getPackageName(),0).versionName)
            .put("widgetOpacity",((HomeLayout)get(this,MainActivity.class,"layout")).uiOpacityPercent);
        Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if(!status.equals("pass")){TextView error=new TextView(this);error.setText(result.toString());setContentView(error);}
    }catch(Exception ignored){}}
}
