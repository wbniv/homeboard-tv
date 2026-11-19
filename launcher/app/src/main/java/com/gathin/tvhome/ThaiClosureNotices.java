package com.gathin.tvhome;

import org.json.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Narrow official-announcement recognition. Ambiguous notices require parser review. */
final class ThaiClosureNotices {
    private static final String MONTHS="มกราคม|กุมภาพันธ์|มีนาคม|เมษายน|พฤษภาคม|มิถุนายน|กรกฎาคม|สิงหาคม|กันยายน|ตุลาคม|พฤศจิกายน|ธันวาคม";
    static void apply(String json,String feed,HolidayStore store,Instant now)throws Exception{
        Object root=new JSONTokener(json).nextValue();List<JSONObject> articles=new ArrayList<>();collect(root,articles,0);
        if(articles.isEmpty())throw new IOException("Thai announcement feed format changed");
        List<String> unresolved=new ArrayList<>();
        for(JSONObject article:articles){
            String title=value(article,"title","topic","name"),content=value(article,"content","description","body","detail"),url=value(article,"link","url");
            String body=HolidayParsers.text(title+" "+content);
            if(!body.contains("วันหยุดราชการ"))continue;
            if(url.isEmpty()||!HolidayHttp.official(url)){unresolved.add("Unverified notice link");continue;}
            if(body.contains("ยกเลิก")||body.contains("แก้ไข")||body.contains("เปลี่ยนแปลง")){
                unresolved.add("Cancellation/amendment needs an exact referenced decision");continue;
            }
            // A weather report, prediction, proposal or general calendar article is not a decision.
            if(!(body.contains("คณะรัฐมนตรี")||body.contains("ครม."))||!body.contains("เห็นชอบ")||body.contains("เสนอให้")||body.contains("คาดว่า"))continue;
            Pattern explicit=Pattern.compile("ให้(?:วัน[ก-๙]+)?(?:ที่|วันที่)\\s*(\\d{1,2})\\s*("+MONTHS+")\\s*(?:พ\\.ศ\\.?\\s*)?(\\d{4})\\s*เป็นวันหยุดราชการ");
            Matcher match=explicit.matcher(body);
            if(!match.find()){unresolved.add("Closure decision has an unrecognized effective-date clause");continue;}
            int year=Integer.parseInt(match.group(3))-543;
            LocalDate day=LocalDate.of(year,Arrays.asList(MONTHS.split("\\|")).indexOf(match.group(2))+1,Integer.parseInt(match.group(1)));
            if(year<now.atZone(CalendarSnapshot.BANGKOK).getYear()-1||year>now.atZone(CalendarSnapshot.BANGKOK).getYear()+2)continue;
            String scope="national";
            if(body.contains("เฉพาะกรุงเทพมหานคร"))scope="bangkok";
            else if(body.contains("เฉพาะ")||body.contains("บางพื้นที่")||body.contains("จังหวัด")){unresolved.add("Closure jurisdiction needs review");continue;}
            JSONArray events=new JSONArray().put(HolidayParsers.event("thai",day,title,title,scope,"HOLIDAY",url,null));
            store.accept("thai",year,Collections.singleton(scope),events,url,HolidayHttp.hash(body),false);
        }
        if(!unresolved.isEmpty())throw new IOException(unresolved.get(0)+"; cached dates retained");
    }
    private static String value(JSONObject value,String... keys){for(String key:keys){Object field=value.opt(key);if(field instanceof String)return (String)field;}return "";}
    private static void collect(Object value,List<JSONObject> out,int depth)throws JSONException{
        if(depth>5)return;
        if(value instanceof JSONArray){JSONArray array=(JSONArray)value;for(int i=0;i<array.length();i++)collect(array.get(i),out,depth+1);}
        else if(value instanceof JSONObject){JSONObject object=(JSONObject)value;
            if(!value(object,"title","topic").isEmpty()&&!value(object,"link","url").isEmpty()){out.add(object);return;}
            Iterator<String> keys=object.keys();while(keys.hasNext())collect(object.opt(keys.next()),out,depth+1);
        }
    }
    private ThaiClosureNotices(){}
}
