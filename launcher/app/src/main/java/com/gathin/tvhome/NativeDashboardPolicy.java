package com.gathin.tvhome;

/** Select native embedding only when it adds UHD detail; never change HDMI mode. */
final class NativeDashboardPolicy {
    static boolean embed(int api, boolean television, int outputWidth, int outputHeight,
                         int uiWidth, int uiHeight, boolean failed) {
        if (failed || api < 30 || !television || uiWidth <= 0 || uiHeight <= 0) return false;
        int outputLong = Math.max(outputWidth, outputHeight);
        int outputShort = Math.min(outputWidth, outputHeight);
        return outputLong >= 3840 && outputShort >= 2160
                && (Math.max(uiWidth, uiHeight) < 3840 || Math.min(uiWidth, uiHeight) < 2160);
    }
    private NativeDashboardPolicy() { }
}
