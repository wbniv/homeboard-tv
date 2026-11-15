# Upstream provenance and maintenance

Imported CleanYourTV's launcher subtree at `ddc3b2cdce299a5f30aa2da2437edc40f3cf6f2e` into a new local Git repository.
This is a source-derived project, not a full-history GitHub fork. The read-only-intended
`upstream` remote identifies the original repository; it is not this project's publication remote.
The public Homeboard repository is `https://github.com/wbniv/homeboard-tv`, configured as `origin`.

`provenance.json` records original SHA-256 values before Homeboard changes. Upstream's MIT
license and font license are retained. The reference instruction files document upstream's
contribution policy; they do not define this independent project's scope.

The debloat/install scripts were intentionally omitted: device sessions belong to the shared
coordinator. Compare future changes against the pinned upstream subtree in a temporary checkout;
port focused fixes and update provenance deliberately. Do not blindly merge upstream root history
into this extracted project.

Homeboard changes: distinct application identity/name/version, unsigned release
packaging, container build/CI, coordinator adapter, documentation and a native
AirBKK PM2.5 card, widget Home/All Apps shell and independent launcher artwork.
The original air-card activity is retained as a reference; the new shell replaces
dock-specific UI behavior, so not every upstream option is exposed.
The Java namespace remains unchanged to keep source comparisons
focused. No Kotlin or dashboard framework is required for the current integration.

## Contribution strategy agreed with Will

Homeboard is intended for independent publication under its own brand and for
participation in CleanYourTV's gallery/showcase. See the [product direction](../product-direction.md).

The air-quality feature stays in Homeboard. Its initial omission from CleanYourTV's
existing Hebrew layout was introduced by Homeboard's new card code. Sharing the
card layout fixes our own omission; there is no upstream bug or patch to submit
for that change.

Potential upstream fixes must reproduce independently in CleanYourTV and be useful
without Homeboard's dashboard. Prepare focused patches with reproduction steps and
validation. Review current upstream scope and AI-attribution requirements before
contributing; obtain tool/model/effort metadata from the actual session when required.

For the larger dashboard, work toward a showcase entry once a public repository,
usable build, screenshots and setup documentation exist. Prior maintainer responses
favor independent networked dashboards over expanding the core launcher. Recheck
current submission rules at that point.

No upstream outreach, issue, PR, showcase submission or publication has happened.
Preparing a contribution is distinct from sending it; obtain authorization before
external publication or messages.
