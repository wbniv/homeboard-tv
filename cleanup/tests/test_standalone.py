import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from cleanup.standalone import Standalone, endpoint
from cleanup.core import Refused


class StandaloneTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.calls=[];self.bad=False;self.serial='TV123'
        def run(args, **kwargs):
            self.calls.append((args,kwargs));out=''
            if 'pair' in args:out='Successfully paired to TV'
            elif 'connect' in args:out='connected to 192.168.1.9:33333'
            elif 'getprop' in args:out=self.serial if args[-1]=='ro.serialno' else 'example'
            elif args[-3:]==['pm','list','features']:out='feature:android.software.leanback'
            elif args[-3:]==['pm','list','packages']:out='package:example.app\n'
            elif args[-2:]==['mdns','services']:out='TV _adb-tls-connect._tcp. 192.168.1.9:33333\n'
            return subprocess.CompletedProcess(args,1 if self.bad else 0,out,'secret error')
        self.client=Standalone(Path(self.tmp.name),'fixture-adb',run)
    def test_inputs_and_pairing_secret(self):
        for value in ['-s evil', 'example.org:55','127.0.0.1:12','0.0.0.0:55','192.168.1.2:0','192.168.1.2:65536','8.8.8.8:55']:
            with self.assertRaises(Refused):endpoint(value)
        self.client.pair('192.168.1.9:22222','123456')
        args,kw=self.calls[-1];self.assertNotIn('123456',args);self.assertEqual(kw['input'],'123456\n')
        self.assertIn('127.0.0.1:',kw['env']['ADB_SERVER_SOCKET']);self.assertNotEqual(kw['env']['HOME'],str(Path.home()))
        with self.assertRaises(Refused):self.client.pair('192.168.1.9:22222','abc123')
    def test_connect_inspect_and_targeted_disconnect(self):
        found=self.client.connect('192.168.1.9:33333');key=found['device']['id']
        self.assertEqual(self.client.devices()[0]['identity'],'TV123')
        result=self.client.scan(key);self.assertEqual(result['package_count'],1);self.assertFalse(result['apply_available'])
        self.assertEqual(len(list(Path(self.tmp.name).glob('inspection-*.json'))),1)
        self.client.disconnect(key);self.assertEqual(self.client.devices(),[])
        self.assertEqual(self.calls[-1][0][-2:],['disconnect','192.168.1.9:33333'])
        for args,_ in self.calls:
            if 'shell' in args:self.assertEqual(args[1:3],['-s','192.168.1.9:33333'])
    def test_discovery_does_not_connect(self):
        self.assertEqual(self.client.discover(),[{'kind':'connect','address':'192.168.1.9:33333'}])
        self.assertEqual(len(self.calls),1)
    def test_identity_change_rejected(self):
        key=self.client.connect('192.168.1.9:33333')['device']['id'];self.serial='different'
        with self.assertRaises(Refused):self.client.scan(key)
        self.assertFalse(list(Path(self.tmp.name).glob('inspection-*.json')))
    def test_failures_do_not_expose_subprocess_output(self):
        self.bad=True
        with self.assertRaises(Refused) as caught:self.client.pair('192.168.1.9:22222','123456')
        self.assertNotIn('secret',str(caught.exception));self.assertNotIn('123456',str(caught.exception))
    def test_non_tv_rejected_and_disconnected(self):
        def run(args,**kw):return subprocess.CompletedProcess(args,0,'connected to x' if 'connect' in args else '', '')
        self.client.run=run
        with self.assertRaises(Refused):self.client.connect('192.168.1.9:33333')
        self.assertEqual(self.client.devices(),[])
    def test_timeout_and_unknown_devices(self):
        def run(args,**kw):raise subprocess.TimeoutExpired(args,25)
        self.client.run=run
        with self.assertRaises(Refused):self.client.discover()
        with self.assertRaises(Refused):self.client.disconnect([])
        with self.assertRaises(Refused):self.client.scan('unknown')
