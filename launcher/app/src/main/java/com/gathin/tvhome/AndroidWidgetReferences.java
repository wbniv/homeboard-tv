package com.gathin.tvhome;

import org.json.*;
import java.util.*;

/** A malformed retained snapshot aborts GC; it must never be read as no references. */
final class AndroidWidgetReferences {
    static Set<Integer> collect(String layout,String history,String pending)throws JSONException {
        Set<Integer> ids=new HashSet<>();readLayout(new JSONObject(layout),ids);
        JSONArray undo=new JSONArray(history);
        for(int i=0;i<undo.length();i++){
            JSONObject entry=undo.getJSONObject(i);String kind=entry.getString("kind");
            if("layout".equals(kind))readLayout(new JSONObject(entry.getString("state")),ids);
            else if("phone".equals(kind)||"home-app-size".equals(kind)){
                JSONObject state=new JSONObject(entry.getString("state"));Object nested=state.get("layout");
                readLayout(nested instanceof JSONObject?(JSONObject)nested:new JSONObject((String)nested),ids);
            }else if(!Set.of("air","language").contains(kind))throw new JSONException("Unknown retained Undo kind");
        }
        if(pending!=null&&!pending.isEmpty()){int id=new JSONObject(pending).getInt("id");if(id<=0)throw new JSONException("Invalid pending widget ID");ids.add(id);}
        return ids;
    }
    private static void readLayout(JSONObject layout,Set<Integer> ids)throws JSONException {
        if(layout.optInt("schemaVersion",1)>HomeLayout.SCHEMA_VERSION)throw new JSONException("Newer retained layout");
        JSONArray widgets=layout.getJSONArray("widgets");JSONObject records=layout.optJSONObject("androidWidgets");
        for(int i=0;i<widgets.length();i++){
            String key=widgets.getString(i);if(!AndroidWidgetSpec.key(key))continue;
            if(records==null||!records.has(key))throw new JSONException("Missing retained Android widget identity");
            ids.add(AndroidWidgetSpec.parse(records.getJSONObject(key)).id);
        }
    }
}
