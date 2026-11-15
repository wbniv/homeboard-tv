package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Fixed-capacity circular history of distinct hourly station samples. */
final class AirQualityHistory {
    static final int HOURS_24 = 24;
    static final int DAYS_30 = 30;
    static final int LIMIT = HOURS_24 * DAYS_30;
    private final AirQualityReading[] samples = new AirQualityReading[LIMIT];
    private int start;
    private int size;

    void add(AirQualityReading reading) {
        int low = 0, high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (at(mid).timeMillis < reading.timeMillis) low = mid + 1;
            else high = mid;
        }
        if (low < size && at(low).timeMillis == reading.timeMillis) {
            put(low, reading);
            return;
        }
        if (size == LIMIT) {
            if (low == 0) return; // An older backfill point cannot displace a newer sample.
            start = (start + 1) % LIMIT;
            size--;
            low--;
        }
        for (int i = size; i > low; i--) put(i, at(i - 1));
        put(low, reading);
        size++;
    }

    void addAll(List<AirQualityReading> values) {
        for (AirQualityReading reading : values) add(reading);
    }

    List<AirQualityReading> readings() {
        List<AirQualityReading> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(at(i));
        return result;
    }

    private AirQualityReading at(int index) { return samples[(start + index) % LIMIT]; }
    private void put(int index, AirQualityReading reading) { samples[(start + index) % LIMIT] = reading; }

    String encode() throws JSONException {
        JSONArray out = new JSONArray();
        for (AirQualityReading reading : readings()) {
            JSONObject sample = new JSONObject().put("time", reading.timeMillis).put("pm25", reading.pm25)
                    .put("model", reading.modelEstimate);
            if (reading.conditionOverride != null) sample.put("condition", reading.conditionOverride.name());
            out.put(sample);
        }
        return out.toString();
    }

    static AirQualityHistory parse(String json) throws JSONException {
        AirQualityHistory history = new AirQualityHistory();
        JSONArray values = new JSONArray(json);
        for (int i = 0; i < values.length(); i++) {
            JSONObject entry = values.getJSONObject(i);
            double pm25 = entry.getDouble("pm25");
            long time = entry.getLong("time");
            if (Double.isFinite(pm25) && pm25 >= 0 && time > 0) {
                AirQualityReading reading;
                if (entry.optBoolean("model", false)) {
                    AirQualityReading.Condition condition = null;
                    try { condition = AirQualityReading.Condition.valueOf(entry.optString("condition", "")); }
                    catch (IllegalArgumentException ignored) { }
                    reading = condition == null ? AirQualityReading.modeled(pm25, time)
                            : new AirQualityReading(pm25, time, true, condition);
                } else reading = new AirQualityReading(pm25, time);
                history.add(reading);
            }
        }
        return history;
    }
}
