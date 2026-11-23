package com.gathin.tvhome;

/** Pure selection logic for display modes; dimensions are the raw mode dimensions. */
final class ResolutionModeChooser {
    private ResolutionModeChooser() { }

    static int bestIndex(int[] widths, int[] heights, float[] refreshRates, int currentIndex) {
        int best = -1;
        float currentRate = currentIndex >= 0 && currentIndex < refreshRates.length
                ? refreshRates[currentIndex] : 0;
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] <= 0 || heights[i] <= 0) continue;
            long area = (long) widths[i] * heights[i];
            long bestArea = best < 0 ? -1 : (long) widths[best] * heights[best];
            float distance = Math.abs(refreshRates[i] - currentRate);
            float bestDistance = best < 0 ? Float.MAX_VALUE : Math.abs(refreshRates[best] - currentRate);
            if (area > bestArea || (area == bestArea && (distance < bestDistance
                    || (distance == bestDistance && i == currentIndex)))) best = i;
        }
        return best;
    }

    static int orientedWidth(int width, int height, int rotation) {
        return rotation == 1 || rotation == 3 ? height : width;
    }

    static int orientedHeight(int width, int height, int rotation) {
        return rotation == 1 || rotation == 3 ? width : height;
    }
}
