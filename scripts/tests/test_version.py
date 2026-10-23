import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

class VersionTest(unittest.TestCase):
    def test_validate_config(self):
        subprocess.run([sys.executable, str(ROOT / 'scripts/validate-config.py')], check=True)

    def test_bump_code_and_keep_name(self):
        with tempfile.TemporaryDirectory() as temp:
            work = Path(temp)
            (work / 'scripts').mkdir()
            (work / 'scripts/bump-android-version.py').write_bytes((ROOT / 'scripts/bump-android-version.py').read_bytes())
            version = {'versionCode': 41, 'versionName': '1.2.3'}
            (work / 'android-version.json').write_text(json.dumps(version))
            subprocess.run([sys.executable, str(work / 'scripts/bump-android-version.py')], check=True)
            updated = json.loads((work / 'android-version.json').read_text())
            self.assertEqual(updated, {'versionCode': 42, 'versionName': '1.2.3'})
            subprocess.run([sys.executable, str(work / 'scripts/bump-android-version.py'), '--patch'], check=True)
            updated = json.loads((work / 'android-version.json').read_text())
            self.assertEqual(updated, {'versionCode': 43, 'versionName': '1.2.4'})

if __name__ == '__main__': unittest.main()
