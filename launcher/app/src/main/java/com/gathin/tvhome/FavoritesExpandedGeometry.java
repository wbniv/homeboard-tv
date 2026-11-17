package com.gathin.tvhome;

/** Dense banner capacity uses the actual viewport and respects larger text. */
final class FavoritesExpandedGeometry {
    final int columns,rows;
    final float cellHeight;
    FavoritesExpandedGeometry(float width,float height,float fontScale){
        float scale=Math.max(1,fontScale);
        columns=Math.max(1,(int)(Math.max(1,width)/(140*scale)));
        rows=Math.max(1,(int)(Math.max(1,height)/(100*scale)));
        cellHeight=Math.max(1,height)/rows;
    }
}
