package com.gathin.tvhome;
import org.json.*;
import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class KitchenTimersTest {
    JSONObject command(KitchenTimers m,String action)throws Exception{return new JSONObject().put("operation",UUID.randomUUID().toString()).put("revision",m.revision()).put("action",action);}
    void create(KitchenTimers m,String name,long ms)throws Exception{m.command(command(m,"create").put("kind","timer").put("name",name).put("duration",ms),1000,100000,1);}
    @Test public void cookingIgnoresWallTimeAndConcurrentDishesStayIndependent()throws Exception{
        KitchenTimers m=KitchenTimers.empty();create(m,"Rice",300000);create(m,"Eggs",60000);JSONObject rice=m.items().getJSONObject(0),eggs=m.items().getJSONObject(1);
        assertEquals(270000,KitchenTimers.remaining(rice,31000,Long.MAX_VALUE-1000));
        m.reconcile(61000,100001,1);assertEquals("done",eggs.getString("state"));assertEquals("running",rice.getString("state"));
        long revision=m.revision();assertFalse(m.reconcile(61001,100002,1));assertEquals(revision,m.revision());
    }
    @Test public void pauseRoundTripResumeAddAndIdempotentRetry()throws Exception{
        KitchenTimers m=KitchenTimers.empty();create(m,"Rice",300000);String id=m.items().getJSONObject(0).getString("id");
        m.command(command(m,"pause").put("id",id),31000,100000,1);assertEquals(270000,m.find(id).getLong("remaining"));
        m=new KitchenTimers(new JSONObject(m.data.toString()));
        JSONObject add=command(m,"add").put("id",id);m.command(add,999999,200000,1);assertFalse(m.command(new JSONObject(add.toString()),999999,200000,1));assertEquals(330000,m.find(id).getLong("remaining"));
        m.command(command(m,"resume").put("id",id),1000000,200000,1);assertEquals(1330000,m.find(id).getLong("deadline"));
    }
    @Test public void rebootInterruptsCookingAndDoesNotRingMissedEvents()throws Exception{
        KitchenTimers m=KitchenTimers.empty();create(m,"Rice",300000);long wall=Instant.now().toEpochMilli();String local=LocalDateTime.ofInstant(Instant.ofEpochMilli(wall+60000),ZoneId.systemDefault()).toString();
        m.command(command(m,"create").put("kind","event").put("name","Call Grab").put("local",local).put("zone",ZoneId.systemDefault().getId()),1000,wall,1);
        m.reconcile(1,wall+120000,2);assertEquals("interrupted",m.items().getJSONObject(0).getString("state"));assertEquals("missed",m.items().getJSONObject(1).getString("state"));assertEquals(0,m.items().getJSONObject(1).getLong("soundUntil"));
    }
    @Test public void eventAndTimerCanFinishTogetherFutureEventSurvivesReboot()throws Exception{
        KitchenTimers m=KitchenTimers.empty();long wall=Instant.now().toEpochMilli();
        m.command(command(m,"create").put("kind","event").put("name","Leave for school").put("local",LocalDateTime.ofInstant(Instant.ofEpochMilli(wall+60000),ZoneId.systemDefault()).toString()).put("zone",ZoneId.systemDefault().getId()),1000,wall,1);
        m.reconcile(100,wall+10000,2);assertEquals("running",m.items().getJSONObject(0).getString("state"));m.reconcile(50100,wall+60000,2);assertEquals("done",m.items().getJSONObject(0).getString("state"));
    }
    @Test public void staleActionsCannotRestartAndInvalidInputDoesNotCreate()throws Exception{
        KitchenTimers m=KitchenTimers.empty();JSONObject stale=command(m,"create").put("kind","timer").put("name","Rice").put("duration",3000);create(m,"Eggs",1000);
        assertThrows(IllegalArgumentException.class,()->m.command(stale,1,1,1));
        assertThrows(IllegalArgumentException.class,()->m.command(command(m,"create").put("kind","timer").put("name","").put("duration",0),1,1,1));
        assertEquals(1,m.items().length());
    }
    @Test public void rejectsPastEventsWrongZoneAndDstGapsAndOverlaps()throws Exception{
        TimeZone before=TimeZone.getDefault();try{TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));KitchenTimers m=KitchenTimers.empty();long now=Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
        for(String local:new String[]{"2025-12-01T12:00","2026-03-08T02:30","2026-11-01T01:30"})assertThrows(IllegalArgumentException.class,()->m.command(command(m,"create").put("kind","event").put("name","School").put("local",local).put("zone","America/New_York"),1,now,1));
        assertEquals(0,m.items().length());}finally{TimeZone.setDefault(before);}
    }
    @Test public void stopCancelsSoundAndRestartUsesOriginalDuration()throws Exception{
        KitchenTimers m=KitchenTimers.empty();create(m,"Rice",1000);String id=m.items().getJSONObject(0).getString("id");m.reconcile(2000,100000,1);assertEquals("done",m.find(id).getString("state"));
        m.command(command(m,"stop").put("id",id),3000,101000,1);assertEquals(0,m.find(id).getLong("soundUntil"));m.command(command(m,"restart").put("id",id),4000,102000,1);assertEquals(5000,m.find(id).getLong("deadline"));
        assertEquals("00:01",KitchenTimers.clock(1));assertEquals("1:00:00",KitchenTimers.clock(3600000));
    }
}
