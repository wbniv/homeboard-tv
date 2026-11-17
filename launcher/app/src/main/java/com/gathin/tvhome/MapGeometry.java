package com.gathin.tvhome;
/** Normalized Web Mercator; tile and marker coordinates use the same projection. */
final class MapGeometry {
    static double x(double longitude){return (longitude+180)/360;}
    static double y(double latitude){double r=Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,latitude)));return (1-Math.log(Math.tan(r)+1/Math.cos(r))/Math.PI)/2;}
    static double latitude(double y){return Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*y))));}
    static double longitude(double x){return x*360-180;}
    static double wrapped(double dx){return dx-Math.floor(dx+.5);}
}
