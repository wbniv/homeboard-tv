#!/usr/bin/env python3
"""Export a verified frozen public deployment and guide into the existing Astro site."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import shutil

root = Path(__file__).resolve().parent.parent


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def export(deployment, site):
    record = json.loads((deployment / 'deployment.json').read_text())
    release = dict(record['verification'] or {})
    if record.get('variant') != 'release' or release.get('packageId') != 'net.biohack.homeboard':
        raise ValueError('Only verified public release deployments can be exported')
    if release['sha256'] != record['sha256'] or digest(deployment / 'homeboard.apk') != release['sha256']:
        raise ValueError('Frozen APK hash mismatch')
    if release['sizeBytes'] != (deployment / 'homeboard.apk').stat().st_size or release['sizeBytes'] > 25 * 1024 * 1024:
        raise ValueError('Artifact size mismatch or Pages limit exceeded')
    version = release['versionName']
    if not re.fullmatch(r'\d+\.\d+\.\d+', version):
        raise ValueError('Invalid version')
    files = site / 'public/homeboard'
    releases = files / 'releases' / version
    releases.mkdir(parents=True, exist_ok=True)
    def immutable_copy(source, target):
        if target.exists() and digest(source) != digest(target):
            raise ValueError(f'Refusing to replace immutable release bytes: {target}')
        if not target.exists():
            shutil.copyfile(source, target)
    apk_name = f'homeboard-tv-{version}.apk'
    immutable_copy(deployment / 'homeboard.apk', releases / apk_name)
    immutable_copy(deployment / 'source.tar.gz', releases / 'source.tar.gz')
    (releases / 'SHA256SUMS').write_text(f"{release['sha256']}  {apk_name}\n{digest(releases / 'source.tar.gz')}  source.tar.gz\n")
    release.update(channel='early-release', sourceCommit=record['base_commit'],
                   publishedDate=datetime.now(timezone.utc).date().isoformat(),
                   downloadUrl=f'/homeboard/releases/{version}/{apk_name}',
                   sourceArchiveUrl=f'/homeboard/releases/{version}/source.tar.gz',
                   sourceArchiveSha256=digest(releases / 'source.tar.gz'),
                   notes=json.loads((root / 'dev/publication/release-notes.json').read_text()))
    for name in ('src/data', 'src/pages/homeboard', 'src/layouts', 'scripts/homeboard', 'docs/sources'):
        (site / name).mkdir(parents=True, exist_ok=True)
    (site / 'src/data/homeboard-release.json').write_text(json.dumps(release, indent=2) + '\n')
    templates = root / 'dev/publication'
    for source, target in [('index.astro','src/pages/homeboard/index.astro'),
                           ('HomeboardGuide.astro','src/layouts/HomeboardGuide.astro'),
                           ('validate-release.mjs','scripts/homeboard/validate-release.mjs'),
                           ('release.test.mjs','tests/homeboard-release.test.mjs')]:
        shutil.copyfile(templates / source, site / target)
    (files / 'images').mkdir(exist_ok=True)
    shutil.copyfile(root / 'docs/how-to/quick-start/dashboard-youtube.png', files / 'images/dashboard.png')
    shutil.copyfile(root / 'docs/testing/2026-11-19-cooking-timers/cast1-expanded.png', files / 'images/timers.png')
    for source, target in [('LICENSE','LICENSE.txt'), ('THIRD_PARTY_NOTICES.md','THIRD_PARTY_NOTICES.txt')]:
        shutil.copyfile(root / source, files / target)
    # Preserve the full notices and licenses of bundled dependencies.
    licenses = files / 'licenses'
    licenses.mkdir(exist_ok=True)
    shutil.copyfile(root / 'launcher/FONT-LICENSE-OFL.txt', licenses / 'FONT-LICENSE-OFL.txt')
    for source in (root / 'launcher/app/src/main/assets/licenses').glob('*'):
        if source.is_file(): shutil.copyfile(source, licenses / source.name)
    guide_dir = files / 'guide'
    guide_dir.mkdir(exist_ok=True)
    shutil.copyfile(root / 'docs/how-to/quick-start-print.pdf', guide_dir / 'quick-start.pdf')
    def asset(path):
        if not path.is_file():
            raise ValueError(f'Missing guide asset: {path}')
        assets = files / 'guide-assets'
        assets.mkdir(exist_ok=True)
        name = digest(path)[:12] + '-' + path.name
        shutil.copyfile(path, assets / name)
        return '/homeboard/guide-assets/' + name
    md = (root / 'docs/how-to/quick-start.md').read_text()
    def rewrite(match):
        prefix, url, suffix = match.groups()
        if url.startswith(('https:', 'http:', '#', '/')):
            return match[0]
        path = (root / 'docs/how-to' / url).resolve()
        if url == 'quick-start-print.html': target = '/homeboard/guide/print.html'
        elif url == 'quick-start-print.pdf': target = '/homeboard/guide/quick-start.pdf'
        elif path.suffix == '.md': target = 'https://github.com/wbniv/homeboard-tv/blob/main/' + str(path.relative_to(root))
        else: target = asset(path)
        return prefix + target + suffix
    md = re.sub(r'(!?\[[^\]]*\]\()([^\)]+)(\))', rewrite, md)
    (site / 'src/pages/homeboard/guide.md').write_text('---\nlayout: ../../layouts/HomeboardGuide.astro\n---\n\n' + md)
    html = (root / 'docs/how-to/quick-start-print.html').read_text()
    html = html.replace('quick-start/download-qr.svg', asset(root / 'docs/how-to/quick-start/download-qr.svg'))
    html = html.replace('https://github.com/wbniv/homeboard-tv/blob/main/docs/how-to/quick-start.md', '/homeboard/guide/')
    (guide_dir / 'print.html').write_text(html)
    homepage = site / 'src/pages/index.astro'
    content = homepage.read_text()
    if 'href="/homeboard/"' not in content:
        anchor = '      <li><a href="/shared-electron/"'
        if anchor not in content: raise ValueError('Homepage insertion point changed')
        content = content.replace(anchor, '      <li><a href="/homeboard/">Homeboard TV</a> — a personal Android TV dashboard with widgets and favorite apps</li>\n' + anchor, 1)
        homepage.write_text(content)
    headers = site / 'public/_headers'
    header_content = headers.read_text() if headers.exists() else ''
    if '/homeboard/releases/*' not in header_content:
        headers.write_text(header_content.rstrip() + '\n\n/homeboard/releases/*\n  Cache-Control: public, max-age=31536000, immutable\n  X-Content-Type-Options: nosniff\n\n/homeboard/releases/*.apk\n  Content-Type: application/vnd.android.package-archive\n  Content-Disposition: attachment\n')
    (site / 'docs/sources/homeboard.md').write_text(f'''# Homeboard TV publication sources

Accessed {release['publishedDate']}. Scope: the Homeboard page and exported guides only.

- Version {version}, package `{release['packageId']}`, minimum API 26, signed certificate and exact size/hash: measured with Android build-tools 34.0.0 `aapt` and `apksigner` in the Homeboard build container. Release metadata is checked against the frozen APK before export and at Astro build time.
- Features, controls and limitations: Homeboard README and `docs/how-to/quick-start.md`, base source commit `{release['sourceCommit']}`. Exact build sources are in `{release['sourceArchiveUrl']}` (SHA-256 `{release['sourceArchiveSha256']}`).
- Dashboard image: Cast1 0.1.74 capture with YouTube in Favorites, `docs/how-to/quick-start/dashboard-youtube.png`; fresh test layout and capture version are visible in its caption.
- Timers screenshot: measured Cast1 native test build 0.1.87, controlled demonstration timers; `docs/testing/2026-11-19-cooking-timers/cast1-expanded.png`. Cooking duration limits (24 hours), one-off event alarms and background alarm behavior: `KitchenTimers.java`, `KitchenTimerStore.java`, `KitchenTimerSound.java` and the cooking-timer acceptance record. A screenshot does not establish speaker audibility; the page asks users to test their own TV. The compact 2×1 size is a design recommendation, not a measured universal preference.
- Android 8.0+ requirement: `launcher/app/build.gradle`, `minSdk 26`; platform mapping: https://developer.android.com/tools/releases/platforms (API 26). Android TV and Google TV are the target platforms; other devices are not claimed tested.
- Signature/update requirement and preserving signing identity: Android Developers, https://developer.android.com/studio/publish/app-signing, signed APKs and key management.
- Sideload installation instructions: Android Developers, https://developer.android.com/distribute/marketing-tools/alternative-distribution, website distribution and unknown-app installation permission.
- Pages 25 MiB asset maximum: Cloudflare, https://developers.cloudflare.com/pages/platform/limits/#file-size. Measured APK size is {release['sizeBytes']} bytes, below that maximum.
- Independent CleanYourTV provenance and MIT terms: Homeboard `LICENSE`, `THIRD_PARTY_NOTICES.md`, `docs/upstream/README.md`. Exact source https://github.com/Gathin23/cleanyourtv/tree/ddc3b2cdce299a5f30aa2da2437edc40f3cf6f2e/launcher.

No account required describes the native Homeboard dashboard; separate wallpaper providers may require accounts. The optional cleanup companion is a separate computer tool, not a component installed by this APK. Public availability and physical acceptance are recorded separately in Homeboard's publication test record.
''')
    print(f'Exported Homeboard {version} to {site}; APK SHA-256 {release["sha256"]}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--deployment', type=Path, required=True)
    parser.add_argument('--site', type=Path, required=True)
    args = parser.parse_args()
    export(args.deployment.resolve(), args.site.resolve())
