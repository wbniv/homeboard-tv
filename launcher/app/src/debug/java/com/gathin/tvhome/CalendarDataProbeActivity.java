package com.gathin.tvhome;

import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.lang.reflect.*;
import org.json.*;

/** Isolated native UI acceptance. Fake source transport never contacts a publisher or changes household prefs. */
public class CalendarDataProbeActivity extends MainActivity {
    private final Set<String> temporary=new HashSet<>();
    private Map<String,?> original;private String savedLanguage,screen;
    private boolean offline;private CalendarWidgetSettings settings;
    private CalendarData data;
    private final java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();
    protected String language(){return "en";}
    @Override protected void attachBaseContext(Context base){Configuration config=new Configuration(base.getResources().getConfiguration());config.setLocales(LocaleList.forLanguageTags(language()));super.attachBaseContext(base.createConfigurationContext(config));}
    @Override public SharedPreferences getSharedPreferences(String name,int mode){
        if(name.equals("widget-home")||name.equals("air-expanded-cache")||name.startsWith("air-quality-widget-")||name.startsWith("rain-")){
            name="calendar-data-proof-"+language()+"-"+name;if(temporary.add(name))super.getSharedPreferences(name,mode).edit().clear().apply();
        }return super.getSharedPreferences(name,mode);
    }
    private String seed()throws Exception{try(InputStream input=getAssets().open("calendar-snapshot.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=input.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}}
    private File directory(){return new File(getCacheDir(),"calendar-data-proof-"+language());}
    @Override protected CalendarData createCalendarData(){try{
        CalendarSnapshot seed=CalendarSnapshot.parse(seed());StringBuilder ics=new StringBuilder("BEGIN:VCALENDAR\n");
        List<CalendarSnapshot.Event> us=seed.providers.get("usa").events;
        for(CalendarSnapshot.Event e:us)if(e.kind==CalendarSnapshot.Kind.HOLIDAY){
            LocalDate day=e.date;for(CalendarSnapshot.Event candidate:us)if(candidate.kind==CalendarSnapshot.Kind.OBSERVED&&e.date.equals(candidate.anchor))day=candidate.date;
            ics.append("BEGIN:VEVENT\nDTSTART;VALUE=DATE:").append(day.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)).append("\nSUMMARY:").append(e.title).append("\nEND:VEVENT\n");
        }
        for(CalendarSnapshot.Event e:us)if(e.kind==CalendarSnapshot.Kind.OBSERVED&&e.anchor!=null&&e.anchor.getYear()>2027)
            ics.append("BEGIN:VEVENT\nDTSTART;VALUE=DATE:").append(e.date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)).append("\nSUMMARY:New Year's Day\nEND:VEVENT\n");
        ics.append("END:VCALENDAR\n");final String body=ics.toString();
        data=new CalendarData(this,directory(),new HolidayHttp.Transport(){
            public HolidayHttp.Capture get(String url)throws Exception{if(offline)throw new IOException("Controlled offline fixture");if(!url.equals(HolidayUpdater.OPM))throw new IOException("Unexpected fixture URL");requests.incrementAndGet();Thread.sleep(120);return new HolidayHttp.Capture(url,body,"fixture-v1","",false);}
            public void accepted(HolidayHttp.Capture ignored){}
        });return data;
    }catch(Exception error){throw new IllegalStateException(error);}}
    @Override protected void onCreate(Bundle state){
        original=new LinkedHashMap<>(super.getSharedPreferences("widget-home",0).getAll());savedLanguage=AppLanguage.mode(this);
        try{screen=getPackageManager().getActivityInfo(getIntent().getComponent(),android.content.pm.PackageManager.GET_META_DATA).metaData.getString("screen");}catch(Exception error){throw new IllegalStateException(error);}
        offline=screen.equals("boundary")||screen.equals("restore");
        HomeLayout layout=HomeLayout.firstRun();settings=new CalendarWidgetSettings();settings.enabled.clear();settings.enabled.addAll(Arrays.asList("usa","buddhist","flowers"));layout.calendarSettings.put(HomeLayout.CALENDAR,settings);
        if(screen.equals("compact"))layout.setPanelSpan(HomeLayout.CALENDAR,5,1);
        getSharedPreferences("widget-home",0).edit().putString("layout",layout.encode()).putBoolean("single-screen-default-v1",true).apply();
        super.onCreate(state);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        new Handler().postDelayed(()->{try{
            if(offline)show();else data.refresh(true,()->{
                try{if(screen.equals("failure")){
                    String before=data.store.snapshot();long success=data.store.checks().getJSONObject("usa:2026").getLong("success");offline=true;
                    data.refresh(true,()->{try{require(before.equals(data.store.snapshot()),"Failed update replaced saved dates");require(data.store.checks().getJSONObject("usa:2026").getLong("success")==success,"Failure advanced successful check");show();}catch(Exception error){report("fail",error.toString());}});
                }else show();}catch(Exception error){report("fail",error.toString());}
            });
        }catch(Exception error){report("fail",error.toString());}},5000);
    }
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    private View find(View view,String label){if(view instanceof TextView&&((TextView)view).getText().toString().equals(label))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=find(((ViewGroup)view).getChildAt(i),label);if(found!=null)return found;}return null;}
    private void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private void show()throws Exception{
        if(!offline){require(!data.store.storageFailed,"Atomic calendar storage failed on Android");require(data.store.checks().getJSONObject("usa:2026").optLong("success",0)>0,"Fixture source refresh did not succeed");}
        HolidayStore reopened=new HolidayStore(directory(),seed());CalendarSnapshot cached=CalendarSnapshot.parse(reopened.snapshot());
        require(reopened.checks().getJSONObject("usa:2026").optLong("success",0)>0,"No successfully refreshed fixture revision survived repository reopen");
        require(cached.providers.get("usa").events.stream().anyMatch(e->e.date.getYear()==2027),"Following year was not retained offline");
        require(!cached.providers.containsKey("buddhist")&&!cached.providers.containsKey("flowers"),"Old religious event arrays are still bundled");
        CalendarSnapshot.Display boundary=cached.display(settings,LocalDate.of(2026,12,28),LocalDate.of(2027,1,3));
        require(boundary.days.stream().anyMatch(d->d.date.equals(LocalDate.of(2026,12,29))&&d.events.stream().anyMatch(e->e.kind==CalendarSnapshot.Kind.MAKE&&e.anchor.equals(LocalDate.of(2027,1,1)))),"Missing preparation for next-year Wan Phra");
        if(screen.equals("dashboard")||screen.equals("compact")){finishProof();return;}
        Bundle saved=new Bundle(),content=new Bundle();content.putString("selected","2027-01-01");content.putString("start","2026-12-28");saved.putBundle("content",content);
        Method expand=MainActivity.class.getDeclaredMethod("expandWidget",String.class,Bundle.class);expand.setAccessible(true);expand.invoke(this,HomeLayout.CALENDAR,saved);
        new Handler().postDelayed(()->{try{
            WidgetExpansion expanded=(WidgetExpansion)field("expandedWidget");require(expanded!=null,"Calendar did not expand");
            if(screen.equals("sources")||screen.equals("failure")){
                View source=find(expanded.getWindow().getDecorView(),getString(R.string.hb_data_title));require(source!=null,"Missing visible source details action");source.performClick();
                String summary=data.describe(this,settings);require(summary.contains(getString(R.string.hb_data_calculated)),"Missing calculated source label");
                require(summary.contains(getString(R.string.hb_data_complete)),"Following-year completeness missing");
                if(screen.equals("failure"))require(summary.contains(getString(R.string.hb_data_failed)),"Failure not explained");
                if(screen.equals("sources")){
                    View refresh=find(expanded.getWindow().getDecorView(),getString(R.string.hb_data_refresh));require(refresh!=null,"Missing visible refresh action");
                    int before=requests.get();refresh.performClick();refresh.performClick();
                    waitForManualRefresh(before,25);return;
                }
            }else if(screen.equals("restore")){
                View restore=find(expanded.getWindow().getDecorView(),getString(R.string.hb_fs_restore));require(restore!=null,"Missing visible Restore dashboard");restore.performClick();
                new Handler().postDelayed(()->{try{
                require(field("expandedWidget")==null,"Restore retained expansion");require(HomeLayout.CALENDAR.equals(field("focused")),"Restore lost Calendar focus");
                expand.invoke(this,HomeLayout.CALENDAR,saved);
                new Handler().postDelayed(()->{try{
                    WidgetExpansion again=(WidgetExpansion)field("expandedWidget");require(again!=null,"Second Calendar expansion missing");
                    again.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK));again.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));
                    new Handler().postDelayed(()->{try{
                    require(field("expandedWidget")==null,"Short Back retained expansion");require(HomeLayout.CALENDAR.equals(field("focused")),"Short Back lost Calendar focus");finishProof();
                    }catch(Exception error){report("fail",error.toString());}},400);
                }catch(Exception error){report("fail",error.toString());}},1200);
                }catch(Exception error){report("fail",error.toString());}},400);return;
            }
            finishProof();
        }catch(Exception error){report("fail",error.toString());}},1200);
    }
    private void finishProof(){try{require(original.equals(super.getSharedPreferences("widget-home",0).getAll()),"Household layout changed");require(savedLanguage.equals(AppLanguage.mode(this)),"Household language changed");
        String detail="Reopened two-year revision, calculated boundary anchor and unchanged household preferences";
        if(screen.equals("sources"))detail+="; visible source/refresh actions and repeated-request coalescing";
        if(screen.equals("failure"))detail+="; failed refresh preserved dates and successful-check time";
        if(screen.equals("restore"))detail+="; visible Restore and short Back returned to Calendar focus";
        if(screen.equals("boundary"))detail+="; next-year calendar displayed with offline fixture transport";
        report("pass",detail);
    }catch(Exception error){report("fail",error.toString());}}
    private void waitForManualRefresh(int before,int remaining){new Handler().postDelayed(()->{try{
        if(data.busy&&remaining>0){waitForManualRefresh(before,remaining-1);return;}
        require(!data.busy,"Manual refresh did not finish");require(requests.get()==before+1,"Repeated refresh was not coalesced");finishProof();
    }catch(Exception error){report("fail",error.toString());}},200);}
    private void report(String status,String detail){try{
        JSONObject result=new JSONObject().put("status",status).put("language",language()).put("screen",screen).put("homeboardVersion",getPackageManager().getPackageInfo(getPackageName(),0).versionName)
            .put("detail",detail).put("network","controlled fixture; no publisher/device network changes").put("offlineTransport",offline);
        Files.write(new File(getExternalFilesDir(null),"wf.log").toPath(),result.toString().getBytes(StandardCharsets.UTF_8));
        if(!status.equals("pass")){TextView failure=new TextView(this);failure.setText(result.toString());setContentView(failure);}
    }catch(Exception ignored){}}
}
