#!/usr/bin/env python3
"""Normalize reviewed source facts into independent provider JSON handoffs.
Run in the build container. Never labels offline facts as a live check.
"""
import argparse
import copy
from datetime import date, timedelta
import hashlib
import json
from pathlib import Path

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_suffix('.tmp')
    temp.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
    temp.replace(path)

def normalize(input_dir, output_dir):
    thai = json.loads((input_dir / 'thai.json').read_text())
    def base(id, end=None):
        return dict(id=id, country='TH', timeZone='Asia/Bangkok', status='offline',
                    reviewedOn=thai['reviewedOn'], coverageStart=thai['coverageStart'],
                    coverageEnd=end or thai['coverageEnd'], events=[])
    government, flowers, buddhist = base('thai'), base('flowers', thai['wanPhra'][-1]), base('buddhist', thai['wanPhra'][-1])
    for h in thai['government']:
        e = dict(id=f"thai:{h['date']}:{h['scope']}", date=h['date'], title=h['english'], original=h['thai'],
                 scope=h['scope'], kind='OBSERVED' if 'substituteFor' in h else 'HOLIDAY', url=thai['sources'][h['source']]['url'])
        if 'substituteFor' in h: e['anchor'] = h['substituteFor']
        government['events'].append(e)
    for day in ['2026-09-28','2026-09-29']:
        government['events'].append(dict(id='thai:'+day+':bangkok',date=day,
            title='Flood closure · online/essential services may continue',original='วันหยุดราชการพิเศษเนื่องจากน้ำท่วม',
            scope='bangkok',kind='HOLIDAY',url='https://www.thaigov.go.th/th/news/169370'))
    for anchor in thai['wanPhra']:
        common=dict(anchor=anchor,scope='national',url=thai['sources']['wan-phra']['url'])
        buddhist['events'].append(dict(common,id='buddhist:'+anchor,date=anchor,title='Wan Phra',original='วันพระ',kind='WAN_PHRA'))
        for offset in range(4):
            day = (date.fromisoformat(anchor)-timedelta(days=offset)).isoformat()
            if day < flowers['coverageStart']: continue
            kind = 'SELL' if offset <= 1 else 'MAKE'
            flowers['events'].append(dict(common,id=f'flowers:{anchor}:{day}',date=day,title='Flower work',original='',kind=kind))
    providers=[government,flowers,buddhist]
    for id in ['spain','usa','china']:
        p=json.loads((input_dir / (id+'.json')).read_text())
        for period in p.pop('periods',[]):
            start,end=date.fromisoformat(period['start']),date.fromisoformat(period['end'])
            if not 0 <= (end-start).days <= 31: raise ValueError('Invalid holiday period')
            for offset in range((end-start).days+1):
                day=(start+timedelta(days=offset)).isoformat()
                p['events'].append(dict(id=f'{id}:{day}:HOLIDAY:national',date=day,title=period['title'],original=period['original'],url=period['url'],scope='national',kind='HOLIDAY'))
        providers.append(p)
    for p in providers:
        p['events'].sort(key=lambda e:(e['date'],e['id']))
        p['schemaVersion']=1; p['stage']='normalize';p['producerVersion']='1'
        p['inputHashes']={f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in sorted(input_dir.glob('*.json'))}
        write(output_dir/(p['id']+'-calendar.json'),p)
    return providers

if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--input',type=Path,required=True);a.add_argument('--output',type=Path,required=True)
    args=a.parse_args();result=normalize(args.input,args.output)
    print(json.dumps({'stage':'normalize','providers':len(result),'events':sum(len(p['events']) for p in result)}))
