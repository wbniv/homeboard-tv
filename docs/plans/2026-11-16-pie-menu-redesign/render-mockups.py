#!/usr/bin/env python3
"""Render paired structural mockups for every Homeboard-authored menu screen."""

from html import escape
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "mockups"
OUT.mkdir(exist_ok=True)

SWATCHES = [
    ("Midnight", "#101a2a"), ("Forest", "#17382d"), ("Ocean", "#123b51"),
    ("Plum", "#3c213e"), ("Sand", "#59422f"), ("Rose", "#542c3b"),
    ("Teal", "#0c3f42"), ("Slate", "#303b49"), ("Sunset", "#6a3828"),
    ("Black", "#151719"), ("Charcoal", "#272a2d"), ("Indigo", "#292b59"),
    ("Lagoon", "#15566a"), ("Moss", "#38402b"), ("Cedar", "#513a32"),
    ("Amber", "#68421d"), ("Cobalt", "#153c78"), ("Pine", "#123c33"),
    ("Emerald", "#0f513a"), ("Copper", "#65402e"),
]

# title, code-visible current choices, proposed compass choices (N,E,S,W), optional center.
MENUS = [
    ("App actions", ["Move", "Favorite", "Delete tile…", "Uninstall…", "App info", "Home settings"],
     ["Move", "Favorite", "Delete tile", "More…"], "Actions"),
    ("Air-quality widget actions", ["Configure location/source", "Move", "Resize", "Delete widget…", "Home settings"],
     ["Configure", "Move", "Resize", "More…"], "Air quality"),
    ("Delete tile confirmation", ["Keep tile", "Delete tile"],
     ["Delete immediately", "Undo banner", "", ""], "Undo last change"),
    ("System-app uninstall message", ["Keep app", "Open app info"],
     ["Keep app", "App info", "", ""], "System app"),
    ("Home settings", ["Widgets & restore", "Appearance", "Home behavior", "Default Home app", "About"],
     ["Widgets", "Appearance", "Behavior", "Undo history"], "More…"),
    ("Widgets & restore", ["Add widget", "Manage deleted app tiles"],
     ["Add widget", "Restore hidden app", "", ""], "Widgets"),
    ("Add to Home", ["Air quality widget", "Favorite an app from All Apps"],
     ["Air quality", "Favorite an app", "", ""], "Add to Home"),
    ("Appearance", ["Background and wallpaper", "Circular icon mask · On/Off", "Icons"],
     ["Background", "Icon shape", "Icons", ""], "Appearance"),
    ("Background and wallpaper", ["Choose color palette…"],
     ["Color palette", "", "", ""], "Background"),
    ("Color palette · 20 colors", [name for name, _ in SWATCHES],
     [name for name, _ in SWATCHES], "Palette grid"),
    ("Circular icon mask", ["Original icon shape", "Circular mask"],
     ["Original", "Circular", "", ""], "Icon shape"),
    ("App artwork / Icons", ["Launcher icons · full-size grid", "TV banners · 4 × 2"],
     ["Launcher icons", "TV banners", "", ""], "Icons"),
    ("Home behavior", ["Grid columns", "Grid rows", "New panel size", "Reduced motion"],
     ["Columns", "Rows", "Panel size", "Motion"], "Home behavior"),
    ("Reduced motion", ["Off", "On"], ["Off", "On", "", ""], "Motion"),
    ("Panel size / New panel size", ["Small", "Medium", "Large"],
     ["Small", "Medium", "Large", ""], "Panel size"),
    ("Grid columns", ["Auto", "4", "6", "8", "10", "12"],
     ["Auto", "4", "6", "8", "10", "12"], "Columns · bounded"),
    ("Grid rows", ["Auto", "2 rows", "3 rows", "4 rows", "… TV fit"],
     ["Auto", "2", "3", "4", "TV fit"], "Rows · bounded"),
    ("Restore empty state", ["Close"], ["No hidden shortcuts", "Back", "", ""], "Restore hidden app"),
    ("Restore hidden app", ["App choices · paged, up to 9"],
     ["App 1", "App 2", "App 3", "More…"], "Restore shortcut"),
    ("About Homeboard TV", ["About card · Back to Home settings"],
     ["Back", "", "", ""], "About"),
    ("Air-quality actions", ["Refresh reading", "Configure location/source"],
     ["Refresh", "Configure", "", ""], "Air quality"),
    ("Air-quality source/location", ["Nearest model grid", "Enter coordinates", "Bangkok station"],
     ["Near me", "Coordinates", "Bangkok", ""], "Data source"),
    ("Coordinate entry", ["Latitude/longitude · Save · Cancel"],
     ["Save", "Cancel", "", ""], "Enter location"),
    ("Bangkok station picker", ["Station names · paged list"],
     ["Station 1", "Station 2", "Station 3", "More…"], "Stations"),
    ("Move mode", ["Arrow-key grid preview · OK saves · Back cancels"],
     ["Preview grid", "Arrow to move", "OK saves", "Back cancels"], "Move tile"),
    ("Resize mode", ["Arrow-key resize HUD · OK saves · Back cancels"],
     ["Grid preview", "←/→ width", "↑/↓ height", "OK / Back"], "Resize tile"),
    ("Remote Quick Settings", ["TV settings", "Picture", "Network", "Apps", "Home settings"],
     ["TV settings", "Picture", "Network", "More…"], "Quick settings"),
    ("Inactive legacy settings code", ["Grid", "Wallpaper", "Dock", "Clock", "Open on start"],
     ["Inactive", "Homeboard settings", "", ""], "No active entry"),
    ("Inactive legacy wallpaper picker", ["Image names · None · Theme default · Cancel"],
     ["Inactive", "Background palette", "", ""], "No active entry"),
    ("Inactive legacy open-on-start picker", ["Off · app names · Cancel"],
     ["Inactive", "Home behavior", "", ""], "No active entry"),
    ("Inactive legacy advanced-effects warning", ["Warning · Enable · Cancel"],
     ["Inactive", "Homeboard settings", "", ""], "No active entry"),
    ("System handoffs", ["Android Home role, app info, uninstall, system settings"],
     ["Android owns", "Review there", "Back returns", ""], "System dialog"),
    ("Undo history", ["No Homeboard undo history"],
     ["Delete shortcut", "Grid rows", "Background", "More…"], "Undo selected change"),
]

def esc(s):
    return escape(s, quote=True)

def text(x, y, value, size=15, color="#e9edf2", weight="400", anchor="start"):
    if not value:
        return ""
    return f'<text x="{x}" y="{y}" font-family="Arial,sans-serif" font-size="{size}" fill="{color}" font-weight="{weight}" text-anchor="{anchor}">{esc(value)}</text>'

def current_screen(x, y, title, labels, w=850, h=220):
    out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#111922" stroke="#34414d"/>',
           text(x + 18, y + 25, title, 17, "#edf3f8", "700")]
    if title == "Color palette · 20 colors":
        cw, ch, gap = 186, 30, 6
        for i, (name, color) in enumerate(SWATCHES):
            row, col = divmod(i, 4)
            xx, yy = x + 18 + col * (cw + gap), y + 38 + row * (ch + 6)
            out.append(f'<rect x="{xx}" y="{yy}" width="{cw}" height="{ch}" rx="7" fill="#222d38" stroke="#3a4652"/>')
            out.append(f'<circle cx="{xx+18}" cy="{yy+ch/2}" r="7" fill="{color}"/>')
            out.append(text(xx + 32, yy + 20, name, 12, "#dce2e8"))
        return "".join(out)
    if title == "About Homeboard TV":
        out += [text(x + 32, y + 65, "Homeboard TV · Version 1.0", 21, "#f2f5f7", "700"),
                text(x + 32, y + 98, "A customizable TV home screen for widgets and favorite apps.", 15, "#c1ccd4"),
                f'<line x1="{x+32}" y1="{y+116}" x2="{x+w-32}" y2="{y+116}" stroke="#536777"/>',
                text(x + 32, y + 145, "Built on CleanYourTV · MIT License", 14, "#c1ccd4"),
                text(x + 32, y + 175, "Outfit · SIL OFL 1.1  |  AndroidX · Apache 2.0", 13, "#9eabb7"),
                text(x + w - 24, y + h - 18, "Back to Home settings", 12, "#80bce3", anchor="end")]
        return "".join(out)
    cols = 3
    n = min(len(labels), 6)
    for i, label in enumerate(labels[:n]):
        row, col = divmod(i, cols)
        bw, bh, gap = 266, 52, 8
        xx, yy = x + 14 + col * (bw + gap), y + 34 + row * (bh + 6)
        out.append(f'<rect x="{xx}" y="{yy}" width="{bw}" height="{bh}" rx="8" fill="#222d38" stroke="#3a4652"/>')
        out.append(f'<circle cx="{xx+24}" cy="{yy+26}" r="10" fill="#80bce3"/>')
        out.append(text(xx + 44, yy + 31, label[:26], 14, "#d7e0e8"))
    if len(labels) > 6:
        out.append(text(x + w - 16, y + h - 8, "paged / more", 9, "#9eabb7", anchor="end"))
    return "".join(out)

def pie_screen(x, y, title, choices, center, w=850, h=420):
    if title == "Undo history":
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#15212b" stroke="#4a5967"/>',
               text(x + 28, y + 42, "Recent Homeboard changes · newest first", 21, "#f2f5f7", "700")]
        entries = [("Delete shortcut · Ticket to Ride", "Just now"),
                   ("Grid rows · 3 → Auto", "2 min ago"),
                   ("Background · Ocean → Midnight", "5 min ago")]
        for i, (label, when) in enumerate(entries):
            yy = y + 82 + i * 82
            fill = "#37556b" if i == 0 else "#1d2b36"
            out += [f'<rect x="{x+24}" y="{yy}" width="{w-48}" height="66" rx="9" fill="{fill}" stroke="#536777"/>',
                    text(x + 44, yy + 28, label, 16, "#f2f5f7", "700"),
                    text(x + 44, yy + 50, when, 12, "#c1ccd4"),
                    text(x + w - 44, yy + 40, "UNDO", 14, "#ffffff", "700", "end")]
        out.append(text(x + w - 18, y + h - 18, "Select a change to restore its previous settings", 13, "#91a1ae", anchor="end"))
        return "".join(out)
    if title == "Delete tile confirmation":
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#15212b" stroke="#4a5967"/>',
               text(x + 28, y + 45, "Delete shortcut now; restore it with Undo", 22, "#f2f5f7", "700"),
               text(x + 28, y + 78, "The app stays installed. Its All Apps shortcut is removed.", 16, "#aebcc7")]
        bx, by, bw, bh = x + 50, y + 220, w - 100, 92
        out += [f'<rect x="{bx}" y="{by}" width="{bw}" height="{bh}" rx="14" fill="#253746" stroke="#71899b" stroke-width="2"/>',
                text(bx + 24, by + 38, "Shortcut deleted", 20, "#f2f5f7", "700"),
                text(bx + 24, by + 66, "Undo is available in the banner and Home settings history", 15, "#aebcc7"),
                f'<rect x="{bx+bw-156}" y="{by+17}" width="130" height="58" rx="10" fill="#37556b" stroke="#91bdd6" stroke-width="2"/>',
                text(bx + bw - 91, by + 52, "UNDO", 18, "#ffffff", "700", "middle"),
                text(x + w - 18, y + h - 18, "Delete is immediate · Undo restores the shortcut", 13, "#91a1ae", anchor="end")]
        return "".join(out)
    if title in {"Grid columns", "Grid rows"}:
        is_columns = title == "Grid columns"
        values = ["Auto", "4", "6", "8", "10", "12"] if is_columns else ["Auto", "2", "3", "4", "5", "6"]
        current_value = "Auto"
        current_index = values.index(current_value)
        previous = "‹   " + (values[current_index - 1] if current_index > 0 else "End")
        following = (values[current_index + 1] if current_index + 1 < len(values) else "End") + "   ›"
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#15212b" stroke="#4a5967"/>',
               text(x + 28, y + 42, f"Home behavior  ›  {title}", 22, "#f2f5f7", "700"),
               text(x + 28, y + 74, "Only values that fit this TV are available. Endpoints stop; choices do not wrap.", 15, "#aebcc7")]
        cy = y + 220
        card_w = 230
        positions = [x + 65, x + w/2 - card_w/2, x + w - 65 - card_w]
        for i, value in enumerate([previous, current_value, following]):
            selected = i == 1
            out.append(f'<rect x="{positions[i]}" y="{cy-45}" width="{card_w}" height="90" rx="12" fill="{"#37556b" if selected else "#1d2b36"}" stroke="{"#91bdd6" if selected else "#536777"}" stroke-width="{3 if selected else 1.5}"/>')
            out.append(text(positions[i]+card_w/2, cy+8, value, 19 if selected else 15,
                            "#ffffff" if selected else "#c1ccd4", "700" if selected else "400", "middle"))
        choices_hint = "Auto · 4 · 6 · 8 · 10 · 12 columns" if is_columns else "Auto · supported rows through this TV's fit"
        out += [text(x + w/2, cy + 76, choices_hint, 15, "#80bce3", "700", "middle"),
                text(x + w/2, y + h - 28, "← / → change · OK apply · Back cancel · Undo after apply", 14, "#91a1ae", anchor="middle")]
        return "".join(out)
    if title == "About Homeboard TV":
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#15212b" stroke="#4a5967"/>',
               f'<rect x="{x+28}" y="{y+28}" width="80" height="80" rx="20" fill="#37556b" stroke="#91bdd6" stroke-width="2"/>',
               text(x + 68, y + 82, "H", 42, "#ffffff", "700", "middle"),
               text(x + 130, y + 58, "Homeboard TV", 27, "#f2f5f7", "700"),
               text(x + 130, y + 88, "Version 1.0 · A flexible home for your TV", 16, "#aebcc7"),
               text(x + 32, y + 160, "WHAT IT DOES", 13, "#80bce3", "700"),
               text(x + 32, y + 188, "Put your favorite apps and useful widgets on Home.", 18, "#f2f5f7"),
               f'<rect x="{x+28}" y="{y+222}" width="370" height="108" rx="10" fill="#1d2b36" stroke="#536777"/>',
               text(x + 48, y + 250, "BUILT ON CLEANYOURTV", 13, "#80bce3", "700"),
               text(x + 48, y + 278, "Gathin and contributors · MIT License", 15, "#f2f5f7"),
               f'<rect x="{x+414}" y="{y+222}" width="400" height="108" rx="10" fill="#1d2b36" stroke="#536777"/>',
               text(x + 434, y + 250, "OPEN-SOURCE NOTICES", 13, "#80bce3", "700"),
               text(x + 434, y + 278, "Outfit · SIL OFL 1.1  |  AndroidX · Apache 2.0", 14, "#f2f5f7"),
               f'<rect x="{x+w-154}" y="{y+h-66}" width="126" height="42" rx="8" fill="#253746" stroke="#71899b"/>',
               text(x+w-91, y+h-39, "←  BACK", 15, "#f2f5f7", "700", "middle")]
        return "".join(out)
    if title == "Restore empty state":
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#15212b" stroke="#4a5967"/>',
               text(x+w/2, y+155, "No hidden app shortcuts", 25, "#f2f5f7", "700", "middle"),
               text(x+w/2, y+190, "Deleted Homeboard tiles leave the app installed.", 17, "#aebcc7", anchor="middle"),
               text(x+w/2, y+220, "When you hide a shortcut, you can restore it here.", 17, "#aebcc7", anchor="middle"),
               text(x+w/2, y+h-30, "Back to Widgets & restore", 14, "#80bce3", "700", "middle")]
        return "".join(out)
    if title in {"Color palette · 20 colors", "Background and wallpaper"}:
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#111922" stroke="#34414d"/>',
               text(x + 18, y + 25,
                    "Background opens palette directly" if title == "Background and wallpaper"
                    else "Palette · cards themselves are full-color swatches",
                    16, "#edf3f8", "700")]
        cw, ch, gap = 186, 52, 6
        for i, (name, color) in enumerate(SWATCHES):
            row, col = divmod(i, 4)
            xx, yy = x + 18 + col * (cw + gap), y + 42 + row * (ch + 8)
            dark = name in {"Midnight", "Forest", "Ocean", "Plum", "Rose", "Teal", "Slate", "Black", "Charcoal", "Indigo", "Lagoon", "Moss", "Cobalt", "Pine", "Emerald"}
            fg = "#f7f8fa" if dark else "#21190f"
            out.append(f'<rect x="{xx}" y="{yy}" width="{cw}" height="{ch}" rx="7" fill="{color}" stroke="#e5ebef" stroke-width="1.4"/>')
            out.append(text(xx + 12, yy + 20, name + (" · Current" if i == 0 else ""), 12, fg, "700"))
        return "".join(out)
    cx, cy = x + w / 2, y + h / 2
    outer, inner = 180, 66
    out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="#15212b" stroke="#4a5967"/>']
    starts = [-135, -45, 45, 135]
    for i, start in enumerate(starts):
        if i >= len(choices) or not choices[i]:
            continue
        end = start + 82
        sx, sy = cx + outer * math.cos(math.radians(start)), cy + outer * math.sin(math.radians(start))
        ex, ey = cx + outer * math.cos(math.radians(end)), cy + outer * math.sin(math.radians(end))
        iex, iey = cx + inner * math.cos(math.radians(end)), cy + inner * math.sin(math.radians(end))
        isx, isy = cx + inner * math.cos(math.radians(start)), cy + inner * math.sin(math.radians(start))
        path = f'M {sx:.1f} {sy:.1f} A {outer} {outer} 0 0 1 {ex:.1f} {ey:.1f} L {iex:.1f} {iey:.1f} A {inner} {inner} 0 0 0 {isx:.1f} {isy:.1f} Z'
        out.append(f'<path d="{path}" fill="#253746" stroke="#71899b" stroke-width="1.5"/>')
    out += [f'<circle cx="{cx}" cy="{cy}" r="{inner-3}" fill="#111922" stroke="#91a4b4" stroke-width="2"/>',
            text(cx, cy - 10, title[:24], 16, "#f2f5f7", "700", "middle"),
            text(cx, cy + 15, "HOME  ›  MENU", 11, "#9fb1c1", "400", "middle"),
            text(cx, cy + 42, center[:18], 12, "#80bce3", "700", "middle")]
    positions = [(cx, cy-148, "N", "middle"), (cx+137, cy+5, "E", "start"),
                 (cx, cy+164, "S", "middle"), (cx-137, cy+5, "W", "end")]
    for i, (px, py, compass, anchor) in enumerate(positions):
        label = choices[i] if i < len(choices) else ""
        if not label:
            continue
        label = label[:19]
        if compass in ("N", "S"):
            py = py + (15 if compass == "N" else -7)
        icon_x = px if compass in ("N", "S") else cx + (92 if compass == "E" else -92)
        icon_y = cy + (-105 if compass == "N" else 105 if compass == "S" else 0)
        out.append(f'<circle cx="{icon_x}" cy="{icon_y}" r="11" fill="#80bce3"/>')
        out.append(text(px + (12 if compass == "E" else -12 if compass == "W" else 0), py,
                        label, 15, "#eff5f9", "700", anchor))
    out.append(text(x + w - 14, y + h - 12, "Direction chooses · OK More/Back · Back returns", 13, "#91a1ae", anchor="end"))
    return "".join(out)

def render_chunk(index, entries):
    row_h, top = 700, 76
    height = top + row_h * len(entries)
    width = 910
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" role="img">',
             '<rect width="100%" height="100%" fill="#0b1118"/>',
             '<style>text{dominant-baseline:alphabetic}</style>',
             text(30, 38, f"Menu redesign · paired wireframes {index}", 24, "#f3f6f9", "700"),
             text(30, 61, "CURRENT: current menu     REDESIGN: proposed menu", 13, "#aab8c4")]
    for i, (title, current, choices, center) in enumerate(entries):
        y = top + i * row_h
        parts += [text(30, y + 18, title, 15, "#80bce3", "700"),
                  text(30, y + 39, "CURRENT", 11, "#aab8c4", "700"),
                  current_screen(30, y + 46, title, current),
                  text(30, y + 276, "REDESIGN", 11, "#aab8c4", "700"),
                  pie_screen(30, y + 283, title, choices, center)]
    parts.append('</svg>')
    (OUT / f"paired-{index:02d}.svg").write_text("\n".join(parts), encoding="utf-8")

for chunk, start in enumerate(range(0, len(MENUS), 4), 1):
    render_chunk(chunk, MENUS[start:start + 4])
