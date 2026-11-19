#!/usr/bin/env python3
"""Coordinator-owned timer UI or background-audio acceptance using a frozen APK."""
import argparse,json,os,re,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
 p=argparse.ArgumentParser();p.add_argument('--version',required=True);p.add_argument('--sound',action='store_true');p.add_argument('--device',default='chromecast-test-01',choices=['chromecast-test-01','chromecast-test-02']);args=p.parse_args()
 apk=ROOT/'artifacts/deployments'/args.version/'homeboard.apk';folder=ROOT/'artifacts/timer-proof'/args.version/('sound' if args.sound else 'ui');folder=folder/args.device if args.device!='chromecast-test-01' else folder;folder.mkdir(parents=True,exist_ok=True)
 env={k:v for k,v in os.environ.items() if not k.startswith('WF_CC_')}
 def task(command,*params,required=True):
  r=subprocess.run(['task','chromecast:'+command,*params],cwd=ROOT,env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
  (folder/(command+'.txt')).write_text(r.stdout)
  if required and r.returncode:raise RuntimeError(r.stdout[-1600:])
  return r.stdout
 task('devices');task('queue')
 activity='TimerAlarmProbeActivity' if args.sound else 'TimerProbeActivity'
 recipe=ROOT/'dev/device'/('timer-sound-check.json' if args.sound else 'timer-check.json')
 try:
  task('app-register','APP=homeboard-tv',f'APK={apk}',f'ACTIVITY=com.gathin.tvhome.{activity}','REPLACE=true')
  output=task('submit',f'DEVICE={args.device}','APP=homeboard-tv',f'APK={apk}',f'RECIPE={recipe}')
  job=re.search(r'Accepted (J-[0-9a-f]+):',output)[1];print('Timer proof:',job,flush=True)
  task('watch',f'JOB={job}',required=False);evidence=folder/job/'evidence';task('evidence',f'JOB={job}',f'OUT={evidence}')
  receipt=json.loads((evidence/'receipt.json').read_text());proof=json.loads((evidence/'wf.log').read_text()) if (evidence/'wf.log').exists() else {'status':'missing'}
  record={'job':job,'version':args.version,'device':args.device,'coordinator':receipt.get('status'),'proof':proof,'evidence':str(evidence)}
  (folder/'result.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps(record,indent=2),flush=True)
  if receipt.get('status')!='succeeded' or proof.get('status')!='pass' or proof.get('homeboardVersion')!=args.version:raise RuntimeError('Timer proof did not pass; inspect evidence')
 finally:task('app-register','APP=homeboard-tv',f'APK={apk}','ACTIVITY=com.gathin.tvhome.MainActivity','REPLACE=true')
if __name__=='__main__':main()
