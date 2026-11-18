# Configure widgets from your phone

1. Open **Home settings → More → Configure from phone** on the TV.
2. Scan the QR code or enter the displayed address in your phone browser. Use the
   same trusted home network and keep Homeboard open on the TV.
3. Match the connection code on both screens, then choose **Allow** on the TV.
4. **Your dashboard** shows the current cards. Tap a card or its named row to
   change its settings. **Arrange dashboard** moves and resizes cards; exact
   page, column and row values under **Advanced placement** start at 1.
5. In **Favorites**, drag a row by its grip to reorder it. Neighbors move to show
   the destination; the list scrolls near the screen edge. A canceled gesture
   leaves the order unchanged. **Undo reorder** reverses the last draft reorder.
   Tap the grip or **More options** beside an app for accessible move actions;
   keyboard users can use Alt+Up/Down on the grip. **Add apps** opens a separate
   picker. Removing a favorite never uninstalls an app.
6. Review the changes and choose **Update [TV name]**. Widget settings, layout and
   app order are sent together. **Undo update** restores the complete update. On the TV, return to Home or All
   Apps and hold Back for one second to undo the latest saved change.
7. Open the target-TV menu and choose **Done editing** when finished. Leaving
   Homeboard or 15 minutes without editor activity ends access. Pair again to
   reconnect; background status polling does not extend access.

The **Top bar** section contains **World clock · Fixed**. Edit its cities, labels,
order and display settings there; its position beside Settings is fixed. Short OK
on the TV still opens the full-screen meeting planner. [Clock guide](world-clock.md).

For one phone icon per TV, open the TV-name menu → **Add TV to home screen** and
follow your browser’s shortcut instructions. The icon remembers the TV identity,
not permanent editing permission. [Shortcut guide](phone-tv-shortcuts.md).

**Get wallpaper providers** is available on the overview and TV-name menu. The
page names the connected TV and shows installed versions, Google Play links and
community publisher pages. In Play, choose the TV yourself; Homeboard pairing
does not select it. Installed providers are configured and previewed on the TV.
This directory does not modify the draft or require **Update TV**. Weather v2.11
has no install link. [Provider guide](provider-directory.md).

Rain and model air quality can use **TV location · Automatic** or a fixed place.
One city/address search uses the TV's existing lookup providers; exact coordinates
are also available. Phone geolocation is not requested by this local HTTP page.

The phone layout diagram is schematic. Drag the selected card's move grip or
resize corner; blocked placements cannot be dropped. Named size choices and
explicit move/resize buttons provide alternatives. **Preview on TV** shows native
geometry for 30 seconds; Rain/Air show the proposed configuration without fetching
draft-location data. Updating refreshes actual data. Back, Cancel, timeout or
session end restores the saved dashboard. Preview creates no Undo entry.

If the TV changes while you edit, independent setting changes are combined in
your draft. Conflicting changes show **On TV** and **Your edit** for you to choose.
Replacing a card on the TV requires explicit resolution rather than silently
applying an old draft to the replacement. Invalid or overlapping arrangements must
be adjusted before updating. Finish any TV menu, Move/Resize or full-screen action
before updating from the phone.

A lost update response is checked against its operation receipt. Until its result
is known, the controller keeps that operation and offers to check or retry the
same update. It does not send a fresh update automatically.

Drafts and pending operations are kept in this browser tab's session storage,
bound to a stable TV identity. After refresh and fresh TV approval, choose
**Restore kept draft for review**. Credentials stay only in memory. The temporary
server reuses its local port when available; a changed address or port creates a
different browser origin, so the old tab's stored draft cannot be recovered there.
Keep the old tab open to review it. Private browsing/storage limits may prevent
persistence; drafts remain in memory while the tab is open.

To change TVs, finish this session and scan the other TV's QR. Cast1 and Cast2 keep
independent settings. A second phone requires TV approval and replaces the first
editor. No phone app or account is needed. Local HTTP is unencrypted; guest Wi-Fi,
network isolation or a denied browser local-network permission can block access.

Run `task phone:setup` once to install the pinned test-only packages in the
browser container, then use `task phone:check` for container browser acceptance. The fixture checks real
Chromium/WebKit rendering and input with controlled API responses. It does not
establish connectivity from a separate physical phone to a Chromecast.
