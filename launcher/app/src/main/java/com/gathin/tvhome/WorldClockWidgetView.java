package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import java.time.Instant;
import java.util.List;

final class WorldClockWidgetView extends View {
    private final WorldClockSettings settings;
    private final WorldClockCards cards;
    private int opacity;
    private boolean header;
    private java.util.function.Consumer<String> cityClick;
    private List<WorldClockSettings.Place> shown=java.util.Collections.emptyList();
    private float cardWidth;
    private String tappedPlace;
    void asHeader(java.util.function.Consumer<String> click){header=true;cityClick=click;}
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        if(!header)return super.onTouchEvent(event);
        if(event.getAction()==android.view.MotionEvent.ACTION_UP){int index=(int)(event.getX()/Math.max(1,cardWidth));tappedPlace=index>=0&&index<shown.size()?shown.get(index).id:settings.primary;performClick();}
        return true;
    }
    @Override public boolean performClick(){super.performClick();if(header&&cityClick!=null){String place=tappedPlace==null?settings.primary:tappedPlace;tappedPlace=null;cityClick.accept(place);}return true;}

    private final PaletteColors palette;
    private final WorldClockTicker ticker;
    WorldClockWidgetView(Context context,WorldClockSettings settings,PaletteColors palette,int opacity) {
        super(context); this.settings=settings; this.opacity=opacity; this.palette=palette; cards=new WorldClockCards(context,settings,palette);
        ticker=new WorldClockTicker(context,this::refresh); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); refresh();
    }
    private void refresh() {
        StringBuilder label=new StringBuilder(settings.title.isEmpty()?getContext().getString(R.string.hb_wc_title):settings.title);
        Instant now=Instant.now(); for(WorldClockSettings.Place p:settings.places) label.append("\n").append(WorldClockLabels.detail(getContext(),settings,p,now,false,null));
        setContentDescription(label);invalidate();
    }
    void start() { ticker.start(false); }
    void stop() { ticker.stop(); }
    void setFillOpacity(int value) { opacity=value;invalidate(); }
    @Override protected void onDetachedFromWindow() { stop();super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); float density=getResources().getDisplayMetrics().density,scale=getResources().getConfiguration().fontScale;
        int save=c.save(); c.scale(density,density);
        if(header){drawHeader(c,density,scale);c.restoreToCount(save);return;}
        float width=getWidth()/density,height=getHeight()/density,header=height>=110?22*scale:0;
        if(header>0) cards.text(c,settings.title.isEmpty()?getContext().getString(R.string.hb_wc_title):settings.title,8,16*scale,12*scale,palette.text,width-16,false);
        int columns=Math.max(1,(int)(width/(140*scale))),rows=Math.max(1,(int)((height-header)/(88*scale)));
        List<WorldClockSettings.Place> places=WorldClockModel.ordered(settings);
        int capacity=columns*rows;if(capacity==1) places=java.util.Collections.singletonList(settings.primary());
        float footer=settings.places.size()>capacity?16*scale:0;
        int count=Math.min(places.size(),capacity);Instant now=Instant.now();float cellWidth=width/columns,cellHeight=(height-header-footer)/rows;
        for(int i=0;i<count;i++) { int cell=c.save();c.translate(i%columns*cellWidth,i/columns*cellHeight+header);
            cards.draw(c,settings,places.get(i),now,cellWidth-3,cellHeight-3,false,opacity);c.restoreToCount(cell); }
        if(footer>0) cards.text(c,"+"+(settings.places.size()-capacity),width-34*scale,height-4,10*scale,palette.text,32*scale,false);
        if(hasFocus()) {
            android.graphics.Paint border=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);border.setColor(palette.text);border.setStyle(android.graphics.Paint.Style.STROKE);border.setStrokeWidth(2);
            c.drawRoundRect(1,1,width-1,height-1,4,4,border);
        }
        c.restoreToCount(save);
    }
    private void drawHeader(Canvas c,float density,float scale){
        float width=getWidth()/density,height=getHeight()/density;
        List<WorldClockSettings.Place> ordered=WorldClockModel.ordered(settings);
        int capacity=Math.max(1,(int)(width/(112*scale))),count=Math.min(ordered.size(),capacity);
        shown=new java.util.ArrayList<>(ordered.subList(0,count));
        if(!shown.contains(settings.primary()))shown.set(count-1,settings.primary());
        boolean overflow=count<ordered.size();float extra=overflow?36*scale:0,cell=(width-extra)/count;
        cardWidth=cell*density;Instant now=Instant.now();
        for(int i=0;i<count;i++){int save=c.save();c.translate(i*cell,0);cards.header(c,settings,shown.get(i),now,cell-4,height,opacity);c.restoreToCount(save);}
        if(overflow)cards.text(c,"+"+(ordered.size()-count),width-extra+2,height*.6f,13*scale,palette.text,extra-2,false);
        if(hasFocus()){android.graphics.Paint border=new android.graphics.Paint();border.setColor(palette.text);border.setStyle(android.graphics.Paint.Style.STROKE);border.setStrokeWidth(2);c.drawRect(1,1,width-1,height-1,border);}
    }
    @Override protected void onFocusChanged(boolean gain,int direction,android.graphics.Rect old) { super.onFocusChanged(gain,direction,old);invalidate(); }
}
