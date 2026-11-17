package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class CalendarWidgetGeometryTest {
    @Test public void shortCalendarUsesOneRowAndFitsBetweenHeadingAndStatus() {
        for (float width : new float[]{140, 300, 480, 900}) {
            for (float height : new float[]{90, 130, 180}) {
                CalendarWidgetGeometry grid = new CalendarWidgetGeometry(width, height);
                assertTrue(grid.compact); assertEquals(1, grid.rows);
                for (int i = 0; i < grid.pageSize(); i++) {
                    assertTrue(grid.x(i) >= 6);
                    assertTrue(grid.x(i) + grid.cardSize <= width - 6);
                    assertTrue(grid.y(i) >= 24);
                    assertTrue(grid.y(i) + grid.cardSize <= height - 20);
                    if (i > 0) assertTrue(grid.x(i) >= grid.x(i - 1) + grid.cardSize);
                }
            }
        }
    }

    @Test public void tallCalendarKeepsItsExistingGridAndCardSize() {
        CalendarWidgetGeometry grid = new CalendarWidgetGeometry(480, 400);
        assertFalse(grid.compact); assertEquals(4, grid.columns); assertEquals(3, grid.rows);
        assertEquals(44, grid.top, .01);
        assertEquals(Math.min(468f / 4 - 6, 322f / 3 - 6), grid.cardSize, .01);
    }

    @Test public void widerStripShowsMoreDatesWithoutShrinkingTheSquares() {
        CalendarWidgetGeometry narrow = new CalendarWidgetGeometry(300, 130);
        CalendarWidgetGeometry wide = new CalendarWidgetGeometry(900, 130);
        assertTrue(wide.pageSize() > narrow.pageSize());
        assertEquals(narrow.cardSize, wide.cardSize, .01);
    }
}
