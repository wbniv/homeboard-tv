"""Controlled HTTP fixture for browser acceptance; never contacts a device."""
import os
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from cleanup.server import Companion


class Fixture:
    standalone = os.environ.get('STANDALONE_FIXTURE') == '1'
    connected = False
    def discover(self): return [{'kind':'pair','address':'192.168.1.9:22222'}]
    def pair(self,address,code): return {'message':'paired'}
    def connect(self,address): self.connected=True; return {'device':{'id':'new-tv-fixture'}}
    def disconnect(self,device): self.connected=False; return {'message':'disconnected'}
    def devices(self):
        if self.standalone and not self.connected:return []
        return [{'identity':'FIXTURE123','id': 'new-tv-fixture', 'model': '<b>Family TV</b>', 'status': 'ready'}]
    def scan(self, device, progress):
        return {'status': 'scan-only', 'apply_available': False, 'device_id': device,
                'device': {'manufacturer': 'Example', 'model': '<b>Family TV</b>', 'product': 'fixture',
                           'board': 'fixture', 'android': '14', 'firmware': 'example/new-tv/fixture'},
                'job': 'J-browser-fixture', 'package_count': 187, 'collected_at': '2026-11-18T12:00:00Z'}


with Companion(Fixture()) as server:
    print(server.origin, flush=True)
    if os.environ.get('CLEANUP_WORKFLOW_FIXTURE') == '1':
        import tempfile
        from cleanup.workflow import Workflow
        from cleanup.tests.test_core import TV
        fixture_tv=TV()
        profile={'approved':True,'revision':'fixture','source_commit':'fixture',
                 'match':fixture_tv.state['device'], 'packages':[{'package':'org.example.promotions','label':'Fixture promotions','reason':'Disposable fixture only'}]}
        directory=tempfile.TemporaryDirectory()
        server.workflow=Workflow(None,Path(directory.name),[profile],lambda device:fixture_tv)
    server.serve_forever()
