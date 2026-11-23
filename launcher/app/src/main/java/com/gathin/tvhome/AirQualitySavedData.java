package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.*;
import java.util.*;
import java.util.function.BiFunction;

/** Read-only startup snapshot, including cache parsing; no Android/UI or preference writes. */
final class AirQualitySavedData {
    static final String[] STATIONS = {"114", "119", "122", "70", "65"};
    final JSONObject configuration;
    final AirQualityReading reading;
    final AirQualityHistory history;
    final ZoneId zone;
    final String stationFeed;
    final int selected;
    private AirQualitySavedData(JSONObject configuration, AirQualityReading reading,
            AirQualityHistory history, ZoneId zone, String stationFeed, int selected) {
        this.configuration = configuration; this.reading = reading; this.history = history;
        this.zone = zone; this.stationFeed = stationFeed; this.selected = selected;
    }
    static AirQualitySavedData read(JSONObject saved, BiFunction<String,String,String> read, long now) throws Exception {
        JSONObject config = new JSONObject(saved.toString());
        int selected = 0;
        for (int i = 0; i < STATIONS.length; i++) if (STATIONS[i].equals(config.optString("station"))) selected = i;
        boolean model = "model".equals(config.optString("source", "model"));
        double lat = config.optDouble("latitude", Double.NaN), lon = config.optDouble("longitude", Double.NaN);
        String suffix = Double.isFinite(lat) && Double.isFinite(lon)
                ? String.format(Locale.US,"%.2f_%.2f",lat,lon) : "unlocated";
        String historyKey = model ? "history-model-" + suffix : "history-" + STATIONS[selected];
        AirQualityHistory history;
        try { history = AirQualityHistory.parse(read.apply(historyKey,"[]")); }
        catch (Exception invalid) { history = new AirQualityHistory(); }
        AirQualityReading reading = null;
        ZoneId zone = java.time.ZoneOffset.UTC;
        String stationFeed = read.apply("feed", "");
        try {
            if (model) {
                String feed = read.apply("feed-model-" + suffix, "");
                if (!feed.isEmpty()) {
                    ModelData parsed = parseModel(feed, lat, lon, now);
                    reading = parsed.current; history.addAll(parsed.samples); zone = parsed.zone;
                }
            } else {
                reading = AirQualityReading.parse(stationFeed, STATIONS[selected], now);
                history.add(reading);
            }
        } catch (Exception invalid) { /* A missing cache is not a zero reading; keep valid saved history. */ }
        return new AirQualitySavedData(config,reading,history,zone,stationFeed,selected);
    }
    static final class ModelData {
        AirQualityReading current;
        final List<AirQualityReading> samples = new ArrayList<>();
        double gridLatitude, gridLongitude;
        ZoneId zone;
    }

    static ModelData parseModel(String json, double latitude, double longitude, long now) throws Exception {
        JSONObject root = new JSONObject(json);
        if (root.optBoolean("error", false)) throw new IllegalStateException(root.optString("reason", "Model unavailable"));
        ModelData result = new ModelData();
        result.zone=ZoneId.of(root.optString("timezone","UTC"));
        result.gridLatitude = root.optDouble("latitude", latitude);
        result.gridLongitude = root.optDouble("longitude", longitude);
        JSONObject hourly = root.optJSONObject("hourly");
        if (hourly != null) {
            JSONArray times = hourly.optJSONArray("time"), values = hourly.optJSONArray("pm2_5");
            long cutoff = now + 60 * 60_000L;
            if (times != null && values != null) for (int i = 0; i < Math.min(times.length(), values.length()); i++) {
                double pm25 = values.optDouble(i, Double.NaN);
                if (!Double.isFinite(pm25) || pm25 < 0) continue;
                long time = modelTime(times.optString(i), root.optString("timezone", "UTC"));
                if (time <= cutoff) result.samples.add(AirQualityReading.modeled(pm25, time));
            }
        }
        JSONObject current = root.optJSONObject("current");
        if (current != null) {
            double pm25 = current.optDouble("pm2_5", Double.NaN);
            if (Double.isFinite(pm25) && pm25 >= 0)
                result.current = AirQualityReading.modeled(pm25, modelTime(current.optString("time"), root.optString("timezone", "UTC")));
        }
        if (result.current == null && !result.samples.isEmpty()) result.current = result.samples.get(result.samples.size() - 1);
        if (result.current == null) throw new IllegalStateException("No model PM2.5 value");
        return result;
    }

    private static long modelTime(String value, String timezone) {
        try { return OffsetDateTime.parse(value).toInstant().toEpochMilli(); }
        catch (java.time.DateTimeException ignored) {
            return LocalDateTime.parse(value).atZone(ZoneId.of(timezone)).toInstant().toEpochMilli();
        }
    }
}
