package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * The one focus highlight for app artwork, shared by All Apps, the Home grid and the Favorites widget so that
 * they cannot drift apart: a halo and ring (circular icons), a plate outline (banners) or a glass sheet (plain
 * icons) that slides from one app to the next on a spring.
 *
 * <p>{@code root} is the view the artwork is a descendant of, whose coordinates the highlight is placed in;
 * {@code host} is the view that holds the highlight and is {@code root} itself, or a child of it whose clipping
 * the highlight should follow.
 */
final class AppFocusHighlight {
    /** Enlargement of a plain icon's tile while focused, and of its glass sheet. */
    static final float SCALE = 1.08f;

    private final ViewGroup root, host;
    private final View view;
    private final SpringAnimation springX, springY;
    private final float density;
    private final boolean reducedMotion;
    private boolean shown;

    /** Whether this kind of highlight is drawn behind the artwork (true) or over it (plain icons). */
    static boolean behindArtwork(boolean banners, boolean circular) {
        return banners || circular;
    }

    /**
     * Adds the highlight to {@code host} at {@code index} (-1 for on top). Pass
     * {@link #behindArtwork} to decide whether that index is behind the artwork.
     */
    AppFocusHighlight(Context context, ViewGroup root, ViewGroup host, int index, boolean banners,
                      boolean circular, int paletteBase, boolean reducedMotion) {
        this.root = root;
        this.host = host;
        density = context.getResources().getDisplayMetrics().density;
        this.reducedMotion = reducedMotion;
        if (banners) {
            view = new BannerFocusView(context);
        } else if (circular) {
            view = new AppFocusView(context);
        } else {
            GlassView glass = new GlassView(context, null);
            glass.setPalette(paletteBase); glass.setRadius(dp(18)); glass.setInset(0);
            view = glass;
        }
        view.setAlpha(0f);
        view.setFocusable(false);
        view.setClickable(false);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        host.addView(view, index, new FrameLayout.LayoutParams(dp(100), dp(100)));
        FloatPropertyCompat<View> x = new FloatPropertyCompat<View>("appFocusX") {
            @Override public float getValue(View v) { return v.getTranslationX(); }
            @Override public void setValue(View v, float value) { v.setTranslationX(value); }
        };
        FloatPropertyCompat<View> y = new FloatPropertyCompat<View>("appFocusY") {
            @Override public float getValue(View v) { return v.getTranslationY(); }
            @Override public void setValue(View v, float value) { v.setTranslationY(value); }
        };
        springX = new SpringAnimation(view, x);
        springY = new SpringAnimation(view, y);
        float damping = behindArtwork(banners, circular) ? 1f : 0.58f;
        float stiffness = 260f;
        if (reducedMotion) { stiffness = 1000f; damping = 1f; }
        springX.setSpring(new SpringForce().setStiffness(stiffness).setDampingRatio(damping));
        springY.setSpring(new SpringForce().setStiffness(stiffness).setDampingRatio(damping));
    }

    /**
     * The view whose bounds the highlight surrounds: the artwork when the tile starts with an image, otherwise
     * the tile.
     */
    static View target(View tile) {
        if (tile instanceof ViewGroup && ((ViewGroup) tile).getChildCount() > 0
                && ((ViewGroup) tile).getChildAt(0) instanceof ImageView)
            return ((ViewGroup) tile).getChildAt(0);
        return tile;
    }

    /** True once the target has a size to surround; before then {@link #move} does nothing. */
    static boolean ready(View card) {
        return card.getParent() != null && card.getWidth() > 0 && card.getHeight() > 0;
    }

    /** Surround {@code card}, sliding there when the highlight is already showing and {@code animate} is set. */
    void move(View card, boolean animate) {
        if (!ready(card)) return;
        Rect rect = new Rect(0, 0, card.getWidth(), card.getHeight());
        root.offsetDescendantRectToMyCoords(card, rect);
        int width, height;
        if (view instanceof AppFocusView) {
            ((AppFocusView) view).setIconDiameter(card.getWidth());
            width = height = AppAppearance.focusExtent(card.getWidth());
        } else if (view instanceof BannerFocusView) {
            width = (int) Math.ceil(card.getWidth() * SCALE) + dp(20);
            height = (int) Math.ceil(card.getHeight() * SCALE) + dp(20);
        } else {
            int padding = 1;
            width = (int) Math.ceil(card.getWidth() * SCALE) + padding * 2;
            height = (int) Math.ceil(card.getHeight() * SCALE) + padding * 2;
            ((GlassView) view).setRadius(dp(10));
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = width; lp.height = height;
        view.setLayoutParams(lp);
        // A host that is not the root sits at an offset inside it.
        float x = rect.exactCenterX() - width / 2f - (host == root ? 0 : host.getLeft());
        float y = rect.exactCenterY() - height / 2f - (host == root ? 0 : host.getTop());
        if (!shown || !animate || reducedMotion) {
            springX.cancel(); springY.cancel();
            view.setTranslationX(x); view.setTranslationY(y);
        } else {
            springX.animateToFinalPosition(x);
            springY.animateToFinalPosition(y);
        }
        shown = true;
        view.setAlpha(1f);
    }

    void hide() {
        springX.cancel(); springY.cancel();
        view.setAlpha(0f);
        shown = false;
    }

    /** Stops any slide in progress, for a highlight that is about to be dropped. */
    void cancel() {
        springX.cancel(); springY.cancel();
        shown = false;
    }

    private int dp(float value) {
        return Math.round(value * density);
    }
}
