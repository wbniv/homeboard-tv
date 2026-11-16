package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A measured 16:9 plate and stationary label, including icon fallback for apps without banners. */
// Homeboard uses platform Activity inflation; AppCompat arrives through Lottie only.
@android.annotation.SuppressLint("AppCompatCustomView")
final class BannerAppTile extends LinearLayout {
    private final ImageView image;
    private final TextView label;
    private final int gap, clearance;

    BannerAppTile(Context context, Drawable artwork, boolean fallback, String name) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        gap = Math.round(6 * density); clearance = Math.round(8 * density);
        setOrientation(VERTICAL); setGravity(Gravity.CENTER);
        setClipChildren(false); setClipToPadding(false);
        image = new ImageView(context) {
            @Override protected void onDraw(Canvas canvas) {
                if (!fallback) { super.onDraw(canvas); return; }
                Drawable icon = getDrawable();
                if (icon == null) return;
                int size = Math.round(Math.min(getWidth(), getHeight()) * 0.65f);
                int intrinsicWidth = icon.getIntrinsicWidth(), intrinsicHeight = icon.getIntrinsicHeight();
                float aspect = intrinsicWidth > 0 && intrinsicHeight > 0 ? (float) intrinsicWidth / intrinsicHeight : 1f;
                int width = aspect > 1 ? size : Math.round(size * aspect);
                int height = aspect > 1 ? Math.round(size / aspect) : size;
                int left = (getWidth() - width) / 2, top = (getHeight() - height) / 2;
                icon.setBounds(left, top, left + width, top + height); icon.draw(canvas);
            }
        };
        image.setImageDrawable(artwork); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        GradientDrawable plate = new UiBackgroundDrawable(0xff272b30);
        plate.setCornerRadius(8 * density); image.setBackground(plate);
        image.setClipToOutline(true);
        image.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 8 * density);
            }
        });
        addView(image, new LayoutParams(0, 0));
        label = new TextView(context); label.setText(name); label.setTextSize(16);
        label.setTextColor(AppAppearance.RESTING_LABEL); label.setGravity(Gravity.CENTER);
        label.setSingleLine(true); label.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams lp = new LayoutParams(-1, -2); lp.topMargin = gap;
        addView(label, lp);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
        int height = Math.max(0, MeasureSpec.getSize(heightSpec) - getPaddingTop() - getPaddingBottom());
        android.graphics.Paint.FontMetricsInt metrics = label.getPaint().getFontMetricsInt();
        int labelHeight = Math.max(label.getLineHeight(), metrics.bottom - metrics.top);
        ((LayoutParams) label.getLayoutParams()).height = labelHeight;
        int[] size = AppAppearance.fitBanner(width, height, labelHeight, gap, clearance);
        LayoutParams lp = (LayoutParams) image.getLayoutParams(); lp.width = size[0]; lp.height = size[1];
        super.onMeasure(widthSpec, heightSpec);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Keep the label below the entire focus envelope, rather than below the resting image.
        int labelTop = getHeight() - getPaddingBottom() - label.getMeasuredHeight();
        label.layout(getPaddingLeft(), labelTop, getWidth() - getPaddingRight(),
                labelTop + label.getMeasuredHeight());
        int imageLeft = (getWidth() - image.getMeasuredWidth()) / 2;
        int imageTop = getPaddingTop() + Math.max(0,
                (labelTop - gap - getPaddingTop() - image.getMeasuredHeight()) / 2);
        image.layout(imageLeft, imageTop, imageLeft + image.getMeasuredWidth(), imageTop + image.getMeasuredHeight());
    }

    void setAppFocused(boolean focused, boolean animate) {
        image.animate().cancel();
        float scale = focused ? AppAppearance.FOCUS_SCALE : 1f;
        if (animate) image.animate().scaleX(scale).scaleY(scale).setDuration(AppAppearance.TRANSITION_MS)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        else { image.setScaleX(scale); image.setScaleY(scale); }
        label.setTextColor(focused ? AppAppearance.SELECTED_LABEL : AppAppearance.RESTING_LABEL);
    }

    @Override protected void onDetachedFromWindow() {
        image.animate().cancel(); super.onDetachedFromWindow();
    }
}
