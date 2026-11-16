package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class PieMenuGeometryTest {
    @Test public void sideLabelsStayBetweenTheirDirectionIconAndOuterEdge() {
        for (float density : new float[]{1f, 1.5f, 2f}) {
            for (float diameterDp : new float[]{520f, 640f, 760f}) {
                float diameter = diameterDp * density;
                float outer = PieMenuGeometry.outerRadius(diameter, 12f * density);
                float inner = PieMenuGeometry.innerRadius(outer, 104f * density);
                float iconRadius = PieMenuGeometry.sideIconRadius(
                        outer, inner, 30f * density, 160f * density);
                float iconSize = 34f * density;
                float gap = 12f * density;
                float edge = 24f * density;
                float labelWidth = PieMenuGeometry.sideLabelWidth(outer, iconRadius, iconSize, gap, edge);
                float labelStart = PieMenuGeometry.sideLabelStart(iconRadius, iconSize, gap);
                assertTrue("a side-arm label must have usable space", labelWidth > 0);
                assertTrue("the label must not overlap its direction icon", labelStart >= iconRadius + iconSize / 2f + gap);
                assertTrue("the label must finish inside the compass edge", labelStart + labelWidth <= outer - edge + 0.01f);
                float labelRadius = (outer + inner) / 2f + 33f * density;
                float verticalWidth = PieMenuGeometry.verticalLabelWidth(outer, labelRadius, 205f * density,
                        20f * density);
                assertTrue("top and bottom labels must stay inside the circular arm area",
                        verticalWidth <= 2 * (float) Math.sqrt(outer * outer - labelRadius * labelRadius));
            }
        }
    }

    @Test public void centerHubShrinksOnSmallScreensWithoutExceedingItsPreferredSize() {
        float outer = 150f;
        float inner = PieMenuGeometry.innerRadius(outer, 104f);
        assertTrue(inner < 104f);
        assertTrue(inner <= outer * 0.28f);
    }
}
