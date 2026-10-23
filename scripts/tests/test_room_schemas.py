import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'scripts/check-room-schemas.py'
DB_SOURCE = 'app/src/main/java/com/actionanand/carechime/app/data/db/CareChimeDatabase.kt'
SCHEMA_DIR = 'app/schemas/com.actionanand.carechime.app.data.db.CareChimeDatabase'


def schema(version, identity='abc123'):
    return json.dumps({
        'formatVersion': 1,
        'database': {'version': version, 'identityHash': identity, 'entities': [{'tableName': 'profiles'}]},
    })


class RoomSchemaCheckTest(unittest.TestCase):
    def run_checker(self, version, files):
        with tempfile.TemporaryDirectory() as temp:
            work = Path(temp)
            (work / 'scripts').mkdir()
            (work / 'scripts/check-room-schemas.py').write_bytes(SCRIPT.read_bytes())
            source = work / DB_SOURCE
            source.parent.mkdir(parents=True)
            source.write_text(f'object X {{\n    const val VERSION = {version}\n}}\n', encoding='utf-8')
            for name, content in files.items():
                target = work / SCHEMA_DIR / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(content, encoding='utf-8')
            return subprocess.run(
                [sys.executable, str(work / 'scripts/check-room-schemas.py')],
                capture_output=True, text=True,
            )

    def test_complete_history_passes(self):
        result = self.run_checker(2, {'1.json': schema(1), '2.json': schema(2)})
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_missing_baseline_fails(self):
        result = self.run_checker(2, {'2.json': schema(2)})
        self.assertEqual(result.returncode, 1)
        self.assertIn('1.json is missing', result.stderr)

    def test_missing_current_version_fails(self):
        result = self.run_checker(2, {'1.json': schema(1)})
        self.assertEqual(result.returncode, 1)
        self.assertIn('2.json is missing', result.stderr)

    def test_no_schema_directory_fails(self):
        result = self.run_checker(1, {})
        self.assertEqual(result.returncode, 1)
        self.assertIn('1.json is missing', result.stderr)

    def test_inconsistent_or_unexpected_files_fail(self):
        result = self.run_checker(1, {
            '1.json': schema(2, identity=''),
            '2.json': schema(2),
            'notes.txt': 'x',
        })
        self.assertEqual(result.returncode, 1)
        for expected in ('declares version 2', 'no identityHash', 'newer than', 'unexpected file'):
            self.assertIn(expected, result.stderr)

    def test_invalid_json_fails(self):
        result = self.run_checker(1, {'1.json': '{not json'})
        self.assertEqual(result.returncode, 1)
        self.assertIn('not a Room schema export', result.stderr)


if __name__ == '__main__':
    unittest.main()
