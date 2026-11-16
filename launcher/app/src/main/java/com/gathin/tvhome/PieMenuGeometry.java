package com.gathin.tvhome;

/** Shared compass-menu measurements; callers pass display-pixel values. */
final class PieMenuGeometry {
    private PieMenuGeometry() { }

    static float outerRadius(float diameter, float inset) {
        return Math.max(0, diameter / 2f - inset);
    }

    static float innerRadius(float outer, float preferred) {
        // Keep the hub proportional on smaller displays so each arm retains room for its label.
        return Math.min(preferred, outer * 0.28f);
    }

    static float sideIconRadius(float outer, float inner, float innerGap, float edgeOffset) {
        return Math.max(inner + innerGap, outer - edgeOffset);
    }

    static float sideLabelStart(float iconRadius, float iconSize, float gap) {
        return iconRadius + iconSize / 2f + gap;
    }

    static float sideLabelWidth(float outer, float iconRadius, float iconSize, float gap, float edge) {
        return Math.max(0, outer - edge - sideLabelStart(iconRadius, iconSize, gap));
    }

    static float verticalLabelWidth(float outer, float labelRadius, float preferred, float edge) {
        float halfWidth = (float) Math.sqrt(Math.max(0, outer * outer - labelRadius * labelRadius));
        return Math.min(preferred, Math.max(0, halfWidth * 2 - edge * 2));
    }
}
