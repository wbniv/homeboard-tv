package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.*;
import java.util.Locale;

public class DisplayTimeTest {
    private final Locale thai=Locale.forLanguageTag("th-TH");
    @Test public void yearFollowsLanguageAndDoesNotMutateStoredDate(){
        LocalDate date=LocalDate.of(2026,11,18);
        assertEquals("2569-11-18",DisplayTime.formatter("uuuu-MM-dd",thai).format(date));
        assertEquals("2026-11-18",DisplayTime.formatter("uuuu-MM-dd",Locale.ENGLISH).format(date));
        assertEquals("2026-11-18",date.toString());
        assertEquals("2569",DisplayTime.formatter("uuuu",Locale.forLanguageTag("th-u-ca-gregory")).format(date));
        assertEquals("2026",DisplayTime.formatter("uuuu",Locale.forLanguageTag("en-u-ca-buddhist")).format(date));
    }
    @Test public void newYearUsesSelectedCityZoneBeforeConvertingEra(){
        Instant instant=Instant.parse("2026-12-31T18:30:00Z");
        assertEquals("2570-01-01 01:30",DisplayTime.formatter("uuuu-MM-dd HH:mm",thai).withZone(ZoneId.of("Asia/Bangkok")).format(instant));
        assertEquals("2569-12-31 10:30",DisplayTime.formatter("uuuu-MM-dd HH:mm",thai).withZone(ZoneId.of("America/Los_Angeles")).format(instant));
        assertEquals("2026-12-31T18:30:00Z",instant.toString());
    }
    @Test public void leapDayAndLocalTransitionDateConvertWithoutChangingClock(){
        LocalDateTime leap=LocalDateTime.of(2024,2,29,14,5);
        assertEquals("2567-02-29 14:05",DisplayTime.formatter("uuuu-MM-dd HH:mm",thai).format(leap));
        assertEquals("2024-02-29 14:05",DisplayTime.formatter("uuuu-MM-dd HH:mm",Locale.ENGLISH).format(leap));
        assertEquals("14:05",DisplayTime.formatter("HH:mm",thai).format(leap));
    }
    @Test public void elapsedUnitsHaveMinuteHourAndDayBoundaries(){
        String[] en={"%1$dm","%1$dh %2$dm","%1$dd %2$dh"};
        long[] minutes={-1,0,59,60,65,1439,1440,3060};
        String[] expected={"0m","0m","59m","1h 0m","1h 5m","23h 59m","1d 0h","2d 3h"};
        for(int i=0;i<minutes.length;i++)assertEquals(expected[i],DisplayTime.duration(minutes[i]*60000,en));
        assertEquals("0m",DisplayTime.duration(59_999,en));
        assertEquals("1 ชม. 5 น.",DisplayTime.duration(65*60000,new String[]{"%1$d นาที","%1$d ชม. %2$d น.","%1$d วัน %2$d ชม."}));
    }
    @Test public void clockFaceAndOffsetsStayUndecorated(){
        Instant now=Instant.parse("2026-11-18T07:05:00Z");
        assertEquals("14:05",WorldClockModel.time(now,ZoneId.of("Asia/Bangkok"),thai,false,false));
        assertEquals("UTC+05:45",WorldClockModel.offset(now,ZoneId.of("Asia/Kathmandu")));
        assertNotEquals(DisplayTime.CLOCK,DisplayTime.STOPWATCH);
    }
}
