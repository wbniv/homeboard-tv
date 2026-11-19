package com.gathin.tvhome;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.util.*;

final class KitchenTimerUi {
    static JSONObject command(Context c,String action)throws JSONException{return new JSONObject().put("action",action).put("operation",UUID.randomUUID().toString()).put("revision",KitchenTimerStore.snapshot(c).getLong("revision"));}
    static String name(Context c,JSONObject t){return t.optString("name").isEmpty()?c.getString("event".equals(t.optString("kind"))?R.string.hb_timer_event:R.string.hb_timer_cooking):t.optString("name");}
    static int state(String s){return switch(s){case "running"->R.string.hb_timer_running;case "paused"->R.string.hb_timer_paused;case "done"->R.string.hb_timer_done;case "interrupted"->R.string.hb_timer_interrupted;case "missed"->R.string.hb_timer_missed;default->R.string.hb_timer_stopped;};}
    static String detail(Context c,JSONObject t){
        String state=t.optString("state");boolean live="running".equals(state)||"paused".equals(state);
        if("event".equals(t.optString("kind")))return DisplayTime.timestamp(c,Instant.ofEpochMilli(t.optLong("at")),ZoneId.systemDefault(),"EEE d MMM yyyy HH:mm")+" · "+c.getString(state(state));
        return (live?DisplayTime.STOPWATCH+KitchenTimers.clock(KitchenTimers.remaining(t,SystemClock.elapsedRealtime(),System.currentTimeMillis()))+" · ":"")+c.getString(state(state));
    }
    static List<JSONObject> ordered(JSONArray items){List<JSONObject> list=new ArrayList<>();for(int i=0;i<items.length();i++)list.add(items.optJSONObject(i));list.sort(Comparator.comparingInt((JSONObject t)->"done".equals(t.optString("state"))?0:"running".equals(t.optString("state"))?1:"paused".equals(t.optString("state"))?2:3).thenComparingLong(t->KitchenTimers.remaining(t,SystemClock.elapsedRealtime(),System.currentTimeMillis())));return list;}
    static void access(Activity a){
        if(!KitchenTimerStore.allowed(a))try{a.startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+a.getPackageName())));return;}catch(ActivityNotFoundException noScreen){Toast.makeText(a,R.string.hb_timer_permission,Toast.LENGTH_LONG).show();return;}
        if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},9104);
    }
}
