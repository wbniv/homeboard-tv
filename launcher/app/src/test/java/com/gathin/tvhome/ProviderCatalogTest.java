package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;

public class ProviderCatalogTest {
    @Test public void reviewedRoutesAreDistinctAndNeverApks() {
        HashSet<String> ids=new HashSet<>(),packages=new HashSet<>();int play=0;
        for(ProviderCatalog.Entry e:ProviderCatalog.ENTRIES){
            assertTrue(ids.add(e.id));assertTrue(packages.add(e.packageName));assertFalse(e.url.endsWith(".apk"));
            if(e.play){play++;assertEquals("https://play.google.com/store/apps/details?id="+e.packageName,e.url);}
            else if(!e.excluded)assertTrue(e.url.startsWith("https://github.com/"));
        }
        assertEquals(3,play);
    }
    @Test public void weatherHasNoInstallationRoute() {
        ProviderCatalog.Entry weather=ProviderCatalog.ENTRIES.stream().filter(e->e.id.equals("weather")).findFirst().get();
        assertTrue(weather.excluded);assertFalse(weather.play);assertEquals("",weather.url);
    }
    @Test(expected=IllegalArgumentException.class) public void exclusionCannotGainLink() {
        new ProviderCatalog.Entry("weather","Weather","pkg","https://github.com/publisher",0,false,true);
    }
}
