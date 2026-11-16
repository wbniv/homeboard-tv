package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Amount/probability remain distinct. Provider timestamps END their preceding-hour intervals. */
final class RainForecast {
    static final long HOUR = 3600;
    static final class Hour {
        final long start;
        final double mm, chance, snowCm;
        Hour(long start, double mm, double chance, double snowCm) {
            this.start = start; this.mm = mm; this.chance = chance; this.snowCm = snowCm;
        }
        boolean valid() { return Double.isFinite(mm) && Double.isFinite(chance) && Double.isFinite(snowCm); }
    }
    static final class Period {
        final long start, end;
        final List<Hour> hours;
        final boolean complete;
        final double mm, chance, snowCm;
        Period(long start, long end, Map<Long, Hour> samples) {
            this.start = start; this.end = end;
            List<Hour> list = new ArrayList<>();
            boolean valid = true; double amount = 0, peak = 0, snow = 0;
            for (long t = start; t < end; t += HOUR) {
                Hour h = samples.get(t);
                if (h == null) h = new Hour(t, Double.NaN, Double.NaN, Double.NaN);
                list.add(h); valid &= h.valid();
                if (h.valid()) { amount += h.mm; peak = Math.max(peak, h.chance); snow += h.snowCm; }
            }
            hours = Collections.unmodifiableList(list); complete = valid && !list.isEmpty();
            mm = complete ? amount : Double.NaN; chance = complete ? peak : Double.NaN;
            snowCm = complete ? snow : Double.NaN;
        }
        Hour firstWet() { for (Hour h : hours) if (h.valid() && h.mm >= .1) return h; return null; }
    }
    final ZoneId zone;
    private final Map<Long, Hour> samples;
    private RainForecast(ZoneId zone, Map<Long, Hour> samples) { this.zone = zone; this.samples = Collections.unmodifiableMap(samples); }
    static RainForecast parse(String body) throws JSONException {
        JSONObject root = new JSONObject(body);
        if (root.optBoolean("error")) throw new JSONException("Forecast error");
        ZoneId zone;
        try { zone = ZoneId.of(root.getString("timezone")); } catch (Exception e) { throw new JSONException("Invalid time zone"); }
        JSONObject units = root.getJSONObject("hourly_units");
        if (!"unixtime".equals(units.optString("time")) || !"mm".equals(units.optString("rain"))
                || !"mm".equals(units.optString("showers")) || !"%".equals(units.optString("precipitation_probability"))
                || !"cm".equals(units.optString("snowfall"))) throw new JSONException("Unexpected forecast units");
        JSONObject data = root.getJSONObject("hourly");
        JSONArray times = data.getJSONArray("time"), rain = data.getJSONArray("rain"), showers = data.getJSONArray("showers"),
                probability = data.getJSONArray("precipitation_probability"), snow = data.getJSONArray("snowfall");
        int n = times.length();
        if (n < 1 || n > 400 || rain.length() != n || showers.length() != n || probability.length() != n || snow.length() != n)
            throw new JSONException("Invalid hourly arrays");
        Map<Long, Hour> samples = new TreeMap<>(); long previous = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            long end = times.getLong(i);
            if (end <= previous || end < 0 || (previous != Long.MIN_VALUE && end - previous != HOUR))
                throw new JSONException("Invalid hourly timestamps");
            previous = end;
            double r = number(rain, i, 1000), s = number(showers, i, 1000);
            samples.put(end - HOUR, new Hour(end - HOUR, r + s, number(probability, i, 100), number(snow, i, 1000)));
        }
        return new RainForecast(zone, samples);
    }
    private static double number(JSONArray a, int i, double max) {
        if (a.isNull(i) || !(a.opt(i) instanceof Number)) return Double.NaN;
        double v = a.optDouble(i, Double.NaN);
        return Double.isFinite(v) && v >= 0 && v <= max ? v : Double.NaN;
    }
    long windowStart(Instant now) { return now.atZone(zone).truncatedTo(ChronoUnit.HOURS).plusHours(1).toEpochSecond(); }
    Period hourly(Instant now, int hours) {
        long start = windowStart(now); return new Period(start, start + hours * HOUR, samples);
    }
    List<Period> following(Instant now, int hours, int days) {
        List<Period> result = new ArrayList<>(); long start = windowStart(now) + hours * HOUR;
        for (int i = 0; i < days; i++) {
            ZonedDateTime local = Instant.ofEpochSecond(start).atZone(zone);
            long end = local.toLocalDate().plusDays(1).atStartOfDay(zone).toEpochSecond();
            result.add(new Period(start, end, samples)); start = end;
        }
        return result;
    }
}
