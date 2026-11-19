package com.gathin.tvhome;

import org.json.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class HolidayStoreTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private String seed()throws Exception{return new String(Files.readAllBytes(Path.of("src/main/assets/calendar-snapshot.json")),StandardCharsets.UTF_8);}
    private HolidayStore store(File folder)throws Exception{return new HolidayStore(folder,seed());}
    private JSONArray event(int year)throws Exception{return new JSONArray().put(HolidayParsers.event("thai",LocalDate.of(year,1,1),"Fixture new year","","national","HOLIDAY","https://www.soc.go.th/test",null));}
    @Test public void newYearUpdatesPreserveExistingYearsAndOtherProvidersThroughRestart()throws Exception{
        File dir=temporary.newFolder();HolidayStore store=store(dir);
        store.accept("thai",2027,Collections.singleton("national"),event(2027),"https://www.soc.go.th/test","fixture1",true);
        CalendarSnapshot before=CalendarSnapshot.parse(store.snapshot()),after=CalendarSnapshot.parse(store(dir).snapshot());
        assertEquals(before.providers.get("thai").events.size(),after.providers.get("thai").events.size());
        assertTrue(after.providers.get("thai").events.stream().anyMatch(e->e.date.equals(LocalDate.of(2026,1,1))));
        assertTrue(after.providers.get("thai").events.stream().anyMatch(e->e.date.equals(LocalDate.of(2027,1,1))));
        assertEquals(27,after.providers.get("usa").events.size());
    }
    @Test public void malformedEmptyOrWrongScopeReplacementCannotEraseOfflineData()throws Exception{
        HolidayStore s=store(temporary.newFolder());String before=s.snapshot();
        try{s.accept("thai",2026,Collections.singleton("national"),new JSONArray(),"https://www.soc.go.th/test","bad",true);fail();}catch(IOException expected){}
        try{s.accept("thai",2027,Collections.singleton("bangkok"),event(2027),"https://www.soc.go.th/test","bad",true);fail();}catch(IOException expected){}
        assertEquals(before,s.snapshot());
    }
    @Test public void failureDoesNotAdvanceSuccessfulCheckAndBackoffCapsAtOneDay()throws Exception{
        HolidayStore s=store(temporary.newFolder());long start=1_000_000L;
        s.check("thai",start,true,"");s.check("thai",start+100,false,"offline");
        assertEquals(start,s.checks().getJSONObject("thai").getLong("success"));
        assertFalse(s.due("thai",start+200,1));
        for(int i=0;i<12;i++)s.check("thai",start+100,false,"offline");
        assertEquals(86_400_000L,s.checks().getJSONObject("thai").getLong("retryAt")-start-100);
    }
    @Test public void failedAtomicPublicationLeavesMemoryAndRestartAtLastGoodRevision()throws Exception{
        File dir=temporary.newFolder();
        class FaultStore extends HolidayStore {boolean fail;FaultStore()throws Exception{super(dir,seed());}
            @Override protected void write(String name,String value)throws IOException{if(fail&&name.equals("active.json"))throw new IOException("disk full");super.write(name,value);}}
        FaultStore s=new FaultStore();String before=s.snapshot();s.fail=true;
        try{s.accept("thai",2027,Collections.singleton("national"),event(2027),"https://www.soc.go.th/test","fixture1",true);fail();}catch(IOException expected){}
        assertEquals(before,s.snapshot());assertEquals(before,store(dir).snapshot());assertTrue(s.storageFailed);
    }
    @Test public void corruptActiveRecordUsesPriorProviderRevision()throws Exception{
        File dir=temporary.newFolder();HolidayStore s=store(dir);String before=s.snapshot();
        s.accept("thai",2027,Collections.singleton("national"),event(2027),"https://www.soc.go.th/test","fixture1",true);
        s.check("thai",1000,true,"");
        Files.write(new File(dir,"active.json").toPath(),"corrupt".getBytes(StandardCharsets.UTF_8));
        assertEquals(before,store(dir).snapshot());
    }
    @Test public void incrementalNoticeDoesNotDowngradeCompleteAnnualCoverage()throws Exception{
        HolidayStore s=store(temporary.newFolder());
        JSONArray notice=new JSONArray().put(HolidayParsers.event("thai",LocalDate.of(2026,11,20),"Fixture closure","","national","HOLIDAY","https://www.soc.go.th/test",null));
        s.accept("thai",2026,Collections.singleton("national"),notice,"https://www.soc.go.th/test","notice",false);
        assertEquals("complete",CalendarSnapshot.parse(s.snapshot()).providers.get("thai").coverage.getJSONObject("2026").getString("national"));
    }
    @Test public void newCalendarYearRequiresNewSourceCheck()throws Exception{
        HolidayStore s=store(temporary.newFolder());s.check("usa:2026",1000,true,"");
        assertFalse(s.due("usa:2026",1001,86_400_000L));assertTrue(s.due("usa:2027",1001,86_400_000L));
    }
    @Test public void rolloverPrunesOnlyAfterBothRequiredYearsHaveValidatedCoverage()throws Exception{
        HolidayStore s=store(temporary.newFolder());Set<String> enabled=Collections.singleton("usa");Instant now=Instant.parse("2027-01-01T18:00:00Z");
        s.prune(now,enabled);assertTrue(CalendarSnapshot.parse(s.snapshot()).providers.get("usa").events.stream().anyMatch(e->e.date.getYear()==2026));
        JSONArray future=new JSONArray().put(HolidayParsers.event("usa",LocalDate.of(2028,1,1),"New Year","","national","HOLIDAY","https://www.opm.gov/test",null));
        s.accept("usa",2028,Collections.singleton("national"),future,"https://www.opm.gov/test","fixture-next-year",true);s.prune(now,enabled);
        assertFalse(CalendarSnapshot.parse(s.snapshot()).providers.get("usa").events.stream().anyMatch(e->e.date.getYear()==2026));
        assertTrue(CalendarSnapshot.parse(s.snapshot()).providers.get("usa").events.stream().anyMatch(e->e.date.getYear()==2027));
    }
}
