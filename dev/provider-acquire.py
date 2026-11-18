#!/usr/bin/env python3
"""Freeze public upstream provider release APKs; never installs or executes them."""
import concurrent.futures
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1] / 'artifacts/provider-compatibility/2026-11-18'
REPOS = {
    'daily-photos': 'spocky/projectivy-plugin-wallpaper-bingwallpaper',
    'aerial-views': 'theothernt/AerialViews',
    'core-motion': 'brevityA/CoreBuildsApps',
    'weather': 'shortusername/projectivy-weather-wallpaper',
    'overflight': 'spocky/projectivy-plugin-wallpaper-overflight',
    'sofadrop': 'rfmarcos/sofadrop',
    'immich': 'raizer88/immich-projectivy-provider',
    'trakt': 'johnneerdael/traktlistbackdrops',
    'booru': 'Kurocon/projectivy-booru-wallpaper-provider',
    'jellyfin-mikescher': 'Mikescher/projectivity-jellyfin-wallpaper',
    'jellyfin-z9m': 'z9m/ProjectIvy-Plugin-Wallpaper-Jellyfin',
    'jellyfin-jfchenier': 'jfchenier/projectivy-plugin-wallpaper-provider-jellyfin',
    'tvbgsuite': 'z9m/projectivy-tvbgsuite-plugin',
    'wallpaparr': 'iManunator/wallpaparr',
    'random': 'Mars-Wave/projectivy-random-wallpapers-plugin',
    'm3u8': 'Stremiouser666/projectivy-plugin-wallpaper-m3u8wallpaper',
}


def fetch(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'Homeboard-compatibility-audit'})
    with urllib.request.urlopen(request, timeout=90) as response:
        return response.read()


def acquire(entry):
    name, repo = entry
    output = ROOT / name
    output.mkdir(parents=True, exist_ok=True)
    result = {'id': name, 'repository': 'https://github.com/' + repo}
    try:
        metadata = fetch('https://api.github.com/repos/' + repo + '/releases?per_page=10')
        (output / 'releases.json').write_bytes(metadata)
        releases = json.loads(metadata)
        candidates = [(r, a) for r in releases if not r.get('draft') for a in r['assets']
                      if a['name'].endswith('.apk')]
        if name == 'core-motion':
            candidates = [(r, a) for r, a in candidates if 'coremotion' in a['name'].lower()]
        if name == 'wallpaparr':
            candidates = [(r, a) for r, a in candidates if 'plugin' in a['name'].lower()]
        if not candidates:
            result.update(status='acquisition-blocked', reason='No public release APK found')
        else:
            release, asset = candidates[0]
            path = output / asset['name']
            data = fetch(asset['browser_download_url'])
            if not data.startswith(b'PK'):
                raise ValueError('Download is not a ZIP/APK')
            path.write_bytes(data)
            result.update(status='acquired', tag=release['tag_name'], url=asset['browser_download_url'],
                          apk=str(path.relative_to(ROOT)), sha256=hashlib.sha256(data).hexdigest(),
                          bytes=len(data))
    except Exception as error:
        result.update(status='acquisition-blocked', reason=str(error))
    (output / 'acquisition.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result), flush=True)
    return result


if __name__ == '__main__':
    ROOT.mkdir(parents=True, exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(acquire, REPOS.items()))
    (ROOT / 'acquisition.json').write_text(json.dumps(results, indent=2) + '\n')
