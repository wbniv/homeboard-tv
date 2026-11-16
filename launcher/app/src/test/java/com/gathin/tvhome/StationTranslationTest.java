package com.gathin.tvhome;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.NodeList;
import static org.junit.Assert.*;

/** The feed station order is 114, 119, 122, 70, 65 in both languages. */
public class StationTranslationTest {
    @Test public void translatedStationOrderPreservesFeedIdentity() throws Exception {
        for (String language : new String[]{"values", "values-th"}) {
            NodeList arrays = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new File("src/main/res/" + language + "/homeboard.xml"))
                    .getElementsByTagName("string-array");
            boolean found = false;
            for (int i = 0; i < arrays.getLength(); i++) {
                org.w3c.dom.Element array = (org.w3c.dom.Element) arrays.item(i);
                if (!"hb_station_names".equals(array.getAttribute("name"))) continue;
                NodeList names = array.getElementsByTagName("item");
                assertEquals(5, names.getLength());
                assertEquals(language.equals("values") ? "Watthana · Ekkamai Road" : "วัฒนา · ถนนเอกมัย",
                        names.item(3).getTextContent());
                assertEquals(language.equals("values") ? "Pathum Wan · Samyan Mitrtown" : "ปทุมวัน · สามย่านมิตรทาวน์",
                        names.item(4).getTextContent());
                found = true;
            }
            assertTrue("Missing station array for " + language, found);
        }
    }
}
