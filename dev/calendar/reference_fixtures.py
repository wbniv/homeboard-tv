import sys,hashlib,json
from pathlib import Path
from datetime import date,timedelta
sys.path.insert(0,'/workspace/vendor/pythaidate')
from reference.csdate import CsDate
root=Path('/workspace/vendor/pythaidate')
(root/'provenance.json').write_text(json.dumps({'repository':'https://github.com/hmmbug/pythaidate','commit':'f526c4d9d9ee2a854bf790aaf6d6ee6db90f3f25','license':'MIT; Copyright 2023 Mark Hollow','files':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [root/'LICENSE.txt',*sorted((root/'reference').glob('*.py'))]}},indent=2)+'\n')
rows=[]
for year in range(1900,2101):
 h=hashlib.sha256();d=date(year,1,1)
 while d.year==year:
  v=CsDate.fromjulianday(d.toordinal()+1721425)
  h.update(f'{d},{v.year},{v.month},{v.day}\n'.encode());d+=timedelta(days=1)
 rows.append(f'{year},{h.hexdigest()}')
Path('/workspace/launcher/app/src/test/resources/calendar/reference-years.csv').write_text('\n'.join(rows)+'\n')
