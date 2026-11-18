#!/usr/bin/env python3
"""Publish reviewed screenshot inputs/metadata; never infer visual acceptance from a job."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil

BASE=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--version',required=True);args=parser.parse_args()
    folder=BASE/'docs/testing/2026-11-18-provider-directory';folder.mkdir(exist_ok=True)
    screenshots=[];rows=[]
    reviews=json.loads((folder/"visual-review.json").read_text()) if (folder/"visual-review.json").exists() else {}
    def keep(src,name,**metadata):
        dest=folder/name;shutil.copyfile(src,dest)
        screenshots.append(dict(file=name,sha256=hashlib.sha256(src.read_bytes()).hexdigest(),
            artifact_timestamp_utc=datetime.fromtimestamp(src.stat().st_mtime,timezone.utc).isoformat(),**metadata))
    for screen in ['Page0','Page1','Page2','Weather','Movie','Daily','Dashboard']:
        proof=BASE/'artifacts/provider-directory'/args.version/screen
        if not (proof/'result.json').exists():continue
        result=json.loads((proof/'result.json').read_text())
        image=proof/'evidence/screenshot.png'
        if result.get('proof',{}).get('status')!='pass' or not image.exists():continue
        review=reviews.get(screen,{})
        reviewed=review.get('result')=='pass' and review.get('job')==result['job'] and review.get('sha256')==hashlib.sha256(image.read_bytes()).hexdigest()
        visual='passed' if reviewed else 'pending'
        name='tv-'+screen.lower()+'.png';keep(image,name,job=result['job'],device=result['device'],version=args.version,
            native_assertions='pass',visual_review=visual,observations=review.get('observations','') if reviewed else '')
        rows.append(f'| {screen} | `{result["job"].replace("J-", "J‑", 1)}` | [Screenshot](2026-11-18-provider-directory/{name}) | Native assertions passed; visual review {visual} |')
    for browser,label in [('android-chrome','chromium'),('iphone-webkit','webkit')]:
        for language in ['en','th']:
            src=BASE/'artifacts/phone-browser'/f'{browser}-providers-{language}.png'
            keep(src,f'phone-{label}-{language}.png',browser=browser,language=language,version=args.version,
                source='Production phone assets, controlled JSON fixture',native_assertions='not-applicable')
    core=BASE/'artifacts/provider-compatibility/2026-11-18/core-motion/test-retry59/evidence'
    if (core/'wf.log').exists() and (core/'screenshot.png').exists():
        native=json.loads((core/'wf.log').read_text())
        keep(core/'screenshot.png','baseline-core-motion.png',job='J-eaf00a2dbd46',device='chromecast-test-01',
            version=native['homeboardVersion'],provider_version=native['providerVersion'],
            native_assertions=native['baseline'],source='Independent provider baseline, separate from directory UI')
    (folder/'manifest.json').write_text(json.dumps(dict(version=args.version,timestamp_note='Artifact filesystem timestamps: browser capture or coordinator evidence export time; job IDs identify the TV session.',screenshots=screenshots),indent=2)+'\n')
    if rows:
        p=BASE/'docs/testing/2026-11-18-provider-directory.md';s=p.read_text();marker='<!-- TV-SCREENSHOTS -->'
        table=marker+'\n\n| Screen | Cast1 job | Actual capture | Result |\n|---|---|---|---|\n'+'\n'.join(rows)+'\n\n[Capture metadata and hashes](2026-11-18-provider-directory/manifest.json).\n'
        if marker in s:s=s[:s.index(marker)]
        p.write_text(s+'\n'+table)

if __name__=='__main__':main()
