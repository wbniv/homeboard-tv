package com.gathin.tvhome;

import android.content.SharedPreferences;
import org.json.*;

/** Pending ownership is durable before a platform call and survives failed writes. */
final class AndroidWidgetPending {
    static final String KEY="android-widget-pending";
    static void write(SharedPreferences prefs,JSONObject value){
        String previous=prefs.getString(KEY,"");SharedPreferences.Editor edit=prefs.edit();
        if(value==null)edit.remove(KEY);else edit.putString(KEY,value.toString());
        if(edit.commit())return;
        SharedPreferences.Editor rollback=prefs.edit();if(previous.isEmpty())rollback.remove(KEY);else rollback.putString(KEY,previous);rollback.commit();
        throw new IllegalStateException("Widget setup could not be saved");
    }
    static boolean matchingResult(JSONObject record,int request,int returnedId){
        String phase=record.optString("phase");
        return (returnedId==-1||returnedId==record.optInt("id"))&&
            (request==7411&&"binding".equals(phase)||request==7412&&("configuration".equals(phase)||"reconfigure".equals(phase)));
    }
}
