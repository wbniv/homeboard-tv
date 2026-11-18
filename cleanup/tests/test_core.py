"""Behavioral cleanup/recovery tests on controlled fake TVs; no device transport."""
from copy import deepcopy
import json
from pathlib import Path
import tempfile
import time
import unittest
from unittest.mock import patch

from cleanup.core import Refused, Session, digest, protected_reason, review, selection, verify_vendor


class TV:
    def __init__(self):
        self.state = {'format': 1, 'user': 0, 'identity_verified': True,
            'device': {'id': 'fixture-tv', 'manufacturer': 'Example', 'model': 'Fixture',
                       'product': 'fixture', 'board': 'fixture', 'fingerprint': 'example/fixture/1'},
            'home': 'net.biohack.homeboard.debug/com.gathin.tvhome.MainActivity',
            'packages': {
                'net.biohack.homeboard.debug': {'enabled': 0, 'uid': 10001, 'version': '62', 'roles': ['home'], 'dependencies_verified': True},
                'org.example.promotions': {'enabled': 0, 'uid': 10002, 'version': '1', 'roles': [], 'dependencies_verified': True},
                'org.example.suggestions': {'enabled': 1, 'uid': 10003, 'version': '1', 'roles': [], 'dependencies_verified': True},
                'org.example.alreadyoff': {'enabled': 3, 'uid': 10004, 'version': '1', 'roles': [], 'dependencies_verified': True}}}
        self.calls = []
        self.fail = None

    def inspect(self):
        result = deepcopy(self.state)
        result['collected_at'] = time.time()
        return result

    def set_enabled(self, name, state):
        self.calls.append((name, state))
        if self.fail == 'before':
            raise ConnectionError('Lost before effect')
        self.state['packages'][name]['enabled'] = state
        if self.fail == 'after':
            raise ConnectionError('Lost after effect, before acknowledgement')


class CleanupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.tv = TV()
        self.profile = {'approved': True, 'revision': 'fixture-1', 'source_commit': 'fixture-only',
            'match': deepcopy(self.tv.state['device']),
            'packages': [{'package': p, 'label': 'Fixture app', 'reason': 'Controlled test only'} for p in
                         ['org.example.promotions', 'org.example.suggestions', 'org.example.alreadyoff']]}
        self.profiles = [self.profile]

    def session(self):
        return Session(self.root / 'primary', self.root / 'recovery-copy')

    def plan(self, names=None):
        return selection(self.tv.inspect(), self.profiles, names or ['org.example.promotions', 'org.example.suggestions'])

    def test_unknown_and_ambiguous_profiles_are_read_only(self):
        for profiles in ([], [self.profile, self.profile]):
            result = review(self.tv.inspect(), profiles)
            self.assertEqual(result['status'], 'scan-only')
            self.assertFalse(result['apply_available'])
            self.assertEqual(result['candidates'], [])
        self.assertEqual(self.tv.calls, [])

    def test_every_candidate_starts_unselected_and_existing_disabled_is_kept(self):
        r = review(self.tv.inspect(), self.profiles)
        self.assertTrue(all(not p['selected'] for p in r['candidates']))
        self.assertEqual([p['package'] for p in r['kept']], ['org.example.alreadyoff'])
        with self.assertRaises(Refused):
            self.plan(['org.example.alreadyoff'])

    def test_copied_guards_and_stricter_retained_services(self):
        s = self.tv.inspect()
        for name in ['android', 'android.ext.services', 'com.cltv.mal', 'com.android.location.fused',
                     'com.google.android.apps.tv.launcherx', 'com.vendor.cast', 'com.vendor.updater',
                     'com.vendor.keyboard', 'com.vendor.tvinput', 'net.biohack.homeboard.debug']:
            self.assertTrue(protected_reason(name, s['packages']['org.example.promotions'], s), name)
        self.tv.state['packages']['org.example.promotions']['uid'] = 10003
        self.assertIn('Shared', protected_reason('org.example.promotions', self.tv.state['packages']['org.example.promotions'], self.tv.inspect()))

    def test_apply_and_individual_then_complete_restore_preserve_exact_states(self):
        before = deepcopy(self.tv.state)
        s = self.session(); p = self.plan()
        self.assertEqual(s.apply(self.tv, self.profiles, p, p['confirmation'])['status'], 'applied')
        self.assertEqual(self.tv.state['packages']['org.example.promotions']['enabled'], 3)
        self.assertEqual(s.restore(self.tv, ['org.example.promotions'])['status'], 'partially-restored')
        self.assertEqual(s.restore(self.tv)['status'], 'restored')
        self.assertEqual(self.tv.state, before)
        self.assertNotIn(('org.example.alreadyoff', 1), self.tv.calls)

    def test_no_mutation_when_either_backup_fails(self):
        s = self.session(); p = self.plan()
        from cleanup import core
        original = core.write_durable
        def fail_mirror(path, record):
            if path.parent == s.mirror:
                raise OSError('Recovery disk full')
            return original(path, record)
        with patch.object(core, 'write_durable', fail_mirror), self.assertRaises(OSError):
            s.apply(self.tv, self.profiles, p, p['confirmation'])
        self.assertEqual(self.tv.calls, [])

    def test_confirmation_expiry_and_changed_inventory(self):
        for change in ('confirmation', 'expiry', 'inventory', 'firmware'):
            with self.subTest(change=change):
                self.tv = TV(); p = self.plan(); s = self.session()
                confirmation = p['confirmation']
                now = None
                if change == 'confirmation': confirmation = 'wrong'
                if change == 'expiry': now = time.time() + 121
                if change == 'inventory': self.tv.state['packages']['org.example.promotions']['version'] = '2'
                if change == 'firmware': self.tv.state['device']['fingerprint'] = 'changed'
                with self.assertRaises(Refused): s.apply(self.tv, self.profiles, p, confirmation, now=now)
                self.assertEqual(self.tv.calls, [])

    def test_command_unknown_outcome_restores_after_process_restart(self):
        before = deepcopy(self.tv.state)
        s = self.session(); p = self.plan()
        self.tv.fail = 'after'
        with self.assertRaises(ConnectionError): s.apply(self.tv, self.profiles, p, p['confirmation'])
        self.assertEqual(s.record['status'], 'uncertain')
        self.assertEqual(len(self.tv.calls), 1)
        self.tv.fail = None
        restarted = Session(s.primary, s.mirror, s.id)
        self.assertEqual(restarted.restore(self.tv)['status'], 'restored')
        self.assertEqual(self.tv.state, before)
        with self.assertRaises(Refused): restarted.apply(self.tv, self.profiles, p, p['confirmation'])

    def test_failure_before_effect_does_not_enable_or_disable_unattempted_apps(self):
        s = self.session(); p = self.plan(); self.tv.fail = 'before'
        with self.assertRaises(ConnectionError): s.apply(self.tv, self.profiles, p, p['confirmation'])
        self.tv.fail = None
        s.restore(self.tv)
        self.assertEqual(len(self.tv.calls), 1)

    def test_cancellation_stops_before_next_package(self):
        s = self.session(); p = self.plan()
        s.apply(self.tv, self.profiles, p, p['confirmation'], stop=lambda: len(self.tv.calls) == 1)
        self.assertEqual(s.record['status'], 'stopped')
        self.assertEqual(self.tv.calls, [('org.example.promotions', 3)])
        s.restore(self.tv)

    def test_no_missing_record_or_blanket_enable_fallback(self):
        with self.assertRaises(FileNotFoundError): self.session().restore(self.tv)
        self.assertEqual(self.tv.calls, [])

    def test_corrupt_or_mismatched_record_refuses(self):
        s = self.session(); p = self.plan(); s.apply(self.tv, self.profiles, p, p['confirmation'])
        path = s.mirror / (s.id + '.json'); path.write_text('{}')
        count = len(self.tv.calls)
        with self.assertRaises(Refused): s.restore(self.tv)
        self.assertEqual(len(self.tv.calls), count)

    def test_external_home_package_or_enabled_state_changes_refuse_restore(self):
        for target in ('home', 'version', 'enabled'):
            with self.subTest(target=target):
                self.tv = TV(); s = self.session(); p = self.plan(); s.apply(self.tv, self.profiles, p, p['confirmation'])
                count = len(self.tv.calls)
                if target == 'home': self.tv.state['home'] = 'com.vendor.home/.Home'
                else: self.tv.state['packages']['org.example.promotions'][target] = 'changed' if target == 'version' else 2
                with self.assertRaises(Refused): s.restore(self.tv)
                self.assertEqual(len(self.tv.calls), count)

    def test_source_files_match_pinned_hashes(self):
        self.assertEqual(verify_vendor(), '3ece7766ad5d6af79a2c4dd73e77b4b9623dfeca')


if __name__ == '__main__':
    unittest.main()
