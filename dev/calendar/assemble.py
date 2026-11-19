#!/usr/bin/env python3
"""Validate provider handoffs and atomically assemble the APK's calendar snapshot."""
import argparse
from datetime import date
import hashlib
import json
from pathlib import Path
from normalize import write
from urllib.parse import urlparse

def require(condition, message):
    if not condition: raise ValueError(message)

IDS=['thai','spain','usa','china']
KINDS={'HOLIDAY','OBSERVED','MAKE','SELL','WAN_PHRA','WORKING'}
SCOPES={'national','bangkok','catalunya','tarragona-province','tivissa'}
def assemble(input_dir, output):
    providers=[]; hashes={}
    for id in IDS:
        path=input_dir/(id+'-calendar.json');p=json.loads(path.read_text())
        require(p['id']==id and p['schemaVersion']==1 and p['status']=='offline', 'Invalid offline provider')
        first,last=date.fromisoformat(p['coverageStart']),date.fromisoformat(p['coverageEnd'])
        require(first<=last and len(p['events'])<=10000, 'Invalid coverage or event count')
        seen=set()
        for e in p['events']:
            require(e['id'] not in seen and e['title'] and e['kind'] in KINDS and e['scope'] in SCOPES, 'Invalid event')
            seen.add(e['id']); require(first<=date.fromisoformat(e['date'])<=last, 'Event outside coverage')
            url=urlparse(e['url']); require(url.scheme=='https' and url.hostname and not url.username, 'Invalid source URL')
        providers.append(p);hashes[path.name]=hashlib.sha256(path.read_bytes()).hexdigest()
    snapshot=dict(schemaVersion=1,stage='assemble',producerVersion='1',inputHashes=hashes,providers=providers)
    write(output,snapshot)
    return snapshot
if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--input',type=Path,required=True);a.add_argument('--output',type=Path,required=True)
    args=a.parse_args();s=assemble(args.input,args.output);print(json.dumps({'stage':'assemble','providers':len(s['providers'])}))
