#!/usr/bin/env python3
"""Coordinator-owned native grocery proof, isolated from household preferences."""
import argparse,json,os,re,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
 p=argparse.ArgumentParser();p.add_argument('--version',required=True);args=p.parse_args()
 apk=ROOT/'artifacts/deployments'/args.version/'homeboard.apk';folder=ROOT/'artifacts/grocery-proof'/args.version;folder.mkdir(parents=True,exist_ok=True)
 env={k:v for k,v in os.environ.items() if not k.startswith('WF_CC_')}
 def task(command,*params,required=True):
  r=subprocess.run(['task','chromecast:'+command,*params],cwd=ROOT,env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
  (folder/(command+'.txt')).write_text(r.stdout)
  if required and r.returncode:raise RuntimeError(r.stdout[-2000:])
  return r.stdout
 task('devices');task('queue')
 try:
  task('app-register','APP=homeboard-tv',f'APK={apk}','ACTIVITY=com.gathin.tvhome.GroceryProbeActivity','REPLACE=true')
  out=task('submit','DEVICE=chromecast-test-01','APP=homeboard-tv',f'APK={apk}',f'RECIPE={ROOT / "dev/device/grocery-check.json"}')
  job=re.search(r'Accepted (J-[0-9a-f]+):',out)[1];print('Native grocery proof:',job,flush=True)
  task('watch',f'JOB={job}',required=False);evidence=folder/job/'evidence';task('evidence',f'JOB={job}',f'OUT={evidence}')
  receipt=json.loads((evidence/'receipt.json').read_text());proof=json.loads((evidence/'wf.log').read_text()) if (evidence/'wf.log').exists() else {'status':'missing'}
  record={'job':job,'version':args.version,'device':'chromecast-test-01','coordinator':receipt.get('status'),'proof':proof,'evidence':str(evidence)}
  (folder/'result.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps(record,indent=2),flush=True)
  if receipt.get('status')!='succeeded' or proof.get('status')!='pass' or proof.get('homeboardVersion')!=args.version:raise RuntimeError('Native proof did not pass; inspect evidence')
 finally:
  task('app-register','APP=homeboard-tv',f'APK={apk}','ACTIVITY=com.gathin.tvhome.MainActivity','REPLACE=true')
if __name__=='__main__':main()
