# Default YouTube favorite

Fresh dashboards seed the installed YouTube TV launcher into Favorites after the
first successful app discovery. Use the actual resolved component rather than
inventing an activity name; fall back to the installed Android YouTube package
when the TV package is absent. Do not create an unavailable shortcut if neither
is installed. Existing favorites, saved empty lists and explicit preset restores
keep their contents. Removing YouTube does not make it return on refresh.

```text
World clock                                         Settings
Rain — next 24 hours
Calendar                                    Air quality
Favorites   [YouTube]                                     >
```

Store a pending first-discovery flag with the fresh layout so a process restart
before discovery does not lose initialization. Consume it once after successful
app discovery; failed discovery can retry. Preserve favorites edited before that
first discovery. Verify installed/missing app cases and removal persistence in
container tests. Deploy a new frozen version to both TVs, preserving their saved
layouts. The seed is for new dashboards, not an update migration of existing TVs.
