package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;

/** Layout and Air configuration share one atomic SharedPreferences file and revision. Caches stay separate. */
final class PhoneConfigurationStore {
    static final String AIR="widget-air-configurations",REVISION="configuration-revision",OPERATIONS="phone-operation-results";
    static SharedPreferences preferences(Context c){return c.getSharedPreferences("widget-home",Context.MODE_PRIVATE);}
    static JSONObject air(SharedPreferences prefs){try{return new JSONObject(prefs.getString(AIR,"{}"));}catch(JSONException e){throw new IllegalStateException("Air settings unreadable",e);}}
    static JSONObject readAir(Context c,String key,JSONObject initial) {
        SharedPreferences prefs=preferences(c);JSONObject all=air(prefs);JSONObject saved=all.optJSONObject(key);
        if(saved!=null)return saved;
        try{all.put(key,initial);if(!prefs.edit().putString(AIR,all.toString()).commit())throw new IllegalStateException("Air save failed");return initial;}
        catch(JSONException e){throw new IllegalStateException(e);}
    }
    static void writeAir(Context c,String key,JSONObject config) {
        SharedPreferences prefs=preferences(c);JSONObject all=air(prefs);
        if(config.toString().equals(String.valueOf(all.optJSONObject(key))))return;
        try{all.put(key,config);if(!revision(prefs,prefs.edit()).putString(AIR,all.toString()).commit())throw new IllegalStateException("Air save failed");}
        catch(JSONException e){throw new IllegalStateException(e);}
    }
    /** Persistent installation identity: draft recovery never depends on a changeable TV name. */
    static String tvId(SharedPreferences prefs){
        String id=prefs.getString("phone-tv-id",null);
        if(id==null){id=java.util.UUID.randomUUID().toString();if(!prefs.edit().putString("phone-tv-id",id).commit())throw new IllegalStateException("TV identity save failed");}
        return id;
    }
    static JSONObject instances(SharedPreferences prefs)throws JSONException{return new JSONObject(prefs.getString("widget-instance-generations","{}"));}
    static JSONObject initializeInstances(SharedPreferences prefs,HomeLayout layout)throws JSONException {
        JSONObject before=instances(prefs),after=nextInstances(layout,layout,before);
        if(!before.toString().equals(after.toString())&&!prefs.edit().putString("widget-instance-generations",after.toString()).commit())throw new IllegalStateException("Widget identity save failed");
        return after;
    }
    static SharedPreferences.Editor trackInstances(SharedPreferences prefs,SharedPreferences.Editor edit,HomeLayout next){
        try{
            String saved=prefs.getString("layout",null);
            HomeLayout previous=saved==null?new HomeLayout():HomeLayout.parse(saved);JSONObject generations=instances(prefs);
            return edit.putString("widget-instance-generations",nextInstances(previous,next,generations).toString());
        }catch(JSONException invalid){throw new IllegalStateException("Widget identity unreadable",invalid);}
    }
    static JSONObject nextInstances(HomeLayout previous,HomeLayout next,JSONObject generations)throws JSONException {
        JSONObject result=new JSONObject(generations.toString());
        for(String key:next.widgets)if(!result.has(key)||!previous.widgets.contains(key))result.put(key,result.optLong(key,0)+1);
        return result;
    }
    static SharedPreferences.Editor revision(SharedPreferences prefs,SharedPreferences.Editor edit){return edit.putLong(REVISION,prefs.getLong(REVISION,0)+1);}
    static JSONObject aggregate(SharedPreferences prefs)throws JSONException{return new JSONObject().put("layout",new JSONObject(prefs.getString("layout","{}"))).put("air",air(prefs));}
    static JSONObject operations(SharedPreferences prefs)throws JSONException{return new JSONObject(prefs.getString(OPERATIONS,"{}"));}
    static String result(SharedPreferences prefs,String id)throws JSONException{return operations(prefs).optString(id,"");}
    static long commit(SharedPreferences prefs,HomeLayout layout,JSONObject air,JSONArray undo,String id,String outcome)throws JSONException {
        return commit(prefs,layout,air,undo,id,outcome,false);
    }
    static long commit(SharedPreferences prefs,HomeLayout layout,JSONObject air,JSONArray undo,String id,String outcome,boolean completeWidgetSetup)throws JSONException {
        long revision=prefs.getLong(REVISION,0)+1;
        JSONObject operations=operations(prefs);
        if(id!=null){if(operations.length()>=32)operations.remove(operations.keys().next());operations.put(id,outcome);}
        String[] keys={"layout",AIR,"undo_history",OPERATIONS,REVISION,"widget-instance-generations","android-widget-pending"};
        java.util.Map<String,?> before=prefs.getAll();
        SharedPreferences.Editor transaction=trackInstances(prefs,prefs.edit(),layout);
        if(completeWidgetSetup)transaction.remove("android-widget-pending");
        if(!transaction.putString("layout",layout.encode()).putString(AIR,air.toString()).putString("undo_history",undo.toString())
                .putString(OPERATIONS,operations.toString()).putLong(REVISION,revision).commit()){
            // SharedPreferences updates memory even when its disk commit fails. Restore the
            // complete transaction so a retry cannot mistake a failed write for a receipt.
            SharedPreferences.Editor rollback=prefs.edit();
            for(String key:keys){Object value=before.get(key);if(value==null)rollback.remove(key);else if(value instanceof Long)rollback.putLong(key,(Long)value);else rollback.putString(key,(String)value);}
            rollback.commit();throw new IllegalStateException("Configuration could not be saved");
        }
        return revision;
    }
}
