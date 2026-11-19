"""Strict parsing of Android package state; missing facts never imply eligibility."""
import re
import time
from .core import Refused, validate_snapshot


def parse_packages(listing, dump):
    names=set()
    for line in listing.splitlines():
        match=re.fullmatch(r'package:([A-Za-z0-9_.]+)',line.strip())
        if not match:raise Refused('Incomplete installed-package list')
        names.add(match[1])
    sections=re.split(r'(?m)^\s{2}Package \[([^\]]+)\].*$',dump)
    result={}
    for i in range(1,len(sections),2):
        name,body=sections[i:i+2]
        if name not in names:continue
        uid=re.search(r'(?m)^\s+(?:userId|appId)=(\d+)\b',body)
        version=re.search(r'\bversionCode=(\d+)\b',body)
        user=re.search(r'(?m)^\s+User 0:\s*([^\n]+)',body)
        enabled=re.search(r'\benabled=([0-4])\b',user[1]) if user else None
        if not uid or not version or not enabled or 'installed=true' not in user[1]:
            raise Refused('Exact package state unavailable: '+name)
        if name in result:raise Refused('Duplicate package state')
        result[name]=dict(uid=int(uid[1]),version=version[1],enabled=int(enabled[1]),
                          roles=[],dependencies_verified=False)
    if not names or set(result)!=names:raise Refused('Package inventory is incomplete')
    return result


class Executor:
    def __init__(self, transport, device, profiles=()):
        self.transport,self.device,self.profiles=transport,device,profiles
    def inspect(self):
        saved=self.transport.connections.get(self.device)
        if not saved:raise Refused('Reconnect the TV before continuing')
        address=saved['address']
        shell=lambda *args:self.transport.command('-s',address,'shell',*args).strip()
        identity=self.transport.identity(address)
        if any(identity[k]!=saved[k] for k in ('serial','firmware')):raise Refused('TV identity changed')
        if shell('am','get-current-user')!='0':raise Refused('Cleanup supports the primary TV user only')
        listing=shell('pm','list','packages','--user','0')
        packages=parse_packages(listing,shell('dumpsys','package'))
        home=shell('cmd','package','resolve-activity','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.HOME').splitlines()[-1]
        launchers=shell('cmd','package','query-activities','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
        for component in launchers.splitlines()+[home]:
            name=component.split('/')[0]
            if name in packages:packages[name]['roles'].append('home')
        # All processes, not just the visible activity, are retained.
        processes=shell('ps','-A','-n','-o','UID,NAME').splitlines()
        if not processes or processes[0].split()!=['UID','NAME']:raise Refused('Active processes could not be verified')
        active=set()
        for line in processes[1:]:
            parts=line.split(maxsplit=1)
            if len(parts)!=2 or not parts[0].isdigit():raise Refused('Active process ownership unavailable')
            active.add(int(parts[0])%100000)
        for item in packages.values():
            if item['uid']%100000 in active:item['roles'].append('active process')
        device={k:identity[k] for k in ('manufacturer','model','product','board')}
        device.update(id=identity['serial'],fingerprint=identity['firmware'])
        # Dependency eligibility comes only from an exact reviewed profile, never
        # from a name heuristic or facts supplied by the browser.
        for profile in self.profiles:
            if profile.get('approved') is True and profile.get('match')==device:
                for entry in profile.get('packages',[]):
                    name=entry.get('package');item=packages.get(name)
                    if item and entry.get('dependencies_reviewed') is True and entry.get('version')==item['version']:
                        item['dependencies_verified']=True
        if self.transport.identity(address)!=identity:raise Refused('TV changed during collection')
        return validate_snapshot(dict(format=1,user=0,identity_verified=True,collected_at=time.time(),device=device,home=home,packages=packages))
    def set_enabled(self,name,state):
        if not re.fullmatch(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+',name) or state not in (0,1,3):raise Refused('Invalid package operation')
        saved=self.transport.connections.get(self.device)
        if not saved:raise Refused('Reconnect the TV')
        current=self.transport.identity(saved['address'])
        if any(current[k]!=saved[k] for k in ('serial','firmware')):raise Refused('TV identity changed')
        action={0:'default-state',1:'enable',3:'disable-user'}[state]
        self.transport.command('-s',saved['address'],'shell','pm',action,'--user','0',name)
