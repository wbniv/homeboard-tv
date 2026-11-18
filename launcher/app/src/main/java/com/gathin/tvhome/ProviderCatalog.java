package com.gathin.tvhome;

import java.util.List;

/** Reviewed links only. Never downloads, installs or activates a provider. */
final class ProviderCatalog {
    static final class Entry {
        final String id, name, packageName, url;
        final int detail;
        final boolean play, excluded;
        Entry(String id, String name, String packageName, String url, int detail, boolean play, boolean excluded) {
            this.id=id; this.name=name; this.packageName=packageName; this.url=url;
            this.detail=detail; this.play=play; this.excluded=excluded;
            if (excluded && !url.isEmpty()) throw new IllegalArgumentException("Excluded provider cannot have an install link");
            if (!url.isEmpty() && !url.startsWith(play ? "https://play.google.com/store/apps/details?id="+packageName : "https://github.com/"))
                throw new IllegalArgumentException("Unreviewed provider link");
        }
    }
    static final List<Entry> ENTRIES=List.of(
        play("overflight", "Overflight", "tv.projectivy.plugin.wallpaperprovider.overflight", R.string.hb_provider_overflight),
        play("aerial-views", "Aerial Views", "com.neilturner.aerialviews", R.string.hb_provider_aerial),
        play("movie-art", "Movie Art", "com.danielkorgel.projectivy.plugin.cinemaglow", R.string.hb_provider_movie),
        community("daily-photos", "Daily Photos", "tv.projectivy.plugin.wallpaperprovider.photooftheday", "spocky/projectivy-plugin-wallpaper-bingwallpaper/releases", R.string.hb_provider_daily),
        community("core-motion", "Core Motion", "tv.corebuilds.motion", "brevityA/CoreBuildsApps/releases/tag/motion", R.string.hb_provider_motion),
        community("sofadrop", "Sofadrop", "com.rfmarcos.sofadrop", "rfmarcos/sofadrop#-google-play-test", R.string.hb_provider_sofadrop),
        community("booru", "Booru Wallpaper", "nl.kurocon.plugin.wallpaperprovider.booru", "Kurocon/projectivy-booru-wallpaper-provider/releases", R.string.hb_provider_booru),
        new Entry("weather", "Weather Wallpaper v2.11", "tv.projectivy.plugin.wallpaperprovider.weather", "", R.string.hb_provider_weather, false, true)
    );
    private static Entry play(String id,String name,String pkg,int detail) {
        return new Entry(id,name,pkg,"https://play.google.com/store/apps/details?id="+pkg,detail,true,false);
    }
    private static Entry community(String id,String name,String pkg,String path,int detail) {
        return new Entry(id,name,pkg,"https://github.com/"+path,detail,false,false);
    }
}
