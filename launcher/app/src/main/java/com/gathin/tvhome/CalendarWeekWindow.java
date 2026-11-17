package com.gathin.tvhome;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Four complete calendar weeks, independent of compact lookahead settings. */
final class CalendarWeekWindow {
    LocalDate start, selected;
    CalendarWeekWindow(LocalDate date){selected=date;start=monday(date);}
    static LocalDate monday(LocalDate date){return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));}
    LocalDate end(){return start.plusDays(27);}
    void select(LocalDate date){
        selected=date;
        while(selected.isBefore(start))start=start.minusWeeks(4);
        while(selected.isAfter(end()))start=start.plusWeeks(4);
    }
    void page(int direction){start=start.plusWeeks(4L*direction);selected=selected.plusWeeks(4L*direction);}
}
