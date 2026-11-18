"""Loopback-only cleanup inspection companion. No package mutation HTTP route."""
import argparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import secrets
import threading
import webbrowser

from .coordinator import Inspector
from .core import verify_vendor

WEB = Path(__file__).parent / 'web'


class Companion(ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, inspector, address=('127.0.0.1', 0)):
        super().__init__(address, Handler)
        self.inspector = inspector
        self.token = secrets.token_urlsafe(32)
        self.lock = threading.Lock()
        self.state = {'status': 'idle', 'apply_available': False}
        self.origin = 'http://127.0.0.1:' + str(self.server_port)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # Never log tokens, IPs, inventories or user request bodies.

    def respond(self, code, value, mime='application/json'):
        data = json.dumps(value, ensure_ascii=False).encode() if mime == 'application/json' else value
        self.send_response(code)
        for key, val in {'Content-Type': mime, 'Content-Length': str(len(data)), 'Cache-Control': 'no-store',
                         'X-Frame-Options': 'DENY', 'X-Content-Type-Options': 'nosniff',
                         'Referrer-Policy': 'no-referrer',
                         'Content-Security-Policy': "default-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'"}.items():
            self.send_header(key, val)
        self.end_headers()
        self.wfile.write(data)

    def trusted_host(self):
        return self.headers.get('Host') == '127.0.0.1:' + str(self.server.server_port)

    def do_GET(self):
        if not self.trusted_host():
            self.respond(403, {'error': 'Use the local companion address'})
            return
        if self.path == '/':
            html = (WEB / 'index.html').read_text().replace('__TOKEN__', self.server.token)
            self.respond(200, html.encode(), 'text/html; charset=utf-8')
        elif self.path in ('/style.css', '/app.js'):
            self.respond(200, (WEB / self.path[1:]).read_bytes(), 'text/css' if self.path.endswith('css') else 'text/javascript')
        elif self.path == '/api/state' and self.headers.get('X-Homeboard-Token') == self.server.token:
            self.respond(200, self.server.state)
        else:
            self.respond(404, {'error': 'Not found'})

    def do_POST(self):
        if (not self.trusted_host() or self.headers.get('Origin') != self.server.origin
                or not secrets.compare_digest(self.headers.get('X-Homeboard-Token', ''), self.server.token)):
            self.respond(403, {'error': 'Open the companion directly on this computer'})
            return
        if self.path not in ('/api/devices', '/api/scan'):
            self.respond(404, {'error': 'No such action'})
            return
        try:
            length = int(self.headers.get('Content-Length', '-1'))
            if not 0 <= length <= 2048 or self.headers.get('Content-Type') != 'application/json':
                raise ValueError('Invalid request')
            data = json.loads(self.rfile.read(length))
            if not isinstance(data, dict):
                raise ValueError('Invalid request')
            if self.path == '/api/devices':
                self.respond(200, {'devices': self.server.inspector.devices()})
                return
            if not self.server.lock.acquire(blocking=False):
                self.respond(409, {'error': 'An inspection is already running'})
                return
            self.server.state = {'status': 'inspecting', 'apply_available': False, 'message': 'Preparing read-only inspection'}
            def work():
                try:
                    def progress(message):
                        self.server.state = {'status': 'inspecting', 'apply_available': False, 'message': message}
                    self.server.state = self.server.inspector.scan(data.get('device'), progress)
                except Exception as error:
                    self.server.state = {'status': 'error', 'apply_available': False,
                                         'message': 'Inspection did not complete. No cleanup was performed. Check the retained job evidence.',
                                         'error_type': type(error).__name__}
                finally:
                    self.server.lock.release()
            threading.Thread(target=work, daemon=True).start()
            self.respond(202, {'status': 'inspecting'})
        except (ValueError, OSError):
            self.respond(400, {'error': 'Invalid companion request'})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--data', type=Path, default=Path.home() / '.local/state/homeboard-cleanup')
    parser.add_argument('--no-open', action='store_true')
    args = parser.parse_args()
    os.umask(0o077)
    verify_vendor()
    with Companion(Inspector(args.data)) as server:
        print('Homeboard TV cleanup companion: ' + server.origin, flush=True)
        print('Read-only inspection. Package changes require a reviewed device profile and execution integration.', flush=True)
        if not args.no_open:
            webbrowser.open(server.origin)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


if __name__ == '__main__':
    main()
