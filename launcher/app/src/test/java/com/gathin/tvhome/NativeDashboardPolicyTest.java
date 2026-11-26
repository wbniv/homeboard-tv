package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeDashboardPolicyTest {
    @Test public void uhdTvWith1080UiEmbedsWithoutChangingWorkspace() {
        assertTrue(NativeDashboardPolicy.embed(34, true, 3840, 2160, 1920, 1080, false));
    }
    @Test public void nativeUiAndHdOutputNeedNoEmbedding() {
        assertFalse(NativeDashboardPolicy.embed(34, true, 3840, 2160, 3840, 2160, false));
        assertFalse(NativeDashboardPolicy.embed(34, true, 1920, 1080, 1920, 1080, false));
        assertFalse(NativeDashboardPolicy.embed(34, true, 2560, 1440, 1920, 1080, false));
    }
    @Test public void unsupportedPlatformAndFailedAllocationUseOrdinaryRenderer() {
        assertFalse(NativeDashboardPolicy.embed(29, true, 3840, 2160, 1920, 1080, false));
        assertFalse(NativeDashboardPolicy.embed(34, false, 3840, 2160, 1920, 1080, false));
        assertFalse(NativeDashboardPolicy.embed(34, true, 3840, 2160, 1920, 1080, true));
        assertFalse(NativeDashboardPolicy.embed(34, true, 3840, 2160, 0, 0, false));
    }
    @Test public void largerOutputStillUsesBoundedUhdCandidate() {
        assertTrue(NativeDashboardPolicy.embed(34, true, 7680, 4320, 1920, 1080, false));
    }
}
