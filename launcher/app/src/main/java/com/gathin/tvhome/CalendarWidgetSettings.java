package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Private, undoable display preferences. Provider JSON never contains these. */
final class CalendarWidgetSettings {
    static final String THAI = "thai", FLOWERS = "flowers", BUDDHIST = "buddhist", SPAIN = "spain", USA = "usa", CHINA = "china";
    static final String[] SOURCES = {THAI, FLOWERS, BUDDHIST, SPAIN, USA, CHINA};
    enum Weeks { THIS, NEXT, BOTH }
    final Set<String> enabled = new LinkedHashSet<>(Arrays.asList(SOURCES));
    int holidayLookaheadDays = 30;
    Weeks flowerWeeks = Weeks.BOTH, buddhistWeeks = Weeks.BOTH;
    boolean bangkok, thaiNames, chinaWorkingDays;
    boolean spanishNational = true, catalunya = true, tarragona = true, tivissa = true;

    static int days(String text) {
        if (text == null || !text.matches("[0-9]+")) throw new IllegalArgumentException("Positive whole days required");
        try {
            int value = Integer.parseInt(text);
            if (value > 0) return value;
        } catch (NumberFormatException invalid) { /* Reject overflow, never clamp. */ }
        throw new IllegalArgumentException("Positive whole days required");
    }
    static CalendarWidgetSettings parse(JSONObject json) {
        CalendarWidgetSettings s = new CalendarWidgetSettings();
        if (json == null) return s;
        try { s.holidayLookaheadDays = days(String.valueOf(json.get("holidayLookaheadDays"))); }
        catch (JSONException | IllegalArgumentException invalid) { s.holidayLookaheadDays = 30; }
        JSONArray sources = json.optJSONArray("enabled");
        if (sources != null) {
            s.enabled.clear();
            for (String id : SOURCES) for (int i = 0; i < sources.length(); i++)
                if (id.equals(sources.optString(i))) s.enabled.add(id);
        }
        s.flowerWeeks = weeks(json.optString("flowerWeeks", "BOTH"));
        s.buddhistWeeks = weeks(json.optString("buddhistWeeks", "BOTH"));
        s.bangkok = json.optBoolean("bangkok", false); s.thaiNames = json.optBoolean("thaiNames", false);
        s.chinaWorkingDays = json.optBoolean("chinaWorkingDays", false);
        s.spanishNational = json.optBoolean("spanishNational", true); s.catalunya = json.optBoolean("catalunya", true);
        s.tarragona = json.optBoolean("tarragona", true); s.tivissa = json.optBoolean("tivissa", true);
        return s;
    }
    private static Weeks weeks(String value) {
        try { return Weeks.valueOf(value); } catch (IllegalArgumentException invalid) { return Weeks.BOTH; }
    }
    JSONObject json() throws JSONException {
        return new JSONObject().put("holidayLookaheadDays", holidayLookaheadDays)
                .put("enabled", new JSONArray(enabled)).put("flowerWeeks", flowerWeeks.name()).put("buddhistWeeks", buddhistWeeks.name())
                .put("bangkok", bangkok).put("thaiNames", thaiNames).put("chinaWorkingDays", chinaWorkingDays)
                .put("spanishNational", spanishNational).put("catalunya", catalunya).put("tarragona", tarragona).put("tivissa", tivissa);
    }
}
