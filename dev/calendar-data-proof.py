#!/usr/bin/env python3
"""Coordinator-owned calendar calculation/cache/source UI screenshot acceptance."""
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
    parser.add_argument('--screens',default='CalendarDataSources,ThaiCalendarDataSources,CalendarDataFailure,CalendarDataBoundary,ThaiCalendarDataBoundary,CalendarDataRestore,CalendarDataCompact')
    parser.add_argument('--duration',type=int,default=16)
    parser.add_argument('--force',action='store_true',help='Capture a new job even when this version has a passed result')
    args=parser.parse_args()
    apk=BASE/'artifacts/deployments'/args.version/'homeboard.apk'
    root=BASE/'artifacts/calendar-data'/args.version
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
            if not args.force and prior.get('coordinator')=='succeeded' and prior.get('proof',{}).get('status')=='pass' and prior.get('proof',{}).get('homeboardVersion')==args.version:
                print(screen,'existing passed evidence retained',flush=True);continue
            task('devices',folder/'devices.txt');task('queue',folder/'queue.txt')
            task('app-register',folder/'registration.json','APP=homeboard-tv',f'APK={apk}',f'ACTIVITY=com.gathin.tvhome.{screen}Probe','REPLACE=true')
            parameters=dict(workflow='check',app='homeboard-tv',duration=args.duration,panel='require-hidden')
            if screen.endswith('Failure'):
                parameters['input_script']=[dict(at=9,key='DPAD_UP',action='hold',ms=80),dict(at=10,key='DPAD_CENTER',action='hold',ms=80),dict(at=13,capture='failed-refresh-details')]
            recipe=folder/'recipe.json';recipe.write_text(json.dumps(parameters))
            submitted=task('submit',folder/'submit.txt','DEVICE=chromecast-test-01','APP=homeboard-tv',f'APK={apk}',f'RECIPE={recipe}')
            job=re.search(r'Accepted (J-[0-9a-f]+):',submitted)[1];print(screen,job,flush=True)
            task('watch',folder/'watch.txt',f'JOB={job}',required=False)
            evidence=folder/job/'evidence'
            task('evidence',folder/'export.txt',f'JOB={job}',f'OUT={evidence}')
            receipt=json.loads((evidence/'receipt.json').read_text())
            proof=evidence/'wf.log'
            report=json.loads(proof.read_text()) if proof.exists() else dict(status='unverified')
            (folder/'result.json').write_text(json.dumps(dict(job=job,version=args.version,device='chromecast-test-01',coordinator=receipt.get('status'),proof=report),indent=2)+'\n')
            print(screen,report,flush=True)
            expected=screen.replace('ThaiCalendarData','').replace('CalendarData','').lower()
            language='th' if screen.startswith('Thai') else 'en'
            if receipt.get('status')!='succeeded' or report.get('status')!='pass' or report.get('screen')!=expected or report.get('language')!=language or report.get('homeboardVersion')!=args.version:
                raise RuntimeError(f'{screen}: inspect evidence before continuing')
    finally:
        task('app-register',root/'restored-main-registration.json','APP=homeboard-tv',f'APK={apk}','ACTIVITY=com.gathin.tvhome.MainActivity','REPLACE=true')

if __name__=='__main__':main()
