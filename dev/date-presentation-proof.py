#!/usr/bin/env python3
"""Coordinator-owned screenshots and native assertions for the date presentation."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

BASE=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--version',required=True)
    parser.add_argument('--screens',default='ThaiDateDashboard,ThaiDateCalendar,ThaiDateClock,EnglishDateClock,ThaiDateAir,ThaiDateFavorites,ThaiDateRestore')
    parser.add_argument('--duration',type=int,default=16)
    args=parser.parse_args()
    apk=BASE/'artifacts/deployments'/args.version/'homeboard.apk'
    root=BASE/'artifacts/date-presentation'/args.version
    env={k:v for k,v in os.environ.items() if not k.startswith('WF_CC_')}
    def task(command,out,*variables,required=True):
        result=subprocess.run(['task','chromecast:'+command,*variables],cwd=BASE,env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        out.parent.mkdir(parents=True,exist_ok=True);out.write_text(result.stdout)
        if required and result.returncode:raise RuntimeError(str(out))
        return result.stdout
    try:
        for screen in args.screens.split(','):
            folder=root/screen;folder.mkdir(parents=True,exist_ok=True)
            existing=folder/'result.json'
            prior=json.loads(existing.read_text()) if existing.exists() else {}
            if prior.get('coordinator')=='succeeded' and prior.get('proof',{}).get('status')=='pass' and prior.get('proof',{}).get('homeboardVersion')==args.version:
                print(screen,'existing passed evidence retained',flush=True);continue
            task('devices',folder/'devices.txt');task('queue',folder/'queue.txt')
            task('app-register',folder/'registration.json','APP=homeboard-tv',f'APK={apk}',f'ACTIVITY=com.gathin.tvhome.{screen}Probe','REPLACE=true')
            recipe=folder/'recipe.json';recipe.write_text(json.dumps(dict(workflow='check',app='homeboard-tv',duration=args.duration,panel='require-hidden')))
            submitted=task('submit',folder/'submit.txt','DEVICE=chromecast-test-01','APP=homeboard-tv',f'APK={apk}',f'RECIPE={recipe}')
            job=re.search(r'Accepted (J-[0-9a-f]+):',submitted)[1];print(screen,job,flush=True)
            task('watch',folder/'watch.txt',f'JOB={job}',required=False)
            task('evidence',folder/'export.txt',f'JOB={job}',f'OUT={folder/"evidence"}')
            receipt=json.loads((folder/'evidence/receipt.json').read_text())
            proof=folder/'evidence/wf.log'
            report=json.loads(proof.read_text()) if proof.exists() else dict(status='unverified')
            (folder/'result.json').write_text(json.dumps(dict(job=job,version=args.version,device='chromecast-test-01',coordinator=receipt.get('status'),proof=report),indent=2)+'\n')
            print(screen,report,flush=True)
            expected=screen.replace('ThaiDate','').replace('EnglishDate','').lower()
            if receipt.get('status')!='succeeded' or report.get('status')!='pass' or report.get('screen')!=expected or report.get('homeboardVersion')!=args.version:
                raise RuntimeError(f'{screen}: inspect evidence before continuing')
    finally:
        task('app-register',root/'restored-main-registration.json','APP=homeboard-tv',f'APK={apk}','ACTIVITY=com.gathin.tvhome.MainActivity','REPLACE=true')

if __name__=='__main__':main()
