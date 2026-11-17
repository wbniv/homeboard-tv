#!/usr/bin/env python3
"""Offline regression checks for freezing once and admitting every target before waiting."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('homeboard_deploy', Path(__file__).with_name('deploy.py'))
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)


class DeploymentTest(unittest.TestCase):
    def run_deploy(self, failure=False):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        (root / 'dev/device').mkdir(parents=True)
        (root / 'dev/device/install-only.json').write_text('{"workflow":"install"}')
        (root / 'version.properties').write_text('major=0\nminor=1\nversion_code=100\n')
        output = root / 'launcher/app/build/outputs/apk/debug'
        output.mkdir(parents=True)
        (output / 'homeboard.apk').write_bytes(b'one immutable APK')
        (output / 'output-metadata.json').write_text(json.dumps({'elements': [
            {'versionCode': 100, 'versionName': '0.1.100', 'outputFile': 'homeboard.apk'}]}))
        calls = []
        def run(command, **kwargs):
            calls.append(command)
            operation = command[1].split(':')[1]
            values = dict(value.split('=', 1) for value in command[2:])
            if operation == 'submit':
                number = 1 if values['DEVICE'] == 'cast1' else 2
                return subprocess.CompletedProcess(command, 0, stdout=f'Accepted J-{number:012x}: queued\n')
            if operation == 'evidence':
                evidence = Path(values['OUT']); evidence.mkdir(parents=True)
                status = 'failed' if failure and values['JOB'].endswith('1') else 'succeeded'
                (evidence / 'receipt.json').write_text(json.dumps({'status': status, 'health': 'ready'}))
            if operation == 'watch' and failure and values['JOB'].endswith('1'):
                raise subprocess.CalledProcessError(201, command)
            return subprocess.CompletedProcess(command, 0, stdout='')
        def output(command, **kwargs):
            return 'fake-revision\n' if command[1] == 'rev-parse' else ' M launcher/example.java\n'
        with patch.object(deploy, '__file__', str(root / 'dev/deploy.py')), \
             patch.object(sys, 'argv', ['deploy', '--device', 'cast1,cast2', '--recipe', 'dev/device/install-only.json']), \
             patch.object(deploy.subprocess, 'run', side_effect=run), \
             patch.object(deploy.subprocess, 'check_output', side_effect=output), \
             contextlib.redirect_stdout(io.StringIO()):
            if failure:
                with self.assertRaisesRegex(RuntimeError, 'cast1'):
                    deploy.main()
            else:
                deploy.main()
        return root, calls

    def test_both_targets_admitted_before_wait_and_share_the_frozen_apk(self):
        root, calls = self.run_deploy()
        submits = [i for i, call in enumerate(calls) if call[1] == 'chromecast:submit']
        watches = [i for i, call in enumerate(calls) if call[1] == 'chromecast:watch']
        self.assertEqual(2, len(submits))
        self.assertLess(max(submits), min(watches))
        apks = [next(arg for arg in calls[i] if arg.startswith('APK=')) for i in submits]
        self.assertEqual(apks[0], apks[1])
        self.assertEqual(b'one immutable APK', Path(apks[0][4:]).read_bytes())
        record = json.loads((root / 'artifacts/deployments/0.1.100/deployment.json').read_text())
        self.assertEqual({'cast1', 'cast2'}, set(record['targets']))
        self.assertTrue(all(target['status'] == 'succeeded' for target in record['targets'].values()))

    def test_first_target_failure_still_exports_the_second_target(self):
        root, calls = self.run_deploy(failure=True)
        record = json.loads((root / 'artifacts/deployments/0.1.100/deployment.json').read_text())
        self.assertEqual('failed', record['targets']['cast1']['status'])
        self.assertEqual('succeeded', record['targets']['cast2']['status'])
        self.assertEqual(2, sum(call[1] == 'chromecast:evidence' for call in calls))


if __name__ == '__main__':
    unittest.main()
