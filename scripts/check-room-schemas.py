#!/usr/bin/env python3
"""Verify that every Room schema version up to CareChimeDatabase.VERSION is committed.

Runs without the Android SDK. CI runs it after Gradle has exported the current schema, together
with a git check that no committed schema was modified and no generated schema is uncommitted.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATABASE_SOURCE = 'app/src/main/java/com/actionanand/carechime/app/data/db/CareChimeDatabase.kt'
SCHEMA_DIR = 'app/schemas/com.actionanand.carechime.app.data.db.CareChimeDatabase'


def check(root: Path) -> list:
    errors = []
    source = (root / DATABASE_SOURCE).read_text(encoding='utf-8')
    match = re.search(r'const\s+val\s+VERSION\s*=\s*(\d+)', source)
    if not match:
        return [f'{DATABASE_SOURCE}: could not find `const val VERSION`']
    version = int(match.group(1))
    schema_dir = root / SCHEMA_DIR

    for expected in range(1, version + 1):
        path = schema_dir / f'{expected}.json'
        if not path.is_file():
            errors.append(f'{SCHEMA_DIR}/{expected}.json is missing: download the room-schemas CI artifact and commit it')
            continue
        try:
            database = json.loads(path.read_text(encoding='utf-8'))['database']
        except (ValueError, KeyError, TypeError) as exc:
            errors.append(f'{SCHEMA_DIR}/{expected}.json is not a Room schema export: {exc}')
            continue
        if database.get('version') != expected:
            errors.append(f'{SCHEMA_DIR}/{expected}.json declares version {database.get("version")}')
        if not database.get('identityHash'):
            errors.append(f'{SCHEMA_DIR}/{expected}.json has no identityHash')
        if not database.get('entities'):
            errors.append(f'{SCHEMA_DIR}/{expected}.json has no entities')

    if schema_dir.is_dir():
        for path in sorted(schema_dir.iterdir()):
            stem = path.stem
            if path.suffix != '.json' or not stem.isdigit():
                errors.append(f'{SCHEMA_DIR}/{path.name}: unexpected file')
            elif int(stem) > version:
                errors.append(f'{SCHEMA_DIR}/{path.name}: newer than CareChimeDatabase.VERSION {version}')
    return errors


def main() -> int:
    errors = check(ROOT)
    if errors:
        print('\n'.join(errors), file=sys.stderr)
        print(f'Room schema check failed with {len(errors)} problem(s).', file=sys.stderr)
        return 1
    print('Room schema check passed: every database version has a committed schema.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
