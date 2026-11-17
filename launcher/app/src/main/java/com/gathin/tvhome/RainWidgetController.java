package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Owns one widget's lifecycle and cache. All user configuration goes through layout Undo. */
final class RainWidgetController {
    final Activity activity;
    private final Supplier<RainSettings> settings;
    private final Consumer<RainSettings> save;
    private final AirQualityCard.Menus menus;
    interface LocationAccess {
        boolean granted();
        void request(boolean prompt, Consumer<Location> callback);
    }
    private final LocationAccess locations;
    private final RainLocation deviceLocation = new RainLocation();
    private final Runnable sizes;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    RainForecast forecast;
    long fetchedAt;
    boolean failed, loading, active, closed;
    private boolean expandedActive;
    void expandedActive(boolean value){expandedActive=value;if(value){deviceLocation.attemptedAt=-1;refresh(false);}}
    private long lastAttempt;
    private int failures, generation, lookupGeneration;
    private String loadedKey = "";
    private RainWidgetView view;
    private RainWidgetView detailsView;
    private Dialog dialog;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!active || closed) return;
            refresh(false); render(); handler.postDelayed(this, 60_000);
        }
    };
    RainWidgetController(Activity activity, String key, Supplier<RainSettings> settings, Consumer<RainSettings> save,
                         AirQualityCard.Menus menus, LocationAccess locations, Runnable sizes) {
        this.activity = activity; this.settings = settings; this.save = save; this.menus = menus; this.locations = locations; this.sizes = sizes;
        prefs = activity.getSharedPreferences("rain-cache-" + key, 0);
        deviceLocation.accept(Double.longBitsToDouble(prefs.getLong("device-lat", Double.doubleToLongBits(Double.NaN))),
                Double.longBitsToDouble(prefs.getLong("device-lon", Double.doubleToLongBits(Double.NaN))),
                prefs.getLong("device-time", 0), System.currentTimeMillis());
        sync();
    }
    private RainSettings configured() { return settings.get(); }
    RainSettings settings() {
        RainSettings result = deviceLocation.effective(configured(), locations.granted());
        if (result.automatic) result.name = s(R.string.hb_rain_device);
        return result;
    }
    String s(int id, Object... args) { return activity.getString(id, args); }
    void bind(RainWidgetView view) { this.view = view; sync(); render(); }
    void unbind() { if (view != null) view.setRunning(false); view = null; }
    void start() { if (active || closed) return; active = true; deviceLocation.attemptedAt = -1; tick.run(); }
    void stop() { active = false; if(!expandedActive)deviceLocation.cancel(); handler.removeCallbacks(tick); if (view != null) view.setRunning(false); }
    void pause() { stop(); handler.removeCallbacks(detailsTick); if (detailsView != null) detailsView.setRunning(false); }
    void resumeDetails() { if (dialog != null) { handler.removeCallbacks(detailsTick); detailsTick.run(); } }
    void close() { closed = true; generation++; lookupGeneration++; stop(); if (dialog != null) dialog.dismiss(); unbind(); }
    void sync() {
        String key = settings().locationKey();
        if (key.equals(loadedKey)) return;
        generation++; loading = false; failed = false; failures = 0; lastAttempt = 0; loadedKey = key;
        forecast = null; fetchedAt = 0;
        if (key.equals(prefs.getString("location", ""))) try {
            forecast = RainForecast.parse(prefs.getString("body", "")); fetchedAt = prefs.getLong("fetched", 0);
        } catch (Exception ignored) { forecast = null; }
    }
    void changed(RainSettings next) {
        deviceLocation.cancel();
        if (next.automatic) { next.latitude = Double.NaN; next.longitude = Double.NaN; next.name = ""; }
        save.accept(next); sync(); refresh(true); render();
    }
    private void resolveLocation(boolean manual, boolean prompt) {
        if (closed || !configured().automatic || !locations.granted() && !prompt) return;
        int token = deviceLocation.begin(SystemClock.elapsedRealtime(), manual);
        if (token < 0) return;
        locations.request(prompt, location -> {
            if (closed || !configured().automatic || token != deviceLocation.generation || !deviceLocation.pending) return;
            boolean accepted = locations.granted() && location != null &&
                    deviceLocation.accept(location.getLatitude(), location.getLongitude(), location.getTime(), System.currentTimeMillis());
            deviceLocation.finish(token, accepted);
            if (accepted) prefs.edit().putLong("device-lat", Double.doubleToLongBits(deviceLocation.latitude))
                    .putLong("device-lon", Double.doubleToLongBits(deviceLocation.longitude)).putLong("device-time", deviceLocation.fixedAt).apply();
            sync();
            // A synchronous cached-location callback must not recursively start another lookup.
            handler.post(() -> { if (!closed && (active || dialog != null || expandedActive)) { refresh(false); render(); } });
            render();
        });
    }
    void refresh(boolean manual) {
        if (configured().automatic && (active || dialog != null || expandedActive || manual)) resolveLocation(manual, false);
        sync();
        if (closed || loading || !settings().located()) return;
        if (configured().automatic && !deviceLocation.usable(System.currentTimeMillis())) return;
        long elapsed = SystemClock.elapsedRealtime();
        long backoff = manual ? 15_000 : forecast != null && !failed ? 30 * 60_000L : Math.min(30 * 60_000L, 60_000L << Math.min(failures, 4));
        if (lastAttempt != 0 && elapsed - lastAttempt < backoff) return;
        if (!manual && !failed && fetchedAt > 0 && System.currentTimeMillis() - fetchedAt < 30 * 60_000L) return;
        lastAttempt = elapsed; loading = true; int token = generation;
        RainSettings request = settings().copy();
        RainClient.forecast(request).whenComplete((body, error) -> {
            RainForecast result = null;
            if (error == null) try { result = RainForecast.parse(body); } catch (Exception invalid) { /* Preserve matching cache. */ }
            final RainForecast parsed = result;
            handler.post(() -> {
                if (closed || token != generation || !request.locationKey().equals(settings().locationKey())) return;
                loading = false; failed = parsed == null;
                if (parsed != null) {
                    forecast = parsed; fetchedAt = System.currentTimeMillis(); failures = 0;
                    prefs.edit().putString("location", request.locationKey()).putString("body", body).putLong("fetched", fetchedAt).apply();
                } else failures++;
                render();
            });
        });
        render();
    }
    String status() {
        if (configured().automatic) {
            if (!locations.granted()) return s(R.string.hb_rain_allow_location);
            if (!deviceLocation.located()) return s(deviceLocation.pending ? R.string.hb_rain_finding_location : R.string.hb_rain_location_unavailable);
            if (!deviceLocation.usable(System.currentTimeMillis())) return s(R.string.hb_rain_last_location);
        }
        if (!settings().located()) return s(R.string.hb_rain_set_location);
        if (forecast == null) return s(loading ? R.string.hb_rain_loading : R.string.hb_rain_unavailable);
        if (failed) return s(R.string.hb_rain_offline);
        if (System.currentTimeMillis() - fetchedAt > 90 * 60_000L || fetchedAt > System.currentTimeMillis()) return s(R.string.hb_rain_stale);
        if (!forecast.hourly(Instant.now(), settings().hours).complete) return s(R.string.hb_rain_incomplete);
        return "";
    }
    boolean fresh() { return forecast != null && status().isEmpty(); }
    String actionHint() {
        if (configured().automatic && !locations.granted()) return s(R.string.hb_rain_allow_hint);
        if (configured().automatic && !deviceLocation.usable(System.currentTimeMillis())) return s(R.string.hb_rain_retry_hint);
        return s(R.string.hb_rain_more);
    }
    String amount(double mm) { return Double.isFinite(mm) ? String.format(Locale.getDefault(), settings().inches ? "%.2f in" : "%.1f mm", settings().inches ? mm / 25.4 : mm) : "—"; }
    String chance(double chance) { return Double.isFinite(chance) ? String.format(Locale.getDefault(), "%.0f%%", chance) : "—"; }
    String time(long epoch, String pattern) { return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).withZone(forecast.zone).format(Instant.ofEpochSecond(epoch)); }
    String headline(RainForecast.Period period) {
        RainForecast.Hour wet = period.firstWet();
        if (wet != null) return s(R.string.hb_rain_wet, time(wet.start, "HH:mm"));
        return s(period.complete ? R.string.hb_rain_dry : R.string.hb_rain_incomplete, settings().hours);
    }
    void render() { if (view != null) { view.refresh(); view.setRunning(active && dialog == null); } if (detailsView != null) { detailsView.refresh(); fillDetails(); } }
    void configure() {
        RainSettings p = configured();
        menus.show(s(R.string.hb_rain_configure), new String[]{s(R.string.hb_rain_location), s(R.string.hb_rain_hourly) + ": " + p.hours,
                s(R.string.hb_rain_following, p.days), s(R.string.hb_rain_units), s(R.string.hb_rain_animation) + ": " + s(p.animation ? R.string.hb_rain_on : R.string.hb_rain_off),
                s(R.string.hb_rain_label), s(R.string.hb_rain_size), s(R.string.hb_rain_refresh)}, choice -> {
            if (choice == 0) chooseLocation();
            if (choice == 1) menus.show(s(R.string.hb_rain_hourly), new String[]{"6", "12", "24", "48"}, i -> { RainSettings n = configured().copy(); n.hours = new int[]{6,12,24,48}[i]; changed(n); });
            if (choice == 2) menus.show(s(R.string.hb_rain_days), new String[]{"0","1","2","3","4","5","6"}, i -> { RainSettings n = configured().copy(); n.days = i; changed(n); });
            if (choice == 3) menus.show(s(R.string.hb_rain_units), new String[]{s(R.string.hb_rain_mm), s(R.string.hb_rain_inches)}, i -> { RainSettings n = configured().copy(); n.inches = i == 1; changed(n); });
            if (choice == 4) { RainSettings n = configured().copy(); n.animation = !n.animation; changed(n); }
            if (choice == 5) input(s(R.string.hb_rain_label), p.label, value -> { RainSettings n = configured().copy(); n.label = value; changed(n); });
            if (choice == 6) sizes.run();
            if (choice == 7) refresh(true);
        });
    }
    void chooseLocation() {
        menus.show(s(R.string.hb_rain_choose), new String[]{s(R.string.hb_rain_automatic), s(R.string.hb_rain_city), s(R.string.hb_rain_address), s(R.string.hb_rain_coordinates)}, i -> {
            if (i == 0) { RainSettings n = configured().copy(); n.automatic = true; changed(n); resolveLocation(true, true); }
            if (i == 1 || i == 2) input(s(i == 1 ? R.string.hb_rain_city : R.string.hb_rain_address), "", query -> lookup(query, i == 2));
            if (i == 3) coordinates();
        });
    }
    private void confirm(double lat, double lon, String name) {
        if (!RainSettings.coordinates(lat, lon)) { message(s(R.string.hb_rain_invalid)); return; }
        new AlertDialog.Builder(activity).setTitle(s(R.string.hb_rain_confirm)).setMessage(name + "\n" + lat + ", " + lon)
                .setNegativeButton(s(R.string.hb_rain_cancel), null).setPositiveButton(s(R.string.hb_rain_save), (d,w) -> {
                    RainSettings n = configured().copy(); n.automatic = false; n.latitude = lat; n.longitude = lon; n.name = name; changed(n);
                }).show();
    }
    private void coordinates() {
        LinearLayout form = form(); RainSettings p = settings();
        EditText lat = field(form, s(R.string.hb_rain_latitude), p.located() ? Double.toString(p.latitude) : "", true);
        EditText lon = field(form, s(R.string.hb_rain_longitude), p.located() ? Double.toString(p.longitude) : "", true);
        AlertDialog d = new AlertDialog.Builder(activity).setTitle(s(R.string.hb_rain_coordinates)).setView(form)
                .setNegativeButton(s(R.string.hb_rain_cancel), null).setPositiveButton(s(R.string.hb_rain_save), null).create();
        d.setOnShowListener(ignored -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                double a = Double.parseDouble(lat.getText().toString().trim()), b = Double.parseDouble(lon.getText().toString().trim());
                if (!RainSettings.coordinates(a,b)) throw new IllegalArgumentException();
                d.dismiss(); confirm(a,b,String.format(Locale.getDefault(), "%.3f°, %.3f°", a,b));
            } catch (Exception invalid) { lat.setError(s(R.string.hb_rain_invalid)); }
        })); d.show();
    }
    private void input(String title, String value, Consumer<String> done) {
        LinearLayout form = form(); EditText input = field(form, title, value, false);
        new AlertDialog.Builder(activity).setTitle(title).setView(form).setNegativeButton(s(R.string.hb_rain_cancel), null)
                .setPositiveButton(s(title.equals(s(R.string.hb_rain_label)) ? R.string.hb_rain_save : R.string.hb_rain_search), (d,w) -> { String q = input.getText().toString().trim(); if (!q.isEmpty() || title.equals(s(R.string.hb_rain_label))) done.accept(q); }).show();
    }
    private LinearLayout form() {
        LinearLayout form = new LinearLayout(activity); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(24,16,24,16);
        TextView privacy = new TextView(activity); privacy.setText(s(R.string.hb_rain_privacy)); form.addView(privacy); return form;
    }
    private EditText field(LinearLayout form, String label, String value, boolean number) {
        EditText e = new EditText(activity); e.setSingleLine(true); e.setHint(label); e.setContentDescription(label);
        e.setInputType(number ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED : InputType.TYPE_CLASS_TEXT);
        e.setText(value); form.addView(e, new LinearLayout.LayoutParams(-1,-2)); return e;
    }
    private void message(String value) { new AlertDialog.Builder(activity).setMessage(value).setPositiveButton(s(R.string.hb_rain_back), null).show(); }
    private void lookup(String query, boolean address) {
        int token = ++lookupGeneration;
        if (address && !Geocoder.isPresent()) { message(s(R.string.hb_rain_address_missing)); return; }
        CompletableFuture<List<Place>> task;
        if (address) task = CompletableFuture.supplyAsync(() -> {
            List<Place> result = new ArrayList<>();
            try {
                List<Address> hits = new Geocoder(activity, Locale.getDefault()).getFromLocationName(query, 5);
                if (hits != null) for (Address a : hits) if (a.hasLatitude() && a.hasLongitude())
                    result.add(new Place(a.getLatitude(), a.getLongitude(), a.getAddressLine(0) == null ? query : a.getAddressLine(0)));
            } catch (Exception e) { throw new java.util.concurrent.CompletionException(e); }
            return result;
        });
        else task = RainClient.search(query, Locale.getDefault().getLanguage()).thenApply(body -> {
            List<Place> result = new ArrayList<>();
            try {
                JSONArray hits = new JSONObject(body).optJSONArray("results");
                if (hits != null) for (int i=0;i<hits.length();i++) {
                    JSONObject p = hits.getJSONObject(i);
                    result.add(new Place(p.getDouble("latitude"),p.getDouble("longitude"),p.getString("name") + ", " + p.optString("admin1") + ", " + p.optString("country")));
                }
            } catch (Exception e) { throw new java.util.concurrent.CompletionException(e); }
            return result;
        });
        AlertDialog waiting = new AlertDialog.Builder(activity).setMessage(s(R.string.hb_rain_loading)).setNegativeButton(s(R.string.hb_rain_cancel), (d,w) -> lookupGeneration++).create();
        waiting.setOnCancelListener(d -> lookupGeneration++); waiting.show();
        handler.postDelayed(() -> { if (token == lookupGeneration && waiting.isShowing()) { lookupGeneration++; waiting.dismiss(); message(s(R.string.hb_rain_lookup_failed)); } }, 20_000);
        task.whenComplete((hits,error) -> handler.post(() -> {
            if (closed || token != lookupGeneration) return;
            waiting.dismiss();
            if (error != null) { message(s(R.string.hb_rain_lookup_failed)); return; }
            if (hits.isEmpty()) { message(s(R.string.hb_rain_no_results)); return; }
            new AlertDialog.Builder(activity).setTitle(s(R.string.hb_rain_choose)).setItems(hits.stream().map(p -> p.name).toArray(String[]::new), (d,w) -> {
                Place p = hits.get(w); confirm(p.lat,p.lon,p.name);
            }).setNegativeButton(s(R.string.hb_rain_cancel), null).show();
        }));
    }
    private static final class Place {
        final double lat,lon; final String name;
        Place(double lat, double lon, String name) { this.lat=lat;this.lon=lon;this.name=name; }
    }
    private final Runnable detailsTick = new Runnable() {
        @Override public void run() {
            if (closed || dialog == null || !dialog.isShowing()) return;
            refresh(false); render();
            if (detailsView != null) detailsView.setRunning(true);
            handler.postDelayed(this, 60_000);
        }
    };
    private boolean daily, expanded;
    private int page;
    private LinearLayout detailsBody;
    void details() {
        if (configured().automatic && (!locations.granted() || !deviceLocation.usable(System.currentTimeMillis()))) {
            if (!locations.granted()) resolveLocation(true, true);
            else if (!deviceLocation.pending) resolveLocation(true, false);
            return;
        }
        if (!settings().located()) { chooseLocation(); return; }
        expanded = false; page = 0; daily = false; openDetails();
    }
    private void openDetails() {
        if (dialog != null) dialog.dismiss();
        dialog = new Dialog(activity); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,12,20,12);
        root.setBackgroundColor(palette.raised);
        TextView title = label(settings().displayName() + " · " + s(R.string.hb_rain_rain), 22); root.addView(title);
        LinearLayout tabs = new LinearLayout(activity);
        button(tabs, s(R.string.hb_rain_hourly), () -> { daily=false;page=0;fillDetails(); });
        button(tabs, s(R.string.hb_rain_days), () -> { daily=true;page=0;fillDetails(); }); root.addView(tabs);
        LinearLayout content = new LinearLayout(activity); content.setOrientation(expanded ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        detailsView = new RainWidgetView(activity,this); detailsView.setStyle(palette,opacity,reducedMotion);
        content.addView(detailsView,expanded ? new LinearLayout.LayoutParams(0,-1,1.2f) : new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll = new ScrollView(activity); detailsBody = new LinearLayout(activity); detailsBody.setOrientation(LinearLayout.VERTICAL); scroll.addView(detailsBody);
        content.addView(scroll,expanded ? new LinearLayout.LayoutParams(0,-1,1) : new LinearLayout.LayoutParams(-1,0,1.5f));
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions = new LinearLayout(activity);
        button(actions,s(R.string.hb_rain_previous),() -> { page=Math.max(0,page-1);fillDetails(); });
        button(actions,s(R.string.hb_rain_next),() -> { page++;fillDetails(); });
        button(actions,s(R.string.hb_rain_refresh),() -> refresh(true));
        if (!expanded) button(actions,s(R.string.hb_rain_fullscreen),() -> { expanded=true;openDetails(); });
        button(actions,s(R.string.hb_rain_back),() -> dialog.dismiss()); root.addView(actions);
        dialog.setContentView(root);
        dialog.setOnKeyListener((d,key,event) -> {
            if (event.getAction()==KeyEvent.ACTION_UP && (key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT) && !actions.hasFocus() && !tabs.hasFocus()) {
                page=Math.max(0,page+(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1));fillDetails();return true;
            } return false;
        });
        dialog.setOnDismissListener(d -> { if(detailsView!=null) detailsView.setRunning(false); detailsView=null; render(); });
        dialog.show();
        Window window=dialog.getWindow();
        if(window!=null) window.setLayout(expanded ? -1 : Math.min(dp(900),activity.getResources().getDisplayMetrics().widthPixels-dp(48)), expanded ? -1 : activity.getResources().getDisplayMetrics().heightPixels-dp(48));
        detailsView.setRunning(true); fillDetails(); handler.removeCallbacks(detailsTick);handler.postDelayed(detailsTick,60_000);
        dialog.setOnDismissListener(d -> { if(detailsView!=null) detailsView.setRunning(false);detailsView=null;dialog=null;handler.removeCallbacks(detailsTick);render(); });
    }
    private void fillDetails() {
        if(detailsBody==null) return; detailsBody.removeAllViews();
        if(forecast==null) { detailsBody.addView(label(status(),18));return; }
        Instant now=Instant.now(); RainSettings p=settings();
        detailsBody.addView(label(forecast.zone.getId()+" · "+status(),16));
        if(daily) {
            for(RainForecast.Period period:forecast.following(now,p.hours,p.days)) {
                detailsBody.addView(label(time(period.start,"EEE dd MMM HH:mm")+" → "+time(period.end,"EEE HH:mm")+"   "+amount(period.mm)+"   "+chance(period.chance),18));
                if(!period.complete) detailsBody.addView(label(s(R.string.hb_rain_incomplete),16));
                if(period.snowCm>0) detailsBody.addView(label(s(R.string.hb_rain_snow),16));
            }
        } else {
            List<RainForecast.Hour> hours=forecast.hourly(now,p.hours).hours;
            page=Math.min(page,(hours.size()-1)/6);
            for(int i=page*6;i<Math.min(hours.size(),page*6+6);i++) {
                RainForecast.Hour h=hours.get(i);
                detailsBody.addView(label(time(h.start,"EEE dd MMM HH:mm")+" → "+time(h.start+3600,"HH:mm")+"   "+amount(h.mm)+"   "+chance(h.chance)+(h.snowCm>0?" · "+s(R.string.hb_rain_snow):""),18));
            }
        }
        detailsBody.addView(label(s(R.string.hb_rain_precipitation),16));
        detailsBody.addView(label(s(R.string.hb_rain_updated,time(fetchedAt/1000,"dd MMM HH:mm"))+" · "+s(R.string.hb_rain_source),16));
    }
    private TextView label(String value,int size) { TextView t=new TextView(activity);t.setText(value);t.setTextColor(palette.text);t.setTextSize(size);t.setPadding(0,4,0,4);return t; }
    private void button(LinearLayout row,String title,Runnable action) { Button b=new Button(activity);b.setText(title);b.setTextSize(16);b.setOnClickListener(v->action.run());row.addView(b,new LinearLayout.LayoutParams(0,-2,1)); }
    PaletteColors palette = PaletteColors.from(0xff101824);
    int opacity=75; boolean reducedMotion;
    void style(PaletteColors palette,int opacity,boolean reducedMotion) {
        this.palette=palette;this.opacity=opacity;this.reducedMotion=reducedMotion;
        if(view!=null) view.setStyle(palette,opacity,reducedMotion);
    }
    private int dp(float value) { return Math.round(value*activity.getResources().getDisplayMetrics().density); }
}
