# Configure widgets from your phone

1. Open **Home settings → More → Configure from phone** on the TV.
2. Scan the QR code using your phone camera, or enter the displayed address in
   your browser. Use the same home network. Homeboard must stay open on the TV.
3. Check that the connection code matches on both screens, then choose **Allow**
   on the TV. This approval lasts for the temporary editing session.
4. Choose **Widgets**, **Layout**, or **Favorites** on the phone. Edits remain in
   a draft. Choose the correct widget instance when there are several Rain or
   Air-quality cards.
5. Review **Changes**, then **Apply to TV**. The acknowledgement names the TV.
   **Undo** restores the complete previous configuration, including mixed widget
   settings and favorite order.
6. Choose **Stop editing** when finished. Leaving Homeboard or 15 minutes without
   editor activity ends access. Pair again to reconnect.

Rain and model air quality can keep **TV device location · Automatic**, or use
fixed coordinates. Rain supports city/address search through the TV's existing
lookup providers. Phone geolocation is not requested by this local HTTP page.

The layout preview on the phone is schematic. **Preview on TV** shows native
widget geometry for 30 seconds; Rain/Air display their proposed configuration
without fetching draft-location data. Apply refreshes actual data. Back, Cancel,
timeout or disconnect restores the saved dashboard. Preview creates no Undo entry.

If someone changes settings on the TV, reload the latest saved configuration and
review your retained draft before applying. The phone cannot overwrite an open
TV menu, Move/Resize or full-screen widget. Finish that action on the TV first.
A second phone needs its own TV approval and replaces the first editor.

The address belongs to one TV. To edit another, stop the session and scan that
TV's QR code. Cast1 and Cast2 keep independent settings. No phone app or account
is required. This first editor uses local HTTP on a trusted home network; pairing
limits access but does not encrypt network traffic. Guest-network isolation or a
browser's local-network denial can prevent connection.

Drafts are retained in the current browser tab's session storage for review.
Session credentials are held only in memory; refreshing the page requires fresh
TV approval. The service does not stay available while another TV app is open.
