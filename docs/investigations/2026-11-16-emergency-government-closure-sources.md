# Official sources for emergency government-office closures

Investigated 16 November 2026. This is publisher/source evidence, not a claim
that the app already checks live notices.

## Selected publisher

Use [Thai Government spokesperson news](https://www.thaigov.go.th/th/news?category=2628)
for national-government decisions, including decisions that apply only to named
provinces. A real [announcement published 27 September 2026 at 17:10](https://www.thaigov.go.th/th/news/169370)
adds emergency holidays for flooding; [Government PRD's English report](https://thailand.prd.go.th/en/content/category/detail/id/48/iid/545500)
corroborates the decision and exceptions.

Normalized facts for an ingestion regression fixture:

| Field | Value |
|---|---|
| Issuer | Thai Government / Deputy Government Spokesperson |
| Published | 2026-09-27 17:10 Asia/Bangkok (Thai Government page) |
| Effective dates | 2026-09-28 through 2026-09-29, inclusive |
| Areas | Bangkok, Nonthaburi, Pathum Thani, Samut Prakan |
| Office scope | Government offices in those areas |
| Cause | Heavy rain and flooding |
| Exceptions | Online services continue; essential duties and existing appointments may continue at agency heads' discretion |
| Other sectors | Banks, state enterprises, private businesses and hospitals decide separately; this is not a blanket business closure |

This must appear for an affected location and remain absent for an unaffected
province. It must not become a nationwide holiday. An agency-specific service
notice can qualify what a particular office does on those dates.

## Retrieval evidence

Direct HTTPS retrieval of the Thai Government article succeeded, returning about
865 KiB of server-rendered HTML with the announcement text, timestamp, geography
and exceptions. JavaScript execution was not needed to retrieve that article.
Use bounded responses (allowing this observed size), DOM/text extraction and a
source-specific parser, rather than depending on generated Next.js chunk names.
The government listing's pagination, historical backfill and update detection
remain to be verified before implementing a reliable twice-daily check.

The [Thailand.go.th official PRD feed catalog](https://www.thailand.go.th/rss)
advertises issue RSS/JSON endpoints. Search-indexed JSON exposes article bodies,
created/updated times and only five posts by default. Direct requests to the JSON
endpoint and an article returned a 212-byte Incapsula JavaScript challenge with
HTTP 200. Validate content type, expected schema and actual records; HTTP 200
alone is inadequate. These feeds are discovery candidates, not a verified sole
native transport. No TLS verification was disabled.

## Implementation contract still required

Check at 06:00 and 18:00 Bangkok time, with resume catch-up and manual refresh.
Read enough overlapping pages to reach the last successful checkpoint; a cap,
unreadable page, challenge, parser failure or ambiguous new notice means a partial
check. Revisit recent known notices for amendments or withdrawal. Store issue
and effective dates separately, preserve explicit areas and exceptions, and
replace cached events atomically only after validation. A weather warning does
not create a closure. Do not claim that all local/individual-office announcements
are covered by the national spokesperson feed.

The source exists and demonstrably announces emergency closures. The remaining
work is ingestion reliability and regional/office coverage, rather than relying
on an annual holiday list to stand in for emergency announcements.
