#!/usr/bin/env python3
"""Inspect frozen APKs with Android build tools inside the build container."""
import json
import os
from pathlib import Path
import re
import subprocess

root = Path('/workspace/artifacts/provider-compatibility/2026-11-18')
sdk = Path(os.environ['ANDROID_HOME'])
build_tools = sorted((sdk / 'build-tools').iterdir())[-1]
results = []
for apk in sorted(root.glob('*/*.apk')):
    def run(tool, *args):
        return subprocess.run([str(build_tools / tool), *args], text=True,
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False).stdout
    badging = run('aapt', 'dump', 'badging', str(apk))
    manifest = run('aapt', 'dump', 'xmltree', str(apk), 'AndroidManifest.xml')
    signature = run('apksigner', 'verify', '--print-certs', str(apk))
    (apk.parent / 'badging.txt').write_text(badging)
    (apk.parent / 'manifest.txt').write_text(manifest)
    (apk.parent / 'signature.txt').write_text(signature)
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']*)'", badging)
    launch = re.search(r"launchable-activity: name='([^']+)'", badging)
    result = {'id': apk.parent.name, 'apk': str(apk.relative_to(root)),
              'package': package[1] if package else None,
              'versionCode': package[2] if package else None,
              'versionName': package[3] if package else None,
              'activity': launch[1] if launch else None,
              'wallpaperAction': 'tv.projectivy.plugin.WALLPAPER_PROVIDER' in manifest,
              'dreamService': 'android.service.dreams.DreamService' in manifest}
    results.append(result)
    print(json.dumps(result), flush=True)
(root / 'inspection.json').write_text(json.dumps(results, indent=2) + '\n')
