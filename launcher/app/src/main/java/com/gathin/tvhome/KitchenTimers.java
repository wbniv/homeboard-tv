package com.gathin.tvhome;

import org.json.*;
import java.time.*;
import java.util.*;

/** Persistent domain state. Duration timers and event instants deliberately use different clocks. */
final class KitchenTimers {
    static final long MAX_DURATION=24*60*60*1000L;
    final JSONObject data;
    KitchenTimers(JSONObject data){this.data=data;}
    static KitchenTimers empty() throws JSONException{return new KitchenTimers(new JSONObject().put("revision",0).put("items",new JSONArray()).put("receipts",new JSONObject()));}
    JSONArray items(){return data.optJSONArray("items");}
    long revision(){return data.optLong("revision");}
    JSONObject find(String id){for(int i=0;i<items().length();i++){JSONObject t=items().optJSONObject(i);if(t.optString("id").equals(id))return t;}throw new IllegalArgumentException("Timer no longer exists");}
    static long remaining(JSONObject t,long elapsed,long wall){return "event".equals(t.optString("kind"))?Math.max(0,t.optLong("at")-wall):"running".equals(t.optString("state"))?Math.max(0,t.optLong("deadline")-elapsed):Math.max(0,t.optLong("remaining"));}
    boolean reconcile(long elapsed,long wall,int boot)throws JSONException{
        boolean changed=false;
        for(int i=0;i<items().length();i++){
            JSONObject t=items().getJSONObject(i);String state=t.getString("state");boolean event="event".equals(t.getString("kind"));
            if(!event&&"running".equals(state)&&t.optInt("boot",-1)!=boot){t.put("state","interrupted").put("soundUntil",0);changed=true;continue;}
            if(event&&"running".equals(state)&&t.optInt("boot",-1)!=boot){t.put("boot",boot);changed=true;if(t.getLong("at")<=wall){t.put("state","missed").put("soundUntil",0);continue;}}
            if("running".equals(state)&&remaining(t,elapsed,wall)==0){t.put("state","done").put("remaining",0).put("soundUntil",wall+60_000);changed=true;}
            if(t.optLong("soundUntil")>0&&t.optInt("boot",-1)!=boot){t.put("soundUntil",0).put("boot",boot);changed=true;}
        }
        if(changed)data.put("revision",revision()+1);return changed;
    }
    /** Commands have stable operation identities: retries cannot add time or start a duplicate. */
    boolean command(JSONObject command,long elapsed,long wall,int boot)throws JSONException{
        String op=command.getString("operation");if(!op.matches("[A-Za-z0-9_-]{16,80}"))throw new IllegalArgumentException("Invalid operation identity");
        String fingerprint=canonical(command);JSONObject receipts=data.getJSONObject("receipts");
        if(receipts.has(op)){if(!receipts.getString(op).equals(fingerprint))throw new IllegalArgumentException("Operation identity already used");return false;}
        if(command.getLong("revision")!=revision())throw new IllegalArgumentException("Timers changed; refresh before trying again");
        String action=command.getString("action");
        if("create".equals(action)){
            if(items().length()>=24)throw new IllegalArgumentException("Remove an old timer first (maximum 24)");
            String name=command.getString("name").trim();if(name.length()>80)throw new IllegalArgumentException("Name is too long");
            String kind=command.getString("kind");JSONObject t=new JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("kind",kind).put("state","running").put("boot",boot).put("soundUntil",0);
            if("timer".equals(kind)){long duration=whole(command,"duration",1000,MAX_DURATION);t.put("duration",duration).put("remaining",duration).put("deadline",elapsed+duration);}
            else if("event".equals(kind)){
                // ISO dates on the wire; locale-specific dates are presentation only.
                LocalDateTime local=LocalDateTime.parse(command.getString("local"));ZoneId zone=ZoneId.systemDefault();
                var offsets=zone.getRules().getValidOffsets(local);if(offsets.size()!=1)throw new IllegalArgumentException("This local time is missing or ambiguous; choose another time");
                long at=local.toInstant(offsets.get(0)).toEpochMilli();if(at<=wall||at-wall>366L*24*60*60*1000)throw new IllegalArgumentException("Choose a future time within the next year");
                if(!zone.getId().equals(command.getString("zone")))throw new IllegalArgumentException("TV timezone changed; refresh before setting the alarm");
                t.put("at",at).put("zone",zone.getId());
            }else throw new IllegalArgumentException("Unknown timer type");
            items().put(t);
        }else if("silence".equals(action)){for(int i=0;i<items().length();i++)items().getJSONObject(i).put("soundUntil",0);}
        else {
            JSONObject t=find(command.getString("id"));String state=t.getString("state");boolean event="event".equals(t.getString("kind"));
            switch(action){
                case "pause":if(event||!"running".equals(state))throw new IllegalArgumentException("This timer cannot be paused");t.put("remaining",remaining(t,elapsed,wall)).put("state","paused");break;
                case "resume":if(event||!"paused".equals(state))throw new IllegalArgumentException("This timer cannot be resumed");t.put("deadline",elapsed+t.getLong("remaining")).put("boot",boot).put("state","running");break;
                case "add":if(event||!("running".equals(state)||"paused".equals(state)))throw new IllegalArgumentException("Add time to a running or paused cooking timer");long rem=remaining(t,elapsed,wall)+60_000;if(rem>MAX_DURATION)throw new IllegalArgumentException("Maximum 24 hours");t.put("remaining",rem);if("running".equals(state))t.put("deadline",elapsed+rem);break;
                case "restart":if(event)throw new IllegalArgumentException("Create another event alarm");t.put("deadline",elapsed+t.getLong("duration")).put("remaining",t.getLong("duration")).put("boot",boot).put("state","running").put("soundUntil",0);break;
                case "stop":t.put("state","stopped").put("remaining",0).put("soundUntil",0);break;
                case "remove":if("running".equals(state)||"paused".equals(state)||t.optLong("soundUntil")>wall)throw new IllegalArgumentException("Stop this timer before removing it");for(int i=0;i<items().length();i++)if(items().getJSONObject(i)==t){items().remove(i);break;}break;
                default:throw new IllegalArgumentException("Unknown timer action");
            }
        }
        data.put("revision",revision()+1);receipts.put(op,fingerprint);
        // A stale retry outside this bounded journal still fails its original revision check.
        while(receipts.length()>96)receipts.remove(receipts.keys().next());return true;
    }
    private static long whole(JSONObject o,String key,long min,long max)throws JSONException{Object v=o.get(key);if(!(v instanceof Number)||!Double.isFinite(((Number)v).doubleValue())||((Number)v).doubleValue()!=((Number)v).longValue())throw new IllegalArgumentException("Whole seconds required");long n=((Number)v).longValue();if(n<min||n>max)throw new IllegalArgumentException("Duration must be between 1 second and 24 hours");return n;}
    private static String canonical(JSONObject o)throws JSONException{List<String> keys=new ArrayList<>();o.keys().forEachRemaining(keys::add);Collections.sort(keys);StringBuilder s=new StringBuilder();for(String key:keys)s.append(key).append(':').append(new JSONArray().put(o.get(key)).toString()).append(';');return s.toString();}
    static String clock(long millis){long seconds=(Math.max(0,millis)+999)/1000;return seconds>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
}
