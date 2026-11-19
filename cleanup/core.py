"""Strict Homeboard policy around the copied CleanYourTV recovery primitives.

There is deliberately no subprocess, ADB client or arbitrary shell interface here.
The executor must be supplied by an explicitly owned device session. Profiles are reviewed
release resources, never browser-provided input.
"""
from copy import deepcopy
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import time
import uuid

VENDOR = Path(__file__).resolve().parents[1] / 'vendor/cleanyourtv-cleanup'


class Refused(ValueError):
    pass


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':'),
                                     ensure_ascii=True).encode()).hexdigest()


def verify_vendor():
    manifest = json.loads((VENDOR / 'provenance.json').read_text())
    for name, expected in manifest['files'].items():
        path = (VENDOR / name).resolve()
        if not path.is_relative_to(VENDOR.resolve()) or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise Refused('Copied upstream file changed: ' + name)
    return manifest['commit']


# Check copied bytes before importing any upstream executable code.
verify_vendor()
spec = importlib.util.spec_from_file_location('homeboard_upstream_recovery', VENDOR / 'tools/recovery.py')
recovery = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recovery)


def validate_snapshot(snapshot, *, fresh=True, now=None):
    now = time.time() if now is None else now
    if not isinstance(snapshot, dict) or snapshot.get('format') != 1 or snapshot.get('user') != 0:
        raise Refused('A complete user-0 inventory is required')
    device = snapshot.get('device', {})
    for key in ('id', 'manufacturer', 'model', 'product', 'board', 'fingerprint'):
        if not isinstance(device.get(key), str) or not device[key].strip():
            raise Refused('Missing verified device field: ' + key)
    if snapshot.get('identity_verified') is not True:
        raise Refused('Device identity has not been verified')
    age = now - snapshot.get('collected_at', 0)
    if fresh and not -5 <= age <= 300:
        raise Refused('Inspect the TV again; inventory is stale')
    home = snapshot.get('home')
    if not isinstance(home, str) or not recovery.HOME_RE.fullmatch(home) or recovery.not_a_launcher(home) or recovery.is_chooser(home):
        raise Refused('Default Home could not be verified')
    packages = snapshot.get('packages')
    if not isinstance(packages, dict) or not packages:
        raise Refused('Package inventory is incomplete')
    for name, item in packages.items():
        if not recovery.PACKAGE_RE.fullmatch(name) or not isinstance(item, dict):
            raise Refused('Invalid package inventory')
        if type(item.get('enabled')) is not int or item['enabled'] not in range(5):
            raise Refused('Exact enabled-setting value missing for ' + name)
        if type(item.get('uid')) is not int or item['uid'] < 0 or not str(item.get('version', '')):
            raise Refused('Package identity missing for ' + name)
    if home.split('/')[0] not in packages:
        raise Refused('Default Home is missing from inventory')
    return snapshot


def state_digest(snapshot):
    return digest({key: snapshot[key] for key in ('device', 'user', 'home', 'packages')})


def protected_reason(name, item, snapshot):
    if recovery.core_packages([name]):
        return 'Protected by CleanYourTV core-package guards'
    # V1 is intentionally narrower than upstream's general-purpose lists.
    if name.startswith(('android.', 'com.android.', 'com.google.', 'net.biohack.homeboard', 'com.gathin.tvhome')) or name == 'android':
        return 'Android, Google services or Homeboard are kept in this release'
    if name == snapshot['home'].split('/')[0] or item.get('roles'):
        return 'Provides Home or another retained TV function'
    if item['uid'] < 10000:
        return 'Privileged Android UID is protected'
    if re.search(r'launcher|\.home(?:\.|$)|cast|airplay|miracast|setup|remote|input|tuner|keyboard|bluetooth|wifi|network|updat|voice|speech|accessib|dream|wallpaper|factory|testmenu', name, re.I):
        return 'May provide a retained TV or recovery function'
    if item.get('dependencies_verified') is not True:
        return 'Dependencies have not been reviewed'
    if sum(p['uid'] == item['uid'] for p in snapshot['packages'].values()) != 1:
        return 'Shared package UID; dependency is not isolated'
    if item['enabled'] not in (0, 1):
        return 'Already disabled; preserve its current state'
    return ''


def review(snapshot, profiles, *, now=None):
    validate_snapshot(snapshot, now=now)
    matches = [p for p in profiles if p.get('approved') is True and p.get('match') == snapshot['device']]
    if len(matches) != 1:
        return {'status': 'scan-only', 'reason': 'No exact reviewed profile for this device and firmware',
                'device': snapshot['device'], 'candidates': [], 'kept': [], 'apply_available': False}
    profile = matches[0]
    if not profile.get('revision') or not profile.get('source_commit'):
        raise Refused('Profile provenance is missing')
    candidates, kept = [], []
    for item in profile.get('packages', []):
        name = item.get('package', '')
        if not recovery.PACKAGE_RE.fullmatch(name) or not item.get('reason') or not item.get('label'):
            raise Refused('Reviewed package entry is incomplete')
        if name not in snapshot['packages']:
            kept.append({'package': name, 'reason': 'Not installed for user 0'})
            continue
        blocked = protected_reason(name, snapshot['packages'][name], snapshot)
        if blocked:
            kept.append({'package': name, 'reason': blocked})
        else:
            candidates.append({**item, 'selected': False})
    if len({p['package'] for p in candidates}) != len(candidates):
        raise Refused('Duplicate package in profile')
    return {'status': 'review', 'reason': '' if candidates else 'Nothing recommended for this TV',
            'device': snapshot['device'], 'profile_revision': profile['revision'],
            'profile_digest': digest(profile), 'state_digest': state_digest(snapshot),
            'created_at': time.time() if now is None else now, 'candidates': candidates, 'kept': kept,
            'apply_available': False}


def selection(snapshot, profiles, chosen, *, now=None):
    result = review(snapshot, profiles, now=now)
    allowed = {p['package'] for p in result['candidates']}
    if not isinstance(chosen, list) or not 1 <= len(chosen) <= 8 or len(set(chosen)) != len(chosen) or not set(chosen) <= allowed:
        raise Refused('Choose 1–8 reviewed, eligible packages')
    plan = {key: result[key] for key in ('device', 'profile_revision', 'profile_digest', 'state_digest', 'created_at')}
    plan['selected'] = list(chosen)
    plan['confirmation'] = digest(plan)
    return plan


def private_directory(path):
    path = Path(path).absolute()
    if path.is_symlink():
        raise Refused('Recovery directory must not be a symlink')
    path.mkdir(parents=True, exist_ok=True, mode=0o700)
    if path.stat().st_uid != os.getuid() or path.stat().st_mode & 0o077:
        raise Refused('Recovery directory must be private to its owner')
    return path


def write_durable(path, record):
    # Reuse upstream's atomic replacement, file flush and fsync, adding directory
    # durability and private modes. No fallback to upstream's command generator.
    if path.is_symlink() or path.with_name(path.name + '.tmp').is_symlink():
        raise Refused('Recovery record must not be a symlink')
    recovery.write_record(path, record)
    os.chmod(path, 0o600)
    handle = os.open(path.parent, os.O_RDONLY | os.O_DIRECTORY)
    try:
        os.fsync(handle)
    finally:
        os.close(handle)


class Session:
    """An exact-state journal; transport is injected by an explicitly owned device session."""
    def __init__(self, primary, mirror, session_id=None):
        self.primary, self.mirror = private_directory(primary), private_directory(mirror)
        if self.primary.resolve() == self.mirror.resolve():
            raise Refused('Two distinct recovery locations are required')
        self.id = str(uuid.UUID(session_id)) if session_id else str(uuid.uuid4())
        self.record = None

    def save(self):
        for folder in (self.primary, self.mirror):
            write_durable(folder / (self.id + '.json'), self.record)
        for folder in (self.primary, self.mirror):
            if json.loads((folder / (self.id + '.json')).read_text()) != self.record:
                raise Refused('Recovery copy verification failed')

    def load(self):
        records = [json.loads((folder / (self.id + '.json')).read_text()) for folder in (self.primary, self.mirror)]
        if records[0] != records[1] or records[0].get('format') != 1 or records[0].get('session') != self.id:
            raise Refused('Recovery copies differ or are invalid; reconcile before continuing')
        self.record = records[0]
        baseline = self.record.get('baseline')
        validate_snapshot(baseline, fresh=False)
        plan = self.record.get('plan', {})
        if (plan.get('state_digest') != state_digest(baseline)
                or plan.get('device') != baseline['device']
                or plan.get('confirmation') != digest({k: v for k, v in plan.items() if k != 'confirmation'})
                or set(plan.get('selected', [])) != set(self.record.get('items', {}))):
            raise Refused('Recovery plan does not match the recorded baseline')
        for name, item in self.record['items'].items():
            if (name not in baseline['packages'] or type(item.get('before')) is not int
                    or item['before'] not in (0, 1)
                    or item['before'] != baseline['packages'][name]['enabled']
                    or item.get('phase') not in ('pending', 'intent', 'changed', 'restore-intent', 'restored')):
                raise Refused('Invalid recovery item; no commands will be sent')
        return self.record

    def prepare(self, snapshot, plan):
        if self.record is not None or any((f / (self.id + '.json')).exists() for f in (self.primary,self.mirror)):
            raise Refused('Session already exists')
        validate_snapshot(snapshot)
        if state_digest(snapshot)!=plan['state_digest']:
            raise Refused('Inventory changed before recovery preparation')
        self.record={'format':1,'session':self.id,'plan':deepcopy(plan),'baseline':deepcopy(snapshot),
                     'status':'prepared','items':{n:{'before':snapshot['packages'][n]['enabled'],'phase':'pending'} for n in plan['selected']}}
        self.save()
        return self.record

    def apply(self, executor, profiles, plan, confirmation, *, now=None, stop=lambda: False):
        existing=self.record is not None or any((f / (self.id + '.json')).exists() for f in (self.primary,self.mirror))
        if existing:
            record=self.load()
            if record['status']!='prepared' or record['plan']!=plan or any(i['phase']!='pending' for i in record['items'].values()):
                raise Refused('Session already exists; inspect its result instead of replaying Apply')
        now = time.time() if now is None else now
        current = executor.inspect()
        expected = selection(current, profiles, plan['selected'], now=now)
        expected['created_at'] = plan['created_at']
        expected['confirmation'] = digest({k: v for k, v in expected.items() if k != 'confirmation'})
        if not 0 <= now - plan['created_at'] <= 120 or expected != plan or confirmation != plan['confirmation']:
            raise Refused('Review changed or expired; inspect and confirm again')
        if not existing:self.prepare(current,plan)
        else:self.save()  # Reverify both copies before the first command.
        for name, item in self.record['items'].items():
            if stop():
                self.record['status'] = 'stopped'
                self.save()
                return self.record
            fresh = executor.inspect()
            validate_snapshot(fresh)
            # Re-run all protection checks; changes outside this session invalidate
            # assumptions. Only our verified prior effects are normalized below.
            adjusted = deepcopy(fresh)
            for previous, info in self.record['items'].items():
                if info['phase'] == 'changed':
                    if fresh['packages'][previous]['enabled'] != 3:
                        raise Refused('An applied state changed externally; stop and reconcile')
                    adjusted['packages'][previous]['enabled'] = info['before']
            if state_digest(adjusted) != plan['state_digest']:
                self.record['status'] = 'conflict'
                self.save()
                raise Refused('TV state changed; no further package will be changed')
            item['phase'] = 'intent'
            self.record['status'] = 'applying'
            self.save()
            try:
                executor.set_enabled(name, 3)
                after = executor.inspect()
                validate_snapshot(after)
                expected_after = deepcopy(fresh)
                expected_after['packages'][name]['enabled'] = 3
                if state_digest(after) != state_digest(expected_after):
                    raise Refused('Package change or retained TV state was not verified')
            except Exception:
                self.record['status'] = 'uncertain'
                self.save()
                raise
            item['phase'] = 'changed'
            self.save()
        self.record['status'] = 'applied'
        self.save()
        return self.record

    def restore(self, executor, chosen=None):
        record = self.load()
        current = executor.inspect()
        validate_snapshot(current)
        baseline = record['baseline']
        if current['device'] != baseline['device'] or current['home'] != baseline['home']:
            raise Refused('Device, firmware or Home changed; reconcile before restoration')
        chosen = list(record['items']) if chosen is None else chosen
        if not chosen or len(set(chosen)) != len(chosen) or not set(chosen) <= set(record['items']):
            raise Refused('Restore only recorded session changes')
        for name in chosen:
            item = record['items'][name]
            if item['phase'] == 'pending':
                continue
            fresh = executor.inspect()
            validate_snapshot(fresh)
            if fresh['device'] != baseline['device'] or fresh['home'] != baseline['home']:
                raise Refused('Device or Home changed before restoration')
            original = baseline['packages'][name]
            actual = fresh['packages'].get(name)
            if not actual or any(actual.get(k) != original.get(k) for k in ('version', 'uid')):
                raise Refused('Package identity changed; no blind restoration')
            if actual['enabled'] == item['before']:
                item['phase'] = 'restored'
                self.save()
                continue
            if actual['enabled'] != 3:
                raise Refused('Package state changed externally; inspect before restoring')
            item['phase'] = 'restore-intent'
            self.save()
            executor.set_enabled(name, item['before'])
            after = executor.inspect()
            if after['device'] != baseline['device'] or after['home'] != baseline['home'] or after['packages'][name]['enabled'] != item['before']:
                raise Refused('Restoration remains unverified')
            item['phase'] = 'restored'
            self.save()
        record['status'] = 'restored' if all(i['phase'] in ('pending', 'restored') for i in record['items'].values()) else 'partially-restored'
        self.save()
        return record
