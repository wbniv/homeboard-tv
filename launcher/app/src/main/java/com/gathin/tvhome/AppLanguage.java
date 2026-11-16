package com.gathin.tvhome;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

/** Owns the per-install language, independently of layout snapshots. */
final class AppLanguage {
    private static final String STORE = "app-language";
    static String mode(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleList locales = context.getSystemService(LocaleManager.class).getApplicationLocales();
            return locales.isEmpty() ? "" : supported(locales.get(0).getLanguage());
        }
        return supported(context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getString("mode", ""));
    }
    static String supported(String language) {
        return "en".equals(language) || "th".equals(language) ? language : "";
    }
    static String resolve(String... tags) {
        for (String tag : tags) {
            String candidate = supported(Locale.forLanguageTag(tag).getLanguage());
            if (!candidate.isEmpty()) return candidate;
        }
        return "en";
    }
    static boolean set(Context context, String mode) {
        if (!mode.equals(supported(mode))) throw new IllegalArgumentException("Unsupported language");
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags(mode));
            return true;
        }
        return context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putString("mode", mode).commit();
    }
    static Context wrap(Context context) {
        if (Build.VERSION.SDK_INT >= 33) return context;
        String mode = mode(context);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        if (mode.isEmpty()) {
            LocaleList device = android.content.res.Resources.getSystem().getConfiguration().getLocales();
            String[] tags = new String[device.size()];
            for (int i = 0; i < device.size(); i++) tags[i] = device.get(i).toLanguageTag();
            mode = resolve(tags);
        }
        configuration.setLocales(LocaleList.forLanguageTags(mode));
        return context.createConfigurationContext(configuration);
    }
    private AppLanguage() { }
}
