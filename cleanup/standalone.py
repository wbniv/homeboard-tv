"""Owned standalone ADB connection; never a fallback for coordinator jobs."""
import datetime
import ipaddress
import os
from pathlib import Path
import re
import socket
import subprocess
import uuid
from .core import Refused, private_directory, write_durable


def endpoint(value):
    if not isinstance(value, str) or ':' not in value:
        raise Refused('Enter the TV IP address and port')
    host, port = value.rsplit(':', 1)
    try:
        address = ipaddress.ip_address(host.strip('[]'))
        if not address.is_private or address.is_loopback or address.is_link_local or address.is_multicast or address.is_unspecified:
            raise ValueError()
        if not port.isdecimal() or not 1 <= int(port) <= 65535:
            raise ValueError()
    except ValueError:
        raise Refused('Use a private network IP address and a valid port') from None
    return (f'[{address}]' if address.version == 6 else str(address)) + ':' + str(int(port))


class Standalone:
    standalone = True
    def __init__(self, data, adb='adb', run=subprocess.run):
        self.data = private_directory(data)
        self.adb, self.run = adb, run
        self.home = private_directory(self.data / 'adb-home')
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0)); self.port = sock.getsockname()[1]
        self.connections = {}

    def command(self, *args, secret=None):
        env = {k:v for k,v in os.environ.items() if not k.startswith(('ADB_', 'ANDROID_'))}
        env.update(HOME=str(self.home), ANDROID_USER_HOME=str(self.home / '.android'),
                   ADB_SERVER_SOCKET=f'tcp:127.0.0.1:{self.port}', ADB_MDNS_AUTO_CONNECT='0')
        try:
            result = self.run([self.adb, *args], input=secret, text=True,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=25, env=env)
        except (OSError, subprocess.TimeoutExpired):
            raise Refused('Connection command failed or timed out. Check platform-tools and the TV address.') from None
        if result.returncode or len(result.stdout) > 2_000_000:
            raise Refused('TV command failed. Check the TV prompt, address and connection.')
        return result.stdout

    def devices(self):
        # Only connections explicitly selected in this companion session.
        return [dict(id=k, model=v['model'], status='connected', identity=v['serial']) for k,v in self.connections.items()]

    def discover(self):
        found = []
        for line in self.command('mdns', 'services').splitlines():
            fields = line.split()
            if len(fields) == 3 and fields[1] in ('_adb-tls-pairing._tcp.', '_adb-tls-connect._tcp.', '_adb-tls-pairing._tcp', '_adb-tls-connect._tcp'):
                try: address = endpoint(fields[2])
                except Refused: continue
                found.append(dict(address=address, kind='pair' if 'pairing' in fields[1] else 'connect'))
        return found

    def pair(self, address, code):
        address = endpoint(address)
        if not isinstance(code, str) or not re.fullmatch(r'\d{6}', code, flags=re.ASCII):
            raise Refused('Enter the six-digit pairing code currently shown on the TV')
        output = self.command('pair', address, secret=code+'\n')
        if 'Successfully paired' not in output:
            raise Refused('Pairing was not confirmed. Open a fresh pairing code on the TV.')
        return {'message':'Paired. Connect using the address on the main Wireless debugging screen.'}

    def identity(self, address):
        def shell(*args): return self.command('-s', address, 'shell', *args).strip()
        features = shell('pm', 'list', 'features')
        if not any(x in features.splitlines() for x in ('feature:android.software.leanback', 'feature:android.hardware.type.television')):
            raise Refused('This connection does not identify an Android TV')
        properties = {k:shell('getprop', v) for k,v in {
            'serial':'ro.serialno', 'model':'ro.product.model', 'manufacturer':'ro.product.manufacturer',
            'product':'ro.product.name', 'board':'ro.product.board', 'firmware':'ro.build.fingerprint',
            'android':'ro.build.version.release'}.items()}
        if not all(properties[k] for k in ('serial','model','firmware')):
            raise Refused('TV identity is incomplete')
        return properties

    def connect(self, address):
        address = endpoint(address)
        if any(v['address']==address for v in self.connections.values()):
            raise Refused('This address is already connected')
        output = self.command('connect', address)
        if 'connected to ' not in output:
            raise Refused('Connection was not confirmed. Use the debugging port, not the pairing port.')
        try: identity = self.identity(address)
        except Exception:
            self.command('disconnect', address)
            raise
        key = str(uuid.uuid4()); self.connections[key] = dict(identity, address=address)
        return {'device':dict(id=key, model=identity['model'], identity=identity['serial'])}

    def disconnect(self, device):
        if not isinstance(device, str): raise Refused('Choose a connected TV')
        saved = self.connections.get(device)
        if saved is None: raise Refused('Choose a connected TV')
        self.command('disconnect', saved['address']); del self.connections[device]
        return {'message':'Disconnected. Pairing authorization remains on the TV.'}

    def scan(self, device, progress=lambda message:None):
        if not isinstance(device, str): raise Refused('Choose a connected TV')
        saved = self.connections.get(device)
        if saved is None: raise Refused('Connect and confirm this TV first')
        progress('Checking TV identity')
        current = self.identity(saved['address'])
        if any(current[k] != saved[k] for k in ('serial','firmware')):
            raise Refused('TV identity or firmware changed. Reconnect and confirm it again.')
        packages = self.command('-s', saved['address'], 'shell', 'pm', 'list', 'packages').splitlines()
        if not packages or any(not re.fullmatch(r'package:[A-Za-z0-9_.]+', x) for x in packages):
            raise Refused('Package report is incomplete')
        if self.identity(saved['address']) != current:
            raise Refused('Identity changed during inspection')
        result = dict(status='scan-only', apply_available=False, device_id=device, device=current,
                      package_count=len(packages), collected_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                      job='Local inspection', candidates=[], notice='Read-only inspection. No packages changed.')
        write_durable(self.data / ('inspection-'+str(uuid.uuid4())+'.json'), result)
        return result

    def close(self):
        self.command('kill-server')  # Only this instance's private server socket.
