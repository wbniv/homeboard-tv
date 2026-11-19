"""Loopback-only reviewed cleanup companion with independent recovery."""
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
        self.workflow = None
        self.cleanup_state = {'status':'idle'}
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
        elif self.path == '/api/config' and self.headers.get('X-Homeboard-Token') == self.server.token:
            self.respond(200, {'standalone': getattr(self.server.inspector, 'standalone', False), 'cleanup': self.server.workflow is not None})
        elif self.path == '/api/state' and self.headers.get('X-Homeboard-Token') == self.server.token:
            self.respond(200, self.server.state)
        else:
            self.respond(404, {'error': 'Not found'})

    def do_POST(self):
        if (not self.trusted_host() or self.headers.get('Origin') != self.server.origin
                or not secrets.compare_digest(self.headers.get('X-Homeboard-Token', ''), self.server.token)):
            self.respond(403, {'error': 'Open the companion directly on this computer'})
            return
        connection_actions = ('/api/discover', '/api/pair', '/api/connect', '/api/disconnect')
        cleanup_actions = tuple('/api/cleanup/'+name for name in ('review','prepare','apply','sessions','restore','check','export','stop','status')) if self.server.workflow else ()
        if self.path not in cleanup_actions + ('/api/devices', '/api/scan') + (connection_actions if getattr(self.server.inspector, 'standalone', False) else ()):
            self.respond(404, {'error': 'No such action'})
            return
        try:
            length = int(self.headers.get('Content-Length', '-1'))
            if not 0 <= length <= 2048 or self.headers.get('Content-Type') != 'application/json':
                raise ValueError('Invalid request')
            data = json.loads(self.rfile.read(length))
            if not isinstance(data, dict):
                raise ValueError('Invalid request')
            if self.path in cleanup_actions:
                self.cleanup_action(data)
                return
            if self.path in connection_actions:
                if not self.server.lock.acquire(blocking=False):
                    self.respond(409, {'error': 'A TV operation is already running'})
                    return
                try:
                    inspector = self.server.inspector
                    if self.path == '/api/discover': value = {'services': inspector.discover()}
                    elif self.path == '/api/pair': value = inspector.pair(data.get('address'), data.get('code'))
                    elif self.path == '/api/connect': value = inspector.connect(data.get('address'))
                    else: value = inspector.disconnect(data.get('device'))
                    self.server.state = {'status':'idle', 'apply_available':False}
                    self.respond(200, value)
                finally:
                    self.server.lock.release()
                return
            if self.path == '/api/scan' and getattr(self.server.inspector, 'standalone', False) and data.get('confirmed') is not True:
                raise ValueError('Confirm the TV identity before inspecting')
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
        except (ValueError, OSError) as error:
            from .core import Refused
            self.respond(400, {'error': str(error) if isinstance(error, Refused) else 'Invalid companion request'})

    def cleanup_action(self, data):
        flow=self.server.workflow; action=self.path.rsplit('/',1)[-1]
        if action=='status':
            self.respond(200,self.server.cleanup_state);return
        if action=='stop':
            flow.stop_event.set();self.respond(200,{'status':'stop-requested'});return
        if action in ('review','prepare','restore','check') and data.get('confirmed') is not True:
            self.respond(400,{'error':'Confirm the TV identity first'});return
        if not self.server.lock.acquire(blocking=False):
            self.respond(409,{'error':'A TV operation is already running'});return
        def operation():
            if action=='review':return flow.inspect(data.get('device'))
            if action=='prepare':return flow.prepare(data.get('device'),data.get('chosen'))
            if action=='apply':return flow.apply(data.get('token'),data.get('confirmation'))
            if action=='sessions':return {'status':'sessions','sessions':flow.sessions()}
            if action=='restore':return flow.restore(data.get('device'),data.get('session'),data.get('chosen'))
            if action=='check':return flow.check(data.get('device'),data.get('session'))
            return {'status':'export','record':flow.export(data.get('session'))}
        if action in ('apply','restore','review','check'):
            if action=='apply':flow.stop_event.clear()
            self.server.cleanup_state={'status':'working','action':action}
            def work():
                try:self.server.cleanup_state=operation()
                except Exception:
                    self.server.cleanup_state={'status':'error','message':'Operation incomplete. Check saved sessions and actual TV state before retrying.'}
                finally:self.server.lock.release()
            threading.Thread(target=work,daemon=True).start()
            self.respond(202,{'status':'working','action':action})
        else:
            try:self.respond(200,operation())
            finally:self.server.lock.release()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--data', type=Path, default=Path.home() / '.local/state/homeboard-cleanup')
    parser.add_argument('--no-open', action='store_true')
    parser.add_argument('--transport', choices=('coordinator','standalone'), default='coordinator')
    parser.add_argument('--adb', default='adb', help='Existing platform-tools executable for standalone mode')
    args = parser.parse_args()
    os.umask(0o077)
    verify_vendor()
    from .standalone import Standalone
    inspector = Standalone(args.data, args.adb) if args.transport == 'standalone' else Inspector(args.data)
    with Companion(inspector) as server:
        if args.transport == 'standalone':
            from .workflow import Workflow
            server.workflow=Workflow(inspector,args.data)
        print('Homeboard TV cleanup companion: ' + server.origin, flush=True)
        print('Inspect first. Cleanup requires an exact approved profile, confirmed choices and verified recovery copies.', flush=True)
        if not args.no_open:
            webbrowser.open(server.origin)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass
        finally:
            if args.transport == 'standalone':
                inspector.close()


if __name__ == '__main__':
    main()
