#!/usr/bin/env python3
"""Increment versionCode; update versionName only with an explicit semantic bump."""
import argparse
import json
import re
from pathlib import Path

parser = argparse.ArgumentParser()
g = parser.add_mutually_exclusive_group()
g.add_argument('--patch', action='store_true')
g.add_argument('--minor', action='store_true')
g.add_argument('--major', action='store_true')
args = parser.parse_args()
version_file = Path(__file__).resolve().parents[1] / 'android-version.json'
version = json.loads(version_file.read_text(encoding='utf-8'))
code, name = version.get('versionCode'), version.get('versionName')
if type(code) is not int or not 1 <= code < 2099999999:
    raise SystemExit('versionCode must be an integer from 1 through 2099999998')
if not isinstance(name, str) or not re.fullmatch(r'\d+\.\d+\.\d+', name):
    raise SystemExit('versionName must be numeric major.minor.patch')
major, minor, patch = map(int, name.split('.'))
version['versionCode'] = code + 1
if args.patch: version['versionName'] = f'{major}.{minor}.{patch+1}'
elif args.minor: version['versionName'] = f'{major}.{minor+1}.0'
elif args.major: version['versionName'] = f'{major+1}.0.0'
version_file.write_text(json.dumps(version, indent=2) + '\n', encoding='utf-8')
print(f"Android version: {version['versionName']} ({version['versionCode']})")
