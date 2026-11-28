package com.gathin.tvhome;

import org.json.*;

/** Host-owned identity and provider bounds in dp; never accepted from a browser. */
final class AndroidWidgetSpec {
    final int id,minWidth,minHeight,minResizeWidth,minResizeHeight,maxWidth,maxHeight,resizeMode;
    final String provider,label;
    AndroidWidgetSpec(int id,String provider,String label,int minWidth,int minHeight,int minResizeWidth,int minResizeHeight,int maxWidth,int maxHeight,int resizeMode){
        if(id<=0||provider==null||!provider.matches("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+")||label==null||label.length()>512)throw new IllegalArgumentException("Invalid Android widget identity");
        this.id=id;this.provider=provider;this.label=label;this.minWidth=Math.max(1,minWidth);this.minHeight=Math.max(1,minHeight);
        this.minResizeWidth=Math.max(1,minResizeWidth);this.minResizeHeight=Math.max(1,minResizeHeight);
        this.maxWidth=maxWidth;this.maxHeight=maxHeight;this.resizeMode=resizeMode&3;
    }
    static AndroidWidgetSpec parse(JSONObject j)throws JSONException {
        try{return new AndroidWidgetSpec(j.getInt("id"),j.getString("provider"),j.getString("label"),j.getInt("minWidth"),j.getInt("minHeight"),j.getInt("minResizeWidth"),j.getInt("minResizeHeight"),j.optInt("maxWidth"),j.optInt("maxHeight"),j.getInt("resizeMode"));}
        catch(IllegalArgumentException e){throw new JSONException(e.getMessage());}
    }
    JSONObject json(){try{return new JSONObject().put("id",id).put("provider",provider).put("label",label).put("minWidth",minWidth).put("minHeight",minHeight).put("minResizeWidth",minResizeWidth).put("minResizeHeight",minResizeHeight).put("maxWidth",maxWidth).put("maxHeight",maxHeight).put("resizeMode",resizeMode);}catch(JSONException e){throw new IllegalStateException(e);}}
    int minimumWidth(){return (resizeMode&1)!=0?minResizeWidth:minWidth;}
    int minimumHeight(){return (resizeMode&2)!=0?minResizeHeight:minHeight;}
    boolean fits(int width,int height){return width>=minimumWidth()&&height>=minimumHeight();}
    boolean legalResize(int beforeW,int beforeH,int w,int h){return ((resizeMode&1)!=0||w==beforeW)&&((resizeMode&2)!=0||h==beforeH);}
    static boolean key(String key){return key!=null&&key.matches("widget:android:[a-f0-9-]{36}");}
    /** The provider's widget name for a removal banner; {@code fallback} when the record is missing or unreadable. */
    static String removalLabel(HomeLayout layout,String key,String fallback){AndroidWidgetSpec s=layout==null?null:layout.androidWidget(key);return s==null?fallback:s.label;}
}
