package com.gathin.tvhome;

import org.json.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

/** Runs one bounded, independently reconciled update per source/year. No Android UI dependency. */
final class HolidayUpdater {
    static final String OPM="https://www.opm.gov/policy-data-oversight/pay-leave/federal-holidays/holidays.ics";
    static final String LOCAL="https://analisi.transparenciacatalunya.cat/resource/b4eh-r8up.json?codi_municipi_ine=43150&%24limit=100";
    final HolidayStore store;final HolidayHttp.Transport http;
    HolidayUpdater(HolidayStore store,HolidayHttp.Transport http){this.store=store;this.http=http;}
    interface Operation {void run()throws Exception;}
    private void attempt(String key,Instant now,long interval,boolean manual,Consumer<String> progress,Operation operation)throws Exception{
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
        if(!manual&&!store.due(key,now.toEpochMilli(),interval))return;
        progress.accept(key);
        try{operation.run();store.check(key,now.toEpochMilli(),true,"");}
        catch(InterruptedIOException stopped){throw stopped;}
        catch(Exception error){store.check(key,now.toEpochMilli(),false,error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());}
    }
    private void publish(String id,HolidayParsers.Update update,HolidayHttp.Capture capture)throws Exception{
        store.accept(id,update.year,update.scopes,update.events,capture.url,HolidayHttp.hash(capture.body),true);
    }
    static int[] years(Instant now,ZoneId zone){
        LocalDate date=now.atZone(zone).toLocalDate();int count=date.getMonthValue()>=10?3:2;
        int[] result=new int[count];for(int i=0;i<count;i++)result[i]=date.getYear()+i;return result;
    }
    void run(Instant now,Set<String> enabled,boolean manual,Consumer<String> progress)throws Exception{
        if(enabled.contains("usa"))attempt("usa:"+now.atZone(ZoneId.of("America/New_York")).getYear(),now,86_400_000L,manual,progress,()->{
            HolidayHttp.Capture capture;List<HolidayParsers.Update> updates;
            try{capture=http.get(OPM);updates=HolidayParsers.opm(capture.body,OPM);}
            catch(InterruptedIOException stopped){throw stopped;}
            catch(Exception calendarUnavailable){String html="https://www.opm.gov/policy-data-oversight/pay-leave/federal-holidays/";capture=http.get(html);updates=HolidayParsers.opmHtml(capture.body,html);}
            int[] wanted=years(now,ZoneId.of("America/New_York"));boolean current=false;
            for(HolidayParsers.Update u:updates)for(int year:wanted)if(u.year==year){publish("usa",u,capture);if(year==wanted[0])current=true;}
            if(!current)throw new IOException("No current OPM annual publication");http.accepted(capture);
        });
        if(enabled.contains("spain"))for(int year:years(now,ZoneId.of("Europe/Madrid"))){
            attempt("spain:"+year+":regional",now,86_400_000L,manual,progress,()->{
                String id=year==2026?"BOE-A-2025-21667":year==2027?"BOE-A-2026-21254":discoverBoe(year);
                String url="https://www.boe.es/diario_boe/xml.php?id="+id;HolidayHttp.Capture capture=http.get(url);
                publish("spain",HolidayParsers.boe(capture.body,year,url),capture);http.accepted(capture);
            });
            attempt("spain:"+year+":tivissa",now,86_400_000L,manual,progress,()->{
                String url=LOCAL+"&any_calendari="+year;HolidayHttp.Capture capture=http.get(url);
                publish("spain",HolidayParsers.tivissa(capture.body,year,url),capture);http.accepted(capture);
            });
        }
        if(enabled.contains("china"))for(int year:years(now,ZoneId.of("Asia/Shanghai")))attempt("china:"+year,now,86_400_000L,manual,progress,()->{
            String url=year==2026?"https://www.beijing.gov.cn/zhengce/zhengcefagui/202511/t20251104_4258873.html":
                year==2027?"https://chinajob.mohrss.gov.cn/h5/c/2026-11-09/635850.shtml":discoverChina(year);
            HolidayHttp.Capture capture=http.get(url);publish("china",HolidayParsers.china(capture.body,year,url),capture);http.accepted(capture);
        });
        if(enabled.contains("thai")){
            for(int year:years(now,CalendarSnapshot.BANGKOK))attempt("thai:"+year+":annual",now,86_400_000L,manual,progress,()->{
                String url=year==2026?"https://www.soc.go.th/?p=33672":discoverThai(year);
                HolidayHttp.Capture capture=http.get(url);publish("thai",HolidayParsers.thai(capture.body,year,url),capture);http.accepted(capture);
            });
            JSONObject prior=store.checks().optJSONObject("thai:closures");
            long interval=prior==null?1:closureInterval(Instant.ofEpochMilli(prior.optLong("attempt",0)));
            attempt("thai:closures",now,interval,manual,progress,()->{
                // An unrecognized announcement is never converted to a closure by guessing.
                String url="https://www.thailand.go.th/issue-rss-json?hl=th";
                HolidayHttp.Capture capture=http.get(url);
                ThaiClosureNotices.apply(capture.body,url,store,now);
                http.accepted(capture);
            });
        }
        store.prune(now,enabled);
    }
    static long closureInterval(Instant now){
        ZonedDateTime local=now.atZone(CalendarSnapshot.BANGKOK),next=local.withHour(local.getHour()<6?6:18).withMinute(0).withSecond(0).withNano(0);
        if(!next.isAfter(local))next=next.plusDays(1).withHour(6);
        return Math.min(43_200_000L,Math.max(1,Duration.between(local,next).toMillis()));
    }
    private String discoverBoe(int year)throws Exception{
        String url="https://www.boe.es/buscar/boe.php?campo%5B0%5D=TIT&dato%5B0%5D="+URLEncoder.encode("fiestas laborales para el año "+year,"UTF-8")+"&accion=Buscar";
        HolidayHttp.Capture index=http.get(url);Matcher m=Pattern.compile("BOE-A-\\d{4}-\\d+").matcher(index.body);Set<String> ids=new LinkedHashSet<>();while(m.find())ids.add(m.group());
        if(ids.size()!=1)throw new IOException("BOE annual publication not uniquely identified for "+year);
        http.accepted(index);return ids.iterator().next();
    }
    private String discoverChina(int year)throws Exception{
        String url="https://www.gov.cn/zhengce/zuixin/";HolidayHttp.Capture index=http.get(url);
        String result=findPublication(index.body,url,year+"年", "节假日");http.accepted(index);return result;
    }
    private String discoverThai(int year)throws Exception{
        String url="https://www.soc.go.th/wp-json/wp/v2/posts?per_page=20&search="+URLEncoder.encode("วันหยุดราชการ "+(year+543),"UTF-8");
        HolidayHttp.Capture index=http.get(url);JSONArray records=new JSONArray(index.body);List<String> matches=new ArrayList<>();
        for(int i=0;i<records.length();i++){JSONObject record=records.getJSONObject(i);String title=record.getJSONObject("title").getString("rendered");
            if(title.contains(String.valueOf(year+543))&&title.contains("วันหยุดราชการ"))matches.add(record.getString("link"));}
        if(matches.size()!=1)throw new IOException("Thai annual publication not uniquely identified for "+year);
        if(!HolidayHttp.official(matches.get(0)))throw new IOException("Unexpected Thai publisher link");http.accepted(index);return matches.get(0);
    }
    private static String findPublication(String html,String base,String year,String subject)throws Exception{
        Matcher links=Pattern.compile("(?is)<a\\b[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>").matcher(html);Set<String> matches=new LinkedHashSet<>();
        while(links.find()){String title=HolidayParsers.text(links.group(2));if(title.contains(year)&&title.contains(subject)){
            String url=URI.create(base).resolve(links.group(1)).toString();if(HolidayHttp.official(url))matches.add(url);}}
        if(matches.size()!=1)throw new IOException("Annual publication not uniquely identified");return matches.iterator().next();
    }
}
