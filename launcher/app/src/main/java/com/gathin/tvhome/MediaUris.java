package com.gathin.tvhome;

import java.net.URI;

/** Keep Apple's published public media routes; try TLS for other HTTP providers. */
final class MediaUris {
    private MediaUris() { }
    static boolean appleHttp(String value) {
        URI uri = URI.create(value);
        if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1 && uri.getPort() != 80) return false;
        String host = uri.getHost(), path = uri.getRawPath();
        return host != null && path != null && ("sylvan.apple.com".equalsIgnoreCase(host) && path.startsWith("/Aerials/")
                || "a1.phobos.apple.com".equalsIgnoreCase(host) && path.startsWith("/us/r1000/"));
    }
    static String secure(String value) {
        URI uri = URI.create(value);
        String scheme = uri.getScheme();
        if ("content".equals(scheme) || "android.resource".equals(scheme)) {
            if (uri.getRawAuthority() == null) throw new IllegalArgumentException("Missing resource authority");
            return value;
        }
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            throw new IllegalArgumentException("Unsupported media scheme");
        if (uri.getHost() == null) throw new IllegalArgumentException("Missing media host");
        if ("https".equalsIgnoreCase(scheme)) return "https" + value.substring(value.indexOf(':'));
        if (appleHttp(value)) return value;
        String authority = uri.getRawAuthority();
        if (uri.getPort() == 80) authority = authority.substring(0, authority.length() - 3);
        return "https://" + authority + (uri.getRawPath() == null ? "" : uri.getRawPath())
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
                + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment());
    }
}
