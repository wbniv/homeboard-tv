package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** AirBKK station PM2.5 is a measured 24-hour average, not an instantaneous value. */
final class AirQualityReading {
    static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    static final DateTimeFormatter SOURCE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(java.time.format.ResolverStyle.STRICT);
    final double pm25;
    final long timeMillis;
    final boolean modelEstimate;
    final Condition conditionOverride;

    enum Condition { VERY_GOOD, GOOD, MODERATE, HIGH, VERY_HIGH }

    /** Thailand PCD AQI bands for 24-hour PM2.5, announced for use from 2023-06-01. */
    Condition condition() {
        if (conditionOverride != null) return conditionOverride;
        if (pm25 <= 15.0) return Condition.VERY_GOOD;
        if (pm25 <= 25.0) return Condition.GOOD;
        if (pm25 <= 37.5) return Condition.MODERATE;
        if (pm25 <= 75.0) return Condition.HIGH;
        return Condition.VERY_HIGH;
    }

    String conditionLabel() {
        if (modelEstimate) return "MODEL · " + conditionText();
        switch (condition()) {
            case VERY_GOOD: return "VERY GOOD";
            case GOOD: return "GOOD";
            case MODERATE: return "MODERATE";
            case HIGH: return "▲ HIGH · limit outdoor time";
            default: return "▲ VERY HIGH · avoid outdoor activity";
        }
    }

    private String conditionText() {
        switch (condition()) {
            case VERY_GOOD: return "GOOD";
            case GOOD: return "FAIR";
            case MODERATE: return "MODERATE";
            case HIGH: return "▲ POOR · limit outdoor time";
            default: return "▲ VERY POOR · avoid outdoor activity";
        }
    }

    AirQualityReading(double pm25, long timeMillis) {
        this(pm25, timeMillis, false, null);
    }

    AirQualityReading(double pm25, long timeMillis, boolean modelEstimate, Condition conditionOverride) {
        this.pm25 = pm25; this.timeMillis = timeMillis;
        this.modelEstimate = modelEstimate; this.conditionOverride = conditionOverride;
    }

    static AirQualityReading modeled(double pm25, long timeMillis) {
        Condition condition = pm25 <= 5 ? Condition.VERY_GOOD : pm25 <= 15 ? Condition.GOOD
                : pm25 <= 50 ? Condition.MODERATE : pm25 <= 90 ? Condition.HIGH : Condition.VERY_HIGH;
        return new AirQualityReading(pm25, timeMillis, true, condition);
    }

    static AirQualityReading parse(String json, String stationId, long now) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!"Success".equals(root.optString("status"))) throw new JSONException("Feed unavailable");
        JSONArray stations = root.getJSONArray("message");
        for (int i = 0; i < stations.length(); i++) {
            JSONObject station = stations.getJSONObject(i);
            if (!stationId.equals(station.optString("MeasIndex"))) continue;
            double value = station.optDouble("PM2.5", Double.NaN);
            if (!Double.isFinite(value) || value < 0) throw new JSONException("Missing PM2.5");
            try {
                long time = LocalDateTime.parse(station.getString("DateTime"), SOURCE_TIME)
                        .atZone(BANGKOK).toInstant().toEpochMilli();
                if (time > now + 10 * 60_000L) throw new JSONException("Future station timestamp");
                return new AirQualityReading(value, time);
            } catch (java.time.DateTimeException e) {
                throw new JSONException("Invalid station timestamp");
            }
        }
        throw new JSONException("Station missing");
    }

    boolean stale(long now) { return now - timeMillis > 3 * 60 * 60_000L; }
}
