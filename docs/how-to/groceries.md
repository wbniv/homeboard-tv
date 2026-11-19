# Groceries on the phone and TV

Add **Home settings → Widgets / Restore → Add widget → Groceries**. The widget
starts empty and does not rearrange the default dashboard. Short OK opens the
complete list with item notes and product/stock detail; hold OK opens Configure,
Move, Resize and Remove widget. Back and **Restore dashboard** return to the
original Home page, widget focus and size.

Open **Configure from phone** and approve the normal TV pairing. Tap the Groceries
card, enter an item name and press Add or the keyboard's Enter key. Keep adding;
these changes stay in the phone draft until **Update TV**. Return the TV to Home
before updating. One update saves the reviewed changes together and supports Undo.
There is no purchase check-off yet: **Remove from list** is a manual edit.

Use **Add several** for one item per line, review the preview, then add the batch.
Tap a row to edit its name, quantity text or note. Drag its grip, use Alt+Up/Down
on the grip, or use its up/down buttons to reorder it. **Undo last grocery draft
change** reverses the most recent saved grocery draft action. The shared Undo
update reverses a committed TV update.

Turn on **Keep as staple** in an item's details to retain it for later use. The
**Staples** tab shows regularly bought products even after their current shopping
row is removed. Re-adding a staple reuses its saved quantity text. **Products**
also contains saved products that are not staples. Brand, variant, aliases and
category are optional. A product is local to its named list.

In product details, choose how to track stock:

- **Stock not tracked yet:** keep a reusable product without reporting inventory.
- **Manual need:** use Need more, Enough or Stock unknown directly on its card.
- **Track quantity:** choose g, ml or pieces as the stock base unit; report stock,
  low-stock threshold and target. Leave stock blank if unknown. Reports show their
  timestamp and never decrease automatically.

For a confirmed package, enter its name and contents in base units: **carton**,
**1000 ml**. Structured shopping quantities can then use cartons or compatible
units. Supported conversions are g/kg, ml/l and piece/dozen. A package has no
universal size, and mass is never converted to volume. Unclear amounts can remain
plain text.

A staple with 500 ml reported stock, a 1000 ml threshold, a 3000 ml target and
1000 ml cartons suggests **3 cartons**. Review the deficit and expected stock,
then **Add to shopping list**. An existing linked need is opened for review;
**Use suggested** explicitly replaces its amount. A second confirmed product
entry offers an explicit total increase instead of creating a duplicate need.
Adding a shopping need does not change stock. Updating stock does not clear needs.

**List settings** lets you rename, create or delete a list. Multiple widgets can
show the same list. Removing a widget preserves its data. Deleting a saved product
keeps its current shopping row as free text. Product merging previews which
product/package/stock report survives and requires explicit confirmation.

Each TV owns independent lists. This temporary home-network controller cannot
sync purchases from a shop or share lists between Cast1 and Cast2. Phase 3 will
add purchase completion and choose its sync architecture. No external shopping
service is connected. See the [plan and app survey](../plans/2026-11-19-grocery-list-widget.md).
