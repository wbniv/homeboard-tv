package com.gathin.tvhome;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

/** Adjust a colored fill without fading its border or the view's content. */
final class UiBackgroundDrawable extends GradientDrawable {
    private final int fillColor;

    UiBackgroundDrawable(int color) { fillColor = color; setFillOpacity(100); }

    void setFillOpacity(int percent) { setColor(colorWithOpacity(fillColor, percent)); }

    static int colorWithOpacity(int color, int percent) {
        int alpha = Math.round((color >>> 24) * WallpaperSettings.percent(percent) / 100f);
        return (color & 0x00ffffff) | (alpha << 24);
    }

    static void apply(View view, int percent) {
        if (view == null) return;
        Drawable background = view.getBackground();
        if (background instanceof UiBackgroundDrawable)
            ((UiBackgroundDrawable) background).setFillOpacity(percent);
        else if (background instanceof States) ((States) background).setFillOpacity(percent);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) apply(group.getChildAt(i), percent);
        }
    }

    /** Update resting and focused fills, including the state that is currently hidden. */
    static final class States extends StateListDrawable {
        private final List<UiBackgroundDrawable> fills = new ArrayList<>();
        @Override public void addState(int[] stateSet, Drawable drawable) {
            super.addState(stateSet, drawable);
            if (drawable instanceof UiBackgroundDrawable) fills.add((UiBackgroundDrawable) drawable);
        }
        void setFillOpacity(int percent) {
            for (UiBackgroundDrawable fill : fills) fill.setFillOpacity(percent);
        }
    }
}
