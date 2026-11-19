#!/usr/bin/env python3
"""Original three-note Homeboard alert; deterministic 16-bit mono PCM, no third-party audio."""
from pathlib import Path
import math,struct,wave
RATE=22050
frames=[]
for frequency,duration in [(660,.18),(0,.12),(880,.18),(0,.12),(660,.24),(0,1.16)]:
    count=round(duration*RATE)
    for i in range(count):
        envelope=min(1,i/(RATE*.012),(count-1-i)/(RATE*.025)) if frequency else 0
        frames.append(struct.pack('<h',round(9000*max(0,envelope)*math.sin(2*math.pi*frequency*i/RATE))))
output=Path(__file__).resolve().parents[1]/'launcher/app/src/main/res/raw/homeboard_alarm.wav'
with wave.open(str(output),'wb') as audio:
    audio.setnchannels(1);audio.setsampwidth(2);audio.setframerate(RATE);audio.writeframes(b''.join(frames))
print(f'Original alert tone: {output.name}, {len(frames)/RATE:.2f}s')
