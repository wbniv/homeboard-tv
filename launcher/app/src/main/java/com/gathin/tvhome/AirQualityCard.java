package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small direct-feed integration; no dashboard server or household credentials. */
final class AirQualityCard {
    static final String ENDPOINT = "https://official.airbkk.com/airbkk/Api";
    static final String MODEL_ENDPOINT = "https://air-quality-api.open-meteo.com/v1/air-quality";
    private static final String[] IDS = {"114", "119", "122", "70", "65"};
    private static final String[] NAMES = {"Lumpini Park", "Benchasiri Park", "Benjakitti Park",
            "Watthana · Ekkamai Road", "Pathum Wan · Samyan Mitrtown"};
    private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.US);
    private static final DateTimeFormatter MOVING_AVERAGE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm", Locale.US);
    private static final String HISTORY_ENDPOINT = "https://official.airbkk.com/airbkk/Report/getData";
    private final Activity activity;
    interface Menus { void show(String title, String[] labels, java.util.function.IntConsumer action); }
    interface LocationProvider { void request(java.util.function.Consumer<Location> callback); }
    private final Menus menus;
    private final LocationProvider locations;
    private final String widgetKey;
    private TextView value, detail, stationLabel;
    private TextView conditionLabel;
    private AirQualityTrendView trend;
    private View tile;
    private boolean tileFocused;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile HttpURLConnection connection;
    private boolean active, closed, loading, failed;
    private long lastAttempt;
    private int selected;
    private String source, locationMode, locationName;
    private double latitude = Double.NaN, longitude = Double.NaN;
    private boolean locationRequested;
    private AirQualityReading reading;
    private AirQualityHistory history = new AirQualityHistory();
    private PaletteColors palette = PaletteColors.from(0xff101010);

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            refresh(false);
            render();
            handler.postDelayed(this, 60_000L);
        }
    };

    AirQualityCard(Activity activity, Menus menus, String widgetKey, LocationProvider locations) {
        this.activity = activity;
        this.menus = menus;
        this.widgetKey = widgetKey; this.locations = locations;
        String preferenceName = "air-quality-widget-" + widgetKey.replaceAll("[^A-Za-z0-9_-]", "_");
        prefs = activity.getSharedPreferences(preferenceName, Activity.MODE_PRIVATE);
        source = prefs.getString("source", "model");
        locationMode = prefs.getString("location-mode", "auto");
        locationName = prefs.getString("location-name", "Near me");
        if (prefs.contains("latitude") && prefs.contains("longitude")) {
            latitude = Double.longBitsToDouble(prefs.getLong("latitude", 0));
            longitude = Double.longBitsToDouble(prefs.getLong("longitude", 0));
        }
        String saved = prefs.getString("station", IDS[0]);
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(saved)) selected = i;
        readHistory();
        readCache();
    }

    void bind(View card) {
        tile = card;
        value = card.findViewById(R.id.airQualityValue);
        detail = card.findViewById(R.id.airQualityDetail);
        stationLabel = card.findViewById(R.id.airQualityStation);
        conditionLabel = card.findViewById(R.id.airQualityCondition);
        trend = card.findViewById(R.id.airQualityTrend);
        render();
    }
    void setPalette(PaletteColors colors) { palette = colors; applyTileBackground(); render(); }
    void unbind() { tile = null; tileFocused = false; value = null; detail = null; stationLabel = null; conditionLabel = null; trend = null; }
    void setTileFocused(boolean focused) { tileFocused = focused; applyTileBackground(); }

    void start() { active = true; handler.removeCallbacks(tick); handler.post(tick); }
    void stop() {
        active = false;
        handler.removeCallbacks(tick);

    }
    void close() {
        stop(); closed = true;
        HttpURLConnection c = connection;
        if (c != null) c.disconnect();
        worker.shutdownNow();
        handler.removeCallbacksAndMessages(null);
    }

    private void readCache() {
        reading = null;
        if ("model".equals(source)) {
            try {
                String cached = prefs.getString("feed-model-" + modelSuffix(), "");
                if (!cached.isEmpty()) {
                    ModelData model = parseModel(cached);
                    reading = model.current;
                    history.addAll(model.samples);
                }
            } catch (Exception ignored) { /* No cached model estimate. */ }
            return;
        }
        try {
            reading = AirQualityReading.parse(prefs.getString("feed", ""), IDS[selected], System.currentTimeMillis());
            history.add(reading);
            prefs.edit().putString("history-" + IDS[selected], history.encode()).apply();
        }
        catch (Exception ignored) { /* An absent/invalid cache is not a zero reading. */ }
    }

    void showActions() {
        menus.show("Air quality · " + locationLabel(), new String[]{"Refresh reading", "Configure location and source"}, which -> {
            if (which == 0) refresh(true);
            else configureWidget();
        });
    }
    void configureWidget() {
        menus.show("Air quality location · each widget is independent",
                new String[]{"Nearest model grid · use this device", "Enter coordinates for a model grid", "Bangkok monitoring station"}, choice -> {
                    if (choice == 0) useCurrentLocation();
                    else if (choice == 1) enterCoordinates();
                    else { source = "airbkk"; saveConfiguration(); chooseStation(); }
                });
    }
    private void useCurrentLocation() {
        source = "model"; locationMode = "auto"; latitude = Double.NaN; longitude = Double.NaN;
        locationName = "Near me"; locationRequested = false; saveConfiguration();
        reading = null; readHistory(); readCache(); render();
        ensureModelLocation();
    }
    private void enterCoordinates() {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(4));
        TextView explanation = new TextView(activity);
        explanation.setText("Choose the forecast grid near this location.");
        explanation.setTextColor(palette.muted);
        form.addView(explanation, new LinearLayout.LayoutParams(-1, -2));
        EditText latitudeInput = coordinateField(form, "Latitude", "−90 to 90");
        EditText longitudeInput = coordinateField(form, "Longitude", "−180 to 180");
        if (Double.isFinite(latitude)) latitudeInput.setText(String.format(Locale.US, "%.5f", latitude));
        if (Double.isFinite(longitude)) longitudeInput.setText(String.format(Locale.US, "%.5f", longitude));

        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Model grid location")
                .setView(form).setNegativeButton("Cancel", (d, which) -> {})
                .setPositiveButton("Save", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                double lat = Double.parseDouble(latitudeInput.getText().toString().trim());
                double lon = Double.parseDouble(longitudeInput.getText().toString().trim());
                if (!Double.isFinite(lat) || !Double.isFinite(lon)
                        || lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new IllegalArgumentException();
                source = "model"; locationMode = "manual"; latitude = lat; longitude = lon;
                locationName = String.format(Locale.US, "%.2f°, %.2f°", lat, lon);
                saveConfiguration(); reading = null; readHistory(); readCache(); render(); refresh(true);
                dialog.dismiss();
            } catch (Exception invalid) {
                latitudeInput.setError("Check both coordinates");
            }
        }));
        dialog.show();
    }

    private EditText coordinateField(LinearLayout form, String label, String hint) {
        TextView caption = new TextView(activity);
        caption.setText(label);
        caption.setTextColor(palette.text);
        caption.setTextSize(15);
        LinearLayout.LayoutParams captionLp = new LinearLayout.LayoutParams(-1, -2);
        captionLp.topMargin = dp(14);
        form.addView(caption, captionLp);
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        input.setHint(hint + "°"); input.setContentDescription(label);
        input.setTextColor(palette.text); input.setHintTextColor(palette.muted);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable fieldBackground = new GradientDrawable();
        fieldBackground.setColor(palette.background);
        fieldBackground.setCornerRadius(dp(6));
        fieldBackground.setStroke(dp(1), palette.edge);
        input.setBackground(fieldBackground);
        form.addView(input, new LinearLayout.LayoutParams(-1, dp(54)));
        return input;
    }
    private void saveConfiguration() {
        SharedPreferences.Editor editor = prefs.edit().putString("source", source).putString("location-mode", locationMode)
                .putString("location-name", locationName);
        if (Double.isFinite(latitude) && Double.isFinite(longitude))
            editor.putLong("latitude", Double.doubleToLongBits(latitude)).putLong("longitude", Double.doubleToLongBits(longitude));
        else editor.remove("latitude").remove("longitude");
        editor.apply();
    }
    private void ensureModelLocation() {
        if (!"model".equals(source) || !"auto".equals(locationMode) || locationRequested) return;
        if (Double.isFinite(latitude) && Double.isFinite(longitude)) return;
        locationRequested = true; render();
        locations.request(location -> handler.post(() -> {
            if (closed) return;
            if (location == null) locationName = "Location unavailable";
            else {
                latitude = location.getLatitude(); longitude = location.getLongitude();
                locationName = String.format(Locale.US, "Near me · %.2f°, %.2f°", latitude, longitude);
                saveConfiguration(); reading = null; readHistory(); readCache();
            }
            render(); if (location != null && active) refresh(false);
        }));
    }
    private String locationLabel() {
        if ("model".equals(source)) return locationName + " · model";
        return NAMES[selected] + " · AirBKK";
    }
    void chooseStation() {
        menus.show("Bangkok monitoring station · " + NAMES[selected], NAMES, index -> {
            source = "airbkk"; saveConfiguration();
            selected = index;
            prefs.edit().putString("station", IDS[index]).apply();
            readHistory();
            readCache(); render(); refresh(true);
        });
    }

    private void refresh(boolean manual) {
        if (!active || closed || loading) return;
        long now = SystemClock.elapsedRealtime();
        if (lastAttempt != 0 && now - lastAttempt < (manual ? 30_000L : 15 * 60_000L)) {
            if (manual) android.widget.Toast.makeText(activity, "Recently checked; retry shortly", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if ("model".equals(source) && (!Double.isFinite(latitude) || !Double.isFinite(longitude))) {
            ensureModelLocation();
            return;
        }
        lastAttempt = now; loading = true; render();
        if ("model".equals(source)) {
            refreshModel(manual);
            return;
        }
        final int stationIndex = selected;
        final String stationId = IDS[stationIndex];
        final boolean backfillHistory = history.readings().size() < AirQualityHistory.LIMIT
                && !prefs.getBoolean("history-backfill-complete-" + stationId, false);
        worker.execute(() -> {
            String body = null;
            AirQualityReading next = null;
            List<AirQualityReading> historical = new ArrayList<>();
            boolean historyFetched = false;
            try {
                body = fetch();
                next = AirQualityReading.parse(body, stationId, System.currentTimeMillis());
            } catch (Exception ignored) { /* Keep cached data, explicitly marked. */ }
            if (next != null && backfillHistory) {
                try { historical = fetchHistory(stationId, next.timeMillis); historyFetched = true; }
                catch (Exception ignored) { /* Live data remains usable if the history endpoint is unavailable. */ }
            }
            final String response = body;
            final AirQualityReading nextReading = next;
            final List<AirQualityReading> backfilled = historical;
            final boolean backfillSucceeded = historyFetched;
            handler.post(() -> {
                if (closed) return;
                loading = false;
                if (stationIndex != selected) { render(); return; }
                try {
                    if (response == null) throw new IllegalStateException("Feed unavailable");
                    if (nextReading == null) throw new IllegalStateException("Feed unavailable");
                    if (reading != null && nextReading.timeMillis < reading.timeMillis)
                        throw new IllegalStateException("Feed moved backwards");
                    reading = nextReading; failed = false;
                    history.addAll(backfilled);
                    history.add(nextReading);
                    SharedPreferences.Editor editor = prefs.edit().putString("feed", response)
                            .putString("history-" + IDS[selected], history.encode());
                    if (backfillSucceeded) editor.putBoolean("history-backfill-complete-" + IDS[selected], true);
                    editor.apply();
                } catch (Exception ignored) { failed = true; }
                if (active) render();
            });
        });
    }

    private void refreshModel(boolean manual) {
        final double requestLatitude = latitude, requestLongitude = longitude;
        worker.execute(() -> {
            String body = null;
            ModelData data = null;
            try { body = fetchModel(requestLatitude, requestLongitude); data = parseModel(body); }
            catch (Exception ignored) { /* Keep saved model data and mark its age/status. */ }
            final String response = body;
            final ModelData result = data;
            handler.post(() -> {
                if (closed) return;
                loading = false;
                if (!"model".equals(source) || latitude != requestLatitude || longitude != requestLongitude) { render(); return; }
                if (result == null || response == null) failed = true;
                else {
                    if (result.current != null && (reading == null || result.current.timeMillis >= reading.timeMillis))
                        reading = result.current;
                    history.addAll(result.samples);
                    if (result.current != null) history.add(result.current);
                    failed = false;
                    latitude = result.gridLatitude; longitude = result.gridLongitude;
                    locationName = String.format(Locale.US, "Grid %.2f°, %.2f°", latitude, longitude);
                    saveConfiguration();
                    try {
                        prefs.edit().putString("feed-model-" + modelSuffix(), response)
                                .putString("history-model-" + modelSuffix(), history.encode())
                                .putBoolean("history-backfill-complete-model", true).apply();
                        prefs.edit().putString("location-name", locationName).apply();
                    } catch (Exception ignored) { failed = true; }
                }
                if (active) render();
            });
        });
    }

    private static final class ModelData {
        AirQualityReading current;
        final List<AirQualityReading> samples = new ArrayList<>();
        double gridLatitude, gridLongitude;
    }

    private String fetchModel(double latitude, double longitude) throws Exception {
        String query = "?latitude=" + latitude + "&longitude=" + longitude
                + "&current=pm2_5&hourly=pm2_5&past_days=30&forecast_days=1&timezone=auto&domains=cams_global";
        HttpURLConnection c = (HttpURLConnection) new URL(MODEL_ENDPOINT + query).openConnection();
        connection = c;
        try {
            c.setConnectTimeout(10_000); c.setReadTimeout(15_000); c.setInstanceFollowRedirects(false);
            c.setRequestMethod("GET"); c.setRequestProperty("Accept", "application/json");
            if (c.getResponseCode() != 200) throw new IllegalStateException("Open-Meteo HTTP " + c.getResponseCode());
            try (InputStream input = c.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                long deadline = SystemClock.elapsedRealtime() + 20_000L;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || SystemClock.elapsedRealtime() > deadline
                            || output.size() + count > 512 * 1024) throw new IllegalStateException("Model response limit");
                    output.write(buffer, 0, count);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } finally { c.disconnect(); connection = null; }
    }

    private ModelData parseModel(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (root.optBoolean("error", false)) throw new IllegalStateException(root.optString("reason", "Model unavailable"));
        ModelData result = new ModelData();
        result.gridLatitude = root.optDouble("latitude", latitude);
        result.gridLongitude = root.optDouble("longitude", longitude);
        JSONObject hourly = root.optJSONObject("hourly");
        if (hourly != null) {
            JSONArray times = hourly.optJSONArray("time"), values = hourly.optJSONArray("pm2_5");
            long cutoff = System.currentTimeMillis() + 60 * 60_000L;
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

    private long modelTime(String value, String timezone) {
        try { return OffsetDateTime.parse(value).toInstant().toEpochMilli(); }
        catch (java.time.DateTimeException ignored) {
            return LocalDateTime.parse(value).atZone(ZoneId.of(timezone)).toInstant().toEpochMilli();
        }
    }
    private String modelSuffix() {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                ? String.format(Locale.US, "%.2f_%.2f", latitude, longitude) : "unlocated";
    }

    /** Backfill the same station's official hourly 24-hour moving averages for the last 30 days. */
    private List<AirQualityReading> fetchHistory(String stationId, long latestMillis) throws Exception {
        LocalDateTime end = Instant.ofEpochMilli(latestMillis).atZone(AirQualityReading.BANGKOK)
                .toLocalDateTime().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime start = end.minusHours(AirQualityHistory.LIMIT);
        String form = "MeasIndex%5B%5D=" + encode(stationId)
                + "&parameterTags%5B%5D=" + encode("PM2.5")
                + "&data_type=hourly&date_s=" + encode(REPORT_TIME.format(start))
                + "&date_e=" + encode(REPORT_TIME.format(end))
                + "&display_type=table";
        HttpURLConnection c = (HttpURLConnection) new URL(HISTORY_ENDPOINT).openConnection();
        connection = c;
        try {
            c.setConnectTimeout(10_000); c.setReadTimeout(15_000);
            c.setInstanceFollowRedirects(false);
            c.setRequestMethod("POST");
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            byte[] request = form.getBytes(StandardCharsets.UTF_8);
            c.setDoOutput(true); c.setFixedLengthStreamingMode(request.length);
            try (OutputStream output = c.getOutputStream()) { output.write(request); }
            if (c.getResponseCode() != 200) throw new IllegalStateException("History HTTP " + c.getResponseCode());
            String body;
            try (InputStream input = c.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                long deadline = SystemClock.elapsedRealtime() + 20_000L;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || SystemClock.elapsedRealtime() > deadline
                            || output.size() + count > 512 * 1024) throw new IllegalStateException("History response limit");
                    output.write(buffer, 0, count);
                }
                body = output.toString(StandardCharsets.UTF_8.name());
            }
            return parseHistory(body, stationId, latestMillis, start);
        } finally { c.disconnect(); connection = null; }
    }

    private List<AirQualityReading> parseHistory(String json, String stationId,
                                                  long latestMillis, LocalDateTime start) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"success".equalsIgnoreCase(root.optString("status")))
            throw new IllegalStateException("AirBKK history unavailable");
        JSONArray parameters = root.getJSONArray("arrParameter");
        String alias = null;
        for (int i = 0; i < parameters.length(); i++) {
            JSONObject parameter = parameters.getJSONObject(i);
            if (stationId.equals(parameter.optString("MeasIndex"))
                    && "PM2.5".equals(parameter.optString("ShortName"))) {
                alias = parameter.optString("Alias", null);
                break;
            }
        }
        if (alias == null || alias.isEmpty()) throw new IllegalStateException("PM2.5 history series missing");

        JSONArray averages = root.optJSONArray("mvavgData");
        List<AirQualityReading> result = new ArrayList<>();
        if (averages == null) return result;
        long startMillis = start.atZone(AirQualityReading.BANGKOK).toInstant().toEpochMilli();
        for (int i = 0; i < averages.length(); i++) {
            JSONObject point = averages.getJSONObject(i);
            String time = point.optString("Date_Time", "");
            double value = point.optDouble("value", Double.NaN);
            if (!Double.isFinite(value) || value < 0) continue;
            LocalDateTime local = LocalDateTime.parse(time, MOVING_AVERAGE_TIME);
            long timestamp = local.atZone(AirQualityReading.BANGKOK).toInstant().toEpochMilli();
            if (timestamp < startMillis || timestamp > latestMillis) continue;
            result.add(new AirQualityReading(value, timestamp));
        }
        return result;
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private String fetch() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection = c;
        try {
            c.setConnectTimeout(10_000); c.setReadTimeout(10_000);
            c.setInstanceFollowRedirects(false);
            c.setRequestMethod("POST"); // AirBKK's own public dashboard uses an empty POST to read this feed.
            c.setRequestProperty("Accept", "application/json");
            c.setDoOutput(true); c.setFixedLengthStreamingMode(0);
            c.getOutputStream().close();
            if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
            try (InputStream input = c.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                long deadline = SystemClock.elapsedRealtime() + 20_000L;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || SystemClock.elapsedRealtime() > deadline
                            || output.size() + count > 512 * 1024) throw new IllegalStateException("Response limit");
                    output.write(buffer, 0, count);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } finally { c.disconnect(); connection = null; }
    }

    private void render() {
        if (value == null) return;
        stationLabel.setText(locationLabel().toUpperCase(Locale.US));
        value.setText(reading == null ? activity.getString(R.string.air_no_value)
                : String.format(Locale.US, "PM2.5  %.1f µg/m³", reading.pm25));
        int color = reading == null ? palette.muted : conditionColor(reading.condition());
        applyTileBackground();
        value.setTextColor(color);
        conditionLabel.setText(reading == null ? "WAITING FOR READING" : reading.conditionLabel());
        conditionLabel.setTextColor(color);
        conditionLabel.setVisibility(reading == null ? View.GONE : View.VISIBLE);
        if (reading != null) {
            GradientDrawable badge = new GradientDrawable();
            badge.setColor(palette.raised);
            badge.setCornerRadius(activity.getResources().getDisplayMetrics().density * 8);
            badge.setStroke(Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density)), color);
            conditionLabel.setBackground(badge);
            int padH = Math.round(activity.getResources().getDisplayMetrics().density * 8);
            int padV = Math.round(activity.getResources().getDisplayMetrics().density * 2);
            conditionLabel.setPadding(padH, padV, padH, padV);
        }
        List<AirQualityReading> month = history.readings();
        long dayStart = month.isEmpty() ? Long.MAX_VALUE
                : month.get(month.size() - 1).timeMillis - AirQualityHistory.HOURS_24 * 3_600_000L;
        List<AirQualityReading> day = new ArrayList<>();
        for (AirQualityReading sample : month) if (sample.timeMillis >= dayStart) day.add(sample);
        trend.setPalette(palette);
        trend.setReadings(day, month);
        String status;
        if (reading == null) status = loading ? "Loading…" : "model".equals(source)
                ? (locationRequested ? "Getting device location…" : locationName + " · configure location")
                : "Reading unavailable";
        else {
            ZoneId displayZone = "model".equals(source) ? ZoneId.systemDefault() : AirQualityReading.BANGKOK;
            String time = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH)
                    .withZone(displayZone).format(Instant.ofEpochMilli(reading.timeMillis));
            status = "Data: " + time + ("model".equals(source) ? " local" : " Bangkok");
            if (reading.stale(System.currentTimeMillis())) status += " · STALE";
            if (failed) status += " · Refresh failed; saved reading";
            if (loading) status += " · Updating…";
        }
        String sourceNote = "model".equals(source) ? "Hourly model estimate · Open-Meteo / CAMS"
                : "PCD 24-hour average · AirBKK / BMA";
        detail.setText(sourceNote + "\n" + status + "\nOK: refresh or configure");
    }

    private void readHistory() {
        String key = "model".equals(source) ? "history-model-" + modelSuffix() : "history-" + IDS[selected];
        try { history = AirQualityHistory.parse(prefs.getString(key, "[]")); }
        catch (Exception ignored) { history = new AirQualityHistory(); }
    }

    static int conditionColor(AirQualityReading.Condition condition) {
        switch (condition) {
            case VERY_GOOD: return 0xFF67C9F2; // PCD blue
            case GOOD: return 0xFF78D69A;      // PCD green
            case MODERATE: return 0xFFFFD166;  // PCD yellow
            case HIGH: return 0xFFFF995E;      // PCD orange
            default: return 0xFFFF6B6B;        // PCD red
        }
    }

    private void applyTileBackground() {
        if (tile == null) return;
        if (tileFocused) {
            tile.setBackground(tileSurface(palette.raised, palette.focus, 3));
        } else {
            tile.setBackground(tileSurface(palette.surface, palette.edge, 1));
        }
    }

    private GradientDrawable tileSurface(int color, int stroke, int width) {
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(color);
        surface.setCornerRadius(activity.getResources().getDisplayMetrics().density * 14);
        surface.setStroke(Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density * width)), stroke);
        return surface;
    }

    private int dp(float value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

}
