package com.gathin.tvhome;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded public HTTPS requests. Identical concurrent forecasts share a single request. */
final class RainClient {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Map<String, CompletableFuture<String>> pending = new HashMap<>();
    static synchronized CompletableFuture<String> forecast(RainSettings settings) {
        String key = settings.locationKey();
        CompletableFuture<String> existing = pending.get(key);
        if (existing != null) return existing;
        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            try { return get("https://api.open-meteo.com/v1/forecast?latitude=" + settings.latitude + "&longitude=" + settings.longitude
                    + "&hourly=rain,showers,precipitation_probability,snowfall&timezone=auto&timeformat=unixtime&past_days=1&forecast_days=10"); }
            catch (Exception e) { throw new java.util.concurrent.CompletionException(e); }
        }, IO);
        pending.put(key, task);
        task.whenComplete((body, error) -> { synchronized (RainClient.class) { pending.remove(key, task); } });
        return task;
    }
    static CompletableFuture<String> search(String query, String language) {
        return CompletableFuture.supplyAsync(() -> {
            try { return get("https://geocoding-api.open-meteo.com/v1/search?count=5&language=" + language
                    + "&name=" + java.net.URLEncoder.encode(query, "UTF-8")); }
            catch (Exception e) { throw new java.util.concurrent.CompletionException(e); }
        }, IO);
    }
    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10_000); c.setReadTimeout(15_000); c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "HomeboardTV (https://github.com/wbniv/homeboard-tv)");
        try {
            if (c.getResponseCode() != 200) throw new java.io.IOException("Weather service unavailable");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = in.read(buffer)) != -1) {
                    if (out.size() + n > 512_000) throw new java.io.IOException("Response too large");
                    out.write(buffer, 0, n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally { c.disconnect(); }
    }
}
