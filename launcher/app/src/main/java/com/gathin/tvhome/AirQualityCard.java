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
    private final String[] NAMES;
    private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.US);
    private static final DateTimeFormatter MOVING_AVERAGE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm", Locale.US);
    private static final String HISTORY_ENDPOINT = "https://official.airbkk.com/airbkk/Report/getData";
    private String getString(int id) { return activity.getString(id); }
    private final Activity activity;
    interface Menus { void show(String title, String[] labels, java.util.function.IntConsumer action); }
    interface LocationProvider { void request(java.util.function.Consumer<Location> callback); }
    interface UndoRecorder { void record(String widgetKey, String previousConfiguration, String label); }
    private final Menus menus;
    private final LocationProvider locations;
    private final UndoRecorder undoRecorder;
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
    private ZoneId expandedZone = java.time.ZoneOffset.UTC;
    private AirQualityReading reading;
    private AirQualityHistory history = new AirQualityHistory();
    private PaletteColors palette = PaletteColors.from(0xff101010);
    private int backgroundOpacity = 75;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            refresh(false);
            render();
            handler.postDelayed(this, 60_000L);
        }
    };

    AirQualityCard(Activity activity, Menus menus, String widgetKey, LocationProvider locations,
                   UndoRecorder undoRecorder) {
        this.activity = activity;
        NAMES = activity.getResources().getStringArray(R.array.hb_station_names);
        this.menus = menus;
        this.widgetKey = widgetKey; this.locations = locations; this.undoRecorder = undoRecorder;
        String preferenceName = "air-quality-widget-" + widgetKey.replaceAll("[^A-Za-z0-9_-]", "_");
        prefs = activity.getSharedPreferences(preferenceName, Activity.MODE_PRIVATE);
        JSONObject initial=legacyConfiguration();
        JSONObject config=PhoneConfigurationStore.readAir(activity,widgetKey,initial);
        source=config.optString("source","model");locationMode=config.optString("location-mode","auto");
        locationName=config.optString("location-name",getString(R.string.hb_near_me));
        latitude=config.optDouble("latitude",Double.NaN);longitude=config.optDouble("longitude",Double.NaN);
        String saved=config.optString("station",IDS[0]);
        for(int i=0;i<IDS.length;i++)if(IDS[i].equals(saved))selected=i;
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
    void setBackgroundOpacity(int percent) {
        backgroundOpacity = WallpaperSettings.percent(percent);
        applyTileBackground();
        UiBackgroundDrawable.apply(conditionLabel, backgroundOpacity);
    }
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
        menus.show(getString(R.string.hb_air_quality) + locationLabel(), new String[]{getString(R.string.hb_refresh_reading), getString(R.string.hb_configure_location_and_source)}, which -> {
            if (which == 0) refresh(true);
            else configureWidget();
        });
    }
    void refreshNow() { refresh(true); }
    void configureWidget() {
        menus.show(getString(R.string.hb_air_quality_location_each_widget_is_independent),
                new String[]{getString(R.string.hb_nearest_model_grid_use_this_device), getString(R.string.hb_enter_coordinates_for_a_model_grid), getString(R.string.hb_bangkok_monitoring_station)}, choice -> {
                    if (choice == 0) useCurrentLocation();
                    else if (choice == 1) enterCoordinates();
                    else { source = "airbkk"; saveConfiguration(); chooseStation(); }
                });
    }
    private void useCurrentLocation() {
        source = "model"; locationMode = "auto"; latitude = Double.NaN; longitude = Double.NaN;
        locationName = getString(R.string.hb_near_me); locationRequested = false; saveConfiguration();
        reading = null; readHistory(); readCache(); render();
        ensureModelLocation();
    }
    private void enterCoordinates() {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(4));
        TextView explanation = new TextView(activity);
        explanation.setText(getString(R.string.hb_choose_the_forecast_grid_near_this_location));
        explanation.setTextColor(palette.muted);
        form.addView(explanation, new LinearLayout.LayoutParams(-1, -2));
        EditText latitudeInput = coordinateField(form, getString(R.string.hb_latitude), "−90 to 90");
        EditText longitudeInput = coordinateField(form, getString(R.string.hb_longitude), "−180 to 180");
        if (Double.isFinite(latitude)) latitudeInput.setText(String.format(Locale.US, "%.5f", latitude));
        if (Double.isFinite(longitude)) longitudeInput.setText(String.format(Locale.US, "%.5f", longitude));

        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(getString(R.string.hb_model_grid_location))
                .setView(form).setNegativeButton(getString(R.string.hb_cancel), (d, which) -> {})
                .setPositiveButton(getString(R.string.hb_save), null).create();
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
                latitudeInput.setError(getString(R.string.hb_check_both_coordinates));
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
    private void saveConfiguration() { saveConfiguration(true); }
    private void saveConfiguration(boolean userChange) {
        String before = configurationFromPreferences().toString();
        String after = configurationFromFields().toString();
        if (userChange && !before.equals(after) && undoRecorder != null)
            undoRecorder.record(widgetKey, before, getString(R.string.hb_air_quality_location));
        persistConfiguration();
    }
    private void persistConfiguration() {
        PhoneConfigurationStore.writeAir(activity,widgetKey,configurationFromFields());
    }
    JSONObject configurationSnapshot(){return configurationFromPreferences();}
    private JSONObject configurationFromPreferences() {
        return PhoneConfigurationStore.readAir(activity,widgetKey,legacyConfiguration());
    }
    private JSONObject legacyConfiguration() {
        JSONObject config = new JSONObject();
        try {
            config.put("source", prefs.getString("source", "model"));
            config.put("location-mode", prefs.getString("location-mode", "auto"));
            config.put("location-name", prefs.getString("location-name", getString(R.string.hb_near_me)));
            config.put("station", prefs.getString("station", IDS[0]));
            if (prefs.contains("latitude") && prefs.contains("longitude")) {
                config.put("latitude", Double.longBitsToDouble(prefs.getLong("latitude", 0)));
                config.put("longitude", Double.longBitsToDouble(prefs.getLong("longitude", 0)));
            }
        } catch (Exception ignored) { }
        return config;
    }
    private JSONObject configurationFromFields() {
        JSONObject config = new JSONObject();
        try {
            config.put("source", source).put("location-mode", locationMode).put("location-name", locationName)
                    .put("station", IDS[selected]);
            if (Double.isFinite(latitude) && Double.isFinite(longitude))
                config.put("latitude", latitude).put("longitude", longitude);
        } catch (Exception ignored) { }
        return config;
    }
    void restoreConfigurationSnapshot(String encoded) {
        try {
            JSONObject config = new JSONObject(encoded);
            source = config.optString("source", "model");
            locationMode = config.optString("location-mode", "auto");
            locationName = config.optString("location-name", getString(R.string.hb_near_me));
            String station = config.optString("station", IDS[0]);
            selected = 0;
            for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(station)) selected = i;
            if (config.has("latitude") && config.has("longitude")) {
                latitude = config.getDouble("latitude"); longitude = config.getDouble("longitude");
            } else { latitude = Double.NaN; longitude = Double.NaN; }
            locationRequested = false;
            persistConfiguration(); readHistory(); readCache(); render();
            if ("model".equals(source) && "auto".equals(locationMode)) ensureModelLocation();
            else if (active) refresh(true);
        } catch (Exception ignored) { }
    }
    private void ensureModelLocation() {
        if (!"model".equals(source) || !"auto".equals(locationMode) || locationRequested) return;
        if (Double.isFinite(latitude) && Double.isFinite(longitude)) return;
        locationRequested = true; render();
        locations.request(location -> handler.post(() -> {
            if (closed) return;
            if (location == null) locationName = getString(R.string.hb_location_unavailable);
            else {
                latitude = location.getLatitude(); longitude = location.getLongitude();
                locationName = String.format(Locale.US, "Near me · %.2f°, %.2f°", latitude, longitude);
                saveConfiguration(false); reading = null; readHistory(); readCache();
            }
            render(); if (location != null && active) refresh(false);
        }));
    }
    private String locationLabel() {
        if ("model".equals(source)) {
            String name = locationName;
            if ("auto".equals(locationMode)) name = Double.isFinite(latitude)
                    ? activity.getString(R.string.hb_near_coordinates, latitude, longitude)
                    : getString(R.string.hb_near_me);
            return name + getString(R.string.hb_model);
        }
        return NAMES[selected] + " · AirBKK";
    }
    void chooseStation() {
        menus.show(getString(R.string.hb_bangkok_monitoring_station_228) + NAMES[selected], NAMES, index -> {
            source = "airbkk"; selected = index; saveConfiguration();
            readHistory();
            readCache(); render(); refresh(true);
        });
    }

    private void refresh(boolean manual) {
        if (!active || closed || loading) return;
        long now = SystemClock.elapsedRealtime();
        if (lastAttempt != 0 && now - lastAttempt < (manual ? 30_000L : 15 * 60_000L)) {
            if (manual) android.widget.Toast.makeText(activity, getString(R.string.hb_recently_checked_retry_shortly), android.widget.Toast.LENGTH_SHORT).show();
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
                    saveConfiguration(false);
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
        ZoneId zone;
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
        result.zone=ZoneId.of(root.optString("timezone","UTC"));
        expandedZone=result.zone;
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
            throw new IllegalStateException(getString(R.string.hb_airbkk_history_unavailable));
        JSONArray parameters = root.getJSONArray("arrParameter");
        String alias = null;
        for (int i = 0; i < parameters.length(); i++) {
            JSONObject parameter = parameters.getJSONObject(i);
            if (stationId.equals(parameter.optString("MeasIndex"))
                    && "PM2.5".equals(parameter.optString("ShortName"))) {
                alias = parameter.optString(getString(R.string.hb_alias), null);
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

    private String fetch() throws Exception { return AirStationClient.fetch().get(30, java.util.concurrent.TimeUnit.SECONDS); }
    String expandedCachedFeed(){return prefs.getString("feed", "");}
    boolean expandedIsModel(){return "model".equals(source);}
    String expandedLabel(){return locationLabel();}
    ZoneId expandedZone(){return expandedIsModel()?expandedZone:AirQualityReading.BANGKOK;}
    List<AirQualityReading> expandedHistory(){return history.readings();}
    void expandedHistory(StationCatalog.Station station, java.util.function.Consumer<List<AirQualityReading>> callback){
        if(closed)return;
        worker.execute(()->{
            List<AirQualityReading> values=new ArrayList<>();
            try{values.addAll(AirQualityHistory.parse(prefs.getString("history-"+station.id,"[]")).readings());}catch(Exception ignored){}
            try{values.addAll(fetchHistory(station.id,station.time>0?station.time:System.currentTimeMillis()));}catch(Exception ignored){}
            if(station.reading()!=null)values.add(station.reading());
            AirQualityHistory bounded=new AirQualityHistory();bounded.addAll(values);List<AirQualityReading> result=bounded.readings();
            handler.post(()->{if(!closed)callback.accept(result);});
        });
    }

    private void render() {
        if (value == null) return;
        stationLabel.setText(locationLabel().toUpperCase(Locale.US));
        value.setText(reading == null ? activity.getString(R.string.air_no_value)
                : String.format(Locale.US, "PM2.5  %.1f µg/m³", reading.pm25));
        int color = reading == null ? palette.muted : conditionColor(reading.condition());
        applyTileBackground();
        value.setTextColor(color);
        conditionLabel.setText(reading == null ? getString(R.string.hb_waiting_for_reading) : activity.getResources().getStringArray(reading.modelEstimate ? R.array.hb_model_conditions : R.array.hb_station_conditions)[reading.condition().ordinal()]);
        conditionLabel.setTextColor(color);
        conditionLabel.setVisibility(reading == null ? View.GONE : View.VISIBLE);
        if (reading != null) {
            UiBackgroundDrawable badge = new UiBackgroundDrawable(palette.raised);
            badge.setFillOpacity(backgroundOpacity);
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
        if (reading == null) status = loading ? getString(R.string.hb_loading) : "model".equals(source)
                ? (locationRequested ? getString(R.string.hb_getting_device_location) : locationName + getString(R.string.hb_configure_location))
                : getString(R.string.hb_reading_unavailable);
        else {
            ZoneId displayZone = "model".equals(source) ? ZoneId.systemDefault() : AirQualityReading.BANGKOK;
            String time = DateTimeFormatter.ofPattern("dd MMM HH:mm", activity.getResources().getConfiguration().getLocales().get(0))
                    .withZone(displayZone).format(Instant.ofEpochMilli(reading.timeMillis));
            status = getString(R.string.hb_hint_data) + time + ("model".equals(source) ? getString(R.string.hb_hint_local) : getString(R.string.hb_hint_bangkok));
            if (reading.stale(System.currentTimeMillis())) status += getString(R.string.hb_stale);
            if (failed) status += getString(R.string.hb_refresh_failed_saved_reading);
            if (loading) status += getString(R.string.hb_updating);
        }
        String sourceNote = "model".equals(source) ? getString(R.string.hb_hourly_model_estimate_open_meteo_cams)
                : getString(R.string.hb_pcd_24_hour_average_airbkk_bma);
        detail.setText(sourceNote + "\n" + status + getString(R.string.hb_nok_refresh_or_configure));
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
        int base = tileFocused ? palette.raised : palette.surface;
        // Use the same condition as the value/badge, including model-specific bands.
        // A subdued tint keeps the text and trend readable across the entire tile.
        int fill = reading == null ? base
                : PaletteColors.mix(base, conditionColor(reading.condition()), 0.28f);
        int edge = tileFocused ? palette.focus
                : reading == null ? palette.edge : conditionColor(reading.condition());
        tile.setBackground(tileSurface(fill, edge, tileFocused ? 3 : 1));
    }

    private GradientDrawable tileSurface(int color, int stroke, int width) {
        UiBackgroundDrawable surface = new UiBackgroundDrawable(color);
        surface.setFillOpacity(backgroundOpacity);
        surface.setCornerRadius(activity.getResources().getDisplayMetrics().density * 14);
        surface.setStroke(Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density * width)), stroke);
        return surface;
    }

    private int dp(float value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

}
