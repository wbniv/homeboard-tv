package com.gathin.tvhome;

import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class ThaiCivilCalendarTest {
    @Test public void everyDateMatchesPinnedReferenceAcrossTwoCenturies() throws Exception {
        for(String line:Files.readAllLines(Path.of("src/test/resources/calendar/reference-years.csv"))){
            String[] row=line.split(",");int year=Integer.parseInt(row[0]);MessageDigest hash=MessageDigest.getInstance("SHA-256");
            for(LocalDate d=LocalDate.of(year,1,1);d.getYear()==year;d=d.plusDays(1)){
                ThaiCivilCalendar.Date v=ThaiCivilCalendar.convert(d);
                assertTrue(d+" invalid day "+v.day,v.day>=1&&v.day<=30);
                hash.update((d+","+v.year+","+v.month+","+v.day+"\n").getBytes(StandardCharsets.UTF_8));
            }
            StringBuilder actual=new StringBuilder();for(byte b:hash.digest())actual.append(String.format("%02x",b&255));
            assertEquals("Reference year "+year,row[1],actual.toString());
        }
    }
    @Test public void published2026AnchorsMatchExactly() throws Exception {
        JSONObject data=new JSONObject(new String(Files.readAllBytes(Path.of("../../dev/calendar/reviewed/thai.json")),StandardCharsets.UTF_8));
        Set<String> expected=new LinkedHashSet<>();JSONArray list=data.getJSONArray("wanPhra");for(int i=0;i<list.length();i++)expected.add(list.getString(i));
        Set<String> actual=new LinkedHashSet<>();for(LocalDate d=LocalDate.of(2026,1,1);d.getYear()==2026;d=d.plusDays(1))if(ThaiCivilCalendar.isWanPhra(d))actual.add(d.toString());
        assertEquals(expected,actual);
    }
    @Test public void monthEndAndRepeatedMonthAreCivilNotAstronomical(){
        assertTrue(ThaiCivilCalendar.isWanPhra(LocalDate.of(2026,11,9)));
        assertFalse(ThaiCivilCalendar.isWanPhra(LocalDate.of(2026,11,10)));
        assertEquals(88,ThaiCivilCalendar.convert(LocalDate.of(2026,7,29)).month);
        assertEquals(15,ThaiCivilCalendar.convert(LocalDate.of(2026,7,29)).day);
        assertTrue(ThaiCivilCalendar.isWanPhra(LocalDate.of(2027,1,1)));
    }
    @Test public void independentlyPublishedYearsAgree() throws Exception {
        for(int year:new int[]{2024,2025,2027}){
            JSONObject source=new JSONObject(new String(Files.readAllBytes(Path.of("src/test/resources/calendar/published-"+year+".json")),StandardCharsets.UTF_8));
            Set<String> expected=new LinkedHashSet<>();JSONArray values=source.getJSONArray("dates");
            for(int i=0;i<values.length();i++)expected.add(values.getString(i));
            assertTrue("Published fixture incomplete "+year,expected.size()>=48);
            Set<String> actual=new LinkedHashSet<>();for(LocalDate d=LocalDate.of(year,1,1);d.getYear()==year;d=d.plusDays(1))if(ThaiCivilCalendar.isWanPhra(d))actual.add(d.toString());
            assertEquals("Published calendar "+year,expected,actual);
        }
    }
}
