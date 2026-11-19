package com.gathin.tvhome;

import android.content.*;
import android.app.job.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Process-wide calendar repository. The UI and background job use one durable cache. */
final class CalendarData {
    private static CalendarData instance;
    final HolidayStore store;
    final HolidayUpdater updater;
    final CalendarSnapshot snapshot;
    final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<Runnable> listeners=new CopyOnWriteArrayList<>(), completions=new ArrayList<>();
    private Thread worker;
    private Set<String> activeSources=Collections.emptySet();
    private final Set<String> pendingSources=new LinkedHashSet<>();
    private boolean activeManual,pendingManual;
    volatile boolean busy;
    volatile String progress="";
    static synchronized CalendarData get(Context context){
        if(instance==null)try{instance=new CalendarData(context.getApplicationContext(),new File(context.getFilesDir(),"calendar-data"),null);}
        catch(Exception error){throw new IllegalStateException("Calendar storage could not be opened",error);}
        return instance;
    }
    CalendarData(Context context,File directory,HolidayHttp.Transport transport)throws Exception{
        this.context=context;String seed;
        try(InputStream input=context.getAssets().open("calendar-snapshot.json")){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=input.read(b))!=-1)bytes.write(b,0,n);seed=bytes.toString("UTF-8");}
        store=new HolidayStore(directory,seed);snapshot=CalendarSnapshot.parse(store.snapshot());
        updater=new HolidayUpdater(store,transport==null?HolidayHttp.network(new File(directory,"captures")):transport);
    }
    void listen(Runnable listener){listeners.add(listener);}
    void unlisten(Runnable listener){listeners.remove(listener);}
    private void announce(){main.post(()->{for(Runnable listener:listeners)listener.run();});}
    Set<String> enabled(){
        Set<String> result=new LinkedHashSet<>();
        try{
            String encoded=context.getSharedPreferences("widget-home",0).getString("layout",null);
            HomeLayout layout=encoded==null?HomeLayout.firstRun():HomeLayout.parse(encoded);
            for(String widget:layout.widgets)if(widget.equals(HomeLayout.CALENDAR)){
                CalendarWidgetSettings settings=layout.calendarSettings.get(widget);result.addAll((settings==null?new CalendarWidgetSettings():settings).enabled);
            }
        }catch(Exception invalid){/* Do not fetch based on an unreadable household configuration. */}
        return result;
    }
    synchronized void refresh(boolean manual,Runnable complete){refresh(manual,complete,enabled(),Instant.now());}
    synchronized void refresh(boolean manual,Runnable complete,Set<String> enabled,Instant now){
        if(complete!=null)completions.add(complete);
        if(busy){
            if(!activeSources.containsAll(enabled)||(manual&&!activeManual)){pendingSources.addAll(enabled);pendingManual|=manual;}
            return;
        }
        busy=true;progress="";activeSources=new LinkedHashSet<>(enabled);activeManual=manual;announce();
        worker=new Thread(()->{
            try{
                Instant at=now;
                while(true){
                    updater.run(at,activeSources,activeManual,key->{progress=key;announce();});
                    synchronized(CalendarData.this){
                        if(pendingSources.isEmpty())break;
                        activeSources=new LinkedHashSet<>(pendingSources);activeManual=pendingManual;pendingSources.clear();pendingManual=false;
                    }
                    at=Instant.now();
                }
            }
            catch(Exception failure){progress="stopped";}
            finally{
                try{snapshot.replace(CalendarSnapshot.parse(store.snapshot()));}catch(Exception ignored){/* Keep last valid display. */}
                List<Runnable> done=new ArrayList<>();
                synchronized(CalendarData.this){
                    Set<String> next=new LinkedHashSet<>(pendingSources);boolean force=pendingManual;
                    busy=false;worker=null;pendingSources.clear();pendingManual=false;
                    // A request can arrive between the loop's final check and
                    // snapshot publication. Hand it off without losing callbacks.
                    if(!next.isEmpty()&&!Thread.currentThread().isInterrupted()&&!progress.equals("stopped"))refresh(force,null,next,Instant.now());
                    else {done.addAll(completions);completions.clear();}
                }
                announce();main.post(()->{for(Runnable callback:done)callback.run();});
            }
        },"homeboard-calendar-update");worker.start();
    }
    synchronized void cancel(){if(worker!=null)worker.interrupt();}
    void schedule(){
        if(!(context instanceof android.app.Application))return; // Isolated activity probes do not schedule household jobs.
        JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler!=null&&scheduler.getPendingJob(CalendarUpdateJob.ID)==null)
            scheduler.schedule(new JobInfo.Builder(CalendarUpdateJob.ID,new ComponentName(context,CalendarUpdateJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(3_600_000L,900_000L).build());
    }
    String describe(Context c,CalendarWidgetSettings settings){
        StringBuilder text=new StringBuilder(c.getString(busy?R.string.hb_data_refreshing:R.string.hb_data_offline_explain));
        if(!busy&&progress.equals("stopped"))text.append("\n").append(c.getString(R.string.hb_data_stopped));
        if(store.storageFailed)text.append("\n").append(c.getString(R.string.hb_data_storage));
        try{
            JSONObject checks=store.checks();
            for(String id:CalendarWidgetSettings.SOURCES){
                if(!settings.enabled.contains(id))continue;
                text.append("\n\n").append(CalendarLabels.source(c,id));
                if(CalendarSnapshot.calculated(id)){
                    text.append("\n").append(c.getString(R.string.hb_data_calculated)).append("\n")
                        .append(CalendarLabels.date(c,LocalDate.of(ThaiCivilCalendar.FIRST_YEAR,1,1))).append(" — ")
                        .append(CalendarLabels.date(c,LocalDate.of(ThaiCivilCalendar.LAST_YEAR,12,31)))
                        .append("\n").append(ThaiCivilCalendar.REVISION);continue;
                }
                CalendarSnapshot.Provider p=snapshot.providers.get(id);
                ZoneId zone=p==null?CalendarSnapshot.BANGKOK:p.zone;int year=LocalDate.now(zone).getYear();
                for(int y=year;y<=year+1;y++){
                    String label=DisplayTime.formatter("uuuu",DisplayTime.locale(c)).format(LocalDate.of(y,1,1));
                    text.append("\n").append(label).append(": ");
                    JSONObject coverage=p==null?null:p.coverage.optJSONObject(String.valueOf(y));
                    List<String> scopes=new ArrayList<>();scopes.add("national");
                    if(id.equals("spain")){
                        if(!settings.spanishNational)scopes.clear();if(settings.catalunya)scopes.add("catalunya");if(settings.tarragona)scopes.add("tarragona-province");if(settings.tivissa)scopes.add("tivissa");
                    }else if(id.equals("thai")&&settings.bangkok)scopes.add("bangkok");
                    boolean full=coverage!=null&&!scopes.isEmpty();List<String> missing=new ArrayList<>();
                    for(String scope:scopes)if(coverage==null||!coverage.optString(scope).equals("complete")){full=false;missing.add(scopeLabel(c,scope));}
                    final int shownYear=y;boolean any=p!=null&&p.events.stream().anyMatch(e->e.date.getYear()==shownYear);
                    text.append(c.getString(full?R.string.hb_data_complete:any?R.string.hb_data_partial:R.string.hb_data_missing));
                    if(!missing.isEmpty())text.append(" · ").append(String.join(", ",missing));
                }
                long success=0;boolean failed=false,overdue=false;Iterator<String> keys=checks.keys();
                while(keys.hasNext()){String key=keys.next();if(!key.equals(id)&&!key.startsWith(id+":"))continue;
                    String[] parts=key.split(":");if(parts.length>1&&parts[1].matches("\\d{4}")&&Integer.parseInt(parts[1])>year+1)continue;
                    JSONObject value=checks.getJSONObject(key);success=Math.max(success,value.optLong("success",0));failed|=value.optInt("failures",0)>0;
                    long attempt=value.optLong("attempt",0),interval=key.endsWith(":closures")?HolidayUpdater.closureInterval(Instant.ofEpochMilli(attempt)):86_400_000L;
                    overdue|=attempt>0&&System.currentTimeMillis()>attempt+interval;
                }
                text.append("\n").append(c.getString(R.string.hb_data_last_check)).append(": ")
                    .append(success==0?c.getString(R.string.hb_data_never):DisplayTime.timestamp(c,Instant.ofEpochMilli(success),ZoneId.systemDefault(),"d MMM uuuu HH:mm"));
                if(failed)text.append("\n").append(c.getString(R.string.hb_data_failed));
                if(overdue)text.append("\n").append(c.getString(R.string.hb_data_overdue));
                if(id.equals("thai")){
                    JSONObject closure=checks.optJSONObject("thai:closures");long last=closure==null?0:closure.optLong("success",0);
                    text.append("\n").append(c.getString(R.string.hb_data_closure_check)).append(": ").append(last==0?c.getString(R.string.hb_data_never):DisplayTime.timestamp(c,Instant.ofEpochMilli(last),CalendarSnapshot.BANGKOK,"d MMM uuuu HH:mm"));
                }
                if(p!=null&&!p.source.isEmpty())text.append("\n").append(p.source);
            }
        }catch(Exception error){text.append("\n").append(c.getString(R.string.hb_data_failed));}
        return text.toString();
    }
    private static String scopeLabel(Context c,String scope){
        switch(scope){case "national":return c.getString(R.string.hb_cal_es_national);case "catalunya":return c.getString(R.string.hb_cal_es_region);
            case "tarragona-province":return c.getString(R.string.hb_cal_es_province);case "tivissa":return c.getString(R.string.hb_cal_es_local);default:return c.getString(R.string.hb_cal_bangkok);}
    }
}
