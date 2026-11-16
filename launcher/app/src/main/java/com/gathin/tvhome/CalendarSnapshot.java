package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Validated provider snapshot and pure range/grouping logic, shared with pipeline tools. */
final class CalendarSnapshot {
    static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    enum Kind { HOLIDAY, OBSERVED, MAKE, SELL, WAN_PHRA, WORKING }
    static final class Event {
        final String id, provider, country, title, original, scope, url;
        final LocalDate date, anchor;
        final Kind kind;
        Event(String id, String provider, String country, String title, String original, String scope,
              String url, LocalDate date, LocalDate anchor, Kind kind) {
            this.id = id; this.provider = provider; this.country = country; this.title = title; this.original = original;
            this.scope = scope; this.url = url; this.date = date; this.anchor = anchor; this.kind = kind;
        }
        String title(CalendarWidgetSettings settings) { return settings.thaiNames && !original.isEmpty() ? original : title; }
    }
    static final class Provider {
        final String id, country, status;
        final ZoneId zone;
        final LocalDate start, end;
        final List<Event> events;
        Provider(String id, String country, String status, ZoneId zone, LocalDate start, LocalDate end, List<Event> events) {
            this.id = id; this.country = country; this.status = status; this.zone = zone; this.start = start; this.end = end;
            this.events = Collections.unmodifiableList(events);
        }
    }
    static final class Window {
        final LocalDate start, end;
        Window(LocalDate start, LocalDate end) { this.start = start; this.end = end; }
        boolean contains(LocalDate date) { return !date.isBefore(start) && !date.isAfter(end); }
    }
    static final class Day {
        final LocalDate date;
        final List<Event> events = new ArrayList<>();
        Day(LocalDate date) { this.date = date; }
        List<String> countries() {
            List<String> result = new ArrayList<>();
            for (String country : Arrays.asList("TH", "ES", "US", "CN"))
                if (events.stream().anyMatch(e -> e.country.equals(country))) result.add(country);
            return result;
        }
    }
    static final class Display {
        final List<Day> days;
        final List<String> incomplete, offline;
        Display(List<Day> days, List<String> incomplete, List<String> offline) {
            this.days = Collections.unmodifiableList(days); this.incomplete = incomplete; this.offline = offline;
        }
    }
    final Map<String, Provider> providers;
    private CalendarSnapshot(Map<String, Provider> providers) { this.providers = Collections.unmodifiableMap(providers); }
    static CalendarSnapshot unavailable() { return new CalendarSnapshot(new LinkedHashMap<>()); }
    static CalendarSnapshot parse(String text) throws JSONException {
        try {
            JSONObject root = new JSONObject(text);
            if (root.getInt("schemaVersion") != 1) throw new IllegalArgumentException("Unsupported snapshot");
            Map<String, Provider> providers = new LinkedHashMap<>();
            JSONArray array = root.getJSONArray("providers");
            if (array.length() > 6) throw new IllegalArgumentException("Too many providers");
            for (int i = 0; i < array.length(); i++) {
                JSONObject p = array.getJSONObject(i);
                String id = p.getString("id"), country = p.getString("country"), status = p.getString("status");
                if (!Arrays.asList(CalendarWidgetSettings.SOURCES).contains(id) || providers.containsKey(id)
                        || !Arrays.asList("TH", "ES", "US", "CN").contains(country)
                        || !Arrays.asList("offline", "verified", "partial", "stale").contains(status))
                    throw new IllegalArgumentException("Invalid provider");
                String expected = "spain".equals(id) ? "ES" : "usa".equals(id) ? "US" : "china".equals(id) ? "CN" : "TH";
                if (!country.equals(expected)) throw new IllegalArgumentException("Country mismatch");
                ZoneId zone = ZoneId.of(p.getString("timeZone"));
                LocalDate start = LocalDate.parse(p.getString("coverageStart")), end = LocalDate.parse(p.getString("coverageEnd"));
                if (start.isAfter(end)) throw new IllegalArgumentException("Invalid coverage");
                List<Event> events = new ArrayList<>(); java.util.Set<String> seen = new java.util.HashSet<>();
                JSONArray entries = p.getJSONArray("events");
                if (entries.length() > 10000) throw new IllegalArgumentException("Too many events");
                for (int n = 0; n < entries.length(); n++) {
                    JSONObject e = entries.getJSONObject(n);
                    String eventId = e.getString("id"), title = e.getString("title"), url = e.getString("url"), scope = e.getString("scope");
                    URI uri = URI.create(url); LocalDate date = LocalDate.parse(e.getString("date"));
                    if (eventId.isEmpty() || !seen.add(eventId) || title.trim().isEmpty() || title.length() > 512
                            || !"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                            || date.isBefore(start) || date.isAfter(end)
                            || !Arrays.asList("national", "bangkok", "catalunya", "tarragona-province", "tivissa").contains(scope))
                        throw new IllegalArgumentException("Invalid event");
                    Kind kind = Kind.valueOf(e.getString("kind"));
                    LocalDate anchor = e.has("anchor") ? LocalDate.parse(e.getString("anchor")) : null;
                    if ((kind == Kind.MAKE || kind == Kind.SELL || kind == Kind.WAN_PHRA) && anchor == null)
                        throw new IllegalArgumentException("Missing anchor");
                    events.add(new Event(eventId, id, country, title, e.optString("original", ""), scope, url, date, anchor, kind));
                }
                events.sort(Comparator.comparing(e -> e.date));
                providers.put(id, new Provider(id, country, status, zone, start, end, events));
            }
            return new CalendarSnapshot(providers);
        } catch (IllegalArgumentException | java.time.DateTimeException invalid) {
            throw new JSONException("Invalid snapshot: " + invalid.getMessage());
        }
    }
    static boolean sellingBreak(Event event, Instant now) {
        java.time.ZonedDateTime local = now.atZone(BANGKOK);
        java.time.LocalTime time = local.toLocalTime();
        return event.kind == Kind.SELL && event.date.equals(local.toLocalDate())
                && !time.isBefore(java.time.LocalTime.of(11, 0)) && time.isBefore(java.time.LocalTime.of(16, 0));
    }
    static Window window(String source, CalendarWidgetSettings settings, Instant now, ZoneId zone) {
        LocalDate today = now.atZone(zone).toLocalDate();
        if (!CalendarWidgetSettings.FLOWERS.equals(source) && !CalendarWidgetSettings.BUDDHIST.equals(source))
            return new Window(today, today.plusDays((long) settings.holidayLookaheadDays - 1));
        CalendarWidgetSettings.Weeks weeks = CalendarWidgetSettings.FLOWERS.equals(source) ? settings.flowerWeeks : settings.buddhistWeeks;
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate start = weeks == CalendarWidgetSettings.Weeks.NEXT ? monday.plusDays(7) : today;
        LocalDate end = monday.plusDays(weeks == CalendarWidgetSettings.Weeks.THIS ? 6 : 13);
        return new Window(start, end);
    }
    Display display(CalendarWidgetSettings settings, Instant now) {
        Map<LocalDate, Day> days = new TreeMap<>();
        List<String> incomplete = new ArrayList<>(), offline = new ArrayList<>();
        for (String id : CalendarWidgetSettings.SOURCES) {
            if (!settings.enabled.contains(id)) continue;
            Provider provider = providers.get(id);
            if (provider == null) { incomplete.add(id); continue; }
            Window window = window(id, settings, now, provider.zone);
            if (provider.start.isAfter(window.start) || provider.end.isBefore(window.end)
                    || "partial".equals(provider.status) || "stale".equals(provider.status)) incomplete.add(id);
            if (!"verified".equals(provider.status)) offline.add(id);
            for (Event event : provider.events) {
                if (!window.contains(event.date) || !matches(event, settings)) continue;
                days.computeIfAbsent(event.date, Day::new).events.add(event);
            }
        }
        return new Display(new ArrayList<>(days.values()), incomplete, offline);
    }
    private static boolean matches(Event e, CalendarWidgetSettings s) {
        if (e.kind == Kind.WORKING && !s.chinaWorkingDays) return false;
        switch (e.scope) {
            case "bangkok": return s.bangkok;
            case "catalunya": return s.catalunya;
            case "tarragona-province": return s.tarragona;
            case "tivissa": return s.tivissa;
            default: return !e.provider.equals("spain") || s.spanishNational;
        }
    }
}
