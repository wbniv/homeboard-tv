#!/usr/bin/env python3
"""Extract public-API widget census records from coordinator-exported logcat."""
import argparse
import json
from pathlib import Path


def extract(text):
    pending = {}
    reports = []
    for line in text.splitlines():
        if 'HB_WIDGET_CENSUS' not in line:
            continue
        if 'provider=' in line:
            record = json.loads(line.split('provider=', 1)[1])
            pending[record['component']] = record
        elif 'summary=' in line:
            summary = json.loads(line.split('summary=', 1)[1])
            if summary.get('schema') != 1 or summary.get('providerCount') != len(pending):
                raise ValueError('Incomplete or unsupported census log; do not infer an empty provider list')
            summary['providers'] = list(pending.values())
            reports.append(summary)
            pending = {}
    if not reports:
        raise ValueError('No completed widget census found in coordinator logs')
    return reports[-1]


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('logcat', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = json.dumps(extract(args.logcat.read_text()), indent=2) + '\n'
    if args.output:
        args.output.write_text(result)
    print(result, end='')
