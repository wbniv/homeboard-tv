package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/** Uses the dashboard's overlay plane when embedded; ordinary windows otherwise. */
class DashboardDialog extends Dialog {
    private final FrameLayout plane;
    private FrameLayout modal;
    private View content, previousFocus;
    private int width = -1, height = -1;
    private boolean showing;
    private DialogInterface.OnDismissListener dismissed;
    private DialogInterface.OnShowListener shown;
    private DialogInterface.OnCancelListener cancelled;
    private DialogInterface.OnKeyListener keys;
    DashboardDialog(Activity activity) {
        super(DashboardDialogs.context(activity));
        plane = activity instanceof MainActivity ? ((MainActivity)activity).dashboardOverlayRoot() : null;
        DashboardDialogs.configure(activity, this);
    }
    @Override public void setContentView(View view) {
        if (plane == null) { super.setContentView(view); return; }
        content = view;
        if (modal != null) { modal.removeAllViews(); attachContent(); }
    }
    void setDashboardLayout(int width, int height) {
        if (plane == null) { getWindow().setLayout(width, height); return; }
        this.width = width; this.height = height;
        if (modal != null && content != null) content.setLayoutParams(contentLayout());
    }
    private FrameLayout.LayoutParams contentLayout() { return new FrameLayout.LayoutParams(width, height, Gravity.CENTER); }
    private void attachContent() {
        if (content.getParent() instanceof ViewGroup) ((ViewGroup)content.getParent()).removeView(content);
        modal.addView(content, contentLayout());
    }
    @Override public void show() {
        if (plane == null) { super.show(); return; }
        if (showing) return;
        if (content == null) throw new IllegalStateException("Missing dashboard dialog content");
        previousFocus = plane.findFocus();
        modal = new FrameLayout(getContext());
        modal.setBackgroundColor(0x99000000); modal.setClickable(true);
        modal.setFocusableInTouchMode(true); modal.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        attachContent();
        plane.addView(modal, new FrameLayout.LayoutParams(-1,-1));
        showing = true;
        content.requestFocus();
        if (shown != null) shown.onShow(this);
    }
    @Override public boolean isShowing() { return plane == null ? super.isShowing() : showing; }
    @Override public void dismiss() {
        if (plane == null) { super.dismiss(); return; }
        if (!showing) return;
        showing = false;
        plane.removeView(modal); modal = null;
        if (previousFocus != null && previousFocus.isAttachedToWindow()) previousFocus.requestFocus();
        previousFocus = null;
        if (dismissed != null) dismissed.onDismiss(this);
    }
    @Override public void cancel() {
        if (plane == null) { super.cancel(); return; }
        if (!showing) return;
        if (cancelled != null) cancelled.onCancel(this);
        dismiss();
    }
    @Override public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        if (plane == null) super.setOnDismissListener(listener); else dismissed = listener;
    }
    @Override public void setOnShowListener(DialogInterface.OnShowListener listener) {
        if (plane == null) super.setOnShowListener(listener); else shown = listener;
    }
    @Override public void setOnCancelListener(DialogInterface.OnCancelListener listener) {
        if (plane == null) super.setOnCancelListener(listener); else cancelled = listener;
    }
    @Override public void setOnKeyListener(DialogInterface.OnKeyListener listener) {
        if (plane == null) super.setOnKeyListener(listener); else keys = listener;
    }
    @Override public View getCurrentFocus() { return plane == null ? super.getCurrentFocus() : modal != null ? modal.findFocus() : null; }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (plane == null) return super.dispatchKeyEvent(event);
        if (!showing) return false;
        int key = event.getKeyCode();
        if (keys != null && keys.onKey(this, key, event)) return true;
        if (modal.dispatchKeyEvent(event)) return true;
        int direction;
        switch (key) {
            case KeyEvent.KEYCODE_DPAD_UP: direction = View.FOCUS_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: direction = View.FOCUS_DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: direction = View.FOCUS_LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: direction = View.FOCUS_RIGHT; break;
            default:
                if (key == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) cancel();
                return key == KeyEvent.KEYCODE_BACK;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            View next = FocusFinder.getInstance().findNextFocus(modal, modal.findFocus(), direction);
            if (next != null) next.requestFocus();
        }
        return true;
    }
}
