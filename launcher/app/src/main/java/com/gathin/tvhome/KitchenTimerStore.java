package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import org.json.*;

/** One app-private runtime, independent of layout drafts, Undo and widget instances. */
final class KitchenTimerStore {
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("kitchen-timers",Context.MODE_PRIVATE);}
    static int boot(Context c){return Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);}
    static boolean allowed(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
    static synchronized KitchenTimers load(Context c)throws JSONException{return new KitchenTimers(new JSONObject(prefs(c).getString("runtime",KitchenTimers.empty().data.toString())));}
    /** Told on the main thread after every save, with the saved revision (TimerTicker re-reads only on a new one). */
    interface Listener{void changed(long revision);}
    private static final java.util.List<Listener> listeners=new java.util.concurrent.CopyOnWriteArrayList<>();
    private static Handler main;
    static void listen(Listener l){if(!listeners.contains(l))listeners.add(l);}
    static void unlisten(Listener l){listeners.remove(l);}
    private static synchronized void notifyChanged(long revision){if(listeners.isEmpty())return;if(main==null)main=new Handler(Looper.getMainLooper());main.post(()->{for(Listener l:listeners)l.changed(revision);});}
    private static void persist(Context c,KitchenTimers model){if(!prefs(c).edit().putString("runtime",model.data.toString()).commit())throw new IllegalStateException("Unable to save timers");notifyChanged(model.revision());}
    static synchronized JSONObject snapshot(Context c){try{
        KitchenTimers model=load(c);if(model.reconcile(SystemClock.elapsedRealtime(),System.currentTimeMillis(),boot(c))){persist(c,model);schedule(c,model);ring(c,model);}
        return new JSONObject(model.data.toString()).put("now",System.currentTimeMillis()).put("elapsed",SystemClock.elapsedRealtime()).put("zone",java.time.ZoneId.systemDefault().getId()).put("alarmAccess",allowed(c)).put("alertError",prefs(c).getBoolean("alert-error",false));
    }catch(JSONException e){throw new IllegalStateException(e);}}
    static synchronized JSONObject command(Context c,JSONObject command){try{
        KitchenTimers model=load(c);if(model.reconcile(SystemClock.elapsedRealtime(),System.currentTimeMillis(),boot(c))){persist(c,model);schedule(c,model);ring(c,model);}
        String action=command.getString("action");if(!model.data.getJSONObject("receipts").has(command.optString("operation"))&&("create".equals(action)||"resume".equals(action)||"restart".equals(action))&&!allowed(c))throw new IllegalArgumentException(c.getString(R.string.hb_timer_permission));
        boolean changed=model.command(command,SystemClock.elapsedRealtime(),System.currentTimeMillis(),boot(c));
        if(changed)persist(c,model);schedule(c,model);ring(c,model);return snapshot(c);
    }catch(JSONException e){throw new IllegalArgumentException("Invalid timer command",e);}}
    static synchronized void reconcile(Context c){try{KitchenTimers model=load(c);boolean changed=model.reconcile(SystemClock.elapsedRealtime(),System.currentTimeMillis(),boot(c));if(changed)persist(c,model);boolean running=false;for(int i=0;i<model.items().length();i++)running|="running".equals(model.items().optJSONObject(i).optString("state"));if(changed||running)schedule(c,model);ring(c,model);}catch(JSONException e){error(c,true);}}
    static synchronized void silence(Context c){try{KitchenTimers m=load(c);for(int i=0;i<m.items().length();i++)m.items().getJSONObject(i).put("soundDeadline",0);m.data.put("revision",m.revision()+1);persist(c,m);}catch(JSONException e){error(c,true);}}
    /** alert-error is outside the revision, so a change asks listeners to re-read (-1 matches no revision). */
    static void error(Context c,boolean value){if(prefs(c).getBoolean("alert-error",false)==value)return;prefs(c).edit().putBoolean("alert-error",value).apply();notifyChanged(-1);}
    static boolean sounding(KitchenTimers m,long elapsed){return m.sounding(elapsed);}
    private static void ring(Context c,KitchenTimers m){if(sounding(m,SystemClock.elapsedRealtime()))try{c.startForegroundService(new Intent(c,KitchenTimerSound.class));}catch(RuntimeException e){error(c,true);}}
    private static PendingIntent pending(Context c){return PendingIntent.getBroadcast(c,9101,new Intent(c,KitchenTimerReceiver.class).setAction("timer-due"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private static void schedule(Context c,KitchenTimers m){
        AlarmManager alarms=c.getSystemService(AlarmManager.class);PendingIntent intent=pending(c);alarms.cancel(intent);long next=Long.MAX_VALUE,wall=System.currentTimeMillis(),elapsed=SystemClock.elapsedRealtime();
        for(int i=0;i<m.items().length();i++){JSONObject t=m.items().optJSONObject(i);if("running".equals(t.optString("state")))next=Math.min(next,wall+KitchenTimers.remaining(t,elapsed,wall));}
        if(next==Long.MAX_VALUE||!allowed(c))return;
        PendingIntent show=PendingIntent.getActivity(c,9102,ScreensaverActivity.timerScreenIntent(c),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try{alarms.setAlarmClock(new AlarmManager.AlarmClockInfo(Math.max(wall+1,next),show),intent);}catch(SecurityException e){error(c,true);}
    }
}
