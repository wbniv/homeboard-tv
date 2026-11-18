"""Read-only companion adapter: complete coordinator jobs, no direct TV access."""
import json
import os
from pathlib import Path
import re
import subprocess
import uuid

from .core import Refused, private_directory, write_durable

ROOT = Path(__file__).resolve().parents[1]
DEVICE = re.compile(r'[A-Za-z0-9][A-Za-z0-9_-]{0,100}')


class Inspector:
    def __init__(self, data):
        self.data = private_directory(data)

    def task(self, name, *values, timeout=180):
        env = {k: v for k, v in os.environ.items() if not k.startswith('WF_CC_')}
        result = subprocess.run(['task', 'chromecast:' + name, *values], cwd=ROOT,
                                env=env, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, timeout=timeout)
        if result.returncode:
            raise Refused('Coordinator ' + name + ' did not complete; inspect the saved job evidence')
        return result.stdout

    def devices(self):
        output = self.task('devices')
        found = []
        for line in output.splitlines():
            match = re.match(r'^[▭▯▢]\s+([A-Za-z0-9_-]+):\s+([^;]+);\s+([^;]+);', line)
            if match and line.startswith('▭'):
                found.append({'id': match[1], 'status': match[2], 'model': match[3]})
        return found

    def scan(self, device, progress=lambda message: None):
        if not isinstance(device, str) or not DEVICE.fullmatch(device):
            raise Refused('Choose an enrolled TV')
        devices = self.devices()  # Current device and queue checks precede every session.
        if device not in {d['id'] for d in devices}:
            raise Refused('Enroll this TV through the coordinator before inspecting it')
        self.task('queue')
        folder = private_directory(self.data / str(uuid.uuid4()))
        progress('Requesting a read-only inspection')
        submitted = self.task('submit', 'DEVICE=' + device, 'WORKFLOW=inspect')
        match = re.search(r'Accepted (J-[0-9a-f]+):', submitted)
        if not match:
            raise Refused('Coordinator did not return a job identifier; do not submit another blindly')
        job = match[1]
        write_durable(folder / 'job.json', {'device': device, 'job': job})
        progress('Inspecting this TV · ' + job)
        # A failed job still needs its evidence. Watcher timeouts leave the owned
        # job reference on disk and never imply cancellation or successful cleanup.
        watch_error = None
        try:
            self.task('watch', 'JOB=' + job, timeout=1000)
        except (Refused, subprocess.TimeoutExpired) as error:
            watch_error = error
        self.task('evidence', 'JOB=' + job, 'OUT=' + str(folder / 'evidence'))
        result = read_evidence(folder / 'evidence', device, job)
        if watch_error and result.get('status') != 'scan-only':
            raise Refused('Inspection is incomplete; check the retained coordinator job')
        return result


def read_evidence(folder, device, job):
    """Do not promote hardware-only evidence into a mutation-ready inventory."""
    folder = Path(folder)
    receipt = json.loads((folder / 'receipt.json').read_text())
    if receipt.get('device') != device or receipt.get('job') != job or receipt.get('status') != 'succeeded':
        raise Refused('A successful matching coordinator receipt is required')
    hardware = json.loads((folder / 'hardware-software.json').read_text())
    if hardware.get('device') != device:
        raise Refused('Inspection belongs to a different TV')
    properties = hardware.get('properties', {})
    packages = hardware.get('normalized', {}).get('installed_packages', [])
    return {'status': 'scan-only', 'apply_available': False, 'device_id': device, 'job': job,
        'collected_at': hardware.get('collected_at'),
        'device': {key: properties.get(prop) for key, prop in {
            'manufacturer': 'ro.product.manufacturer', 'model': 'ro.product.model',
            'product': 'ro.product.name', 'board': 'ro.product.board',
            'firmware': 'ro.build.fingerprint', 'android': 'ro.build.version.release'}.items()},
        'package_count': len(packages),
        'reason': 'No exact approved cleanup profile. Enabled-state and dependency review are required before any changes.',
        'candidates': [], 'kept': ['Homeboard and all launchers', 'Casting and streaming',
                                  'Voice and updates', 'Network, remote, keyboard and inputs'],
        'foreground_unchanged': hardware.get('foreground_unchanged'),
        'notice': 'Read-only inspection. No package, Home assignment or TV setting was changed.'}
