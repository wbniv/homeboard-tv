#!/usr/bin/env python3
"""Coordinator-owned acceptance of plan docs/plans/2026-11-20-screensavers.md, Verification 7, on a frozen APK:
an alarm that rings during a Homeboard screensaver ends it and opens the timer screen.

    python3 dev/screensaver-alarm-proof.py --version 0.1.104 --device chromecast-test-01

Registers the app with the probe activity, submits dev/device/screensaver-alarm-check.json, watches, exports
evidence under artifacts/screensaver-proof/, and always registers the normal MainActivity again."""
import argparse, json, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROBE = 'com.gathin.tvhome.ScreensaverAlarmProbeActivity'
MAIN = 'com.gathin.tvhome.MainActivity'


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--version', required=True, help='frozen deployment, e.g. 0.1.104')
    parser.add_argument('--device', default='chromecast-test-01')
    args = parser.parse_args()
    apk = ROOT / 'artifacts/deployments' / args.version / 'homeboard.apk'
    if not apk.is_file():
        sys.exit(f'No frozen APK at {apk}')
    folder = ROOT / 'artifacts/screensaver-proof' / args.version / args.device
    folder.mkdir(parents=True, exist_ok=True)

    def task(command, *params, required=True):
        result = subprocess.run(['task', 'chromecast:' + command, *params], cwd=ROOT, text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (folder / (command + '.txt')).write_text(result.stdout)
        if required and result.returncode:
            raise RuntimeError(result.stdout[-1600:])
        return result.stdout

    task('devices'); task('queue')
    try:
        task('app-register', 'APP=homeboard-tv', f'APK={apk}', f'ACTIVITY={PROBE}', 'REPLACE=true')
        output = task('submit', f'DEVICE={args.device}', 'APP=homeboard-tv', f'APK={apk}',
                      f'RECIPE={ROOT / "dev/device/screensaver-alarm-check.json"}')
        job = re.search(r'Accepted (J-[0-9a-f]+):', output)[1]
        print('Screensaver alarm proof:', job, flush=True)
        task('watch', f'JOB={job}', required=False)
        evidence = folder / job / 'evidence'
        task('evidence', f'JOB={job}', f'OUT={evidence}')
        receipt = json.loads((evidence / 'receipt.json').read_text())
        log = evidence / 'wf.log'
        proof = json.loads(log.read_text()) if log.exists() else {'status': 'missing'}
        record = {'job': job, 'version': args.version, 'device': args.device,
                  'coordinator': receipt.get('status'), 'proof': proof, 'evidence': str(evidence)}
        (folder / 'result.json').write_text(json.dumps(record, indent=2) + '\n')
        print(json.dumps(record, indent=2), flush=True)
        if receipt.get('status') != 'succeeded' or proof.get('status') != 'pass':
            raise SystemExit('Screensaver alarm proof did not pass; inspect the evidence')
    finally:
        task('app-register', 'APP=homeboard-tv', f'APK={apk}', f'ACTIVITY={MAIN}', 'REPLACE=true', required=False)


if __name__ == '__main__':
    main()
