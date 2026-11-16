#!/usr/bin/env python3
"""Freeze a numbered build and run one complete coordinator-owned deployment."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--device', required=True)
    parser.add_argument('--recipe', required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    recipe = (root / args.recipe).resolve()
    if not recipe.is_file():
        parser.error(f'Missing recipe: {recipe}')
    properties = dict(line.split('=', 1) for line in (root / 'version.properties').read_text().splitlines() if line)
    code = int(properties['version_code'])
    name = f"{properties['major']}.{properties['minor']}.{code}"
    output = root / 'launcher/app/build/outputs/apk/debug'
    metadata = json.loads((output / 'output-metadata.json').read_text())
    artifact, = metadata['elements']
    if artifact['versionCode'] != code or artifact['versionName'] != name:
        parser.error('APK metadata does not match version.properties; rebuild before deploying')
    destination = root / 'artifacts/deployments' / name
    if destination.exists():
        parser.error(f'{name} already has a deployment artifact; use task deploy to bump again')

    def task(command, *variables, capture=False):
        # Task includes export WF_CC_* values. A nested Task otherwise inherits empty
        # APP/APK/JOB values ahead of the child adapter's explicitly supplied arguments.
        env = os.environ.copy()
        for parameter in ('REPLACE RECIPE DEVICE POOL POOL_MODE SELECTOR JOB BATCH OUT '
                          'APP APK ACTIVITY WORKFLOW VALIDATOR DURATION DRY_RUN TEXT '
                          'RETRY SCENE REINSTALL APK_ARGS').split():
            env.pop('WF_CC_' + parameter, None)
        return subprocess.run(['task', f'chromecast:{command}', *variables], cwd=root,
                              env=env, check=True, text=True,
                              stdout=subprocess.PIPE if capture else None)

    task('devices')
    task('queue')
    destination.mkdir(parents=True)
    apk = destination / 'homeboard.apk'
    shutil.copyfile(output / artifact['outputFile'], apk)
    shutil.copyfile(recipe, destination / 'recipe.json')
    record = {'versionName': name, 'versionCode': code, 'device': args.device,
              'apk': str(apk), 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
              'base_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()}

    def save_record():
        (destination / 'deployment.json').write_text(json.dumps(record, indent=2) + '\n')

    save_record()
    task('app-register', 'APP=homeboard-tv', f'APK={apk}',
         'ACTIVITY=com.gathin.tvhome.MainActivity', 'REPLACE=true')
    result = task('submit', f'DEVICE={args.device}', f'APK={apk}',
                  f'RECIPE={destination / "recipe.json"}', capture=True)
    print(result.stdout, end='', flush=True)
    match = re.search(r'Accepted (J-[0-9a-f]+):', result.stdout)
    if not match:
        raise RuntimeError('Coordinator did not return a job ID; inspect submission before retrying')
    record['job'] = job = match.group(1)
    save_record()
    task('watch', f'JOB={job}')
    task('evidence', f'JOB={job}', f'OUT={destination / "evidence"}')
    receipt = json.loads((destination / 'evidence/receipt.json').read_text())
    record['status'] = receipt.get('status')
    record['health'] = receipt.get('health')
    save_record()
    if record['status'] != 'succeeded':
        raise RuntimeError(f'Coordinator deployment ended {record["status"]}; inspect exported evidence')
    print(f'Deployment {name}: {job}\nEvidence: {destination / "evidence"}', flush=True)


if __name__ == '__main__':
    main()
