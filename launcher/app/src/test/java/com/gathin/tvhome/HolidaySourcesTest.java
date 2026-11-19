package com.gathin.tvhome;

import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class HolidaySourcesTest {
    private String source(String file)throws Exception{return new String(Files.readAllBytes(Path.of("src/test/resources/calendar/sources/"+file)),StandardCharsets.UTF_8);}
    @Test public void boe2027ContainsOnlyCatalunyaApplicableNationalAndRegionalDays()throws Exception{
        HolidayParsers.Update result=HolidayParsers.boe(source("boe-2027.xml"),2027,"https://www.boe.es/test");
        assertEquals(12,result.events.length());Set<String> dates=new HashSet<>();
        for(int i=0;i<result.events.length();i++)dates.add(result.events.getJSONObject(i).getString("date"));
        assertTrue(dates.contains("2027-03-29"));assertTrue(dates.contains("2027-09-11"));assertFalse(dates.contains("2027-08-16"));
    }
    @Test public void chineseAnnualNoticesKeepExplicitWorkingDays()throws Exception{
        for(int year:new int[]{2026,2027}){
            HolidayParsers.Update result=HolidayParsers.china(source("china-"+year+".html"),year,"https://www.gov.cn/test");
            assertTrue(result.events.length()>30);Set<String> working=new LinkedHashSet<>();
            for(int i=0;i<result.events.length();i++){JSONObject e=result.events.getJSONObject(i);if(e.getString("kind").equals("WORKING"))working.add(e.getString("date"));}
            assertTrue(working.contains(year==2026?"2026-02-14":"2027-01-31"));
            assertEquals(year==2026?6:5,working.size());
        }
    }
    @Test public void localCalendarFiltersMunicipalityAndPedania()throws Exception{
        HolidayParsers.Update result=HolidayParsers.tivissa(source("tivissa.json"),2026,"https://analisi.transparenciacatalunya.cat/test");
        assertEquals(2,result.events.length());
        for(int i=0;i<2;i++)assertEquals("tivissa",result.events.getJSONObject(i).getString("scope"));
    }
    @Test public void unknownAndIncompleteFormatsFailInsteadOfProducingEmptyCalendars()throws Exception{
        for(String kind:new String[]{"boe","china","local","opm","thai"})try{
            switch(kind){case "boe":HolidayParsers.boe("<html>No calendar</html>",2027,"https://www.boe.es/test");break;
                case "china":HolidayParsers.china("2027年 国务院 放假",2027,"https://www.gov.cn/test");break;
                case "local":HolidayParsers.tivissa("[]",2027,"https://analisi.transparenciacatalunya.cat/test");break;
                case "opm":HolidayParsers.opm("BEGIN:VCALENDAR\nEND:VCALENDAR","https://www.opm.gov/test");break;
                default:HolidayParsers.thai("วันหยุดราชการ 2570",2027,"https://www.soc.go.th/test");}
            fail(kind+" accepted an empty publication");
        }catch(java.io.IOException expected){}
    }
    @Test public void yearSelectionAndClosureCheckWindowsRespectSourceTimeZones(){
        Instant yearEnd=Instant.parse("2026-12-31T18:00:00Z");
        assertArrayEquals(new int[]{2027,2028},HolidayUpdater.years(yearEnd,CalendarSnapshot.BANGKOK));
        assertArrayEquals(new int[]{2026,2027,2028},HolidayUpdater.years(yearEnd,ZoneId.of("America/New_York")));
        assertEquals(3_600_000L,HolidayUpdater.closureInterval(Instant.parse("2026-11-19T22:00:00Z")));
    }
    @Test public void opmObservedNewYearIsKeptInThePreviousCivilYear()throws Exception{
        String[] dates={"20270101","20270118","20270215","20270531","20270618","20270705","20270906","20271011","20271111","20271125","20271224","20271231"};
        String[] titles={"New Year's Day","Birthday of Martin Luther King, Jr.","Washington's Birthday","Memorial Day","Juneteenth National Independence Day","Independence Day","Labor Day","Columbus Day","Veterans Day","Thanksgiving Day","Christmas Day","New Year's Day"};
        StringBuilder ics=new StringBuilder("BEGIN:VCALENDAR\n");for(int i=0;i<dates.length;i++)ics.append("BEGIN:VEVENT\nDTSTART;VALUE=DATE:").append(dates[i]).append("\nSUMMARY:").append(titles[i]).append("\nEND:VEVENT\n");ics.append("END:VCALENDAR\n");
        HolidayParsers.Update result=HolidayParsers.opm(ics.toString(),HolidayUpdater.OPM).get(0);
        assertEquals(2027,result.year);JSONObject observed=null;
        for(int i=0;i<result.events.length();i++)if(result.events.getJSONObject(i).getString("date").equals("2027-12-31"))observed=result.events.getJSONObject(i);
        assertNotNull(observed);assertEquals("2028-01-01",observed.getString("anchor"));assertEquals("OBSERVED",observed.getString("kind"));
    }
    @Test public void xmlDeclarationsAreRejectedBeforeParsing()throws Exception{
        try{HolidayParsers.boe("<!DOCTYPE document [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><document>&x;</document>",2027,"https://www.boe.es/test");fail();}catch(java.io.IOException expected){}
    }
}
