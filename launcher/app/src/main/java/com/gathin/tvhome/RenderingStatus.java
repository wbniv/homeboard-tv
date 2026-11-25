package com.gathin.tvhome;

/** Independent observed sizes: a 4K output never substitutes for the dashboard. */
final class RenderingStatus {
    final Size dashboard, surface, output;
    final float refreshRate;
    RenderingStatus(int dashboardWidth, int dashboardHeight, int[] surfaceSize,
                    int outputWidth, int outputHeight, float refreshRate) {
        dashboard = new Size(dashboardWidth, dashboardHeight);
        surface = surfaceSize == null ? null : new Size(surfaceSize[0], surfaceSize[1]);
        output = new Size(outputWidth, outputHeight);
        this.refreshRate = refreshRate;
    }
    static final class Size {
        final int width, height;
        Size(int width, int height) { this.width = width; this.height = height; }
        boolean known() { return width > 0 && height > 0; }
        boolean is4k() { return width == 3840 && height == 2160 || width == 2160 && height == 3840; }
    }
}
