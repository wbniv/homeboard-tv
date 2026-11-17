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
    static SharedPreferences.Editor revision(SharedPreferences prefs,SharedPreferences.Editor edit){return edit.putLong(REVISION,prefs.getLong(REVISION,0)+1);}
    static JSONObject aggregate(SharedPreferences prefs)throws JSONException{return new JSONObject().put("layout",new JSONObject(prefs.getString("layout","{}"))).put("air",air(prefs));}
    static JSONObject operations(SharedPreferences prefs)throws JSONException{return new JSONObject(prefs.getString(OPERATIONS,"{}"));}
    static String result(SharedPreferences prefs,String id)throws JSONException{return operations(prefs).optString(id,"");}
    static long commit(SharedPreferences prefs,HomeLayout layout,JSONObject air,JSONArray undo,String id,String outcome)throws JSONException {
        long revision=prefs.getLong(REVISION,0)+1;
        JSONObject operations=operations(prefs);
        if(id!=null){if(operations.length()>=32)operations.remove(operations.keys().next());operations.put(id,outcome);}
        if(!prefs.edit().putString("layout",layout.encode()).putString(AIR,air.toString()).putString("undo_history",undo.toString())
                .putString(OPERATIONS,operations.toString()).putLong(REVISION,revision).commit())throw new IllegalStateException("Configuration could not be saved");
        return revision;
    }
}
