package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppLanguageTest {
    @Test public void resolvesFirstSupportedDevicePreference() {
        assertEquals("th", AppLanguage.resolve("fr-FR", "th-TH", "en-US"));
        assertEquals("en", AppLanguage.resolve("en-GB", "th-TH"));
        assertEquals("en", AppLanguage.resolve("fr", "de"));
        assertEquals("en", AppLanguage.resolve());
    }
    @Test public void acceptsOnlySupportedExplicitModes() {
        assertEquals("en", AppLanguage.supported("en"));
        assertEquals("th", AppLanguage.supported("th"));
        assertEquals("", AppLanguage.supported(""));
        assertEquals("", AppLanguage.supported("fr"));
        assertEquals("", AppLanguage.supported(null));
        assertEquals("", AppLanguage.supported("corrupt"));
    }
}
