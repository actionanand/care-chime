#!/usr/bin/env python3
"""Validate the source-of-truth Android config before building."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
app = json.loads((ROOT / 'app-config.json').read_text(encoding='utf-8'))
version = json.loads((ROOT / 'android-version.json').read_text(encoding='utf-8'))
sdk = {}
for line in (ROOT / 'android-sdk.properties').read_text(encoding='utf-8').splitlines():
    line = line.strip()
    if line and not line.startswith('#'):
        key, val = line.split('=', 1)
        sdk[key.strip()] = val.strip()

assert app['applicationId'] == 'com.actionanand.carechime.app', 'Application ID must never drift'
assert app['appName'] == 'Care Chime'
assert app['tagline'] == 'Care, right on time.'
assert re.fullmatch(r'[A-Za-z][A-Za-z0-9_-]*', app['releaseBaseName'])
assert type(version['versionCode']) is int and 1 <= version['versionCode'] < 2100000000
assert isinstance(version['versionName'], str) and re.fullmatch(r'\d+\.\d+\.\d+', version['versionName'])
for name in ('MIN_SDK_VERSION', 'COMPILE_SDK_VERSION', 'TARGET_SDK_VERSION'):
    assert name in sdk and sdk[name].isdigit(), f'Missing or invalid {name}'
assert int(sdk['MIN_SDK_VERSION']) <= int(sdk['TARGET_SDK_VERSION']) <= int(sdk['COMPILE_SDK_VERSION'])
assert re.fullmatch(r'\d+\.\d+\.\d+', sdk['BUILD_TOOLS_VERSION'])
print('Configuration validated:', app['applicationId'], version)
