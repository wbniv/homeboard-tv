package com.gathin.tvhome;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class FavoritesContainerTest {
    private static HomeLayout airOnly() { HomeLayout layout = new HomeLayout(); layout.addPanel(HomeLayout.AIR); return layout; }
    private static final String A="app:one/One",B="app:two/Two";
    @Test public void convertsSavedShortcutsInOrderWithoutChangingOtherSettings() throws Exception {
        HomeLayout s=airOnly();s.favorite(B);s.favorite(A);s.palette="forest";s.uiOpacityPercent=40;
        s.addPanel(HomeLayout.RAIN);s.rainSettings.get(HomeLayout.RAIN).name="Preserved";
        s.homePositions.put(A,new HomeLayout.HomePosition(2,1,1));
        s=HomeLayout.parse(s.encode());s.convertFavorites();
        assertEquals(Arrays.asList(B,A),s.favorites);assertFalse(s.widgets.contains(A));assertFalse(s.homePositions.containsKey(A));
        assertTrue(s.widgets.contains(HomeLayout.FAVORITES));assertEquals("forest",s.palette);assertEquals(40,s.uiOpacityPercent);
        assertEquals("Preserved",s.rainSettings.get(HomeLayout.RAIN).name);
        s.convertFavorites();assertEquals(Arrays.asList(B,A),s.favorites);
    }
    @Test public void rainAndFavoritesSharePageOneAcrossSupportedGridSizes(){
        for(int columns:new int[]{4,6,8,10,12})for(int rows:new int[]{2,3,4}){
            HomeLayout s=airOnly();s.addPanel(HomeLayout.CALENDAR);s.addPanel(HomeLayout.RAIN);
            s.homePositions.put(HomeLayout.RAIN,new HomeLayout.HomePosition(3,0,0));s.arrangeFirstPage(HomeLayout.RAIN,columns,rows,false);
            assertEquals(0,s.pageOf(HomeLayout.RAIN,s.widgets,columns,rows));assertEquals(0,s.pageOf(HomeLayout.FAVORITES,s.widgets,columns,rows));
            Set<String> occupied=new HashSet<>();for(HomeLayout.Cell c:s.cells(s.widgets,columns,rows))for(int y=c.row;y<c.row+c.height;y++)for(int x=c.column;x<c.column+c.width;x++)assertTrue(occupied.add(c.page+":"+y+":"+x));
            assertTrue(s.widgets.contains(HomeLayout.CALENDAR));assertTrue(s.widgets.contains(HomeLayout.AIR));
        }
    }
    @Test public void hundredsOfFavoritesCannotChangeOuterPagesOrSpans()throws Exception{
        HomeLayout s=airOnly();s.arrangeFirstPage(HomeLayout.RAIN,8,4,false);
        int pages=s.pageCount(s.widgets,8,4);String positions=s.encode();
        for(int i=0;i<100;i++){s.addFavorite("app:pkg"+i+"/Main");s.addFavorite("app:pkg"+i+"/Main");}
        assertEquals(100,s.favorites.size());assertEquals(pages,s.pageCount(s.widgets,8,4));assertEquals(0,s.pageOf(HomeLayout.RAIN,s.widgets,8,4));
        HomeLayout before=HomeLayout.parse(positions);assertEquals(before.panelSpan(HomeLayout.FAVORITES).width,s.panelSpan(HomeLayout.FAVORITES).width);
        assertEquals(before.homePositions.get(HomeLayout.RAIN).column,s.homePositions.get(HomeLayout.RAIN).column);
    }
    @Test public void removeReaddAndEmptyPreserveListAndRoundTrip()throws Exception{
        HomeLayout s=new HomeLayout();s.addFavorite(A);s.addFavorite(B);s.delete(HomeLayout.FAVORITES,true);
        s=HomeLayout.parse(s.encode());assertEquals(Arrays.asList(A,B),s.favorites);assertFalse(s.widgets.contains(HomeLayout.FAVORITES));
        s.addPanel(HomeLayout.FAVORITES);assertEquals(Arrays.asList(A,B),s.favorites);s.removeFavorite(A);s.removeFavorite(B);
        assertTrue(s.widgets.contains(HomeLayout.FAVORITES));assertTrue(s.favorites.isEmpty());
    }
    @Test public void packageRemovalAndActivityDedupUpdateFavorites(){
        HomeLayout s=new HomeLayout();s.addFavorite(A);s.addFavorite("app:one/Other");s.addFavorite(B);
        assertTrue(s.canonicalizeAppPackage("one",A));assertEquals(Arrays.asList(A,B),s.favorites);
        s.packageRemoved("one");assertEquals(Collections.singletonList(B),s.favorites);
    }
    @Test public void snapshotUndoRestoresPriorFavoriteOrderAndOuterLayout()throws Exception{
        HomeLayout s=airOnly();s.favorite(A);s.favorite(B);String before=s.encode();
        s.convertFavorites();s.arrangeFirstPage(HomeLayout.RAIN,8,4,false);HomeLayout undo=HomeLayout.parse(before);
        assertEquals(Arrays.asList(HomeLayout.AIR,A,B),undo.widgets);assertFalse(undo.favoritesContainer);
        assertTrue(s.favoritesContainer);assertTrue(s.widgets.contains(HomeLayout.RAIN));
    }
    @Test public void containerArtworkDoesNotReflowOuterPanels(){
        HomeLayout s=new HomeLayout();s.arrangeFirstPage(HomeLayout.RAIN,8,4,false);
        HomeLayout.HomePosition p=s.homePositions.get(HomeLayout.FAVORITES);int width=s.panelSpan(HomeLayout.FAVORITES).width;
        assertTrue(s.setHomeAppSize(HomeLayout.APP_BANNER,8,4,12,3));assertEquals(width,s.panelSpan(HomeLayout.FAVORITES).width);assertSame(p,s.homePositions.get(HomeLayout.FAVORITES));
    }
    @Test public void gridNavigationScrollsByRowAndNeverWrapsSideways(){
        assertEquals(7,FavoritesNavigation.next(3,10,4,false,0,1));assertEquals(-1,FavoritesNavigation.next(3,10,4,false,1,0));
        assertEquals(-1,FavoritesNavigation.next(4,10,4,false,-1,0));assertEquals(9,FavoritesNavigation.next(7,10,4,false,0,1));
        assertEquals(-1,FavoritesNavigation.next(9,10,4,false,0,1));assertEquals(5,FavoritesNavigation.next(9,10,4,false,0,-1));
    }
    @Test public void ribbonNavigationHasFiniteEndsAndVerticalExit(){
        assertEquals(4,FavoritesNavigation.next(3,10,1,true,1,0));assertEquals(-1,FavoritesNavigation.next(9,10,1,true,1,0));
        assertEquals(-1,FavoritesNavigation.next(0,10,1,true,-1,0));assertEquals(-1,FavoritesNavigation.next(3,10,1,true,0,1));
        assertEquals(-1,FavoritesNavigation.next(-1,0,1,true,1,0));
    }
}
