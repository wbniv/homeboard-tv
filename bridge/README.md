# Integration bridge boundary

Optional future design, not implemented or required for the air-quality card. The bridge authorizes Calendar in a browser, talks to selected
Home Assistant entities and other providers, and supplies typed snapshots/updates to the TV.
Each value needs source, units, observation/forecast time and freshness. Credentials stay out of
scene exports and source control. Provider caching and attribution terms still apply.

The native AirBKK card fetches a public feed directly. If a future provider needs a
bridge, choose the server runtime, pairing protocol,
authentication, reconnect behavior and schema together before implementing transport. No network
server, account, secret, API key or production endpoint is created by the project scaffold.

Flashcard progress currently belongs to the existing phone web app. A shared session requires
an explicit design; the launcher must not silently become a second grading authority.
