package com.gathin.tvhome;

import org.json.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

/** Source-specific extractors. Unknown/empty formats fail; they never erase a calendar. */
final class HolidayParsers {
    static final class Update {
        final int year;final Set<String> scopes;final JSONArray events;
        Update(int year,Set<String> scopes,JSONArray events){this.year=year;this.scopes=scopes;this.events=events;}
    }
    static JSONObject event(String id,LocalDate date,String title,String original,String scope,String kind,String url,LocalDate anchor)throws JSONException{
        JSONObject e=new JSONObject().put("id",id+":"+date+":"+kind+":"+scope).put("date",date.toString()).put("title",title).put("original",original)
            .put("scope",scope).put("kind",kind).put("url",url);
        if(anchor!=null)e.put("anchor",anchor.toString());return e;
    }
    static String text(String html){return html.replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>"," ").replaceAll("<[^>]+>"," ")
        .replace("&nbsp;"," ").replace("&amp;","&").replace("&#160;"," ").replace('\u00a0',' ').replace('\u3000',' ').replaceAll("\\s+"," ").trim();}
    static List<Update> opm(String body,String url)throws Exception{
        if(!body.contains("BEGIN:VCALENDAR")||!body.contains("END:VCALENDAR"))throw new IOException("OPM calendar format changed");
        Map<Integer,JSONArray> years=new TreeMap<>();String unfolded=body.replaceAll("\\r?\\n[ \\t]","");
        Matcher events=Pattern.compile("(?s)BEGIN:VEVENT(.*?)END:VEVENT").matcher(unfolded);
        while(events.find()){
            String part=events.group(1),date=null,title=null;
            for(String line:part.split("\\r?\\n")){
                if(line.startsWith("DTSTART")){int colon=line.indexOf(':');if(colon>=0)date=line.substring(colon+1).trim();}
                if(line.startsWith("SUMMARY:"))title=line.substring(8).trim().replace("\\,",",").replace("\\n"," ").replace("\\;",";");
            }
            if(date==null||title==null||!date.matches("\\d{8}"))throw new IOException("Unsupported OPM event");
            if(title.toLowerCase(Locale.ROOT).contains("inauguration"))continue; // DC-area-only event is outside federal national scope.
            LocalDate observed=LocalDate.parse(date,DateTimeFormatter.BASIC_ISO_DATE),actual=observed;
            String lower=title.toLowerCase(Locale.ROOT);
            if(lower.contains("new year"))actual=LocalDate.of(observed.getMonthValue()==12?observed.getYear()+1:observed.getYear(),1,1);
            else if(lower.contains("juneteenth"))actual=LocalDate.of(observed.getYear(),6,19);
            else if(lower.contains("independence")&&!lower.contains("juneteenth"))actual=LocalDate.of(observed.getYear(),7,4);
            else if(lower.contains("veteran"))actual=LocalDate.of(observed.getYear(),11,11);
            else if(lower.contains("christmas"))actual=LocalDate.of(observed.getYear(),12,25);
            if(Math.abs(java.time.temporal.ChronoUnit.DAYS.between(actual,observed))>1)throw new IOException("Unrecognized OPM observed date");
            years.computeIfAbsent(actual.getYear(),x->new JSONArray()).put(event("usa",actual,title,"","national","HOLIDAY",url,null));
            if(!actual.equals(observed))years.computeIfAbsent(observed.getYear(),x->new JSONArray()).put(event("usa",observed,title+" (observed)","","national","OBSERVED",url,actual));
        }
        List<Update> result=new ArrayList<>();for(Map.Entry<Integer,JSONArray> year:years.entrySet())if(year.getValue().length()>=11)
            result.add(new Update(year.getKey(),Collections.singleton("national"),year.getValue()));
        if(result.isEmpty())throw new IOException("OPM publication has no complete year");return result;
    }
    static Update boe(String xml,int year,String url)throws Exception{
        // Android's DOM factory supports different feature flags than desktop
        // Xerces. Reject declarations before parsing and refuse every resolver call.
        if(Pattern.compile("(?i)<!\\s*(?:DOCTYPE|ENTITY)\\b").matcher(xml).find())throw new IOException("BOE XML declarations are unsupported");
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setValidating(false);
        javax.xml.parsers.DocumentBuilder builder=factory.newDocumentBuilder();
        builder.setEntityResolver((publicId,systemId)->{throw new org.xml.sax.SAXException("External XML entities are unsupported");});
        Document document=builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        if(!document.getDocumentElement().getTextContent().contains("fiestas laborales para el año "+year)
                &&!document.getDocumentElement().getTextContent().replace('\u00a0',' ').contains("fiestas laborales para el año "+year))throw new IOException("BOE year mismatch");
        JSONArray events=new JSONArray();int month=0,catalunya=-1;
        NodeList rows=document.getElementsByTagName("tr");
        List<String> months=Arrays.asList("Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre");
        for(int i=0;i<rows.getLength();i++){
            Element row=(Element)rows.item(i);NodeList headers=row.getElementsByTagName("th");
            for(int j=0;j<headers.getLength();j++)if(text(headers.item(j).getTextContent()).startsWith("Cataluña"))catalunya=j+1;
            NodeList cells=row.getElementsByTagName("td");if(cells.getLength()==0)continue;
            String label=text(cells.item(0).getTextContent());
            if(months.contains(label)){month=months.indexOf(label)+1;continue;}
            if(month==0||catalunya<1||cells.getLength()<=catalunya)continue;
            String mark=text(cells.item(catalunya).getTextContent());if(mark.isEmpty())continue;
            if(!mark.matches("\\*{1,3}"))throw new IOException("BOE holiday marker changed");
            Matcher date=Pattern.compile("^(\\d{1,2})\\s+(.+)").matcher(label);if(!date.find())throw new IOException("BOE row changed");
            LocalDate day=LocalDate.of(year,month,Integer.parseInt(date.group(1)));
            String scope=mark.length()==3?"catalunya":"national";
            events.put(event("spain",day,date.group(2),date.group(2),scope,"HOLIDAY",url,null));
        }
        if(events.length()<10||events.length()>16)throw new IOException("BOE annual calendar incomplete");
        return new Update(year,new LinkedHashSet<>(Arrays.asList("national","catalunya")),events);
    }
    static List<Update> opmHtml(String html,String url)throws Exception{
        StringBuilder calendar=new StringBuilder("BEGIN:VCALENDAR\n");
        Matcher tables=Pattern.compile("(?is)<table\\b[^>]*>(.*?)</table>").matcher(html);int annual=0;
        while(tables.find()){
            String table=tables.group(1);Matcher caption=Pattern.compile("(20\\d{2})\\s+Holiday Schedule").matcher(text(table));
            if(!caption.find())continue;int year=Integer.parseInt(caption.group(1)),count=0;
            Matcher rows=Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(table);
            while(rows.find()){
                Matcher cells=Pattern.compile("(?is)<td\\b[^>]*>(.*?)</td>").matcher(rows.group(1));List<String> values=new ArrayList<>();while(cells.find())values.add(text(cells.group(1)));
                if(values.size()!=2)continue;
                Matcher date=Pattern.compile("(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),?\\s+([A-Za-z]+)\\s+(\\d{1,2})").matcher(values.get(0));
                if(!date.find())throw new IOException("OPM table date format changed");
                int month=java.time.Month.from(DateTimeFormatter.ofPattern("MMMM",Locale.US).parse(date.group(1))).getValue();
                int observedYear=month==12&&values.get(1).toLowerCase(Locale.ROOT).contains("new year")?year-1:year;
                calendar.append("BEGIN:VEVENT\nDTSTART;VALUE=DATE:").append(LocalDate.of(observedYear,month,Integer.parseInt(date.group(2))).format(DateTimeFormatter.BASIC_ISO_DATE))
                    .append("\nSUMMARY:").append(values.get(1).replaceAll("\\*+$","").trim()).append("\nEND:VEVENT\n");count++;
            }
            if(count<11)throw new IOException("OPM annual table is incomplete");annual++;
        }
        if(annual==0)throw new IOException("OPM table structure changed");return opm(calendar.append("END:VCALENDAR\n").toString(),url);
    }
    static Update tivissa(String json,int year,String url)throws Exception{
        JSONArray rows=new JSONArray(json),events=new JSONArray();Set<String> seen=new HashSet<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);
            if(!row.getString("any_calendari").equals(String.valueOf(year)))continue;
            if(!row.getString("codi_municipi_ine").equals("43150")||!row.optString("pedania","000").equals("000")
                    ||!row.getString("ajuntament_o_nucli_municipal").equals("Tivissa"))continue;
            LocalDate date=LocalDate.parse(row.getString("data").substring(0,10));
            if(date.getYear()!=year)throw new IOException("Local calendar year mismatch");
            if(seen.add(date.toString()))events.put(event("spain",date,"Tivissa local holiday",row.optString("festiu","Festa local"),"tivissa","HOLIDAY",url,null));
        }
        if(events.length()!=2)throw new IOException("Tivissa annual publication missing or incomplete");
        return new Update(year,Collections.singleton("tivissa"),events);
    }
    static Update china(String html,int year,String url)throws Exception{
        String plain=text(html);
        if(!plain.contains(year+"年")||!plain.contains("国务院")||!plain.contains("放假"))throw new IOException("China publication identity mismatch");
        JSONArray events=new JSONArray();Matcher sections=Pattern.compile("(?is)<p\\b[^>]*>(.*?)</p>").matcher(html);int blocks=0;
        while(sections.find()){
            String p=text(sections.group(1));int colon=Math.max(p.indexOf('：'),p.indexOf(':'));if(colon<0||!p.contains("放假"))continue;
            String label=p.substring(0,colon).replaceFirst("^[一二三四五六七八九十]+、","").trim();
            if(!Arrays.asList("元旦","春节","清明节","劳动节","端午节","中秋节","国庆节","中秋节、国庆节","国庆节、中秋节").contains(label))continue;
            String clean=p.substring(colon+1).replaceAll("（[^）]*）|\\([^)]*\\)","");
            Matcher range=Pattern.compile("(?:(\\d{4})年)?(\\d{1,2})月(\\d{1,2})日(?:至(?:(\\d{1,2})月)?(\\d{1,2})日)?").matcher(clean);
            if(!range.find())throw new IOException("China holiday date missing");
            int firstYear=range.group(1)==null?year:Integer.parseInt(range.group(1));
            LocalDate start=LocalDate.of(firstYear,Integer.parseInt(range.group(2)),Integer.parseInt(range.group(3)));
            LocalDate end=range.group(5)==null?start:LocalDate.of(year,range.group(4)==null?start.getMonthValue():Integer.parseInt(range.group(4)),Integer.parseInt(range.group(5)));
            if(end.isBefore(start)||java.time.temporal.ChronoUnit.DAYS.between(start,end)>14)throw new IOException("China holiday period invalid");
            for(LocalDate d=start;!d.isAfter(end);d=d.plusDays(1))if(d.getYear()==year)events.put(event("china",d,label,label,"national","HOLIDAY",url,null));
            int finish=clean.indexOf("上班");if(finish>=0){
                int sentence=clean.lastIndexOf('。',finish);String working=clean.substring(sentence<0?range.end():sentence+1,finish);
                Matcher dates=Pattern.compile("(\\d{1,2})月(\\d{1,2})日").matcher(working);int count=0;
                while(dates.find()){events.put(event("china",LocalDate.of(year,Integer.parseInt(dates.group(1)),Integer.parseInt(dates.group(2))),"Make-up working day","调休上班","national","WORKING",url,null));count++;}
                if(count==0)throw new IOException("China working-day format changed");
            }
            blocks++;
        }
        if(blocks<6||events.length()<25)throw new IOException("China annual calendar incomplete");
        return new Update(year,Collections.singleton("national"),events);
    }
    private static final String[] THAI_MONTHS={"มกราคม","กุมภาพันธ์","มีนาคม","เมษายน","พฤษภาคม","มิถุนายน","กรกฎาคม","สิงหาคม","กันยายน","ตุลาคม","พฤศจิกายน","ธันวาคม"};
    static Update thai(String html,int year,String url)throws Exception{
        String plain=text(html);if(!plain.contains("วันหยุดราชการ")||!plain.contains(String.valueOf(year+543)))throw new IOException("Thai government calendar identity/year mismatch");
        JSONArray events=new JSONArray();Set<String> seen=new HashSet<>();
        Matcher rows=Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(html);
        while(rows.find()){
            String row=text(rows.group(1));Matcher date=Pattern.compile("(?:วันที่\\s*)?(\\d{1,2})\\s*("+String.join("|",THAI_MONTHS)+")").matcher(row);
            if(!date.find())continue;
            if(row.contains("ธนาคาร")||row.contains("แรงงาน")||row.contains("โมซัมบิก"))continue;
            int month=Arrays.asList(THAI_MONTHS).indexOf(date.group(2))+1;
            LocalDate day=LocalDate.of(year,month,Integer.parseInt(date.group(1)));
            String title=row.substring(date.end()).replaceFirst("^\\s*(?:พ\\.ศ\\.?\\s*)?"+(year+543),"").trim();
            if(title.length()<4)throw new IOException("Thai holiday title missing");
            if(seen.add(day.toString()))events.put(event("thai",day,title,title,"national",row.contains("ชดเชย")?"OBSERVED":"HOLIDAY",url,null));
        }
        if(events.length()<16||events.length()>35)throw new IOException("Thai annual table missing or incomplete");
        return new Update(year,Collections.singleton("national"),events);
    }
    private HolidayParsers(){}
}
