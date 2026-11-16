package com.gathin.tvhome;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Stable label and artwork center; only the artwork enlarges on focus. */
final class CircularAppTile extends LinearLayout {
    private final ImageView icon;
    private final TextView label;
    private final int preferredDiameter, labelGap;
    private ObjectAnimator labelAnimation;

    CircularAppTile(Context context, Drawable artwork, String name) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        preferredDiameter = Math.round(AppAppearance.ICON_DIAMETER_DP * density);
        labelGap = Math.round(8 * density);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setClipChildren(false); setClipToPadding(false);
        icon = new ImageView(context) {
            @Override protected void onDraw(Canvas canvas) {
                Drawable drawable = getDrawable();
                if (!(drawable instanceof AdaptiveIconDrawable)) { super.onDraw(canvas); return; }
                // Draw the original layers through our circle, avoiding the device's squircle mask.
                AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable) drawable;
                drawLayer(canvas, adaptive.getBackground());
                drawLayer(canvas, adaptive.getForeground());
            }
            private void drawLayer(Canvas canvas, Drawable layer) {
                if (layer == null) return;
                int inset = Math.round(getWidth() * AdaptiveIconDrawable.getExtraInsetFraction());
                layer.setBounds(-inset, -inset, getWidth() + inset, getHeight() + inset);
                layer.draw(canvas);
            }
        };
        icon.setImageDrawable(artwork);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.setClipToOutline(true);
        icon.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        addView(icon, new LayoutParams(preferredDiameter, preferredDiameter));
        label = new TextView(context);
        label.setText(name); label.setTextSize(16);
        label.setTextColor(AppAppearance.RESTING_LABEL);
        label.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setFontFeatureSettings("kern");
        LayoutParams lp = new LayoutParams(-1, -2); lp.topMargin = labelGap;
        addView(label, lp);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
        int height = Math.max(0, MeasureSpec.getSize(heightSpec) - getPaddingTop() - getPaddingBottom());
        int line = label.getLineHeight();
        int fontPadding = label.getCompoundPaddingTop() + label.getCompoundPaddingBottom()
                + Math.max(0, label.getPaint().getFontMetricsInt().bottom - label.getPaint().getFontMetricsInt().top - line);
        int lines = height >= preferredDiameter * AppAppearance.FOCUS_SCALE + labelGap + line * 2 + fontPadding ? 2 : 1;
        int labelHeight = line * lines + fontPadding;
        label.setMaxLines(lines); label.setMinLines(lines);
        LayoutParams labelLp = (LayoutParams) label.getLayoutParams();
        labelLp.height = labelHeight;
        int diameter = AppAppearance.fitIcon(preferredDiameter, width, height, labelHeight, labelGap);
        LayoutParams iconLp = (LayoutParams) icon.getLayoutParams();
        iconLp.width = diameter; iconLp.height = diameter;
        super.onMeasure(widthSpec, heightSpec);
    }

    void setAppFocused(boolean focused, boolean animate) {
        icon.animate().cancel();
        float scale = focused ? AppAppearance.FOCUS_SCALE : 1f;
        if (animate) icon.animate().scaleX(scale).scaleY(scale)
                .setDuration(AppAppearance.TRANSITION_MS)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        else { icon.setScaleX(scale); icon.setScaleY(scale); }
        if (labelAnimation != null) labelAnimation.cancel();
        int color = focused ? AppAppearance.SELECTED_LABEL : AppAppearance.RESTING_LABEL;
        if (animate) {
            labelAnimation = ObjectAnimator.ofArgb(label, "textColor", label.getCurrentTextColor(), color);
            labelAnimation.setDuration(AppAppearance.TRANSITION_MS); labelAnimation.start();
        } else label.setTextColor(color);
    }

    void stopAppearanceAnimation() {
        setAppFocused(isFocused(), false);
    }

    @Override protected void onDetachedFromWindow() {
        stopAppearanceAnimation(); super.onDetachedFromWindow();
    }
}
