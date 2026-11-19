# Thai civil-calendar reference

Pinned source: https://github.com/hmmbug/pythaidate/tree/f526c4d9d9ee2a854bf790aaf6d6ee6db90f3f25

Copyright 2023 Mark Hollow; MIT notice retained in LICENSE.txt. The five Python
files in `reference/` are unchanged and individually hash checked by the calendar
pipeline tests. They are not packaged as a runtime dependency.

`ThaiCivilCalendar.java` is a narrow Java adaptation of LSYear classification and
CsDate's five-year reconciliation and Julian-day conversion. It omits formatting,
date operators and the separate PakDate/Pakkhakhananaa system. Its month/day
results are checked against reference hashes for every date in 1900–2100 and
independently published Wan Phra fixtures for 2024–2027. The initial supported
civil-calendar interval is 2024–2027; arithmetic equivalence beyond that interval
does not claim independent published-calendar validation.

`task run -- python3 /workspace/dev/calendar/reference_fixtures.py` reproduces the
reference hashes in the build container. Review the port, source hashes, fixtures
and license together when deliberately updating this pin.
