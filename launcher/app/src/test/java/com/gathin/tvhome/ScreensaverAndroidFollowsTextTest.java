package com.gathin.tvhome;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import static org.junit.Assert.*;

/** The Screensaver screen says, in English and Thai, that Android's screensaver replaces Homeboard's at Android's idle time. */
public class ScreensaverAndroidFollowsTextTest {
    private static String note(String language) throws Exception {
        NodeList strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File("src/main/res/" + language + "/homeboard_screensaver.xml"))
                .getElementsByTagName("string");
        for (int i = 0; i < strings.getLength(); i++) {
            Element string = (Element) strings.item(i);
            if ("hb_ss_android_follows".equals(string.getAttribute("name"))) return string.getTextContent();
        }
        return null;
    }

    @Test public void englishSaysAndroidReplacesHomeboardsAndHomeboardKeepsNothingOn() throws Exception {
        String english = note("values");
        assertNotNull(english);
        assertTrue(english, english.contains("replaces Homeboard"));
        assertTrue(english, english.contains("idle time"));
        assertTrue(english, english.contains("does not keep the screen on"));
    }

    @Test public void thaiSaysTheSameWithoutPlaceholders() throws Exception {
        String thai = note("values-th");
        assertNotNull(thai);
        assertTrue(thai, thai.contains("โปรแกรมรักษาหน้าจอของ Android"));
        assertTrue(thai, thai.contains("แทนของ Homeboard"));
        assertTrue(thai, thai.contains("ไม่ได้เปิดหน้าจอค้างไว้"));
        assertFalse(thai, thai.contains("%"));
    }
}
