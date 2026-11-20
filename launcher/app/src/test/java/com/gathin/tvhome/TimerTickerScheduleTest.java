package com.gathin.tvhome;

import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** The ticker's schedule with a fake clock, a fake handler and a counting store. */
public class TimerTickerScheduleTest {
    /** Fake clock and handler in one: postDelayed records a due time; advance runs what falls due. */
    static final class Fake implements TimerTickerSchedule.Clock, TimerTickerSchedule.Poster {
        long elapsed = 1_000_000, wall = 1_800_000_000_000L;
        final Map<Runnable, Long> posted = new LinkedHashMap<>();
        int maxPending, posts;
        public long elapsed() { return elapsed; }
        public long wall() { return wall; }
        public void postDelayed(Runnable r, long delay) { assertTrue(delay >= 0); posted.put(r, elapsed + delay); posts++; maxPending = Math.max(maxPending, posted.size()); }
        public void cancel(Runnable r) { posted.remove(r); }
        void advance(long ms) {
            long end = elapsed + ms;
            while (true) {
                Map.Entry<Runnable, Long> next = null;
                for (Map.Entry<Runnable, Long> e : posted.entrySet()) if (next == null || e.getValue() < next.getValue()) next = e;
                if (next == null || next.getValue() > end) break;
                long step = next.getValue() - elapsed; elapsed += step; wall += step;
                posted.remove(next.getKey());
                next.getKey().run();
            }
            wall += end - elapsed; elapsed = end;
        }
    }

    /** A store whose read reconciles and parses, as KitchenTimerStore.snapshot does, and counts reads. */
    static final class Store implements TimerTickerSchedule.Source {
        final Fake clock; KitchenTimers model; int reads;
        Store(Fake clock) throws JSONException { this.clock = clock; model = KitchenTimers.empty(); }
        public JSONObject read() {
            reads++;
            try { model.reconcile(clock.elapsed, clock.wall, 1); return new JSONObject(model.data.toString()); }
            catch (JSONException e) { throw new IllegalStateException(e); }
        }
        void create(String name, long ms) throws JSONException {
            model.command(new JSONObject().put("operation", UUID.randomUUID().toString()).put("revision", model.revision())
                    .put("action", "create").put("kind", "timer").put("name", name).put("duration", ms), clock.elapsed, clock.wall, 1);
        }
        void stopAll() throws JSONException {
            for (int i = 0; i < model.items().length(); i++)
                model.command(new JSONObject().put("operation", UUID.randomUUID().toString()).put("revision", model.revision())
                        .put("action", "stop").put("id", model.items().getJSONObject(i).getString("id")), clock.elapsed, clock.wall, 1);
        }
    }

    static final class Counter implements TimerTickerSchedule.Listener {
        int ticks; TimerTickerSchedule.Snapshot last;
        public void tick(TimerTickerSchedule.Snapshot s) { ticks++; last = s; }
    }

    @Test public void parsesOncePerRevisionNotOncePerTick() throws Exception {
        Fake fake = new Fake(); Store store = new Store(fake); store.create("Rice", 300_000);
        TimerTickerSchedule schedule = new TimerTickerSchedule(fake, fake, store);
        Counter widget = new Counter(), screen = new Counter();
        schedule.listen(widget); schedule.listen(screen);
        assertEquals(1, store.reads);
        fake.advance(60_000);
        assertTrue("about one tick a second", widget.ticks >= 60 && widget.ticks <= 62);
        assertEquals("60 s of ticks, still one parse", 1, store.reads);
        assertEquals("04:00", KitchenTimers.clock(KitchenTimers.remaining(widget.last.data.getJSONArray("items").getJSONObject(0), widget.last.elapsed, widget.last.wall)));

        schedule.changed(store.model.revision());
        fake.advance(5_000);
        assertEquals("a revision already held costs nothing", 1, store.reads);

        store.create("Eggs", 60_000); schedule.changed(store.model.revision());
        fake.advance(1);
        assertEquals("a new revision is parsed once", 2, store.reads);
        assertEquals(2, widget.last.entries.size());
        fake.advance(30_000);
        assertEquals(2, store.reads);
        assertEquals(1, fake.maxPending);
    }

    @Test public void aDeadlineReadsOnceSoTheStoreReconciles() throws Exception {
        Fake fake = new Fake(); Store store = new Store(fake); store.create("Tea", 5_000);
        TimerTickerSchedule schedule = new TimerTickerSchedule(fake, fake, store);
        Counter view = new Counter(); schedule.listen(view);
        fake.advance(4_999);
        assertEquals(1, store.reads);
        fake.advance(1);
        assertEquals("the deadline tick reconciles through one read", 2, store.reads);
        assertEquals("done", view.last.data.getJSONArray("items").getJSONObject(0).getString("state"));
        assertEquals(TimerIndicatorRules.State.RINGING, TimerIndicatorRules.chip(view.last.entries, view.last.elapsed, view.last.wall));
        schedule.changed(store.model.revision()); // the store's own notice of that save
        fake.advance(60_000);
        assertEquals("ringing then done: no further parse", 2, store.reads);
        assertEquals(TimerIndicatorRules.State.DONE, TimerIndicatorRules.chip(view.last.entries, view.last.elapsed, view.last.wall));
        assertFalse("done after the sound needs no tick", schedule.pending());
    }

    @Test public void oneCallbackAtATimeAndNoneWithoutListeners() throws Exception {
        Fake fake = new Fake(); Store store = new Store(fake); store.create("Rice", 300_000);
        TimerTickerSchedule schedule = new TimerTickerSchedule(fake, fake, store);
        schedule.changed(5);
        assertEquals("no listeners: nothing posted", 0, fake.posts);
        Counter a = new Counter(), b = new Counter();
        schedule.listen(a); schedule.listen(b); schedule.refresh(); schedule.changed(99);
        assertEquals(1, fake.posted.size());
        fake.advance(10_000);
        assertEquals(1, fake.maxPending);
        schedule.unlisten(a);
        assertTrue(schedule.pending());
        schedule.unlisten(b);
        assertFalse(schedule.pending()); assertTrue(fake.posted.isEmpty());
        int ticks = b.ticks, reads = store.reads;
        fake.advance(60_000);
        assertEquals(ticks, b.ticks); assertEquals(reads, store.reads);
    }

    @Test public void stopsTickingWhenNothingNeedsATick() throws Exception {
        Fake fake = new Fake(); Store store = new Store(fake); store.create("Rice", 300_000);
        TimerTickerSchedule schedule = new TimerTickerSchedule(fake, fake, store);
        Counter view = new Counter(); schedule.listen(view);
        fake.advance(3_000);
        store.stopAll(); schedule.changed(store.model.revision());
        fake.advance(1);
        assertFalse("stopped: no callback posted", schedule.pending());
        int ticks = view.ticks;
        fake.advance(600_000);
        assertEquals(ticks, view.ticks);
    }

    @Test public void entriesComeFromTheStoredJson() throws Exception {
        JSONArray items = new JSONArray()
                .put(new JSONObject().put("id", "a").put("name", "Rice").put("kind", "timer").put("state", "running").put("deadline", 9000).put("remaining", 5000).put("soundDeadline", 0))
                .put(new JSONObject().put("id", "b").put("name", "").put("kind", "event").put("state", "running").put("at", 123456789L).put("soundDeadline", 0));
        List<TimerIndicatorRules.Entry> list = TimerTickerSchedule.entries(items);
        assertEquals(TimerIndicatorRules.Kind.TIMER, list.get(0).kind); assertEquals(9000, list.get(0).deadline);
        assertEquals(TimerIndicatorRules.Kind.EVENT, list.get(1).kind); assertEquals(123456789L, list.get(1).at);
        assertEquals(KitchenTimers.remaining(items.getJSONObject(0), 4000, 0), list.get(0).remaining(4000, 0));
        assertEquals(KitchenTimers.remaining(items.getJSONObject(1), 0, 123000000L), list.get(1).remaining(0, 123000000L));
    }
}
