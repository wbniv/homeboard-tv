"""Controlled HTTP fixture for browser acceptance; never contacts a device."""
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from cleanup.server import Companion


class Fixture:
    def devices(self):
        return [{'id': 'new-tv-fixture', 'model': '<b>Family TV</b>', 'status': 'ready'}]
    def scan(self, device, progress):
        return {'status': 'scan-only', 'apply_available': False, 'device_id': device,
                'device': {'manufacturer': 'Example', 'model': '<b>Family TV</b>', 'product': 'fixture',
                           'board': 'fixture', 'android': '14', 'firmware': 'example/new-tv/fixture'},
                'job': 'J-browser-fixture', 'package_count': 187, 'collected_at': '2026-11-18T12:00:00Z'}


with Companion(Fixture()) as server:
    print(server.origin, flush=True)
    server.serve_forever()
