#!/usr/bin/env python3
"""Verify the actual debug probe assertion screen exported by a coordinator job."""
import argparse
import json
from pathlib import Path
import re
import subprocess


def normalized(value):
    return re.sub(r'[^a-z0-9]', '', value.lower())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('evidence', type=Path)
    parser.add_argument('--expect-text', default='ALL CHECKS PASSED')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    receipt = json.loads((args.evidence / 'receipt.json').read_text())
    ocr = subprocess.run(['tesseract', str(args.evidence / 'screenshot.png'), 'stdout', '--psm', '6'],
                         check=True, text=True, capture_output=True).stdout
    observed = normalized(ocr)
    expected = normalized(args.expect_text)
    outcome = {'job': receipt['job'], 'status': receipt['status'], 'health': receipt['health'],
               'expected_text': args.expect_text, 'text_found': expected in observed,
               'ocr': ocr}
    outcome['passed'] = (outcome['status'] == 'succeeded' and outcome['health'] == 'ready'
                         and outcome['text_found'])
    if args.output:
        args.output.write_text(json.dumps(outcome, indent=2) + '\n')
    print(json.dumps(outcome, indent=2))
    return 0 if outcome['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
