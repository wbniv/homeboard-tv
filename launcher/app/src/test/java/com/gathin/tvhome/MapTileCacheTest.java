package com.gathin.tvhome;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class MapTileCacheTest {
    private Set<String> keys(String prefix, int count) {
        Set<String> keys = new LinkedHashSet<>();
        for (int i=0;i<count;i++) keys.add(prefix+i);
        return keys;
    }
    private void fill(MapTileCache<String> cache, Set<String> keys) {
        for (String key : keys) if (cache.get(key)==null) {
            assertTrue(cache.beginLoad(key)); cache.completeLoad(key,key);
        }
    }
    @Test public void aNativeViewportStaysCompleteAcrossRepeatedRedraws() {
        Set<String> viewport = MapGeometry.tileKeys(10,130017.5,122023.5,2348,1496);
        assertTrue(viewport.size()>40);
        MapTileCache<String> cache = new MapTileCache<>(); cache.visible(viewport); fill(cache,viewport);
        for (int redraw=0;redraw<100;redraw++) {
            cache.visible(viewport);
            for (String key : viewport) assertEquals(key,cache.get(key));
        }
        assertEquals(viewport.size(),cache.size()); assertFalse(cache.loading());
    }
    @Test public void duplicateRequestsCannotEraseAnotherInflightRequestOrBypassTheLimit() {
        MapTileCache<String> cache = new MapTileCache<>(); cache.visible(keys("t",80));
        for(int i=0;i<32;i++)assertTrue(cache.beginLoad("t"+i));
        for(int redraw=0;redraw<100;redraw++)assertFalse(cache.beginLoad("t0"));
        assertEquals(32,cache.pendingCount());assertFalse(cache.beginLoad("t32"));
        cache.completeLoad("t0","tile");assertEquals(31,cache.pendingCount());
        assertTrue(cache.beginLoad("t32"));assertFalse(cache.beginLoad("t1"));
        assertEquals(32,cache.pendingCount());
    }
    @Test public void obsoleteCompletionsCannotEvictTheCurrentViewport() {
        MapTileCache<String> cache = new MapTileCache<>(); Set<String> before=keys("a",77), after=keys("b",70);
        cache.visible(before);fill(cache,before);assertTrue(cache.beginLoad("a0"));
        cache.visible(after);fill(cache,after);
        assertFalse(cache.isVisible("a0"));cache.completeLoad("a0","late");cache.publish("a1","late disk read");
        assertEquals(70,cache.size());assertNull(cache.get("a0"));assertNull(cache.get("a1"));
        for(String key:after)assertEquals(key,cache.get(key));
    }
    @Test public void recentHistoryIsBoundedAndVisibleTilesRemainPinned() {
        MapTileCache<String> cache = new MapTileCache<>();
        for(int pan=0;pan<20;pan++) {
            Set<String> viewport=keys("view"+pan+"-",12);cache.visible(viewport);fill(cache,viewport);
            assertTrue(cache.size()<=40);
            for(String key:viewport)assertEquals(key,cache.get(key));
        }
    }
    @Test public void staleTileSurvivesFailedRefreshAndClosedCacheRejectsLateCallbacks() {
        MapTileCache<String> cache=new MapTileCache<>();cache.visible(keys("t",1));assertTrue(cache.beginLoad("t0"));
        cache.publish("t0","stale");assertTrue(cache.loading());cache.completeLoad("t0",null);
        assertEquals("stale",cache.get("t0"));assertFalse(cache.loading());
        cache.clear();cache.completeLoad("t0","late");assertNull(cache.get("t0"));assertEquals(0,cache.pendingCount());
    }
    @Test public void wrappedAndClippedWorldProducesOnlyUniqueValidTileKeys() {
        Set<String> keys=MapGeometry.tileKeys(2,-513,-256,3840,2160);
        assertEquals(16,keys.size());
        assertTrue(MapGeometry.tileKeys(10,0,0,0,100).isEmpty());
        assertTrue(MapGeometry.tileKeys(10,0,-1000,100,100).isEmpty());
    }
}
