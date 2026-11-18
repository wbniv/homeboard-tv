#!/usr/bin/env python3
"""Run complete coordinator sessions against frozen artifacts, sequentially."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

BASE = Path(__file__).resolve().parents[1]
ROOT = BASE / 'artifacts/provider-compatibility/2026-11-18'
DEVICE = 'chromecast-test-01'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('mode', choices=['install', 'test', 'controls'])
    parser.add_argument('--ids', default='daily-photos,aerial-views,core-motion,weather,overflight,sofadrop,booru,random,immich,trakt,jellyfin-z9m,tvbgsuite,wallpaparr')
    parser.add_argument('--version', default='0.1.56')
    parser.add_argument('--attempt', default='')
    parser.add_argument('--duration', type=int, default=171)
    args = parser.parse_args()
    inspection = {row['id']: row for row in json.loads((ROOT / 'inspection.json').read_text())}
    original = json.loads((ROOT / 'inventory-before/hardware-software.json').read_text())
    original_packages = {x['package']: x['version_code'] for x in original['normalized']['installed_packages']}
    homeboard = BASE / 'artifacts/deployments' / args.version / 'homeboard.apk'

    def task(command, output, *variables, required=True):
        environment = {k: v for k, v in os.environ.items() if not k.startswith('WF_CC_')}
        result = subprocess.run(['task', 'chromecast:' + command, *variables], cwd=BASE,
                                env=environment, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(result.stdout)
        if required and result.returncode:
            raise RuntimeError(f'{command} failed ({result.returncode}); {output}')
        return result.stdout

    def save(path, value):
        path.write_text(json.dumps(value, indent=2) + '\n')

    for name in args.ids.split(','):
        metadata = inspection.get(name)
        folder = ROOT / name / (args.mode + ('-' + args.attempt if args.attempt else ''))
        folder.mkdir(parents=True, exist_ok=True)
        summary = {'provider': name, 'mode': args.mode, 'device': DEVICE}
        outcome = folder / 'outcome.json'
        disposition = ROOT / name / 'disposition.json'
        if disposition.exists() and json.loads(disposition.read_text()).get('status') == 'excluded':
            save(outcome, dict(summary, status='excluded', reason='Provider excluded after security review; no device job submitted'))
            print(f'{name}: excluded after security review', flush=True)
            continue
        if outcome.exists() and json.loads(outcome.read_text()).get('status') in ('completed', 'already-installed'):
            print(f'{name}: existing completed {args.mode} evidence retained', flush=True)
            continue
        if not metadata:
            save(outcome, dict(summary, status='blocked', reason='No inspected APK')); continue
        apk = ROOT / metadata['apk']
        if args.mode == 'install' and metadata['package'] in original_packages:
            save(outcome, dict(summary, status='already-installed', version_code=original_packages[metadata['package']],
                               reason='Preserved preexisting provider; no update or configuration changes'))
            print(f'{name}: preserving preexisting provider', flush=True); continue
        registered = False
        try:
            task('devices', folder / 'devices.txt')
            task('queue', folder / 'queue.txt')
            if args.mode == 'install':
                if not metadata['activity']:
                    save(outcome, dict(summary, status='blocked', reason='Service-only APK has no activity for coordinator app registration'))
                    print(f'{name}: service-only registration needs coordinator support', flush=True); continue
                app = 'provider-' + name
                task('app-register', folder / 'registration.json', f'APP={app}', f'APK={apk}',
                     'ACTIVITY=' + metadata['activity'], 'REPLACE=true')
                # check installs and briefly opens settings; install adds a separate
                # launcher-artwork continuation irrelevant to wallpaper interoperability.
                submitted = task('submit', folder / 'submit.txt', f'DEVICE={DEVICE}',
                                 f'APP={app}', f'APK={apk}', 'WORKFLOW=check', 'DURATION=3')
            else:
                alias = ('Controls' if args.mode == 'controls' else 'Provider') + ''.join(part.title() for part in name.split('-')) + 'Probe'
                task('app-register', folder / 'registration.json', 'APP=homeboard-tv', f'APK={homeboard}',
                     f'ACTIVITY=com.gathin.tvhome.{alias}', 'REPLACE=true')
                registered = True
                recipe = json.loads((BASE / 'dev/device/provider-compatibility.json').read_text())
                recipe['duration'] = args.duration
                save(folder / 'recipe.json', recipe)
                submitted = task('submit', folder / 'submit.txt', f'DEVICE={DEVICE}', 'APP=homeboard-tv',
                                 f'APK={homeboard}', f'RECIPE={folder / "recipe.json"}')
            match = re.search(r'Accepted (J-[0-9a-f]+):', submitted)
            if not match:
                raise RuntimeError('No accepted job ID; inspect submit evidence before retrying')
            job = match[1]
            summary.update(job=job, status='submitted')
            save(outcome, summary)
            print(f'{name}: {args.mode} {job}', flush=True)
            task('watch', folder / 'watch.txt', f'JOB={job}', required=False)
            evidence = folder / 'evidence'
            task('evidence', folder / 'export.txt', f'JOB={job}', f'OUT={evidence}')
            receipt = json.loads((evidence / 'receipt.json').read_text())
            summary.update(status='completed' if receipt.get('status') == 'succeeded' else 'coordinator-failed',
                           coordinator_status=receipt.get('status'), health=receipt.get('health'))
            if args.mode != 'install':
                try:
                    native = json.loads((evidence / 'wf.log').read_text())
                    save(folder / 'native-report.json', native)
                    summary.update(baseline=native.get('baseline'), reason=native.get('reason'),
                                   compatibility=native.get('compatibility'), household_unchanged=native.get('householdPreferencesUnchanged'))
                except (ValueError, FileNotFoundError):
                    summary.update(baseline='unverified', reason='No parseable native report; inspect screenshot/logcat')
            save(outcome, summary)
            print(json.dumps(summary), flush=True)
        except Exception as error:
            summary.update(status='blocked', reason=str(error)); save(outcome, summary)
            print(json.dumps(summary), flush=True)
        finally:
            if registered:
                task('app-register', folder / 'restored-main-registration.json', 'APP=homeboard-tv',
                     f'APK={homeboard}', 'ACTIVITY=com.gathin.tvhome.MainActivity', 'REPLACE=true')


if __name__ == '__main__':
    main()
