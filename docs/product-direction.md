# Homeboard TV: product and community direction

Homeboard TV is our own branded TV dashboard and launcher, built on CleanYourTV.
The intention is to publish it as an independent product and participate in the
CleanYourTV community, including its gallery/showcase and useful upstream contributions.
The working name is **Homeboard TV**.

## What we are building

Will's Home screen contains chosen widgets and favorite apps, or nothing at all.
Scrolling right past the widget pages opens All Apps. Holding OK exposes tile
management, with separate Delete tile and Uninstall app actions.

The first integration is public Bangkok air quality, with no account or extra
server to set up. The native widget Home is implemented locally. Future candidates
include school travel time, Calendar through a suitable integration or custom bridge,
Home Assistant sensors when available, hourly weather and air quality, market data,
Thai holidays, language practice and exercise. Scheduled weekday/weekend layouts,
moving backgrounds and broader theme customization remain part of the direction.

See the [interaction plan and mockups](plans/2026-11-15-widget-home.md) and
[illustrated quick start](how-to/quick-start.md) for the current implementation.

## How CleanYourTV fits

CleanYourTV supplies the source foundation and the community we want to participate
in. This repository imports its launcher at a recorded revision, preserves its MIT
license and attribution, and adds Homeboard's own identity and dashboard behavior.
Homeboard will have its own repository, builds, release decisions and maintenance.

The gallery/showcase is a route for people to discover Homeboard and for us to share
what we build. It is a community relationship, not currently an embedded gallery
service or a runtime dependency in the APK. Direct in-app gallery browsing or theme
installation would be a separate feature decision; this statement does not claim
those features exist today.

We intend to contribute a Homeboard listing once a public, usable build and its
screenshots/documentation are ready. Reusable fixes or small improvements that fit
CleanYourTV's scope should be prepared as focused upstream contributions. Homeboard's
larger dashboard and integrations can evolve in this repository. Preserve provenance,
check the applicable contribution rules, and do not imply official endorsement.

The [upstream strategy](upstream/README.md) and [research](investigations/2026-11-15-tv-dashboard-launcher-comparison.md)
record the basis for this approach. The public Homeboard repository is
[github.com/wbniv/homeboard-tv](https://github.com/wbniv/homeboard-tv). Listing
acceptance and upstream merges remain maintainer decisions; neither has been
requested yet.

## Publication path

- [x] Establish the Homeboard identity, source provenance and local project.
- [x] Implement widget Home, All Apps and the first public-data widget.
- [x] Write an illustrated quick start and record the product direction.
- [ ] Finish device acceptance checks and document remaining limitations.
- [x] Create the public repository at [github.com/wbniv/homeboard-tv](https://github.com/wbniv/homeboard-tv).
- [ ] Prepare release artifacts and attribution.
- [ ] Publish a production release under the Homeboard brand.
- [ ] Submit a CleanYourTV gallery/showcase listing and suitable upstream contributions when authorized.
- [ ] Evaluate Google Play distribution, including current TV requirements, signing and provider terms.

Google Play is an intended distribution option, not yet an approved or completed
release route. No store release or upstream submission has happened.
