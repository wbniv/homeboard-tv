# Choose and configure the screensaver

On the TV, open **Home settings → Screensaver**. The list on the left shows what can
play when the TV is idle: **Clock** (clock and date on a slow glow) and **Home
background** (the background Home is showing, full screen). Both are built in. Four
entries fit a page; there is no scrollbar. The entry in use carries an **In use** label.

Move to an entry and the right side shows a preview, one sentence about it, its actions
and **When it starts**. Press OK on an entry to reach its actions:

- **Preview** plays it full screen at once. OK uses it; Back cancels and nothing is saved.
- **Use this screensaver** saves the choice, with the usual Undo banner. Hold Back to
  Undo, or open **Home settings → Undo history**.
- **Options** opens that entry's settings and **When it starts**.

## Options

| Entry | Options |
|---|---|
| Clock | Backdrop: Glow, Home background or Black (for OLED panels). Dim backdrop, used with Home background. |
| Home background | Show clock on or off. Dim. |
| All | Start on Home after: Android decides, 1, 2, 5, 10, 15 or 30 min without input. |

Each change saves as you make it, with Undo. These settings are kept apart from Home's
own background, so dimming the screensaver does not dim Home. Some things are not
options: sound is always off, the clock moves every minute so nothing burns in, and the
time format follows Home (**Launcher settings → Clock**).

## When it starts

**Android decides** is the default: Homeboard runs no timer of its own, which is how
Homeboard behaved before this screen existed. Choose a time and Homeboard shows your
screensaver after that long without input, but only while Home itself is on screen and
no menu, editor or full-screen widget is open. It never starts over live TV or an app.

Android's own screensaver and idle time are not changed by this screen. If Android's
timeout is shorter than Homeboard's, Android's screensaver starts first. If Android's
setting already names Homeboard's screensaver (`com.gathin.tvhome/.ClockDream`), Android
plays the entry chosen here.

Any remote key ends the screensaver and returns to Home where you left it; that key does
not also act on Home. A ringing timer or alarm from
[Timers & alarms](timers-and-alarms.md) always ends it so you see it.

Choosing an Android screensaver app, playing wallpaper providers as screensavers and
using the choice in every app are later stages of the
[screensaver plan](../plans/2026-11-20-screensavers.md).
