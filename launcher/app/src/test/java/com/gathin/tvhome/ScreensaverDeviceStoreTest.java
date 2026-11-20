package com.gathin.tvhome;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** The managed flag and the kept original: a separate file, recorded once, never overwritten. */
public class ScreensaverDeviceStoreTest {
    private static ScreensaverValues values(String component, String enabled) {
        Map<String, String> map = new HashMap<>();
        map.put(ScreensaverGateway.COMPONENTS, component);
        if (enabled != null) map.put(ScreensaverGateway.ENABLED, enabled);
        return ScreensaverValues.of(map);
    }

    @Test public void startsUnmanagedWithNothingKept() {
        ScreensaverDeviceStore store = new ScreensaverDeviceStore(new FakeScreensaverGateway.MemoryBacking());
        assertFalse(store.managed());
        assertNull(store.original());
    }

    @Test public void theOriginalIsNeverOverwritten() {
        FakeScreensaverGateway.MemoryBacking backing = new FakeScreensaverGateway.MemoryBacking();
        ScreensaverDeviceStore store = new ScreensaverDeviceStore(backing);
        assertTrue(store.recordOriginal(values("a/a.First", "0")));
        assertTrue(store.recordOriginal(values("b/b.Second", "1")));
        assertEquals("a/a.First", store.original().component());
        assertEquals("0", store.original().get(ScreensaverGateway.ENABLED));
        assertEquals(1, backing.puts);   // the second record never reached storage
        // Survives a new store over the same file, as after a restart.
        assertEquals(values("a/a.First", "0"), new ScreensaverDeviceStore(backing).original());
    }

    @Test public void anUnsetOriginalIsKeptAsUnsetAndAnUnreadableKeyIsNotKept() {
        ScreensaverDeviceStore store = new ScreensaverDeviceStore(new FakeScreensaverGateway.MemoryBacking());
        assertTrue(store.recordOriginal(values(null, null)));
        ScreensaverValues kept = store.original();
        assertNotNull(kept);
        assertTrue(kept.has(ScreensaverGateway.COMPONENTS));
        assertNull(kept.component());
        assertFalse(kept.has(ScreensaverGateway.ENABLED));
    }

    @Test public void aFailedRecordIsReported() {
        FakeScreensaverGateway.MemoryBacking backing = new FakeScreensaverGateway.MemoryBacking();
        backing.allowedPuts = 0;
        ScreensaverDeviceStore store = new ScreensaverDeviceStore(backing);
        assertFalse(store.recordOriginal(values("a/a.First", null)));
        assertNull(store.original());
        assertFalse(store.setManaged(true));
        assertFalse(store.managed());
    }

    @Test public void managedFlagRoundTrips() {
        FakeScreensaverGateway.MemoryBacking backing = new FakeScreensaverGateway.MemoryBacking();
        ScreensaverDeviceStore store = new ScreensaverDeviceStore(backing);
        assertTrue(store.setManaged(true));
        assertTrue(new ScreensaverDeviceStore(backing).managed());
        assertTrue(store.setManaged(false));
        assertFalse(store.managed());
        assertFalse(backing.values.containsKey(ScreensaverDeviceStore.MANAGED));
    }

    @Test public void undoStateRoundTripsThroughJsonAndIgnoresUnknownKeys() throws Exception {
        ScreensaverValues v = values("a/a.First", "1");
        assertEquals(v, ScreensaverValues.parse(v.json().toString()));
        assertNull(ScreensaverValues.parse("{}"));
        assertNull(ScreensaverValues.parse("not json"));
        ScreensaverValues parsed = ScreensaverValues.parse("{\"screensaver_components\":\"a/a.X\",\"sleep_timeout\":\"5\"}");
        assertFalse(parsed.has("sleep_timeout"));
    }
}
