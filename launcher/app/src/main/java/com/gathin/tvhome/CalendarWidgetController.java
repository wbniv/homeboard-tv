package com.gathin.tvhome;

import android.content.Context;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Same-window remote menus for the single combined widget. */
final class CalendarWidgetController {
    interface Menus { void show(String title, String subtitle, String[] labels, IntConsumer action); }
    interface Details { void show(String title, String detail); }
    interface NumberEditor { void show(int current, Consumer<Integer> save); }
    private final Context c;
    private final CalendarSnapshot snapshot;
    private final CalendarWidgetSettings settings;
    private final Menus menus;
    private final Details details;
    private final NumberEditor editor;
    private final Runnable changed;
    private final Supplier<CalendarWidgetView> view;
    CalendarWidgetController(Context c, CalendarSnapshot snapshot, CalendarWidgetSettings settings, Menus menus,
                             Details details, NumberEditor editor, Runnable changed, Supplier<CalendarWidgetView> view) {
        this.c = c; this.snapshot = snapshot; this.settings = settings; this.menus = menus;
        this.details = details; this.editor = editor; this.changed = changed; this.view = view;
    }
    private String t(int resource) { return c.getString(resource); }
    private String toggle(int name, boolean enabled) { return c.getString(enabled ? R.string.hb_cal_enabled : R.string.hb_cal_disabled, t(name)); }
    void actions() {
        menus.show(t(R.string.hb_cal_calendar), c.getString(R.string.hb_cal_days, settings.holidayLookaheadDays),
                new String[]{t(R.string.hb_cal_all_dates), t(R.string.hb_cal_next_page), t(R.string.hb_cal_previous_page), t(R.string.hb_cal_configure)}, choice -> {
                    if (choice == 0) dates(); else if (choice == 3) configure();
                    else if (view.get() != null) view.get().turnPage(choice == 1 ? 1 : -1);
                });
    }
    void configure() {
        menus.show(t(R.string.hb_cal_configure), c.getString(R.string.hb_cal_days, settings.holidayLookaheadDays),
                new String[]{t(R.string.hb_cal_day_count), t(R.string.hb_cal_sources), t(R.string.hb_cal_weeks), t(R.string.hb_cal_week_start)}, choice -> {
                    if (choice == 0) dayCount(); else if (choice == 1) sources(); else if (choice == 2) weeks();
                    else details.show(t(R.string.hb_cal_week_start), t(R.string.hb_cal_week_explain));
                });
    }
    private void dayCount() {
        menus.show(t(R.string.hb_cal_day_count), "", new String[]{t(R.string.hb_cal_seven), t(R.string.hb_cal_thirty), t(R.string.hb_cal_custom)}, choice -> {
            if (choice == 2) editor.show(settings.holidayLookaheadDays, value -> { settings.holidayLookaheadDays = value; changed.run(); });
            else { settings.holidayLookaheadDays = choice == 0 ? 7 : 30; changed.run(); }
        });
    }
    private void weeks() {
        menus.show(t(R.string.hb_cal_weeks), "", new String[]{t(R.string.hb_cal_source_flowers), t(R.string.hb_cal_source_buddhist)}, source -> {
            String[] labels = {t(R.string.hb_cal_week_this), t(R.string.hb_cal_week_next), t(R.string.hb_cal_week_both)};
            CalendarWidgetSettings.Weeks current = source == 0 ? settings.flowerWeeks : settings.buddhistWeeks;
            menus.show(CalendarLabels.source(c, source == 0 ? "flowers" : "buddhist"), labels[current.ordinal()], labels, choice -> {
                if (source == 0) settings.flowerWeeks = CalendarWidgetSettings.Weeks.values()[choice];
                else settings.buddhistWeeks = CalendarWidgetSettings.Weeks.values()[choice];
                changed.run();
            });
        });
    }
    private void sources() {
        List<String> labels = new ArrayList<>();
        for (String id : CalendarWidgetSettings.SOURCES) labels.add(c.getString(settings.enabled.contains(id) ? R.string.hb_cal_enabled : R.string.hb_cal_disabled, CalendarLabels.source(c, id)));
        labels.add(toggle(R.string.hb_cal_bangkok, settings.bangkok)); labels.add(t(R.string.hb_cal_spain_scope));
        labels.add(toggle(R.string.hb_cal_working_days, settings.chinaWorkingDays)); labels.add(toggle(R.string.hb_cal_names, settings.thaiNames));
        labels.add(t(R.string.hb_cal_data_sources));
        menus.show(t(R.string.hb_cal_sources), "", labels.toArray(new String[0]), choice -> {
            if (choice < CalendarWidgetSettings.SOURCES.length) {
                String id = CalendarWidgetSettings.SOURCES[choice]; if (!settings.enabled.remove(id)) settings.enabled.add(id); changed.run(); return;
            }
            switch (choice - CalendarWidgetSettings.SOURCES.length) {
                case 0: settings.bangkok = !settings.bangkok; changed.run(); break;
                case 1: spanishScope(); break;
                case 2: settings.chinaWorkingDays = !settings.chinaWorkingDays; changed.run(); break;
                case 3: settings.thaiNames = !settings.thaiNames; changed.run(); break;
                default: coverage(); break;
            }
        });
    }
    private void spanishScope() {
        menus.show(t(R.string.hb_cal_spain_scope), "", new String[]{toggle(R.string.hb_cal_es_national, settings.spanishNational),
                toggle(R.string.hb_cal_es_region, settings.catalunya), toggle(R.string.hb_cal_es_province, settings.tarragona), toggle(R.string.hb_cal_es_local, settings.tivissa)}, choice -> {
            if (choice == 0) settings.spanishNational = !settings.spanishNational;
            if (choice == 1) settings.catalunya = !settings.catalunya;
            if (choice == 2) settings.tarragona = !settings.tarragona;
            if (choice == 3) settings.tivissa = !settings.tivissa;
            changed.run();
        });
    }
    private void coverage() {
        StringBuilder body = new StringBuilder(t(R.string.hb_cal_live_pending));
        for (String id : CalendarWidgetSettings.SOURCES) {
            CalendarSnapshot.Provider p = snapshot.providers.get(id);
            body.append("\n\n").append(p == null ? CalendarLabels.source(c, id) + ": " + t(R.string.hb_cal_unknown)
                    : c.getString(R.string.hb_cal_coverage_detail, CalendarLabels.source(c, id), p.start.toString(), p.end.toString(), t(R.string.hb_cal_offline)));
        }
        details.show(t(R.string.hb_cal_data_sources), body.toString());
    }
    void dates() {
        CalendarSnapshot.Display display = snapshot.display(settings, Instant.now());
        List<String> labels = new ArrayList<>(), bodies = new ArrayList<>();
        for (CalendarSnapshot.Day day : display.days) {
            labels.add(CalendarLabels.date(c, day.date)); StringBuilder body = new StringBuilder();
            for (CalendarSnapshot.Event e : day.events) {
                body.append(c.getString(R.string.hb_cal_source_detail, CalendarLabels.event(c, e, settings, Instant.now()), e.scope, e.url)).append("\n\n");
                if (e.anchor != null && (e.kind == CalendarSnapshot.Kind.MAKE || e.kind == CalendarSnapshot.Kind.SELL))
                    body.append(c.getString(R.string.hb_cal_flower_detail, CalendarLabels.date(c, e.anchor),
                            CalendarLabels.date(c, e.anchor.minusDays(3)) + "–" + CalendarLabels.date(c, e.anchor.minusDays(2)),
                            CalendarLabels.date(c, e.anchor.minusDays(1)) + "–" + CalendarLabels.date(c, e.anchor))).append("\n\n");
            }
            bodies.add(body.toString());
        }
        if (labels.isEmpty()) { details.show(t(R.string.hb_cal_calendar), t(display.incomplete.isEmpty() ? R.string.hb_cal_empty : R.string.hb_cal_incomplete)); return; }
        menus.show(t(R.string.hb_cal_all_dates), c.getString(R.string.hb_cal_days, settings.holidayLookaheadDays), labels.toArray(new String[0]), choice -> details.show(labels.get(choice), bodies.get(choice)));
    }
}
