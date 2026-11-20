#!/usr/bin/env python3
"""Draw the concept sketches for ../2026-11-21-3d-timepieces.md.

Usage: python3 build.py        writes NN-slug.svg beside this file
       python3 build.py -h     shows this text

Every sketch is a schematic in one small isometric toolkit, not a render.
Most show 10:08:22 so the reading can be compared between ideas.
"""
import math
import pathlib
import random
import sys

OUT = pathlib.Path(__file__).resolve().parent
BG = "#0f1420"
BRASS, STEEL, WATER, SAND = "#d9a441", "#9fb3c8", "#4fc3f7", "#e8c98a"
RED, GREEN, VIOLET, INK = "#ff6b5e", "#7bd389", "#b392f0", "#e8eef7"
MUTE, DARK, WOOD, STONE = "#7f8aa3", "#2a3347", "#b07a4a", "#aab0bc"
SUN = "#ffd166"
HA, MA, SA = 304.0, 48.0, 132.0          # hand angles for 10:08:22
UW, UP, NW = (0.9, -0.436, 0.0), (0.0, 0.0, 1.0), (0.436, 0.9, 0.0)


def cd(t):
    return math.cos(math.radians(t))


def sd(t):
    return math.sin(math.radians(t))


def shade(c, f):
    r, g, b = (int(c[i:i + 2], 16) for i in (1, 3, 5))
    if f <= 1:
        r, g, b = (int(v * f) for v in (r, g, b))
    else:
        r, g, b = (int(v + (255 - v) * min(f - 1, 1)) for v in (r, g, b))
    return f"#{r:02x}{g:02x}{b:02x}"


def add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def mul(a, k):
    return tuple(x * k for x in a)


def at(o, u, v, a, b):
    return tuple(o[i] + u[i] * a + v[i] * b for i in range(3))


def q(a, z, n=0.0):
    """Point on the three-quarter-view wall: a along it, z up, n toward the viewer."""
    return (UW[0] * a + NW[0] * n, UW[1] * a + NW[1] * n, z)


def hd(theta, r):
    """Floor direction for a clock angle: 0 is away from the viewer, clockwise from above."""
    k = r / math.sqrt(2)
    return (k * (sd(theta) - cd(theta)), k * (-sd(theta) - cd(theta)))


def Hh(theta, r, z=0.0):
    x, y = hd(theta, r)
    return (x, y, z)


PL = {"xy": ((1, 0, 0), (0, 1, 0)), "xz": ((1, 0, 0), (0, 0, 1)),
      "yz": ((0, 1, 0), (0, 0, 1)), "w": (UW, UP)}
BACK = {"xy": (0, 0, -1), "xz": (0, -1, 0), "yz": (-1, 0, 0), "w": (-NW[0], -NW[1], 0)}
SEG = {"a": ((0, 2), (1, 2)), "b": ((1, 2), (1, 1)), "c": ((1, 1), (1, 0)), "d": ((0, 0), (1, 0)),
       "e": ((0, 0), (0, 1)), "f": ((0, 1), (0, 2)), "g": ((0, 1), (1, 1))}
DIG = {"0": "abcdef", "1": "bc", "2": "abged", "3": "abgcd", "4": "fgbc", "5": "afgcd",
       "6": "afgedc", "7": "abc", "8": "abcdefg", "9": "abcdfg"}
F3 = {"0": "111101101101111", "1": "010110010010111", "2": "111001111100111",
      "3": "111001111001111", "4": "101101111001001", "5": "111100111001111",
      "6": "111100111101111", "7": "111001001001001", "8": "111101111101111",
      "9": "111101111001111", ":": "000010000010000"}


def segs(text, o, u, v, w, h, gap):
    """Seven-segment strokes for text in the plane (o, u, v); returns (char, name, p, q)."""
    out, cx = [], 0.0
    for ch in text:
        if ch == ":":
            for yy in (0.6, 1.4):
                p = at(o, u, v, cx + w * 0.25, yy * h)
                out.append((ch, ":", p, at(p, u, v, 0, h * 0.12)))
            cx += w * 0.5 + gap
            continue
        for name in DIG[ch]:
            (x1, y1), (x2, y2) = SEG[name]
            out.append((ch, name, at(o, u, v, cx + x1 * w, y1 * h), at(o, u, v, cx + x2 * w, y2 * h)))
        cx += w + gap
    return out


def dots(text):
    """Lit cells of a 3x5 dot font, as (column, row) with row 0 at the top."""
    return [(i * 4 + k % 3, k // 3) for i, ch in enumerate(text) for k, b in enumerate(F3[ch]) if b == "1"]


class S:
    def __init__(s):
        s.e, s.g = [], {}
        s.view()

    def view(s, ox=240, oy=165, k=1.0):
        s.ox, s.oy, s.k = ox, oy, k

    def p(s, x, y, z=0.0):
        return (s.ox + (x - y) * 0.866 * s.k, s.oy + (x + y) * 0.5 * s.k - z * s.k)

    def _pts(s, pts):
        return " ".join(f"{a:.1f},{b:.1f}" for a, b in (s.p(*t) for t in pts))

    def raw(s, txt):
        s.e.append(txt)

    def poly(s, pts, fill, stroke=None, sw=1, op=1):
        s.e.append(f'<polygon points="{s._pts(pts)}" fill="{fill}" stroke="{stroke or "none"}" '
                   f'stroke-width="{sw}" stroke-linejoin="round" opacity="{op}"/>')

    def line(s, pts, col, sw=2, dash=None, op=1):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        s.e.append(f'<polyline points="{s._pts(pts)}" fill="none" stroke="{col}" stroke-width="{sw}" '
                   f'stroke-linecap="round" stroke-linejoin="round" opacity="{op}"{d}/>')

    def box(s, x, y, z, dx, dy, dz, col, op=1):
        X, Y, Z = x + dx, y + dy, z + dz
        s.poly([(x, Y, z), (X, Y, z), (X, Y, Z), (x, Y, Z)], shade(col, 0.6), op=op)
        s.poly([(X, y, z), (X, Y, z), (X, Y, Z), (X, y, Z)], shade(col, 0.8), op=op)
        s.poly([(x, y, Z), (X, y, Z), (X, Y, Z), (x, Y, Z)], col, op=op)

    def ring(s, c, r, plane="xy", fill="none", stroke=None, sw=1.5, teeth=0, depth=0.14,
             a0=0, a1=360, op=1, dash=None):
        u, v = PL[plane]
        n = max(48, teeth * 8)
        pts = []
        for i in range(n + 1):
            f = i / n
            rr = r * (1 - depth) if teeth and (f * teeth) % 1 >= 0.5 else r
            t = a0 + (a1 - a0) * f
            pts.append(s.p(*at(c, u, v, rr * cd(t), rr * sd(t))))
        d = "M" + " L".join(f"{a:.1f},{b:.1f}" for a, b in pts) + ("Z" if a1 - a0 >= 360 else "")
        da = f' stroke-dasharray="{dash}"' if dash else ""
        s.e.append(f'<path d="{d}" fill="{fill}" stroke="{stroke or "none"}" stroke-width="{sw}" '
                   f'stroke-linejoin="round" stroke-linecap="round" opacity="{op}"{da}/>')

    def gear(s, c, r, plane, teeth, col, th=4):
        u, v = PL[plane]
        s.ring(add(c, mul(BACK[plane], th)), r, plane, fill=shade(col, 0.45), teeth=teeth)
        s.ring(c, r, plane, fill=col, stroke=shade(col, 0.55), sw=0.8, teeth=teeth)
        s.ring(c, r * 0.58, plane, fill="#141b2b")
        for t in (20, 110, 200, 290):
            s.line([c, at(c, u, v, r * 0.62 * cd(t), r * 0.62 * sd(t))], col, max(1.5, r * 0.11))
        s.ring(c, r * 0.18, plane, fill=shade(col, 1.3))

    def cyl(s, x, y, z, r, h, col, op=1):
        cx, cy = s.p(x, y, z)
        tx, ty = s.p(x, y, z + h)
        rx, ry = r * 1.2247 * s.k, r * 0.7071 * s.k
        s.e.append(f'<path d="M{cx - rx:.1f},{cy:.1f} A{rx:.1f},{ry:.1f} 0 0 0 {cx + rx:.1f},{cy:.1f} '
                   f'L{tx + rx:.1f},{ty:.1f} L{tx - rx:.1f},{ty:.1f} Z" fill="{shade(col, 0.66)}" opacity="{op}"/>')
        s.e.append(f'<ellipse cx="{tx:.1f}" cy="{ty:.1f}" rx="{rx:.1f}" ry="{ry:.1f}" fill="{col}" opacity="{op}"/>')

    def _g(s, col):
        if col not in s.g:
            s.g[col] = f"g{len(s.g)}"
        return s.g[col]

    def ball(s, x, y, z, r, col, op=1):
        cx, cy = s.p(x, y, z)
        s.e.append(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r * s.k:.1f}" fill="url(#{s._g(col)})" opacity="{op}"/>')

    def c2(s, cx, cy, r, fill, stroke=None, sw=1, op=1, dash=None):
        da = f' stroke-dasharray="{dash}"' if dash else ""
        s.e.append(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r}" fill="{fill}" stroke="{stroke or "none"}" '
                   f'stroke-width="{sw}" opacity="{op}"{da}/>')

    def l2(s, x1, y1, x2, y2, col, sw=1, dash=None, op=1):
        da = f' stroke-dasharray="{dash}"' if dash else ""
        s.e.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{col}" '
                   f'stroke-width="{sw}" stroke-linecap="round" opacity="{op}"{da}/>')

    def t(s, x, y, txt, col=MUTE, size=10.5, anchor="start", weight=400):
        s.e.append(f'<text x="{x:.1f}" y="{y:.1f}" fill="{col}" font-size="{size}" text-anchor="{anchor}" '
                   f'font-weight="{weight}">{txt}</text>')

    def t3(s, pt, txt, **kw):
        x, y = s.p(*pt)
        s.t(x, y, txt, **kw)

    def tp(s, o, u, v, txt, size, col=INK, anchor="middle", weight=700):
        """Text lying in the plane (o, u, v)."""
        ox, oy = s.p(*o)
        ux, uy = s.p(*add(o, u))
        vx, vy = s.p(*add(o, v))
        s.e.append(f'<text transform="matrix({ux - ox:.3f},{uy - oy:.3f},{ox - vx:.3f},{oy - vy:.3f},{ox:.1f},{oy:.1f})" '
                   f'fill="{col}" font-size="{size}" text-anchor="{anchor}" font-weight="{weight}">{txt}</text>')

    def note(s, x, y, txt, to=None, anchor="start", col=MUTE):
        s.t(x, y, txt, col=col, anchor=anchor)
        if to is None:
            return
        tx, ty = s.p(*to) if len(to) == 3 else to
        w = len(txt) * 5.5
        x0 = x if anchor == "start" else (x - w if anchor == "end" else x - w / 2)
        sx = x0 - 4 if tx < x0 + w / 2 else x0 + w + 4
        s.l2(sx, y - 3.5, tx, ty, MUTE, 0.8, op=0.8)
        s.c2(tx, ty, 1.8, MUTE)

    def arrow(s, x1, y1, x2, y2, bend=0.0, col="#c9d3e6", sw=1.3):
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        dx, dy = x2 - x1, y2 - y1
        s.e.append(f'<path d="M{x1:.1f},{y1:.1f} Q{mx - dy * bend:.1f},{my + dx * bend:.1f} {x2:.1f},{y2:.1f}" '
                   f'fill="none" stroke="{col}" stroke-width="{sw}" marker-end="url(#ah)"/>')

    def hands(s, c, rh, rm, rs=None):
        z = c[2]
        s.line([c, add((c[0], c[1], 0), Hh(HA, rh, z))], INK, 4)
        s.line([c, add((c[0], c[1], 0), Hh(MA, rm, z))], INK, 2.5)
        if rs:
            s.line([c, add((c[0], c[1], 0), Hh(SA, rs, z))], RED, 1.2)

    def save(s, num, slug, title, reads):
        defs = ['<marker id="ah" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" '
                'orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#c9d3e6"/></marker>']
        for col, gid in s.g.items():
            defs.append(f'<radialGradient id="{gid}" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="{shade(col, 1.55)}"/>'
                        f'<stop offset=".5" stop-color="{col}"/><stop offset="1" stop-color="{shade(col, 0.42)}"/></radialGradient>')
        head = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 270" width="480" height="270" '
                f'font-family="Inter, \'Segoe UI\', \'DejaVu Sans\', sans-serif" role="img" aria-label="{title}">')
        body = [head, "<defs>" + "".join(defs) + "</defs>", f'<rect width="480" height="270" rx="14" fill="{BG}"/>']
        body += s.e
        body.append(f'<text x="16" y="24" fill="{INK}" font-size="12.5" font-weight="700">{num:02d}  {title}</text>')
        body.append(f'<text x="16" y="258" fill="{BRASS}" font-size="10.5">{reads}</text>')
        body.append("</svg>")
        (OUT / f"{num:02d}-{slug}.svg").write_text("\n".join(body) + "\n", encoding="utf-8")


# --------------------------------------------------------------- gears

def exploded(s):
    s.view(oy=208)
    R = 58

    def axis(z0, z1):
        s.line([(0, 0, z0), (0, 0, z1)], MUTE, 1, dash="2 4")

    s.cyl(0, 0, -6, R, 6, DARK)
    s.gear((-14, 8, 5), 30, "xy", 20, BRASS)
    axis(5, 38)
    s.gear((-24, -10, 38), 17, "xy", 12, shade(BRASS, 0.8))
    s.gear((16, -8, 38), 23, "xy", 16, BRASS)
    s.gear((10, 26, 38), 12, "xy", 10, shade(BRASS, 1.25))
    axis(38, 76)
    c = (-12, 4, 76)
    s.ring(c, 25, "xy", stroke=STEEL, sw=3.5)
    for t in (30, 150, 270):
        s.line([c, (c[0] + 25 * cd(t), c[1] + 25 * sd(t), 76)], STEEL, 2)
    for r in (5, 9, 13):
        s.ring(c, r, "xy", stroke=RED, sw=1)
    s.gear((26, -12, 76), 11, "xy", 15, STEEL, th=3)
    axis(76, 118)
    top = (0, 0, 118)
    s.ring(top, R, "xy", fill="#1b2438", stroke=INK, sw=1.5, op=0.93)
    for i in range(12):
        s.line([Hh(i * 30, 49, 118), Hh(i * 30, 55, 118)], INK, 2 if i % 3 == 0 else 1)
    s.hands(top, 28, 44, 48)
    s.note(352, 80, "dial and hands", to=Hh(90, R, 118))
    s.note(352, 128, "balance wheel", to=(26, -12, 76))
    s.note(352, 170, "going train", to=(16, -8, 38))
    s.note(352, 212, "mainspring barrel", to=(-14, 8, 5))
    s.note(128, 150, "layers drift apart,", anchor="end")
    s.note(128, 163, "then close on the hour", anchor="end")


def gimbal(s):
    s.view(oy=192)
    s.cyl(0, 0, 0, 58, 7, DARK)
    C = (0, 0, 98)
    for th in (90, 270):
        x, y = hd(th, 70)
        s.line([(x * 0.8, y * 0.8, 7), (x, y, 98)], STEEL, 3)
    s.ring(C, 70, "xy", stroke=STEEL, sw=4)
    s.ring(C, 56, "xz", stroke=BRASS, sw=4)
    s.ring(C, 42, "yz", stroke=VIOLET, sw=4)
    s.ring(C, 17, "w", stroke=INK, sw=2.5)
    s.line([at(C, UW, UP, -17, 0), at(C, UW, UP, 17, 0)], INK, 1.5)
    s.ball(*C, 5, RED)
    s.ball(*Hh(SA, 70, 98), 5.5, GREEN)
    cx, cy = 404, 196
    s.c2(cx, cy, 24, "#1b2438", stroke=INK, sw=1.2)
    s.l2(cx, cy, cx + 13 * sd(HA), cy - 13 * cd(HA), INK, 3)
    s.l2(cx, cy, cx + 20 * sd(MA), cy - 20 * cd(MA), INK, 1.8)
    s.t(cx, cy + 40, "hours, minutes", anchor="middle")
    s.arrow(150, 70, 178, 52, bend=-0.3)
    s.arrow(330, 126, 322, 150, bend=-0.3)
    s.note(352, 62, "three cages, three axes", to=at(C, (1, 0, 0), UP, 40, 40))
    s.note(120, 118, "balance at the centre", to=C, anchor="end")
    s.note(352, 110, "bead: seconds", to=Hh(SA, 70, 98))


def planetary(s):
    s.view(oy=142, k=0.85)
    c = (0, 0, 0)

    def pc(th, r):
        return at(c, UW, UP, r * sd(th), r * cd(th))

    s.ring(c, 104, "w", fill="#141b2b", stroke="#2a3550", sw=1)
    for i in range(12):
        s.line([pc(i * 30, 96), pc(i * 30, 103)], INK, 2 if i % 3 == 0 else 1)
    s.ring(c, 90, "w", fill="#1b2438", stroke=STEEL, sw=5)
    s.ring(c, 84, "w", stroke=shade(STEEL, 0.7), sw=1, teeth=48, depth=0.07)
    s.line([pc(HA, 52), pc(HA + 120, 52), pc(HA + 240, 52), pc(HA, 52)], INK, 1.5, op=0.6)
    for k in (1, 2, 0):
        s.gear(pc(HA + 120 * k, 52), 28, "w", 18, RED if k == 0 else shade(STEEL, 0.75), th=3)
    s.gear(c, 22, "w", 14, BRASS, th=3)
    s.ball(*pc(MA, 15), 4, GREEN)
    s.note(118, 62, "red planet: the hour", to=pc(HA, 52), anchor="end")
    s.note(364, 62, "sun-gear dot: minute", to=pc(MA, 15))
    s.note(364, 222, "ring gear, fixed", to=pc(135, 88))


def verge(s):
    s.view(oy=222)
    s.box(-60, -5, 0, 8, 10, 125, WOOD)
    s.line([(-52, 0, 62), (72, 0, 62)], STEEL, 3)
    s.ring((-34, 0, 62), 10, "yz", fill=shade(WOOD, 0.7), stroke=WOOD)
    s.ring((-18, 0, 62), 10, "yz", fill=WOOD, stroke=shade(WOOD, 0.6))
    s.line([(-26, 10, 62), (-26, 10, 26)], INK, 1)
    s.cyl(-26, 10, 8, 7, 18, STONE)
    s.gear((16, 0, 62), 27, "yz", 15, BRASS)
    s.box(52, -5, 0, 8, 10, 125, WOOD)
    s.box(-60, -5, 125, 120, 10, 7, WOOD)
    s.line([(34, 0, 34), (34, 0, 142)], STEEL, 2.5)
    for z in (50, 74):
        s.line([(34, 0, z), (25, 0, z)], STEEL, 3)
    a = add((34, 0, 0), Hh(62, 50, 142))
    b = add((34, 0, 0), Hh(242, 50, 142))
    s.line([a, b], STEEL, 3.5)
    s.ball(*a, 5.5, STONE)
    s.ball(*b, 5.5, STONE)
    d = (72, 0, 62)
    s.ring(d, 22, "yz", fill="#1b2438", stroke=INK, sw=1.5)
    for i in range(12):
        s.line([(72, -sd(i * 30) * 17, 62 + cd(i * 30) * 17), (72, -sd(i * 30) * 21, 62 + cd(i * 30) * 21)], INK, 1)
    s.line([d, (72, -sd(HA) * 15, 62 + cd(HA) * 15)], INK, 3.5)
    s.arrow(338, 66, 350, 92, bend=-0.35)
    s.arrow(200, 126, 190, 100, bend=-0.35)
    s.note(362, 60, "foliot bar swings", to=a)
    s.note(362, 150, "one hand: the hour", to=(72, 0, 84))
    s.note(120, 110, "crown wheel", to=(16, 0, 80), anchor="end")
    s.note(120, 205, "falling weight", to=(-26, 10, 18), anchor="end")


# --------------------------------------------------------------- pendulums

def wave(s):
    s.view(oy=190)
    Zb = 112
    for a in (-122, 122):
        s.line([q(a, 0), q(a, Zb)], STEEL, 3)
        s.line([q(a, 0, -18), q(a, 0, 18)], STEEL, 3)
    items = []
    for i in range(12):
        a, L = -104 + i * 19, 96 - i * 5
        ph = math.radians(26) * math.sin(i * 0.62 + 0.3)
        items.append((L * math.sin(ph), a, Zb - L * math.cos(ph), i))
    for n, a, z, i in sorted(items):
        s.line([q(a, Zb), q(a, z, n)], MUTE, 1)
        s.ball(*q(a, z, n), 6.5, BRASS if i < 10 else "#55627d")
    s.line([q(-126, Zb), q(126, Zb)], WOOD, 6)
    for j in range(60):
        x, y = s.p(*q(-118 + j * 4, Zb + 7))
        s.c2(x, y, 1.5, GREEN if j < 8 else "#3a4660")
    s.note(300, 56, "beads on the beam: minutes", to=q(-88, Zb + 7))
    s.note(196, 234, "10 lit bobs: the hour", to=q(-9, 41, 0))
    s.note(40, 234, "all realign every 60 s")


def foucault(s):
    s.view(oy=178, k=0.82)
    s.ring((0, 0, 0), 100, fill="#161d2e", stroke="#2a3550", sw=1)
    al = 24
    s.line([Hh(al, 88), Hh(al + 180, 88)], MUTE, 1, dash="3 4")
    pegs = []
    for j in range(60):
        th = j * 6
        pegs.append((sum(hd(th, 88)), th, th <= al or 180 <= th <= 180 + al))
    for _, th, down in sorted(pegs):
        if down:
            s.line([Hh(th, 88, 1), Hh(th, 99, 1)], RED, 2, op=0.85)
        else:
            s.line([Hh(th, 88), Hh(th, 88, 10)], INK, 1.6)
    for h in range(12):
        s.ball(*Hh(h * 30, 112, 3), 3.5, BRASS if 1 <= h <= 10 else "#3a4660")
    bob = Hh(al, 62, 16)
    s.ring(Hh(al, 62, 0.5), 9, fill="#000000", op=0.35)
    s.line([(0, 0, 160), bob], STEEL, 1)
    s.ball(*bob, 9, BRASS)
    s.note(372, 70, "swing plane turns", to=Hh(al, 88))
    s.t(372, 83, "half a turn an hour")
    s.note(108, 92, "fallen pegs: minutes", to=Hh(196, 95, 1), anchor="end")
    s.note(372, 222, "10 lamps: the hour", to=Hh(120, 112, 3))


def conical(s):
    s.view(oy=196, k=0.8)
    s.ring((0, 0, 0), 100, fill="#161d2e", stroke="#2a3550", sw=1)
    for r in (78, 92):
        s.ring((0, 0, 0), r, stroke="#33405c", sw=1)
    for i in range(12):
        s.line([Hh(i * 30, 58), Hh(i * 30, 67)], INK, 2 if i % 3 == 0 else 1)
    apex = (0, 0, 172)
    s.line([Hh(0, 98), apex], STEEL, 2.5)
    s.line([(0, 0, 0.5), Hh(SA, 60, 0.5)], RED, 1, dash="3 3")
    s.ring(Hh(SA, 60, 0.5), 8, fill="#000000", op=0.4)
    s.ball(*Hh(MA, 78, 4), 5, GREEN)
    s.ball(*Hh(HA, 92, 4), 5, BRASS)
    s.ring((0, 0, 48), 60, stroke=MUTE, sw=1, dash="2 4")
    s.line([apex, Hh(SA + 180, 60, 48)], MUTE, 0.8, dash="2 5")
    s.line([apex, Hh(SA, 60, 48)], INK, 1.2)
    s.ball(*Hh(SA, 60, 48), 8, RED)
    for th in (125, 235):
        s.line([Hh(th, 98), apex], STEEL, 2.5)
    s.note(366, 66, "one lap = 60 s", to=Hh(SA, 60, 110))
    s.note(366, 120, "minute bead", to=Hh(MA, 78, 4))
    s.note(366, 236, "shadow: seconds", to=Hh(SA, 60, 0.5))
    s.note(114, 120, "hour bead", to=Hh(HA, 92, 4), anchor="end")


# --------------------------------------------------------------- water and sand

def clepsydra(s):
    s.view(oy=222)

    def P(a):
        return (UW[0] * a, UW[1] * a)

    x, y = P(-98)
    for dx, dy in ((-14, -14), (14, -14), (-14, 14), (14, 14)):
        s.line([(x + dx, y + dy, 0), (x + dx, y + dy, 88)], WOOD, 3)
    s.cyl(x, y, 88, 24, 38, STONE)
    s.ring((x, y, 126.5), 21, fill=WATER)
    s.line([q(-76, 96), q(-52, 90)], STONE, 3)
    s.line([q(-52, 90), q(-52, 48)], WATER, 1.5, dash="1 5")
    x, y = P(-30)
    s.cyl(x, y, 0, 29, 44, WATER, op=0.8)
    s.cyl(x, y, 44, 15, 6, WOOD)
    s.cyl(x, y, 0, 31, 78, INK, op=0.1)
    s.ring((x, y, 78), 31, stroke=INK, sw=1, op=0.5)
    s.line([(x, y, 50), (x, y, 113)], STEEL, 2)
    s.line([(x, y, 113), q(40, 113)], INK, 2)
    s.ball(*q(42, 113), 3, RED)
    x, y = P(58)
    s.cyl(x, y, 0, 13, 140, shade(STONE, 1.2))
    for i in range(13):
        cx, cy = s.p(x, y, 20 + i * 110 / 12)
        s.l2(cx - 12, cy + 1, cx - 3, cy + 3.5, "#3a3f4a", 1.3)
        if i % 6 == 0:
            s.t(cx + 22, cy + 5, str(i), size=10)
    s.note(150, 50, "reservoir, kept full", to=(P(-98)[0], P(-98)[1], 126))
    s.note(350, 84, "pointer on the float", to=q(42, 113))
    s.note(20, 228, "one mark an hour;")
    s.note(20, 241, "drained at 12")


def timeflow(s):
    s.view(oy=216)
    s.line([q(-102, 0), q(-102, 160), q(126, 160), q(126, 0)], "#3a4660", 2)
    s.line([q(0, 128), q(-80, 154), q(-80, 150)], WATER, 1.2, op=0.5)
    s.line([q(0, 128), q(70, 138), q(70, 130)], WATER, 1.2, op=0.5)
    s.line([q(70, 138), q(108, 138), q(108, 130)], WATER, 1.2, op=0.5)
    s.ball(*q(0, 146), 12, WATER)
    s.line([q(0, 135), q(0, 128)], WATER, 2)
    s.line([q(0, 128), q(22, 62)], STEEL, 2)
    s.ball(*q(22, 62), 7, BRASS)
    for i in range(12):
        pt = q(-80, 20 + i * 11.5)
        if i < 10:
            s.ball(*pt, 5.6, WATER)
        else:
            px, py = s.p(*pt)
            s.c2(px, py, 5.6, "#141b2b", stroke=MUTE)
    x, y, _ = q(70, 0)
    for i in range(5):
        s.cyl(x, y, 18 + i * 24, 15, 10, WATER if i < 3 else "#2a3347")
    x, y, _ = q(108, 0)
    s.cyl(x, y, 18, 3.5, 88, WATER)
    s.cyl(x, y, 18, 4.5, 110, INK, op=0.15)
    s.t3(q(-80, -6), "hours", anchor="middle")
    s.t3(q(64, -8), "×10 min", anchor="middle")
    s.t3(q(110, -8), "min", anchor="middle")
    s.arrow(250, 160, 282, 164, bend=0.4)
    for i, line in enumerate(("pendulum doses", "the flow; full", "columns siphon", "empty")):
        s.note(392, 110 + i * 13, line)


def buckets(s):
    s.view(oy=196)

    def step(a, n, z, bit, label):
        x, y, _ = q(a, 0, n)
        s.box(x - 15, y - 11, z - 4, 30, 22, 4, "#2a3347")
        s.box(x - 9, y - 8, z, 18, 16, 15, WATER if bit else "#39445c")
        px, py = s.p(x, y, z - 4)
        s.t(px, py + 22, label, col=INK if bit else MUTE, size=10, anchor="middle", weight=700 if bit else 400)

    s.line([q(-150, 100, -30), q(-118, 100, -30), q(-118, 94, -30)], STEEL, 3)
    s.line([q(-118, 94, -30), q(-116, 84, -30)], WATER, 1.5, dash="1 4")
    for i, (wgt, bit) in enumerate(zip((1, 2, 4, 8, 16, 32), (0, 0, 0, 1, 0, 0))):
        step(-115 + i * 40, -30, 70 - i * 10, bit, str(wgt))
    s.line([q(98, 22, -30), q(98, 44, 38), q(-55, 44, 38)], WATER, 1, dash="2 4", op=0.6)
    for i, (wgt, bit) in enumerate(zip((1, 2, 4, 8), (0, 1, 0, 1))):
        step(-55 + i * 40, 38, 34 - i * 10, bit, str(wgt))
    s.note(128, 50, "one fill a minute")
    s.note(262, 70, "a full bucket tips into the next")
    s.note(372, 150, "minutes: 8")
    s.note(336, 226, "hours: 2 + 8")
    s.note(40, 150, "at 60 the carry")
    s.note(40, 163, "feeds the hour row")


def carousel(s):
    s.view(oy=128)
    c = (0, 0, 0)

    def pc(th, r):
        return at(c, UW, UP, r * sd(th), r * cd(th))

    def hg(cx, cy, top, bot, col, sw=1):
        w, h = 7, 11
        s.raw(f'<path d="M{cx - w:.1f},{cy - h:.1f} L{cx + w:.1f},{cy - h:.1f} L{cx:.1f},{cy:.1f} L{cx + w:.1f},{cy + h:.1f} '
              f'L{cx - w:.1f},{cy + h:.1f} L{cx:.1f},{cy:.1f} Z" fill="#1b2438" stroke="{col}" stroke-width="{sw}"/>')
        if bot:
            s.raw(f'<path d="M{cx - w * 0.85:.1f},{cy + h - 1:.1f} L{cx + w * 0.85:.1f},{cy + h - 1:.1f} '
                  f'L{cx:.1f},{cy + h - 1 - (h - 2) * bot:.1f} Z" fill="{SAND}"/>')
        if top:
            s.raw(f'<path d="M{cx - w * 0.8 * top:.1f},{cy - (h - 1) * top:.1f} L{cx + w * 0.8 * top:.1f},{cy - (h - 1) * top:.1f} '
                  f'L{cx:.1f},{cy - 1:.1f} Z" fill="{SAND}"/>')

    s.line([q(0, 0), q(-45, -92)], WOOD, 4)
    s.line([q(0, 0), q(45, -92)], WOOD, 4)
    s.ring(c, 68, "w", stroke=STEEL, sw=3)
    for h in range(12):
        s.line([c, pc(h * 30, 68)], "#3a4660", 1)
    s.ball(0, 0, 0, 5, STEEL)
    for h in range(1, 13):
        x, y = s.p(*pc(h * 30, 68))
        if h <= 10:
            hg(x, y, 0, 1, INK)
        elif h == 11:
            hg(x, y, 0.85, 0.3, BRASS, 1.8)
            s.l2(x, y, x, y + 8, SAND, 1)
        else:
            hg(x, y, 1, 0, INK)
        tx, ty = s.p(*pc(h * 30, 88))
        s.t(tx, ty + 4, str(h), size=9.5, anchor="middle", col=INK if h == 11 else MUTE)
    s.note(124, 70, "glass 11 is running", to=pc(330, 68), anchor="end")
    s.note(372, 96, "10 glasses spent", to=pc(90, 68))
    s.note(372, 196, "wheel indexes and")
    s.note(372, 209, "flips one an hour")


def sandtable(s):
    s.view(oy=150, k=0.88)
    s.box(-118, -68, -16, 236, 136, 16, WOOD)
    s.poly([(-110, -60, 0.5), (110, -60, 0.5), (110, 60, 0.5), (-110, 60, 0.5)], SAND)
    for k in range(-5, 6):
        s.line([(-108, k * 11, 0.6), (108, k * 11, 0.6)], shade(SAND, 0.92), 1)
    strokes = segs("10:08", (-84, 24, 0.8), (1, 0, 0), (0, -1, 0), 24, 22, 12)
    for _, _, a, b in strokes[:-2]:
        s.line([a, b], shade(SAND, 0.5), 4.5)
        s.line([a, b], shade(SAND, 0.72), 1.5)
    for _, _, a, b in strokes[-2:]:
        s.line([a, b], "#6b5a3a", 1.2, dash="2 3")
    end = strokes[-3][3]
    s.ball(end[0], end[1], 6, 5.5, STEEL)
    s.line([(-104, -58, 3), (-104, 58, 3)], STEEL, 3.5)
    s.note(330, 70, "ball follows a magnet", to=(end[0], end[1], 8))
    s.t(330, 83, "under the table")
    s.note(22, 70, "wiper bar levels the", to=(-104, -20, 3))
    s.t(22, 83, "sand before a rewrite")


def curtain(s):
    s.view(oy=208)
    s.poly([q(-116, 0, -20), q(116, 0, -20), q(116, 0, 22), q(-116, 0, 22)], shade(WATER, 0.3), stroke=shade(WATER, 0.7))
    for a in (-80, -30, 22, 70):
        s.ring(q(a, 0.5, 4), 6, stroke=WATER, sw=0.8, op=0.6)
    for c, r in dots("10:07"):
        z = 40 - r * 13
        if z > 5:
            s.ball(*q((c - 9) * 10.5, z), 3.4, WATER, op=0.35)
    s.line([q(-112, 142), q(112, 142)], STEEL, 8)
    for c in range(19):
        s.line([q((c - 9) * 10.5, 136), q((c - 9) * 10.5, 133)], "#55627d", 2)
    for c, r in dots("10:08"):
        a, z = (c - 9) * 10.5, 118 - r * 13
        s.line([q(a, z + 9), q(a, z + 2)], WATER, 1.2, op=0.5)
        s.ball(*q(a, z), 3.6, WATER)
    s.note(322, 62, "valve bar, 19 nozzles", to=q(96, 142))
    s.note(374, 156, "this minute", to=q(98, 92))
    s.note(374, 206, "last minute,", to=q(98, 27))
    s.t(374, 219, "still falling")


# --------------------------------------------------------------- balls and chain reactions

def congreve(s):
    s.view(oy=186, k=0.88)

    def zt(y):
        return 64 - 0.2 * y

    s.box(-92, -52, 0, 184, 104, 9, WOOD)
    for x in (-80, 80):
        s.line([(x, -50, 9), (x, -50, 104)], BRASS, 2.5)
    s.line([(-80, -50, 104), (80, -50, 104)], BRASS, 2.5)
    s.line([(-80, -50, 80), (80, -50, 80)], BRASS, 2.5)
    for x, th, lab in ((-42, HA, "h"), (0, MA, "min"), (42, 0, "s")):
        d = (x, -50, 92)
        s.ring(d, 11, "xz", fill="#1b2438", stroke=INK, sw=1.2)
        s.line([d, (x + sd(th) * 8, -50, 92 + cd(th) * 8)], INK, 2)
    s.box(-6, -6, 9, 12, 12, 48, STEEL)
    s.poly([(-85, 45, zt(45)), (85, 45, zt(45)), (85, 45, zt(45) - 4), (-85, 45, zt(45) - 4)], shade(BRASS, 0.5))
    s.poly([(85, -45, zt(-45)), (85, 45, zt(45)), (85, 45, zt(45) - 4), (85, -45, zt(-45) - 4)], shade(BRASS, 0.65))
    s.poly([(-85, -45, zt(-45)), (85, -45, zt(-45)), (85, 45, zt(45)), (-85, 45, zt(45))], "#3b3524", stroke=BRASS)
    pts = []
    for j in range(6):
        y = -38 + j * 15.2
        xs = (-78, 78) if j % 2 == 0 else (78, -78)
        pts += [(xs[0], y, zt(y) + 0.5), (xs[1], y, zt(y) + 0.5)]
    s.line(pts, shade(BRASS, 1.4), 1.3)
    s.ball(-20, -7.6, zt(-7.6) + 5, 5.5, STEEL)
    s.arrow(392, 150, 396, 182, bend=-0.4)
    s.note(196, 50, "h, min and s dials", to=(-42, -50, 103), anchor="end")
    s.note(40, 150, "ball zigzags down")
    s.note(40, 163, "for 30 s")
    s.note(318, 236, "then the table tips")
    s.note(318, 249, "and the hands step")


def railclock(s):
    s.view(oy=218)
    s.line([q(104, 8), q(104, 146)], "#3a4660", 3)
    for z0, n, col, lab in ((128, 3, GREEN, "minutes: 3"), (92, 1, BRASS, "×5 min: 1"), (56, 10, INK, "hours: 10")):
        s.line([q(-62, z0 + 3.1), q(100, z0 - 5)], STEEL, 2.5)
        for i in range(n):
            a = 93 - i * 11.5
            s.ball(*q(a, z0 - 0.05 * a + 5.5), 5.5, col)
        x, y = s.p(*q(104, z0 - 5))
        s.t(x + 10, y + 2, lab, col=INK)
    s.line([q(100, 30), q(-112, 16)], "#55627d", 2)
    for i in range(4):
        a = -100 + i * 11.5
        s.ball(*q(a, 16.8 + (a + 112) * 0.066 + 5.5), 5.5, "#8a94a8")
    c = q(-104, 76)
    s.ring(c, 44, "w", stroke=STEEL, sw=2)
    tip = at(c, UW, UP, 44 * sd(40), 44 * cd(40))
    s.line([c, tip], STEEL, 3)
    s.ball(*tip, 5.5, "#8a94a8")
    s.ball(*c, 4, STEEL)
    s.line([q(-90, 134), q(-62, 131.5)], STEEL, 2.5)
    s.note(60, 52, "arm lifts one ball a minute", to=tip)
    s.note(96, 238, "a full rail tips: one ball carries down, the rest run home")


def domino(s):
    s.view(oy=158, k=0.92)
    s.ring((0, 0, 0), 104, fill="#161d2e", stroke="#2a3550", sw=1)
    front = 22
    s.line([(0, 0, 1), Hh(front * 6 + 180, 92, 1)], STEEL, 2)
    s.ball(*Hh(front * 6 + 180, 92, 5), 4, STEEL)
    items = []
    for j in range(60):
        items.append((sum(hd(j * 6, 92)), "sec", j, (front - j) % 60))
        items.append((sum(hd(j * 6, 64)), "min", j, 0))
    for h in range(12):
        items.append((sum(hd(h * 30, 38)), "hr", h, 0))
    for _, kind, j, age in sorted(items):
        if kind == "sec":
            th = j * 6
            if age <= 2:
                s.line([Hh(th, 92), Hh(th + 2 + age, 92, 11 - age * 3)], RED, 3)
            elif age < 30:
                s.line([Hh(th, 92, 1.5), Hh(th + 5, 92, 2.5)], RED, 3, op=0.7)
            else:
                s.line([Hh(th, 92), Hh(th, 92, 13)], INK, 3)
        elif kind == "min":
            if j == 8:
                s.ball(*Hh(j * 6, 64, 4), 4.8, GREEN)
            else:
                x, y = s.p(*Hh(j * 6, 64, 1))
                s.c2(x, y, 1.4, "#4a5876")
        else:
            x, y = hd(j * 30, 38)
            s.box(x - 5, y - 5, 0, 10, 10, 10 if j == 10 else 4, BRASS if j == 10 else "#39445c")
    s.note(362, 232, "falling front: 22 s", to=Hh(SA, 92, 6))
    s.note(372, 84, "minute tile 08", to=Hh(48, 64, 4))
    s.note(116, 70, "hour block 10", to=Hh(300, 38, 10), anchor="end")
    s.note(104, 150, "reset arm", to=Hh(312, 92, 5), anchor="end")
    s.t(104, 163, "half a lap behind", anchor="end")


def rube(s):
    s.view(oy=200)
    s.line([q(-128, 122), q(-72, 98)], STEEL, 3)
    s.ball(*q(-112, 121), 6, BRASS)
    s.line([q(-72, 82), q(2, 82)], "#55627d", 2.5)
    for i in range(6):
        a = -62 + i * 10
        lean = (3 - i) * 4 if i < 3 else 0
        s.line([q(a, 82), q(a + lean, 99 - lean * 0.5)], INK, 3)
    s.poly([q(14, 50), q(34, 50), q(24, 62)], "#55627d")
    s.line([q(-2, 72), q(50, 52)], WOOD, 4)
    s.ball(*q(45, 60), 5, STEEL)
    s.line([q(66, 46), q(66, 128), q(132, 128), q(132, 102)], INK, 1)
    for a in (66, 132):
        px, py = s.p(*q(a, 128))
        s.c2(px, py, 4, "#141b2b", stroke=STEEL, sw=1.5)
    x, y, _ = q(66, 0)
    s.box(x - 8, y - 8, 30, 16, 16, 16, "#39445c")
    for a0, txt in ((80, "10"), (116, "08")):
        s.poly([q(a0, 58), q(a0 + 32, 58), q(a0 + 32, 102), q(a0, 102)], "#0a0d14", stroke="#2a3550")
        s.tp(q(a0 + 16, 72), UW, UP, txt, 21)
    s.poly([q(116, 92), q(148, 92), q(148, 100, 11), q(116, 100, 11)], "#232c44", stroke="#3a4660")
    for pt, lab in ((q(-98, 128), "0 s"), (q(-46, 108), "15 s"), (q(14, 84), "30 s"), (q(80, 26), "45 s"), (q(104, 44), "60 s: flap drops")):
        s.t3(pt, lab, col=BRASS, weight=700, size=10)
    s.arrow(350, 240, 104, 240, col=MUTE)
    s.note(120, 232, "the ball is lifted back and every piece re-arms")


def marbles(s):
    s.view(oy=148)
    s.poly([q(-112, 40), q(112, 40), q(112, 40, -10), q(-112, 40, -10)], "#2a3347")
    s.poly([q(112, -40), q(112, 40), q(112, 40, -10), q(112, -40, -10)], "#141b2b")
    s.poly([q(-112, -40), q(112, -40), q(112, 40), q(-112, 40)], "#1b2438", stroke="#2a3550")
    lit = set(dots("10:08"))
    for c in range(19):
        for r in range(5):
            pt = q((c - 9) * 11, (2 - r) * 13)
            if (c, r) in lit:
                s.ball(*pt, 4.8, BRASS)
            else:
                x, y = s.p(*pt)
                s.c2(x, y, 2, "#0b0f18")
    s.poly([q(-40, 80), q(40, 80), q(16, 48), q(-16, 48)], "#232c44", stroke=STEEL)
    for a, z in ((-14, 71), (0, 73), (14, 70), (-6, 61), (8, 62)):
        s.ball(*q(a, z), 4.8, shade(BRASS, 0.85))
    s.line([q(-116, -56), q(120, -64)], STEEL, 3)
    for a in (60, 74, 88, 102):
        s.ball(*q(a, -56 - (a + 116) * 0.034 + 5.5), 4.8, shade(BRASS, 0.85))
    s.line([q(124, -64), q(124, 86), q(44, 86)], "#55627d", 2, dash="4 3")
    s.arrow(398, 190, 398, 120, col=MUTE)
    s.note(52, 52, "gates under the hopper")
    s.note(52, 65, "route each marble")
    s.note(392, 206, "screw lift")
    s.note(30, 216, "trapdoors dump the")
    s.note(30, 229, "old minute")


# --------------------------------------------------------------- light, shadow and fire

def sundial(s):
    s.view(oy=182, k=0.9)
    s.cyl(0, 0, 0, 92, 8, STONE)
    Z = 8.4
    N = -35                                # north, turned so the gnomon is not edge-on
    for h in range(6, 19):
        s.line([Hh(N + (h - 12) * 15, 30, Z), Hh(N + (h - 12) * 15, 66, Z)], "#5b6270", 1.2)
    for h in (6, 9, 12, 15, 18):
        x, y = s.p(*Hh(N + (h - 12) * 15, 78, Z))
        s.t(x, y + 4, str(h), col="#3a3f4a", size=10, anchor="middle", weight=700)
    s.poly([Hh(N + 180, 18, Z), Hh(N, 58, Z), Hh(N - 28, 86, Z)], "#000000", op=0.5)
    apex = Hh(N, 58, 52)
    s.poly([Hh(N + 180, 18, Z), Hh(N, 58, Z), apex], BRASS, stroke=shade(BRASS, 0.6))
    ax, ay = s.p(*apex)
    s.c2(278, 40, 26, SUN, op=0.15)
    s.c2(278, 40, 12, SUN)
    s.l2(278, 40, ax, ay, SUN, 1, dash="3 4", op=0.6)
    s.raw(f'<path d="M168,60 Q290,-14 446,100" fill="none" stroke="{MUTE}" stroke-width="1" stroke-dasharray="2 5"/>')
    s.note(310, 36, "sun where it really is")
    s.note(388, 122, "its path today")
    s.note(16, 92, "shadow = hour hand", to=Hh(N - 28, 76, Z))
    s.note(20, 226, "no sun at night: the")
    s.note(20, 239, "moon takes over")


def shadowblocks(s):
    s.view(oy=172, k=0.86)
    random.seed(7)
    s.poly([(-115, -55, 0), (115, -55, 0), (115, 55, 0), (-115, 55, 0)], "#1e2944", stroke="#2f3b57")
    strokes = segs("10:08", (-52, 46, 0.5), (1, 0, 0), (0, -1, 0), 19, 16, 9)
    for _, _, a, b in strokes:
        s.line([a, b], "#070a12", 5)
    lamp = (245, 36)
    for pt in ((-56, 30, 0), (72, 30, 0)):
        x, y = s.p(*pt)
        s.l2(lamp[0], lamp[1], x, y, SUN, 1, dash="3 4", op=0.4)
    blocks = []
    for _ in range(26):
        x, y, z = (-30 + random.uniform(-24, 24), -25 + random.uniform(-24, 24), 62 + random.uniform(-18, 18))
        blocks.append((x + y, z, x, y, random.uniform(6, 13), shade(STEEL, random.uniform(0.6, 1.1))))
    for _, z, x, y, d, col in sorted(blocks):
        s.box(x, y, z, d, d, d, col)
    s.c2(lamp[0], lamp[1], 18, SUN, op=0.18)
    s.c2(lamp[0], lamp[1], 7, SUN)
    s.note(326, 96, "from the room: noise", to=(-12, -30, 62))
    s.note(346, 236, "in shadow: the time", to=(72, 30, 0))
    s.note(20, 222, "blocks reshuffle")
    s.note(20, 235, "every minute")


def anamorph(s):
    s.view(oy=160)
    random.seed(21)
    data = [(random.uniform(-60, 60), a, b) for _, _, a, b in segs("10:08", q(-98, -34), UW, UP, 30, 30, 14)]
    for _ in range(12):
        a0, z0 = random.uniform(-130, 110), random.choice((random.uniform(40, 60), random.uniform(-62, -46)))
        s.line([q(a0, z0), q(a0 + random.uniform(-14, 14), z0 + random.uniform(-10, 10))], MUTE, random.uniform(2, 5), op=0.35)
    for d, a, b in sorted(data, key=lambda t: t[0]):
        mx, my = s.p(*mul(add(a, b), 0.5))
        s.l2(mx, my, mx, 34, MUTE, 0.5, op=0.3)
    for d, a, b in sorted(data, key=lambda t: t[0]):
        s.line([a, b], shade(INK, 0.55 + (d + 60) / 120 * 0.45), 5 + d / 20)
    s.raw('<rect x="350" y="36" width="118" height="78" rx="8" fill="#141b2b" stroke="#2a3550"/>')
    s2 = S()
    s2.view(ox=406, oy=72, k=0.3)
    for d, a, b in data:
        off = add(mul(NW, d * 1.0), mul(UW, d * 0.6))
        s2.line([add(a, off), add(b, off)], shade(INK, 0.55 + (d + 60) / 120 * 0.45), 1.8)
    s.e += s2.e
    s.t(409, 127, "same shards, 30 s later", size=9.5, anchor="middle")
    s.note(60, 222, "shards hang at different depths and line up")
    s.note(60, 235, "from the drifting camera once a minute")


def candle(s):
    s.view(oy=212)
    zh = lambda h: 172 - 13.5 * h
    wax = zh(10.13)
    s.cyl(0, 0, 0, 48, 5, BRASS)
    balls = [(-162 + i * 36) for i in range(10)]
    for th in balls:
        if abs(th) < 90:
            s.ball(*Hh(th, 36, 9), 4.2, STEEL)
    for sign in (90, 270):
        s.line([Hh(sign, 13, wax), Hh(sign, 13, 167)], MUTE, 0.9, dash="2 4")
    s.ring((0, 0, 167), 13, stroke=MUTE, sw=0.9, dash="2 4")
    for h in range(1, 11):
        s.ring((0, 0, zh(h)), 13, stroke="#3a4660", sw=0.8, a0=-45, a1=135)
        if h % 2 == 0 and h < 10:
            s.t3(Hh(90, 24, zh(h) - 3), str(h), size=9)
    s.cyl(0, 0, 5, 13, wax - 5, "#efe6d0")
    for h in (11, 12):
        s.ring((0, 0, zh(h)), 13.3, stroke="#b8ad90", sw=1, a0=-45, a1=135)
        s.line([Hh(90, 11, zh(h)), Hh(90, 22, zh(h))], STEEL, 1.5)
        s.ball(*Hh(90, 24, zh(h)), 4.2, STEEL)
        s.t3(Hh(90, 39, zh(h) - 3), str(h), col=INK, size=9.5)
    fx, fy = s.p(0, 0, wax + 3)
    s.c2(fx, fy - 10, 34, "#ffb347", op=0.13)
    s.raw(f'<path d="M{fx:.1f},{fy - 24:.1f} C{fx + 9:.1f},{fy - 9:.1f} {fx + 7:.1f},{fy:.1f} {fx:.1f},{fy:.1f} '
          f'C{fx - 7:.1f},{fy:.1f} {fx - 9:.1f},{fy - 9:.1f} {fx:.1f},{fy - 24:.1f} Z" fill="#ffb347"/>')
    s.raw(f'<path d="M{fx:.1f},{fy - 14:.1f} C{fx + 4:.1f},{fy - 6:.1f} {fx + 3:.1f},{fy - 1:.1f} {fx:.1f},{fy - 1:.1f} '
          f'C{fx - 3:.1f},{fy - 1:.1f} {fx - 4:.1f},{fy - 6:.1f} {fx:.1f},{fy - 14:.1f} Z" fill="#fff3c4"/>')
    for th in balls:
        if abs(th) >= 90:
            s.ball(*Hh(th, 36, 9), 4.2, STEEL)
    s.note(318, 62, "ghost: wax burnt since 00:00", to=Hh(90, 13, 160))
    s.note(190, 150, "wax level: minutes", to=Hh(270, 13, wax), anchor="end")
    s.note(150, 226, "10 balls rung", to=Hh(234, 36, 9), anchor="end")
    s.note(336, 186, "pins 11 and 12", to=Hh(90, 52, zh(11)))
    s.t(336, 199, "still held in the wax")


# --------------------------------------------------------------- sky

def orrery(s):
    s.view(oy=160, k=0.92)
    s.ring((0, 0, 0), 122, fill="#131a2a", stroke="#2a3550", sw=1)
    for i in range(60):
        s.line([Hh(i * 6, 109 if i % 5 == 0 else 113), Hh(i * 6, 118)], INK if i % 5 == 0 else MUTE, 1.5 if i % 5 == 0 else 0.8)
    for r in (42, 72, 100):
        s.ring((0, 0, 0), r, stroke="#3a4660", sw=1, dash="2 4")
    bodies = [(HA, 42, 8, RED), (MA, 72, 9, WATER), (SA, 100, 4.5, INK)]
    for th, r, _, _ in bodies:
        s.line([(0, 0, 6), Hh(th, r, 6)], "#8a94a8", 1.2)
    for j in range(1, 9):
        s.ball(*Hh(SA - j * 5, 100, 6), 4.5 - j * 0.45, shade(INK, 0.9), op=1 - j * 0.11)

    def body(th, r, size, col):
        s.ball(*Hh(th, r, 6), size, col)
        if col == WATER:
            s.ring(Hh(th, r, 6), 15, stroke="#55627d", sw=0.8)
            s.ball(*add(Hh(th, r, 6), (10.6, 10.6, 0)), 3, STEEL)

    for th, r, size, col in bodies:
        if sum(hd(th, r)) < 0:
            body(th, r, size, col)
    cx, cy = s.p(0, 0, 8)
    s.c2(cx, cy, 26, SUN, op=0.15)
    s.ball(0, 0, 8, 13, "#ffc94d")
    for th, r, size, col in bodies:
        if sum(hd(th, r)) >= 0:
            body(th, r, size, col)
    x, y = s.p(*Hh(0, 128))
    s.t(x, y + 3, "12", col=INK, size=10, anchor="middle", weight=700)
    s.note(20, 52, "inner planet laps in 12 h", to=Hh(HA, 42, 6))
    s.note(326, 52, "blue planet laps hourly", to=Hh(MA, 72, 6))
    s.note(372, 240, "comet: 60 s lap", to=Hh(SA, 100, 6))


def globe(s):
    cx, cy, R, rx, ry = 232, 142, 76, 124, 36

    def rp(t, k=1.0):
        return (cx + rx * k * cd(t), cy + ry * k * sd(t))

    def ticks(front):
        for i in range(24):
            t = i * 15
            if (sd(t) > 0.01) == front:
                (x1, y1), (x2, y2) = rp(t), rp(t, 1.08)
                s.l2(x1, y1, x2, y2, INK if i % 6 == 0 else MUTE, 1.6 if i % 6 == 0 else 0.9)

    s.raw(f'<path d="M{cx - rx},{cy} A{rx},{ry} 0 0 1 {cx + rx},{cy}" fill="none" stroke="{STEEL}" stroke-width="3"/>')
    ticks(False)
    s.raw(f'<clipPath id="gl"><circle cx="{cx}" cy="{cy}" r="{R}"/></clipPath>')
    s.raw(f'<circle cx="{cx}" cy="{cy}" r="{R}" fill="url(#{s._g("#2f7fc2")})"/>')
    land = "".join(f'<ellipse cx="{cx + dx}" cy="{cy + dy}" rx="{a}" ry="{b}" transform="rotate({rot} {cx + dx} {cy + dy})"/>'
                   for dx, dy, a, b, rot in ((22, -32, 30, 17, -20), (40, 20, 13, 24, 15), (-30, -8, 20, 30, 10), (-6, 48, 18, 9, 0), (58, -8, 9, 14, 0)))
    s.raw(f'<g clip-path="url(#gl)" fill="#4f9d69">{land}</g>')
    s.raw(f'<path d="M{cx - R},{cy} A{R},{R * 0.29:.1f} 0 0 0 {cx + R},{cy}" fill="none" stroke="#ffffff" stroke-width="1" '
          f'stroke-dasharray="2 4" opacity=".35"/>')
    s.raw(f'<path d="M{cx},{cy - R} A{R},{R} 0 0 0 {cx},{cy + R} A20,{R} 0 0 0 {cx},{cy - R} Z" fill="#05070d" opacity=".66"/>')
    for dx, dy in ((-40, -20), (-52, 6), (-30, 30), (-18, -44), (-60, -30), (-24, 2)):
        s.c2(cx + dx, cy + dy, 1.2, SUN)
    px, py = cx + R * 0.96 * cd(28), cy + R * 0.29 * sd(28) - 17
    ex, ey = rp(28)
    s.l2(px, py, ex, ey, RED, 1.2, dash="3 3")
    s.c2(px, py, 3.6, RED)
    s.t(px - 4, py - 8, "home", col=INK, size=10, anchor="end", weight=700)
    s.raw(f'<path d="M{cx - rx},{cy} A{rx},{ry} 0 0 0 {cx + rx},{cy}" fill="none" stroke="{STEEL}" stroke-width="3"/>')
    ticks(True)
    for t, lab in ((0, "12"), (90, "6"), (180, "24"), (270, "18")):
        x, y = rp(t, 1.2)
        s.t(x, y + 4, lab, col=INK, size=10, anchor="middle", weight=700)
    for y in (96, 190):
        s.arrow(468, y, 404, y, col=SUN)
    s.t(436, 88, "sunlight", col=SUN, size=10, anchor="middle")
    s.note(300, 50, "globe turns once a day")
    s.note(300, 236, "the ring stays facing the sun")


def stardome(s):
    random.seed(25)
    s.raw('<ellipse cx="240" cy="224" rx="204" ry="26" fill="#121a2b" stroke="#2a3550"/>')
    s.raw('<path d="M36,224 A204,196 0 0 1 444,224" fill="none" stroke="#2a3550" stroke-dasharray="3 5"/>')
    n = 0
    while n < 70:
        x, y = random.uniform(50, 430), random.uniform(40, 210)
        if ((x - 240) / 204) ** 2 + ((224 - y) / 196) ** 2 < 0.9:
            s.c2(x, y, round(random.uniform(0.5, 1.3), 1), INK, op=round(random.uniform(0.35, 0.85), 2))
            n += 1
    P = (240, 116)
    s.c2(P[0], P[1], 64, "none", stroke="#3a4660", dash="2 4")
    for h in range(24):
        a = h * 15
        r1 = 58 if h % 6 == 0 else 61
        s.l2(P[0] - r1 * sd(a), P[1] - r1 * cd(a), P[0] - 66 * sd(a), P[1] - 66 * cd(a), INK if h % 6 == 0 else MUTE, 1.4 if h % 6 == 0 else 0.8)
        if h % 6 == 0:
            s.t(P[0] - 76 * sd(a), P[1] - 76 * cd(a) + 4, str(h), col=INK, size=10, anchor="middle", weight=700)
    ang = 22.133 * 15
    d = (-sd(ang), -cd(ang))
    pe = (d[1], -d[0])
    loc = [(52, 50), (46, 38), (48, 27), (51, 15), (62, 14), (60, 0), (48, 0)]
    pts = [(P[0] + r * d[0] + t * pe[0], P[1] + r * d[1] + t * pe[1]) for r, t in loc]
    s.l2(P[0], P[1], P[0] + 70 * d[0], P[1] + 70 * d[1], BRASS, 1.4, dash="4 3")
    for (x1, y1), (x2, y2) in zip(pts, pts[1:] + [pts[3]]):
        s.l2(x1, y1, x2, y2, STEEL, 1)
    for x, y in pts:
        s.c2(x, y, 2.6, INK)
    s.c2(P[0], P[1], 3.2, SUN)
    s.t(P[0] - 8, P[1] + 4, "Polaris", size=9.5, anchor="end")
    s.raw('<path d="M96,224 L96,208 L108,198 L120,208 L120,224 Z" fill="#070a12"/>')
    s.raw('<rect x="132" y="210" width="3" height="14" fill="#070a12"/><circle cx="133.5" cy="205" r="8" fill="#070a12"/>')
    s.arrow(168, 84, 160, 130, bend=0.25)
    s.note(332, 60, "Dubhe and Merak:", to=pts[5])
    s.t(332, 73, "the sky's hour hand")
    s.note(36, 150, "turns anticlockwise,")
    s.note(36, 163, "gaining 4 min a day")


# --------------------------------------------------------------- digit machines

def splitflap(s):
    s.view(oy=150)
    A0, A1 = -142, 76
    s.poly([q(A0, 50), q(A1, 50), q(A1, 50, -14), q(A0, 50, -14)], "#2a3347")
    s.poly([q(A1, -50), q(A1, 50), q(A1, 50, -14), q(A1, -50, -14)], "#1b2233")
    s.poly([q(A0, -50), q(A1, -50), q(A1, 50), q(A0, 50)], "#141a28", stroke="#2a3550")
    for a0, ch in ((-134, "1"), (-88, "0"), (-20, "0"), (26, "8")):
        s.poly([q(a0, -36), q(a0 + 42, -36), q(a0 + 42, 36), q(a0, 36)], "#0a0d14", stroke="#2a3550")
        s.tp(q(a0 + 21, -20), UW, UP, ch, 54)
        s.line([q(a0, 0), q(a0 + 42, 0)], "#141a28", 2)
    for z in (-12, 12):
        x, y = s.p(*q(-33, z))
        s.c2(x, y, 3, INK)
    s.poly([q(26, 0), q(68, 0), q(68, 16, 22), q(26, 16, 22)], "#1d2538", stroke="#3a4660")
    cx, cy = 412, 168
    for i in range(20):
        s.l2(cx + 8 * cd(i * 18), cy + 8 * sd(i * 18), cx + 30 * cd(i * 18), cy + 30 * sd(i * 18), MUTE, 0.8)
    s.c2(cx, cy, 30, "none", stroke=STEEL, sw=1.5)
    s.c2(cx, cy, 8, "#141b2b", stroke=STEEL)
    s.l2(cx - 8, cy, cx - 40, cy, INK, 2)
    ex, ey = s.p(*q(68, 0))
    s.l2(cx - 40, cy, ex, ey, MUTE, 0.8, dash="2 3")
    s.t(cx, cy + 46, "flaps on a drum", anchor="middle")
    s.note(292, 70, "a flap falls each minute", to=q(60, 12, 16))


def pins(s):
    s.view(oy=150, k=0.95)
    s.box(-110, -40, -10, 220, 80, 10, "#232c44")
    lit = set(dots("10:08"))
    cells = [(c, r) for c in range(-1, 20) for r in range(-1, 6)]
    for c, r in sorted(cells, key=lambda t: t[0] + t[1]):
        on = (c, r) in lit
        s.box((c - 9) * 10 - 4, (r - 2) * 10 - 4, 0, 8, 8, 17 if on else 3, VIOLET if on else "#39445c")
    s.arrow(60, 118, 108, 142, col=SUN)
    s.note(22, 106, "raking light", col=SUN)
    s.note(312, 70, "pins rise to form the digits")
    s.note(22, 216, "the relief casts its own")
    s.note(22, 229, "shadow, so no backlight")


def crew(s):
    s.view(oy=152)

    def fig(x, y):
        s.c2(x, y - 15, 3.2, INK)
        s.l2(x, y - 12, x, y - 4, INK, 1.6)
        s.l2(x, y - 4, x - 3, y, INK, 1.6)
        s.l2(x, y - 4, x + 3, y, INK, 1.6)
        s.l2(x - 4, y - 9, x + 4, y - 9, INK, 1.6)

    for a in range(-130, 111, 30):
        s.line([q(a, -58), q(a, 52)], "#33405c", 1)
    for z in (-58, -20, 16, 52):
        s.line([q(-130, z), q(110, z)], "#33405c", 1)
    s.line([q(-140, -58), q(120, -58)], "#55627d", 2)
    for i, z in enumerate((-55, -50, -45)):
        s.line([q(-128 + i * 4, z), q(-100 + i * 4, z)], shade(WOOD, 0.7 + i * 0.1), 5)
    for _, _, a, b in segs("10:0", q(-116, -36), UW, UP, 28, 36, 15):
        s.line([a, b], WOOD, 7)
    for _, name, a, b in segs("8", q(42, -36), UW, UP, 28, 36, 15):
        if name in "afgb":
            s.line([a, b], WOOD, 7)
        else:
            s.line([a, b], MUTE, 1.3, dash="3 3")
    s.line([q(96, -58), q(96, 86)], STEEL, 3)
    s.line([q(26, 86), q(114, 86)], STEEL, 3)
    s.line([q(96, 100), q(40, 86)], STEEL, 1)
    s.line([q(56, 86), q(56, 63)], INK, 1)
    s.line([q(44, 56), q(68, 62)], shade(WOOD, 1.2), 7)
    fig(*s.p(*q(80, -20)))
    x, y = s.p(*q(-12, -58))
    fig(x, y)
    s.l2(x - 12, y - 21, x + 14, y - 19, WOOD, 4)
    s.note(232, 52, "each minute takes 60 s to build")
    s.note(40, 240, "last minute's planks", to=q(-112, -50))
    s.note(372, 236, "missing planks", to=q(70, -18))


# --------------------------------------------------------------- living things and toys

def trains(s):
    s.view(oy=160, k=0.92)
    s.ring((0, 0, 0), 112, fill="#16201c", stroke="#2a3550", sw=1)
    for r in (96, 68, 40):
        for d in (-2.5, 2.5):
            s.ring((0, 0, 0), r + d, stroke="#6a7690", sw=1)
    for i in range(12):
        pt = Hh(i * 30, 106)
        s.line([pt, (pt[0], pt[1], 8)], INK if i % 3 == 0 else MUTE, 1.6)
    for i, lab in ((0, "12"), (3, "3"), (6, "6"), (9, "9")):
        x, y = s.p(*Hh(i * 30, 121))
        s.t(x, y + 4, lab, col=INK, size=10, anchor="middle", weight=700)
    items = []

    def car(x, y, col, engine):
        def draw():
            s.box(x - 5.5, y - 4.5, 1, 11, 9, 7, col)
            if engine:
                s.cyl(x, y, 8, 2, 4, "#141b2b")
        return draw

    def tree(x, y):
        def draw():
            s.line([(x, y, 0), (x, y, 8)], WOOD, 2)
            s.ball(x, y, 11, 5.5, "#4f9d69")
        return draw

    for th, r, n, col in ((HA, 40, 2, RED), (MA, 68, 4, GREEN), (SA, 96, 1, BRASS)):
        for j in range(n):
            x, y = hd(th - j * math.degrees(12.5 / r), r)
            items.append((x + y, car(x, y, shade(col, 1.25) if j == 0 else col, j == 0)))
    for th, r in ((200, 54), (95, 82), (262, 83), (20, 54)):
        x, y = hd(th, r)
        items.append((x + y, tree(x, y)))
    items.append((0, lambda: s.box(-10, -8, 0, 20, 16, 12, STONE)))
    for _, fn in sorted(items, key=lambda t: t[0]):
        fn()
    s.note(20, 52, "hour train: short and slow", to=Hh(HA, 40, 8))
    s.note(330, 52, "minute train", to=Hh(MA, 68, 8))
    s.note(372, 240, "express: seconds", to=Hh(SA, 96, 8))


def tree(s):
    s.view(oy=222)
    s.ring((0, 0, 0), 44, fill="#18251f", stroke="#2a3550", sw=1)
    br = []
    for h in range(1, 13):
        L = 58 - h * 2.6 if h <= 10 else (20 if h == 11 else 6)
        x, y = hd(h * 137.5, L)
        br.append((x + y, h, 24 + h * 10, x, y))

    def draw(h, zb, x, y):
        if h <= 10:
            s.line([(0, 0, zb), (x * 0.6, y * 0.6, zb + 7), (x, y, zb + 16)], WOOD, 2.6)
            s.ball(x, y, zb + 18, 11.5 - h * 0.35, shade(GREEN, 0.7 + 0.03 * h))
        elif h == 11:
            s.line([(0, 0, zb), (x, y, zb + 8)], WOOD, 2)
            s.ball(x, y, zb + 10, 4, GREEN)
            for f in (0.35, 0.55, 0.78):
                s.ball(x * f, y * f, zb + 8 * f + 3, 2, "#c9e8a8")
        else:
            s.ball(x, y, zb + 3, 2, "#c9e8a8")

    for _, h, zb, x, y in sorted(b for b in br if b[0] < 0):
        draw(h, zb, x, y)
    s.line([(0, 0, 0), (0, 0, 90)], WOOD, 9)
    s.line([(0, 0, 90), (0, 0, 150)], WOOD, 5)
    for _, h, zb, x, y in sorted(b for b in br if b[0] >= 0):
        draw(h, zb, x, y)
    b11 = br[10]
    b9 = br[8]
    s.note(318, 60, "newest branch: a leaf per", to=(b11[3], b11[4], b11[2] + 10))
    s.t(318, 73, "5 min, buds for the rest")
    s.note(118, 110, "one branch per hour", to=(b9[3], b9[4], b9[2] + 18), anchor="end")
    s.note(318, 210, "at 00:00 and 12:00 the")
    s.note(318, 223, "leaves fall and it regrows")


SCENES = [
    ("exploded-movement", "Exploded skeleton movement", "Reads 10:08:22 on the top dial; every layer below is turning", exploded),
    ("gimbal-tourbillon", "Triple-axis tourbillon", "Reads 10:08 on the sub-dial, 22 s on the green bead", gimbal),
    ("planetary-ring", "Planetary ring-gear clock", "Reads 10:08: red planet at 10, sun-gear dot at 8 min", planetary),
    ("verge-foliot", "Verge and foliot tower clock", "Reads a little past 10: one hand, hours only", verge),
    ("pendulum-wave", "Pendulum wave", "Reads 10:08: 10 lit bobs, 8 beads; the wave shape is the seconds", wave),
    ("foucault-pegs", "Foucault pendulum and pegs", "Reads 10:08: 10 lamps lit, 8 pegs down (4 each side)", foucault),
    ("conical-pendulum", "Conical pendulum", "Reads 10:08:22: hour bead, minute bead, the ball's shadow", conical),
    ("clepsydra", "Float clepsydra", "Reads 10:08 where the pointer meets the hour column", clepsydra),
    ("time-flow", "Siphon time-flow clock", "Reads 10:38: 10 hour bulbs, 3 ten-minute vessels, tube at 8", timeflow),
    ("binary-buckets", "Tipping-bucket binary cascade", "Reads 10:08 by adding the full buckets", buckets),
    ("hourglass-carousel", "Hourglass carousel", "Reads 10:08: 10 spent, the 11th 8 min into its run", carousel),
    ("sand-table", "Sand table that writes the time", "Reads 10:08, with the 8 still being drawn", sandtable),
    ("water-curtain", "Water-curtain digits", "Reads 10:08 in the upper drops; 10:07 is still falling", curtain),
    ("congreve", "Congreve rolling-ball clock", "Reads 10:08 on the three dials; ball mid-run", congreve),
    ("ball-rails", "Rolling-ball rail clock", "Reads 10:08: 10 hours + 1×5 min + 3 min", railclock),
    ("domino-ring", "Domino ring", "Reads 10:08:22: hour block, minute tile, falling front", domino),
    ("minute-machine", "Sixty-second chain reaction", "Reads 10:08 on the flaps; the run has just begun", rube),
    ("marble-pixels", "Marble pixel board", "Reads 10:08 in marbles", marbles),
    ("sundial", "Sundial under a real sun", "Reads about 10:08 where the shadow crosses the hour lines", sundial),
    ("shadow-digits", "Shadow-digit sculpture", "Reads 10:08, but only in the shadow", shadowblocks),
    ("anamorphic", "Anamorphic shards", "Reads 10:08 from this one camera position", anamorph),
    ("candle", "Candle clock with dropping balls", "Reads 10:08: 10 balls down, wax just into band 11", candle),
    ("orrery", "Orrery geared to the clock", "Reads 10:08:22: inner planet, blue planet, comet", orrery),
    ("globe", "Globe in a 24-hour sun ring", "Reads 10:08 where the home meridian meets the ring", globe),
    ("star-dome", "Star dome with the Plough as hand", "Reads 22:08 on the 24 h ring, after date correction", stardome),
    ("split-flap", "Split-flap wall", "Reads 10:08; the last flap is mid-fall", splitflap),
    ("pin-field", "Pin-field relief", "Reads 10:08 in raised pins", pins),
    ("construction-crew", "Construction crew", "Reads 10:08 once the crane sets the last planks", crew),
    ("train-loops", "Train loops", "Reads 10:08:22: one train per hand", trains),
    ("hour-tree", "Hour tree", "Reads 10:08: 10 branches, then 1 leaf and 3 buds", tree),
]


def main():
    if len(sys.argv) > 1:
        print(__doc__.strip())
        sys.exit(0 if sys.argv[1] in ("-h", "--help") else 2)
    for i, (slug, title, reads, fn) in enumerate(SCENES, 1):
        sc = S()
        fn(sc)
        sc.save(i, slug, title, reads)
    print(f"wrote {len(SCENES)} sketches to {OUT}")


if __name__ == "__main__":
    main()
