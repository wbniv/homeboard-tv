package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ProjectivyProviderRegistry {
    static final String ACTION = "tv.projectivy.plugin.WALLPAPER_PROVIDER";
    static final class Provider {
        final ComponentName component;
        final String uuid, name, settingsClass, problem;
        final int events;
        final long cacheMillis;
        Provider(ComponentName component, String uuid, String name, String settingsClass,
                 int events, long cacheMillis, String problem) {
            this.component = component; this.uuid = uuid; this.name = name; this.settingsClass = settingsClass;
            this.events = events; this.cacheMillis = cacheMillis; this.problem = problem;
        }
        boolean usable() { return problem.isEmpty(); }
        String readiness() {
            if (!usable()) return "Unavailable · " + problem;
            String missing = WallpaperSettings.missingEvents(events);
            return missing.isEmpty() ? "Ready" : "Partial support · no " + missing;
        }
    }
    private final Context context;
    ProjectivyProviderRegistry(Context context) { this.context = context; }

    List<Provider> discover() {
        PackageManager pm = context.getPackageManager();
        List<Provider> providers = new ArrayList<>();
        for (ResolveInfo result : pm.queryIntentServices(new Intent(ACTION), PackageManager.GET_META_DATA)) {
            ServiceInfo si = result.serviceInfo;
            if (si == null) continue;
            try {
                Resources res = pm.getResourcesForApplication(si.applicationInfo);
                Bundle meta = si.metaData == null ? new Bundle() : si.metaData;
                String uuid = string(meta, res, "uuid", "");
                int version = integer(meta, res, "apiVersion", -1);
                int events = integer(meta, res, "updateMode", 0);
                String problem = !si.enabled || !si.applicationInfo.enabled ? "disabled service"
                        : !si.exported ? "service is private"
                        : si.permission != null && context.checkSelfPermission(si.permission) != PackageManager.PERMISSION_GRANTED
                        ? "service requires permission"
                        : version != 1 ? "API " + version + " is unsupported"
                        : !uuid.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")
                        ? "invalid provider UUID" : !WallpaperSettings.usableEvents(events) ? "no supported event source" : "";
                providers.add(new Provider(new ComponentName(si.packageName, si.name), uuid,
                        string(meta, res, "name", String.valueOf(si.loadLabel(pm))),
                        string(meta, res, "settingsActivity", ""), events,
                        Math.max(0, Math.min(7 * 86400_000L, integer(meta, res, "itemsCacheDurationMillis", 0))), problem));
            } catch (RuntimeException | PackageManager.NameNotFoundException invalid) {
                providers.add(new Provider(new ComponentName(si.packageName, si.name), "", si.packageName,
                        "", 0, 0, "invalid provider metadata"));
            }
        }
        providers.sort(Comparator.comparing((Provider p) -> p.name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(p -> p.component.flattenToString()));
        return providers;
    }
    Provider find(WallpaperSettings s) {
        for (Provider p : discover()) if (p.component.flattenToString().equals(s.component) && p.uuid.equals(s.uuid)) return p;
        return null;
    }
    Intent settings(Provider provider) {
        if (provider.settingsClass.isEmpty()) return null;
        String pkg = provider.component.getPackageName();
        String name = provider.settingsClass.startsWith(".") ? pkg + provider.settingsClass
                : provider.settingsClass.contains(".") ? provider.settingsClass : pkg + "." + provider.settingsClass;
        ComponentName component = new ComponentName(pkg, name);
        try {
            ActivityInfo info = context.getPackageManager().getActivityInfo(component, 0);
            if (!info.exported || !info.enabled || !info.applicationInfo.enabled
                    || info.permission != null && context.checkSelfPermission(info.permission) != PackageManager.PERMISSION_GRANTED) return null;
            return new Intent().setComponent(component);
        } catch (PackageManager.NameNotFoundException | RuntimeException missing) { return null; }
    }
    private static String string(Bundle b, Resources r, String key, String fallback) {
        Object value = b.get(key);
        if (value instanceof String) return (String) value;
        if (value instanceof Integer) try { return r.getString((Integer) value); } catch (Resources.NotFoundException ignored) { }
        return fallback;
    }
    private static int integer(Bundle b, Resources r, String key, int fallback) {
        Object value = b.get(key);
        if (value instanceof Integer) {
            int n = (Integer) value;
            try { if ("integer".equals(r.getResourceTypeName(n))) return r.getInteger(n); }
            catch (Resources.NotFoundException ignored) { }
            return n;
        }
        return fallback;
    }
}
