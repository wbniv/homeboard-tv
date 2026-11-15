package com.gathin.tvhome;

import android.graphics.Color;

/** Interface colors derived from the user's selected base palette. */
final class PaletteColors {
    final int background, surface, raised, focus, edge, text, muted, scrim;

    private PaletteColors(int base) {
        background = base;
        surface = mix(base, Color.WHITE, 0.075f);
        raised = mix(base, Color.WHITE, 0.15f);
        focus = mix(base, Color.WHITE, 0.42f);
        edge = mix(base, Color.WHITE, 0.26f);
        double luma = (Color.red(base) * 0.2126 + Color.green(base) * 0.7152 + Color.blue(base) * 0.0722) / 255.0;
        text = luma > 0.48 ? Color.rgb(24, 26, 28) : mix(base, Color.WHITE, 0.88f);
        muted = luma > 0.48 ? mix(base, Color.BLACK, 0.55f) : mix(base, Color.WHITE, 0.70f);
        scrim = Color.argb(205, Color.red(base) / 3, Color.green(base) / 3, Color.blue(base) / 3);
    }

    static PaletteColors from(int base) { return new PaletteColors(base); }

    static int mix(int from, int to, float amount) {
        return Color.rgb(Math.round(Color.red(from) * (1 - amount) + Color.red(to) * amount),
                Math.round(Color.green(from) * (1 - amount) + Color.green(to) * amount),
                Math.round(Color.blue(from) * (1 - amount) + Color.blue(to) * amount));
    }
}
