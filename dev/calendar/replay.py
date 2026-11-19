#!/usr/bin/env python3
"""Compile/run the native calendar modules in the existing build container."""
import os
from pathlib import Path
import subprocess
import sys

root=Path(__file__).resolve().parents[2]
cache=Path(os.environ.get('GRADLE_USER_HOME','/cache/gradle'))
jars=list(cache.glob('caches/modules-2/files-2.1/org.json/json/20240303/*/*.jar'))
if len(jars)!=1:raise SystemExit('Run task check once to populate the test-only JSON dependency')
output=root/'artifacts/calendar-cli/classes';output.mkdir(parents=True,exist_ok=True)
names=['CalendarSnapshot','CalendarWidgetSettings','ThaiCivilCalendar','HolidayStore','HolidayParsers','HolidayHttp','HolidayUpdater','ThaiClosureNotices']
sources=[root/'launcher/app/src/main/java/com/gathin/tvhome'/f'{name}.java' for name in names]
subprocess.run(['javac','-cp',str(jars[0]),'-d',str(output),*[str(p) for p in sources],str(root/'dev/calendar/CalendarCli.java')],check=True)
subprocess.run(['java','-cp',os.pathsep.join([str(output),str(jars[0])]),'com.gathin.tvhome.CalendarCli',*sys.argv[1:]],check=True)
