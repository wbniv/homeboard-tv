#!/usr/bin/env python3
"""Verify the signed release APK and emit public, non-secret artifact metadata."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile


def inspect(apk):
    tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/34.0.0'
    def run(tool, *args):
        return subprocess.check_output([str(tools / tool), *args], text=True)
    signature = run('apksigner', 'verify', '--verbose', '--print-certs', str(apk))
    run('zipalign', '-c', '-p', '4', str(apk))
    badging = run('aapt', 'dump', 'badging', str(apk))
    manifest = run('aapt', 'dump', 'xmltree', str(apk), 'AndroidManifest.xml')
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    certificate = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})', signature)
    if not package or package[1] != 'net.biohack.homeboard' or not certificate:
        raise ValueError('Expected signed public Homeboard package')
    if "sdkVersion:'26'" not in badging:
        raise ValueError('Unexpected minimum Android SDK')
    if 'application-debuggable' in badging or re.search(r'android:debuggable[^\n]*0xffffffff', manifest):
        raise ValueError('Public APK must not be debuggable')
    # The inherited, private ProbeActivity is used by HomeKeyService at boot.
    # Debug UI probes have distinct names and must never enter a public APK.
    probes = re.findall(r'com\.gathin\.tvhome\.\w*ProbeActivity', manifest)
    if any(probe != 'com.gathin.tvhome.ProbeActivity' for probe in probes) or 'OfflineWallpaper' in manifest or 'Fixture' in manifest:
        raise ValueError('Debug fixture or probe found in release manifest')
    with zipfile.ZipFile(apk) as archive:
        if any('fixture' in name.lower() for name in archive.namelist()):
            raise ValueError('Fixture payload found in APK')
    props = dict(line.split('=', 1) for line in Path('/workspace/version.properties').read_text().splitlines() if line)
    code = int(package[2])
    if code != int(props['version_code']) or package[3] != f"{props['major']}.{props['minor']}.{code}":
        raise ValueError('APK version does not match version.properties')
    return {'packageId': package[1], 'versionCode': code, 'versionName': package[3],
            'sizeBytes': apk.stat().st_size, 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
            'certificateSha256': certificate[1], 'minSdk': 26}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = inspect(args.apk)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(f"Verified signed public APK {result['versionName']}; SHA-256 {result['sha256']}")
