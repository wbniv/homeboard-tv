from http.client import HTTPConnection
import json
from pathlib import Path
import tempfile
import threading
import time
import unittest

from cleanup.coordinator import read_evidence
from cleanup.core import Refused
from cleanup.server import Companion


class FakeInspector:
    def __init__(self):
        self.calls = []
    def devices(self):
        return [{'id': 'fixture-tv', 'model': 'Example TV', 'status': 'ready'}]
    def scan(self, device, progress):
        self.calls.append(device)
        return {'status': 'scan-only', 'apply_available': False, 'device_id': device,
                'device': {'model': 'Example TV'}, 'job': 'J-fixture', 'package_count': 3}


class CompanionTest(unittest.TestCase):
    def setUp(self):
        self.inspector = FakeInspector()
        self.server = Companion(self.inspector)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.addCleanup(self.close)
    def close(self):
        self.server.shutdown(); self.server.server_close(); self.thread.join(3)
    def request(self, path, body=None, headers=None):
        conn = HTTPConnection('127.0.0.1', self.server.server_port, timeout=5)
        self.addCleanup(conn.close)
        h = {'Content-Type': 'application/json', 'Origin': self.server.origin,
             'X-Homeboard-Token': self.server.token}
        h.update(headers or {})
        conn.request('GET' if body is None else 'POST', path, None if body is None else json.dumps(body), h)
        response = conn.getresponse()
        return response.status, response.read(), dict(response.getheaders())
    def test_same_origin_authenticated_inspection(self):
        code, content, headers = self.request('/')
        self.assertEqual(code, 200)
        self.assertIn('no-store', headers['Cache-Control'])
        self.assertEqual(self.request('/api/devices', {})[0], 200)
        self.assertEqual(self.request('/api/scan', {'device': 'fixture-tv'})[0], 202)
        for _ in range(100):
            state = json.loads(self.request('/api/state')[1])
            if state['status'] != 'inspecting': break
            time.sleep(.01)
        self.assertEqual(state['status'], 'scan-only')
        self.assertFalse(state['apply_available'])
        self.assertEqual(self.inspector.calls, ['fixture-tv'])
    def test_standalone_routes_require_confirmation_and_authorization(self):
        self.inspector.standalone = True
        self.inspector.pair = lambda address, code: {'message':'paired'}
        self.assertEqual(self.request('/api/config')[0], 200)
        self.assertEqual(self.request('/api/pair', {'address':'192.168.1.9:1234','code':'123456'}, {'Origin':'https://evil.example'})[0],403)
        self.assertEqual(self.request('/api/pair', {'address':'192.168.1.9:1234','code':'123456'})[0],200)
        self.assertEqual(self.request('/api/scan', {'device':'fixture-tv'})[0],400)
        self.assertEqual(self.request('/api/scan', {'device':'fixture-tv','confirmed':True})[0],202)
        self.server.lock.acquire()
        try:self.assertEqual(self.request('/api/pair', {})[0],409)
        finally:self.server.lock.release()

    def test_workflow_http_apply_restore_and_export_are_authenticated(self):
        from cleanup.workflow import Workflow
        from cleanup.tests.test_core import TV
        with tempfile.TemporaryDirectory() as root:
            tv=TV();profile={'approved':True,'revision':'fixture','source_commit':'fixture',
                'match':tv.state['device'],'packages':[{'package':'org.example.promotions','label':'Fixture','reason':'Test only'}]}
            self.server.workflow=Workflow(None,Path(root),[profile],lambda device:tv)
            self.assertEqual(self.request('/api/cleanup/prepare',{'device':'tv','chosen':['org.example.promotions']})[0],400)
            code,body,_=self.request('/api/cleanup/prepare',{'device':'tv','confirmed':True,'chosen':['org.example.promotions']})
            self.assertEqual(code,200);plan=json.loads(body)
            self.assertEqual(self.request('/api/cleanup/apply',plan,{'X-Homeboard-Token':''})[0],403)
            self.assertEqual(tv.calls,[])
            self.assertEqual(self.request('/api/cleanup/apply',plan)[0],202)
            for _ in range(100):
                state=json.loads(self.request('/api/cleanup/status',{})[1])
                if state['status']!='working':break
                time.sleep(.01)
            self.assertEqual(state['record']['status'],'applied');key=state['session']
            code,body,_=self.request('/api/cleanup/export',{'session':key})
            self.assertEqual(code,200);self.assertEqual(json.loads(body)['record']['session'],key)
            self.assertEqual(self.request('/api/cleanup/restore',{'device':'tv','confirmed':True,'session':key})[0],202)
            for _ in range(100):
                state=json.loads(self.request('/api/cleanup/status',{})[1])
                if state['status']!='working':break
                time.sleep(.01)
            self.assertEqual(state['record']['status'],'restored')

    def test_no_mutation_routes(self):
        for route in ('apply', 'shell', 'restore', 'enable', 'disable', 'pair', 'connect', 'disconnect'):
            self.assertEqual(self.request('/api/' + route, {})[0], 404)
        self.assertEqual(self.inspector.calls, [])
    def test_csrf_dns_rebinding_and_missing_token_are_refused(self):
        for headers in ({'Origin': 'https://untrusted.example'}, {'Host': 'untrusted.example'},
                        {'X-Homeboard-Token': ''}):
            self.assertEqual(self.request('/api/scan', {}, headers)[0], 403)
        self.assertEqual(self.request('/api/state', headers={'X-Homeboard-Token': ''})[0], 404)
    def test_wrong_or_failed_evidence_cannot_become_a_valid_scan(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp); (p / 'receipt.json').write_text(json.dumps({'job': 'J-test', 'device': 'other', 'status': 'succeeded'}))
            with self.assertRaises(Refused): read_evidence(p, 'fixture-tv', 'J-test')
            (p / 'receipt.json').write_text(json.dumps({'job': 'J-test', 'device': 'fixture-tv', 'status': 'failed'}))
            with self.assertRaises(Refused): read_evidence(p, 'fixture-tv', 'J-test')
    def test_hardware_only_scan_is_never_claimed_mutation_ready(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp); (p / 'receipt.json').write_text(json.dumps({'job': 'J-test', 'device': 'fixture-tv', 'status': 'succeeded'}))
            (p / 'hardware-software.json').write_text(json.dumps({'device': 'fixture-tv','properties': {},'normalized': {'installed_packages': []}}))
            result = read_evidence(p, 'fixture-tv', 'J-test')
            self.assertFalse(result['apply_available'])
            self.assertEqual(result['status'], 'scan-only')



class CompletionLockTest(unittest.TestCase):
    def test_client_can_start_next_request_when_completion_is_sent(self):
        """A response must not advertise completion while the next request is refused."""
        from types import SimpleNamespace
        from cleanup.server import Handler
        server = SimpleNamespace(lock=threading.Lock(), workflow=SimpleNamespace(
            export=lambda session: {'session': session}, sessions=lambda: []))
        export, followup = object.__new__(Handler), object.__new__(Handler)
        export.server = followup.server = server
        export.path, followup.path = '/api/cleanup/export', '/api/cleanup/sessions'
        responses = []
        followup.respond = lambda code, body: responses.append(('followup', code))
        def received(code, body):
            responses.append(('export', code))
            # Model a client sending its next request at the first completion byte.
            followup.cleanup_action({})
        export.respond = received
        export.cleanup_action({'session': 'fixture'})
        self.assertEqual(responses, [('export', 200), ('followup', 200)])
        self.assertFalse(server.lock.locked())


if __name__ == '__main__': unittest.main()
