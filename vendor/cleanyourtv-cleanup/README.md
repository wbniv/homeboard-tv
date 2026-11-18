# Pinned CleanYourTV cleanup subset

Source: [CleanYourTV](https://github.com/Gathin23/cleanyourtv/tree/3ece7766ad5d6af79a2c4dd73e77b4b9623dfeca),
commit `3ece7766ad5d6af79a2c4dd73e77b4b9623dfeca`. Original MIT license and copyright
are retained in [LICENSE](LICENSE). [provenance.json](provenance.json) records the
original hash of every copied file. These files are unchanged.

The runtime adapter in `cleanup/core.py` reuses the recovery module's package and
Home validation, core-package protections and atomic record-writing primitive.
The original disable/restore shell entry points are retained for their copied
fake-TV regression tests. **Do not run them against a shared TV.** No upstream
ADB transport or `tools/tv.py` is included. The inspection companion talks only to
the coordinator through the project's Task adapter.

The original recovery module includes legacy record handling and a no-record
blanket-enable fallback. Those remain as upstream source behavior and are covered
by the original tests. Homeboard does not call that command generator or claim its
records are interchangeable: it uses a strict separate journal preserving exact
enabled-setting values and requires verified recovery records. Original upstream
tests passing do not authorize the fallback or physical package changes.

The Chromecast profiles are reference evidence, not approved execution profiles.
They have broad effects outside this pilot's allowed scope. No profile in this
directory is automatically loaded into the companion's Apply policy.

Run `task cleanup:check` to verify hashes and run Homeboard's fault/security tests
and the copied recovery suites in the container. The copied troubleshooting page
is a fixture for upstream's address-change regression test. Update this subset
only by reviewing a new commit and its dependencies, recording hashes and local
diffs, then rerunning the tests. No CleanMyTV implementation was copied.
