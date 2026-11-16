package com.gathin.tvhome;

import java.util.Locale;

/** Fixed source destinations and their offline phone QR codes. */
enum AboutSource {
    HOMEBOARD("Homeboard TV source", "Project repository", "https://github.com/wbniv/homeboard-tv", R.drawable.qr_about_homeboard),
    CLEANYOURTV("CleanYourTV source", "Launcher foundation · MIT", "https://github.com/Gathin23/cleanyourtv", R.drawable.qr_about_cleanyourtv),
    OUTFIT("Outfit font", "Font source · SIL Open Font License", "https://github.com/google/fonts/tree/main/ofl/outfit", R.drawable.qr_about_outfit),
    ANDROIDX("AndroidX", "Library source · Apache License 2.0", "https://github.com/androidx/androidx", R.drawable.qr_about_androidx);

    final String title, detail, url, qrName;
    final int qrResource;
    AboutSource(String title, String detail, String url, int qrResource) {
        this.title = title; this.detail = detail; this.url = url; this.qrResource = qrResource;
        qrName = "qr_about_" + name().toLowerCase(Locale.ROOT);
    }
}
