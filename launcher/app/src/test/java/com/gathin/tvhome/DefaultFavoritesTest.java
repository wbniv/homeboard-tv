package com.gathin.tvhome;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
public class DefaultFavoritesTest {
    private final String youtube="app:com.google.android.youtube.tv/example.ActualLauncher";
    @Test public void freshDashboardSeedsResolvedYouTubeOnce() throws Exception {
        HomeLayout home=HomeLayout.parse(HomeLayout.firstRun().encode());
        assertTrue(home.seedFavoritesFromApps(List.of(youtube)));
        assertEquals(List.of(youtube),home.favorites);
        home.removeFavorite(youtube);
        assertFalse(HomeLayout.parse(home.encode()).seedFavoritesFromApps(List.of(youtube)));
        assertTrue(home.favorites.isEmpty());
    }
    @Test public void missingAppDoesNotCreateUnavailableFavorite() {
        HomeLayout home=HomeLayout.firstRun();home.seedFavoritesFromApps(List.of("app:other.app/Main"));
        assertTrue(home.favorites.isEmpty());assertFalse(home.seedFavoritesFromApps(List.of(youtube)));
    }
    @Test public void existingAndExplicitlyEmptyLayoutsRemainUnchanged() throws Exception {
        HomeLayout existing=new HomeLayout();existing.seedFavoritesFromApps(List.of(youtube));assertTrue(existing.favorites.isEmpty());
        HomeLayout pending=HomeLayout.firstRun();pending.addFavorite("app:my.app/Main");pending.seedFavoritesFromApps(List.of(youtube));
        assertEquals(List.of("app:my.app/Main"),pending.favorites);
    }
}
