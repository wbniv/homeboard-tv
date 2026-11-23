package com.gathin.tvhome;

import org.junit.Test;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class AirQualityStartupTest {
    @Test public void blockedPreferencesDoNotBlockUiAndPublishOnlyAfterUiDispatch() throws Exception {
        ExecutorService worker=Executors.newSingleThreadExecutor();
        BlockingQueue<Runnable> ui=new LinkedBlockingQueue<>();
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        AtomicReference<AirQualitySavedData> published=new AtomicReference<>();
        AirQualityStartup<AirQualitySavedData> startup=new AirQualityStartup<>(worker,ui::add);
        try {
            startup.start(()->{
                entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));
                return AirQualitySavedData.read(new JSONObject().put("source","airbkk").put("station","119"),
                    (key,fallback)->fallback,1234);
            },published::set,()->fail("Unexpected failure"));
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            assertFalse(startup.ready());assertNull(published.get());
            AtomicBoolean inputHandled=new AtomicBoolean();ui.add(()->inputHandled.set(true));
            ui.take().run();assertTrue(inputHandled.get()); // The UI still handles input during the blocked read.
            release.countDown();Runnable complete=ui.poll(5,TimeUnit.SECONDS);assertNotNull(complete);
            assertFalse(startup.ready());assertNull(published.get());complete.run();
            assertTrue(startup.ready());assertEquals("119",published.get().configuration.getString("station"));
        } finally { release.countDown();startup.close();worker.shutdownNow(); }
    }
    @Test public void closeDiscardsResultEvenWhenIoHasAlreadyFinished() {
        List<Runnable> work=new ArrayList<>(),ui=new ArrayList<>();
        AirQualityStartup<String> startup=new AirQualityStartup<>(work::add,ui::add);
        startup.start(()->"saved",v->fail("Closed widget received a result"),()->fail("Closed widget failed"));
        work.remove(0).run();startup.close();ui.remove(0).run();assertFalse(startup.ready());
    }
    @Test public void failureRemainsUnavailableAndRetryPublishesOnlyTheSavedValue() {
        List<Runnable> work=new ArrayList<>(),ui=new ArrayList<>();AtomicReference<String> value=new AtomicReference<>();
        AirQualityStartup<String> startup=new AirQualityStartup<>(work::add,ui::add);AtomicInteger failures=new AtomicInteger();
        startup.start(()->{throw new IllegalStateException("unreadable preferences");},value::set,failures::incrementAndGet);
        work.remove(0).run();ui.remove(0).run();assertFalse(startup.ready());assertTrue(startup.failed());assertNull(value.get());
        startup.start(()->"saved selection",value::set,failures::incrementAndGet);work.remove(0).run();ui.remove(0).run();
        assertEquals("saved selection",value.get());assertEquals(1,failures.get());assertTrue(startup.ready());
    }
    @Test public void stationHistoryAndConfigurationAreReadWithoutChangingStoredValues() throws Exception {
        Map<String,String> values=new HashMap<>();
        AirQualityHistory history=new AirQualityHistory();history.add(new AirQualityReading(27,1000));
        values.put("history-119",history.encode());values.put("feed","bad cache");Map<String,String> before=new HashMap<>(values);
        JSONObject config=new JSONObject().put("source","airbkk").put("station","119").put("location-mode","manual")
            .put("location-name","My saved place").put("latitude",13.7).put("longitude",100.5);
        AirQualitySavedData state=AirQualitySavedData.read(config,values::getOrDefault,2000);
        assertEquals(config.toString(),state.configuration.toString());assertEquals(1,state.selected);
        assertEquals(27,state.history.readings().get(0).pm25,0.001);assertNull(state.reading);assertEquals(before,values);
    }
    @Test public void modelCoordinatesCachedReadingHistoryAndZoneSurviveLoading() throws Exception {
        JSONObject config=new JSONObject().put("source","model").put("location-mode","manual")
            .put("location-name","Saved coordinates").put("latitude",13.7).put("longitude",100.5);
        AirQualityHistory history=new AirQualityHistory();history.add(AirQualityReading.modeled(22,1000));
        Map<String,String> values=new HashMap<>();values.put("history-model-13.70_100.50",history.encode());
        values.put("feed-model-13.70_100.50","{\"timezone\":\"Asia/Bangkok\",\"latitude\":13.7,\"longitude\":100.5,\"current\":{\"time\":\"2026-11-23T19:00\",\"pm2_5\":31},\"hourly\":{\"time\":[\"2026-11-23T18:00\"],\"pm2_5\":[29]}}");
        Map<String,String> before=new HashMap<>(values);
        AirQualitySavedData state=AirQualitySavedData.read(config,values::getOrDefault,Long.MAX_VALUE/2);
        assertEquals(config.toString(),state.configuration.toString());assertEquals(31,state.reading.pm25,0.001);
        assertEquals("Asia/Bangkok",state.zone.getId());assertEquals(2,state.history.readings().size());
        assertEquals(22,state.history.readings().get(0).pm25,0.001);assertEquals(before,values);
    }
    @Test public void validStationCacheMergesWithSavedHistoryWithoutWritingIt() throws Exception {
        long now=java.time.Instant.parse("2026-11-23T12:00:00Z").toEpochMilli();
        AirQualityHistory history=new AirQualityHistory();history.add(new AirQualityReading(18,now-7200000));
        String feed="{\"status\":\"Success\",\"message\":[{\"MeasIndex\":\"119\",\"PM2.5\":27,\"DateTime\":\"2026-11-23 18:00:00\"}]}";
        Map<String,String> values=new HashMap<>();values.put("history-119",history.encode());values.put("feed",feed);
        Map<String,String> before=new HashMap<>(values);
        AirQualitySavedData state=AirQualitySavedData.read(new JSONObject().put("source","airbkk").put("station","119"),values::getOrDefault,now);
        assertEquals(27,state.reading.pm25,0.001);assertEquals(2,state.history.readings().size());
        assertEquals(feed,state.stationFeed);assertEquals(before,values);
    }
    @Test public void duplicateStartDoesNotQueueCompetingLoadsAndCloseDiscardsFailure() {
        List<Runnable> work=new ArrayList<>(),ui=new ArrayList<>();
        AirQualityStartup<String> startup=new AirQualityStartup<>(work::add,ui::add);
        startup.start(()->{throw new IllegalStateException();},v->fail(),()->fail());
        startup.start(()->"default",v->fail(),()->fail());assertEquals(1,work.size());
        startup.close();work.remove(0).run();ui.remove(0).run();assertFalse(startup.ready());assertFalse(startup.failed());
    }

}
