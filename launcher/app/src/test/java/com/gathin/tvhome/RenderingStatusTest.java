package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class RenderingStatusTest {
    @Test public void outputCannotPromoteAnOrdinaryDashboardTo4k() {
        RenderingStatus status = new RenderingStatus(1920, 1080, null, 3840, 2160, 60);
        assertFalse(status.dashboard.is4k());
        assertTrue(status.output.is4k());
        assertNull(status.surface);
        assertEquals(1920, status.dashboard.width);
    }
    @Test public void missingMeasurementsStayUnknownDespite4kOutput() {
        RenderingStatus status = new RenderingStatus(0, 0, new int[]{0, 0}, 3840, 2160, 60);
        assertFalse(status.dashboard.known());
        assertFalse(status.dashboard.is4k());
        assertFalse(status.surface.known());
        assertTrue(status.output.known());
    }
    @Test public void measuredNativeHierarchyAndSurfaceRemainSeparate() {
        RenderingStatus status = new RenderingStatus(3840, 2160, new int[]{1920, 1080}, 3840, 2160, 59.94f);
        assertTrue(status.dashboard.is4k());
        assertFalse(status.surface.is4k());
        assertEquals(59.94f, status.refreshRate, .001f);
        assertTrue(new RenderingStatus.Size(2160, 3840).is4k());
        assertFalse(new RenderingStatus.Size(3840, 0).known());
        assertFalse(new RenderingStatus.Size(4096, 2160).is4k());
    }
}
