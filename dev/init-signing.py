#!/usr/bin/env python3
"""Provision a durable key inside a private, explicitly mounted directory."""
import json
import os
from pathlib import Path
import secrets
import subprocess

directory = Path('/signing')
key = directory / 'release.jks'
config = directory / 'release.json'
if key.exists() or config.exists():
    raise SystemExit('Signing material already exists; refusing to replace it.')
os.umask(0o077)
password = secrets.token_urlsafe(48)
env = dict(os.environ, HOMEBOARD_KEY_PASSWORD=password)
subprocess.run(['keytool', '-genkeypair', '-keystore', str(key), '-alias', 'homeboard',
                '-storetype', 'JKS', '-keyalg', 'RSA', '-keysize', '4096', '-validity', '10000',
                '-dname', 'CN=Homeboard TV, O=biohack.net', '-storepass:env', 'HOMEBOARD_KEY_PASSWORD',
                '-keypass:env', 'HOMEBOARD_KEY_PASSWORD'], env=env, check=True,
               stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
config.write_text(json.dumps({'password': password}) + '\n')
print('Created Homeboard release key. Keep the private signing directory and its backup.')
