package net.biohack.homeboard.wallpaperfixture;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.util.Arrays;
import java.util.List;
import tv.projectivy.plugin.wallpaperprovider.api.*;
public final class ProviderService extends Service {
    private String prefs = "{}";
    @Override public IBinder onBind(Intent intent) { return new IWallpaperProviderService.Stub() {
        @Override public List<Wallpaper> getWallpapers(Event event) {
            if (!"{}".equals(prefs)) {
                String path; int type;
                switch (prefs) {
                    case "image": path = "raw/test_image"; type = 0; break;
                    case "drawable": path = "drawable/test_shape"; type = 1; break;
                    case "animated": path = "drawable/test_animation"; type = 2; break;
                    case "lottie": path = "raw/gradient"; type = 3; break;
                    case "video": path = "raw/light"; type = 4; break;
                    case "empty": return java.util.Collections.emptyList();
                    case "hang": try { Thread.sleep(20_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } return java.util.Collections.emptyList();
                    default: path = "raw/missing"; type = 0;
                }
                return Arrays.asList(new Wallpaper("android.resource://" + getPackageName() + "/" + path, type, 1, prefs, null, "Homeboard fixture / Projectivy sample media", null));
            }
            String color = event instanceof Event.CardFocused ? "#32676c"
                    : event instanceof Event.LauncherIdleModeChanged && ((Event.LauncherIdleModeChanged) event).isIdle() ? "#37294f" : "#173f68";
            return Arrays.asList(new Wallpaper(color, 5, 1, "Fixture color", null, "Homeboard TV", null),
                    new Wallpaper("#4b3256", 5, 1, "Second color", null, "Homeboard TV", null));
        }
        @Override public String getPreferences() { return prefs; }
        @Override public void setPreferences(String value) { prefs = value; }
    }; }
}
