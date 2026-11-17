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
    parser.add_argument('--device', required=True, help='Comma-separated coordinator devices; one frozen APK for all')
    parser.add_argument('--recipe', required=True)
    args = parser.parse_args()
    devices = list(dict.fromkeys(device.strip() for device in args.device.split(',') if device.strip()))
    if not devices:
        parser.error('At least one device is required')
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
    record = {'versionName': name, 'versionCode': code, 'devices': devices, 'targets': {},
              'apk': str(apk), 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
              'base_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip(),
              'local_changes': subprocess.check_output(['git', 'status', '--short'], cwd=root, text=True).splitlines()}
    source_files = [path for path in (root / 'launcher').rglob('*') if path.is_file()
                    and 'build' not in path.relative_to(root).parts and '.gradle' not in path.relative_to(root).parts]
    (destination / 'source-sha256.json').write_text(json.dumps({str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest()
                                                             for path in sorted(source_files)}, indent=2) + '\n')

    def save_record():
        (destination / 'deployment.json').write_text(json.dumps(record, indent=2) + '\n')

    save_record()
    task('app-register', 'APP=homeboard-tv', f'APK={apk}',
         'ACTIVITY=com.gathin.tvhome.MainActivity', 'REPLACE=true')
    # Submit all targets before following any job: a held device must not prevent
    # the other device receiving its requested installation.
    for device in devices:
        result = task('submit', f'DEVICE={device}', f'APK={apk}',
                      f'RECIPE={destination / "recipe.json"}', capture=True)
        print(result.stdout, end='', flush=True)
        match = re.search(r'Accepted (J-[0-9a-f]+):', result.stdout)
        if not match:
            raise RuntimeError('Coordinator did not return a job ID; inspect submission before retrying')
        record['targets'][device] = {'job': match.group(1)}
        save_record()
    failed = []
    for device, target in record['targets'].items():
        job = target['job']
        evidence = destination / device / 'evidence'
        try:
            task('watch', f'JOB={job}')
        except subprocess.CalledProcessError:
            # A failed job makes watch exit nonzero. Its receipt still needs to
            # be downloaded, and the other target still needs to be followed.
            pass
        task('evidence', f'JOB={job}', f'OUT={evidence}')
        receipt = json.loads((evidence / 'receipt.json').read_text())
        target.update(status=receipt.get('status'), health=receipt.get('health'), evidence=str(evidence))
        save_record()
        if target['status'] != 'succeeded':
            failed.append(device)
        print(f'Deployment {name} on {device}: {target["status"]}; {job}\nEvidence: {evidence}', flush=True)
    if failed:
        raise RuntimeError(f'Installation unconfirmed on {", ".join(failed)}; inspect exported evidence')


if __name__ == '__main__':
    main()
