package com.gathin.tvhome;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Two small layouts for the Screensaver chooser that adapt to the measured width and the text size instead of fixed
 * columns (ScreensaverPicker's layout rules decide; these only measure and place).
 */
final class ScreensaverLayouts {
    private ScreensaverLayouts() { }

    /** Children placed left to right, wrapping whole to new lines; wrapping text may share a line when room is left. */
    static final class Wrap extends ViewGroup {
        private final Set<View> shrinkable = new HashSet<>();
        private final int gapX, gapY;
        private int[] lefts = new int[0], tops = new int[0];

        Wrap(Context context, int gapX, int gapY) { super(context); this.gapX = gapX; this.gapY = gapY; }

        /** Adds wrapping text that may stay beside earlier children while at least about nine characters fit. */
        void addShrinkable(View child, MarginLayoutParams lp) { shrinkable.add(child); addView(child, lp); }

        @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) { return p instanceof MarginLayoutParams; }
        @Override protected LayoutParams generateDefaultLayoutParams() { return new MarginLayoutParams(-2, -2); }
        @Override protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) { return new MarginLayoutParams(p); }
        @Override public LayoutParams generateLayoutParams(android.util.AttributeSet attrs) { return new MarginLayoutParams(getContext(), attrs); }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
            int n = getChildCount();
            lefts = new int[n]; tops = new int[n];
            int[] lineOf = new int[n];
            int used = 0, line = 0, lineTop = 0, lineHeight = 0, maxRight = 0;
            boolean empty = true;
            int[] lineHeights = new int[n + 1];
            for (int i = 0; i < n; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int margins = lp.leftMargin + lp.rightMargin;
                child.measure(childSpec(lp.width, width - margins), childSpec(lp.height, 0));
                int natural = child.getMeasuredWidth() + margins;
                boolean shrink = shrinkable.contains(child);
                int minShrink = child instanceof TextView ? Math.round(((TextView) child).getTextSize() * 9) : 0;
                int start = empty ? 0 : used + gapX;
                if (!ScreensaverPicker.staysOnLine(start, width, natural, shrink, minShrink + margins, empty)) {
                    lineHeights[line] = lineHeight; lineTop += lineHeight + gapY; line++;
                    used = 0; lineHeight = 0; start = 0;
                } else if (natural > width - start) {
                    child.measure(childSpec(lp.width, width - start - margins), childSpec(lp.height, 0));
                }
                lefts[i] = start + lp.leftMargin; tops[i] = lineTop + lp.topMargin; lineOf[i] = line;
                used = start + child.getMeasuredWidth() + margins;
                lineHeight = Math.max(lineHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                maxRight = Math.max(maxRight, used);
                empty = false;
            }
            lineHeights[line] = lineHeight;
            // Centre each child vertically within its line.
            for (int i = 0; i < n; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int extra = lineHeights[lineOf[i]] - (child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                tops[i] += Math.max(0, extra / 2);
            }
            int height = (empty ? 0 : lineTop + lineHeight) + getPaddingTop() + getPaddingBottom();
            int measuredWidth = MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(widthSpec)
                    : maxRight + getPaddingLeft() + getPaddingRight();
            setMeasuredDimension(measuredWidth, resolveSize(Math.max(height, getSuggestedMinimumHeight()), heightSpec));
        }

        private static int childSpec(int size, int available) {
            if (size >= 0) return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
            if (available <= 0) return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            return MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST);
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            for (int i = 0; i < getChildCount() && i < lefts.length; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                int x = getPaddingLeft() + lefts[i], y = getPaddingTop() + tops[i];
                child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
            }
        }
    }

    /**
     * A row's name and its badge: side by side only when the whole name fits on one line beside the badge
     * (ScreensaverPicker.badgeInline); otherwise the name takes the full width, wrapping at words to two lines, and
     * the badge goes below it.
     */
    static final class NameBadge extends ViewGroup {
        private final TextView name;
        private final View badge;
        private final int gap;
        private boolean inline;

        NameBadge(Context context, TextView name, View badge, int gap) {
            super(context); this.name = name; this.badge = badge; this.gap = gap;
            addView(name, new LayoutParams(-2, -2)); addView(badge, new LayoutParams(-2, -2));
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            boolean hasBadge = badge.getVisibility() != GONE;
            int badgeW = 0, badgeH = 0;
            if (hasBadge) {
                badge.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                badgeW = badge.getMeasuredWidth(); badgeH = badge.getMeasuredHeight();
            }
            int natural = (int) Math.ceil(name.getPaint().measureText(String.valueOf(name.getText()))) + name.getPaddingLeft() + name.getPaddingRight();
            inline = !hasBadge || ScreensaverPicker.badgeInline(width, natural, badgeW, gap);
            int nameWidth = hasBadge && inline ? width - badgeW - gap : width;
            name.measure(MeasureSpec.makeMeasureSpec(Math.max(0, nameWidth), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int height = inline ? Math.max(name.getMeasuredHeight(), badgeH) : name.getMeasuredHeight() + (hasBadge ? gap / 2 + badgeH : 0);
            setMeasuredDimension(width, resolveSize(height, heightSpec));
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int width = r - l, height = b - t;
            if (inline) {
                int nameTop = (height - name.getMeasuredHeight()) / 2;
                name.layout(0, nameTop, name.getMeasuredWidth(), nameTop + name.getMeasuredHeight());
                if (badge.getVisibility() != GONE) {
                    int top = (height - badge.getMeasuredHeight()) / 2;
                    badge.layout(width - badge.getMeasuredWidth(), top, width, top + badge.getMeasuredHeight());
                }
            } else {
                name.layout(0, 0, name.getMeasuredWidth(), name.getMeasuredHeight());
                if (badge.getVisibility() != GONE) {
                    int top = name.getMeasuredHeight() + gap / 2;
                    badge.layout(0, top, badge.getMeasuredWidth(), top + badge.getMeasuredHeight());
                }
            }
        }
    }

    /**
     * Text that wraps only at word boundaries, never splitting or hyphenating a word to fit a line. Layout's constants
     * equal LineBreaker's (API 29, above minSdk 26), which lint asks for, hence the suppression.
     */
    @android.annotation.SuppressLint("WrongConstant")
    static void wholeWords(TextView text) {
        text.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        text.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
    }
}
