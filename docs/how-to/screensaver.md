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

Until you use the screensaver everywhere (below), Android's own screensaver and idle
time are not changed by this screen; **In other apps** shows what Android has now, for
example "Android's Ambient mode after 10&nbsp;min · unchanged by Homeboard". If Android's
timeout is shorter than Homeboard's, Android's screensaver starts first. If Homeboard
cannot read Android's setting on your TV, the line says so ("Unknown").

Any remote key ends the screensaver and returns to Home where you left it; that key does
not also act on Home. A ringing timer or alarm from
[Timers & alarms](timers-and-alarms.md) always ends it so you see it.

## Android screensaver apps

Screensaver apps installed on the TV (for example Aerial Views, or an Ambient mode that
came with the TV) appear in the list after the built-in entries, under their own names,
marked **☾ Android screensaver app**. Android runs them, not Homeboard, so they have no
Preview: Android gives apps no way to start another app's screensaver, and the right side
says so. When the app has its own settings screen, **Open ‹app› settings** opens it. To
use one, choose **Use everywhere…**.

## Use everywhere

"Everywhere" means Android itself starts the screensaver after its own idle time, in every
app and on Home, so one timer is in charge. Homeboard's own **Start on Home after** timer
is then off, and **When it starts** says "Android starts it after 10&nbsp;min". Android's
idle time is shown but not changed here; change it in Android's own settings.

### One-time setup

Changing Android's screensaver needs a permission that only a developer command can
grant. Until it is granted, **Use everywhere…** shows **This TV needs one-time setup**
with the command for this install, for example:

```bash
adb shell pm grant net.biohack.homeboard android.permission.WRITE_SECURE_SETTINGS
```

The package name differs between builds (a debug build ends in `.debug`), so copy the one
on the TV's screen; **Show QR code** puts the command on your phone. Run it once from a
computer with `adb` connected to the TV, then press **Retry**. Opening the screen and
Retry change nothing. Without setup, everything else on this page still works on Home.

### Asked once

With the permission granted, **Use everywhere…** asks one question, once: **Let
Homeboard manage this TV's screensaver?** It shows what Android uses now, what it will
use after, and Android's idle time. **Let Homeboard manage it** makes the change:
Homeboard first keeps the original, then writes the new setting and reads it back to make
sure Android took it. **Cancel** or Back changes nothing.

After that, every **Use this screensaver** applies in every app at once, with the usual
Undo banner and no further question. Undo puts back the previous screensaver, which is
not necessarily the original.

### This TV's screensaver

Once Homeboard manages it, **This TV's screensaver…** (beside **In other apps**) shows
what Android starts, after how long, and the kept original. It offers:

- **Start now**: Android's own "start screensaver now" shortcut.
- **Open ‹app› settings**, when an Android screensaver app is in use and has settings.
- **Restore ‹original›**: puts the original back, reads it back and ends Homeboard's
  management, with Undo. A Homeboard choice stays on Home; an app choice falls back to
  Clock on Home. Homeboard keeps the original for good, so Restore always returns to the
  screensaver the TV had before Homeboard first changed it.

### When something goes wrong

- **Android did not accept the change. This TV's screensaver is still ‹x›.** Android
  refused the write, or read back something else; Homeboard put back what was there and
  changed nothing else. **Retry**, or check the one-time setup.
- **‹app› is no longer installed.** The app in use was uninstalled. Homeboard restores the
  TV's original screensaver, plays Clock on Home and says so once on Home, without Undo
  because the app is gone.

A remote button mapped to the **screensaver** action (`RemoteKeys`) plays your Homeboard
choice at once, or, when an Android screensaver app is chosen, asks
Android to start it.

Playing wallpaper providers as screensavers and the phone panel are later stages of the
[screensaver plan](../plans/2026-11-20-screensavers.md).
