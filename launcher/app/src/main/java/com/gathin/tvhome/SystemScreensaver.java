package com.gathin.tvhome;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.provider.Settings;
import android.service.dreams.DreamService;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The real ScreensaverGateway: the only class that touches Android's screensaver settings, the "start now"
 * shortcut and the installed screensaver services. Every call is caught and reported as a typed value, so a TV
 * that refuses a read or a write never crashes Homeboard. Whether these reads and writes work on Cast1 and Cast2
 * is Stage 0 questions 3 to 5 (docs/plans/2026-11-20-screensavers-stage0-probe.md), not yet answered.
 */
final class SystemScreensaver implements ScreensaverGateway {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private final Context context;

    SystemScreensaver(Context context) { this.context = context; }

    /** Android's "start screensaver now" shortcut, also used by the remote's screensaver action. */
    static Intent startNowIntent() {
        return new Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.Somnambulator");
    }

    /** The managed flag and the kept original, in their own preferences file, committed synchronously. */
    static ScreensaverDeviceStore store(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ScreensaverDeviceStore.FILE, Context.MODE_PRIVATE);
        return new ScreensaverDeviceStore(new ScreensaverDeviceStore.Backing() {
            @Override public String get(String key) {
                try { return prefs.getString(key, null); } catch (ClassCastException wrongType) { return null; }
            }
            @Override public boolean put(Map<String, String> changes) {
                SharedPreferences.Editor edit = prefs.edit();
                for (Map.Entry<String, String> change : changes.entrySet()) {
                    if (change.getValue() == null) edit.remove(change.getKey()); else edit.putString(change.getKey(), change.getValue());
                }
                return edit.commit();
            }
        });
    }

    static EverywhereScreensaver everywhere(Context context) {
        return new EverywhereScreensaver(new SystemScreensaver(context), store(context));
    }

    @Override public Reading readSecure(String key) {
        try { return Reading.of(Settings.Secure.getString(context.getContentResolver(), key)); }
        catch (RuntimeException refused) { return Reading.UNREADABLE; }   // SecurityException on firmware that hides the key
    }

    @Override public Reading readTimeout() {
        try { return Reading.of(Settings.System.getString(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT)); }
        catch (RuntimeException refused) { return Reading.UNREADABLE; }
    }

    @Override public boolean mayWrite() {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override public Write writeSecure(String key, String value) {
        try { return Settings.Secure.putString(context.getContentResolver(), key, value) ? Write.OK : Write.REFUSED; }
        catch (SecurityException notPermitted) { return Write.NOT_PERMITTED; }
        catch (RuntimeException refused) { return Write.REFUSED; }
    }

    @Override public boolean startNow() {
        Intent intent = startNowIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (context.getPackageManager().resolveActivity(intent, 0) == null) return false;
            context.startActivity(intent);
            return true;
        } catch (RuntimeException unavailable) { return false; }
    }

    @Override public List<InstalledDream> installedDreams() {
        List<InstalledDream> found = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> services;
        try { services = pm.queryIntentServices(new Intent(DreamService.SERVICE_INTERFACE), PackageManager.GET_META_DATA); }
        catch (RuntimeException failed) { return found; }
        for (ResolveInfo result : services) {
            ServiceInfo si = result.serviceInfo;
            if (si == null) continue;
            String label;
            try { label = String.valueOf(si.loadLabel(pm)); } catch (RuntimeException failed) { label = si.packageName; }
            found.add(new InstalledDream(si.packageName + "/" + si.name, label,
                    ScreensaverRegistry.settingsComponent(si.packageName, declaredSettings(pm, si)), appScreen(pm, si)));
        }
        return found;
    }

    /**
     * The preview screen the service declares in its meta-data (ScreensaverRegistry.appScreen), kept only when that
     * activity is installed, enabled, exported and needs no permission Homeboard lacks; otherwise null, and the app
     * is listed as one without a preview screen.
     */
    private ScreensaverGateway.AppScreen appScreen(PackageManager pm, ServiceInfo si) {
        android.os.Bundle meta = si.metaData;
        if (meta == null) return null;
        ScreensaverGateway.AppScreen screen;
        try {
            screen = ScreensaverRegistry.appScreen(si.packageName, meta.getString(ScreensaverRegistry.PREVIEW_ACTIVITY),
                    meta.getString(ScreensaverRegistry.PREVIEW_EXTRA), meta.getString(ScreensaverRegistry.PREVIEW_VALUE));
        } catch (RuntimeException unreadable) { return null; }
        if (screen == null) return null;
        try {
            ComponentName component = ComponentName.unflattenFromString(screen.component);
            if (component == null) return null;
            android.content.pm.ActivityInfo activity = pm.getActivityInfo(component, 0);
            if (!activity.exported || !activity.isEnabled()) return null;
            if (activity.permission != null && context.checkSelfPermission(activity.permission) != PackageManager.PERMISSION_GRANTED) return null;
            return screen;
        } catch (PackageManager.NameNotFoundException | RuntimeException missing) { return null; }
    }

    /** Whether choice is an installed screensaver app with a usable preview screen; false for a built-in id. */
    static boolean hasAppScreen(Context context, String choice) {
        if (!ScreensaverRules.isApp(choice)) return false;
        for (InstalledDream dream : new SystemScreensaver(context).installedDreams())
            if (dream.component.equals(choice)) return dream.screen != null;
        return false;
    }

    /** The intent that starts an app's preview screen, with its extra when declared; null when it cannot be built. */
    static Intent appScreenIntent(ScreensaverGateway.AppScreen screen) {
        ComponentName component = screen == null ? null : ComponentName.unflattenFromString(screen.component);
        if (component == null) return null;
        Intent intent = new Intent().setComponent(component);
        return screen.extra == null ? intent : intent.putExtra(screen.extra, screen.value);
    }

    /** settingsActivity from the service's android.service.dream metadata, or null. */
    private static String declaredSettings(PackageManager pm, ServiceInfo si) {
        try (XmlResourceParser parser = si.loadXmlMetaData(pm, DreamService.DREAM_META_DATA)) {
            if (parser == null) return null;
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) { }
            return type == XmlPullParser.START_TAG ? parser.getAttributeValue(ANDROID_NS, "settingsActivity") : null;
        } catch (Exception unreadable) { return null; }
    }

    @Override public String ownComponent() {
        return new ComponentName(context.getPackageName(), ScreensaverDream.class.getName()).flattenToString();
    }
}
