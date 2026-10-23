import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'scripts/check-source-policy.py'

MANIFEST_OK = '<manifest><application android:allowBackup="false" /></manifest>\n'
PKG_DIR = 'app/src/main/java/com/actionanand/carechime/app/feature'


class SourcePolicyTest(unittest.TestCase):
    def run_checker(self, files):
        with tempfile.TemporaryDirectory() as temp:
            work = Path(temp)
            (work / 'scripts').mkdir()
            (work / 'scripts/check-source-policy.py').write_bytes(SCRIPT.read_bytes())
            for relative, content in files.items():
                target = work / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(content, encoding='utf-8')
            return subprocess.run(
                [sys.executable, str(work / 'scripts/check-source-policy.py')],
                capture_output=True, text=True,
            )

    def test_repository_passes(self):
        result = subprocess.run([sys.executable, str(SCRIPT)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_clean_tree_passes(self):
        result = self.run_checker({
            'app/src/main/AndroidManifest.xml': MANIFEST_OK,
            f'{PKG_DIR}/A.kt': 'package com.actionanand.carechime.app.feature\n\nfun <T> List<T>.helper() = "x{"\n',
            f'{PKG_DIR}/B.kt': 'package com.actionanand.carechime.app.feature\n\nimport com.actionanand.carechime.app.feature.helper\n',
        })
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_violations_are_reported(self):
        result = self.run_checker({
            'app/src/main/AndroidManifest.xml':
                '<manifest><uses-permission android:name="android.permission.INTERNET" /><application /></manifest>',
            f'{PKG_DIR}/Bad.kt': (
                'package com.actionanand.carechime.app.wrong\n\n'
                'import android.util.Log\n'
                'import com.actionanand.carechime.app.feature.Missing\n\n'
                'fun f() { Log.d("t", "x"); println("x")\n'
            ),
        })
        self.assertEqual(result.returncode, 1)
        for expected in ('INTERNET', 'allowBackup', 'does not match directory', 'unresolved project import',
                         'Android logging', 'stdout logging', 'unclosed'):
            self.assertIn(expected, result.stderr)


if __name__ == '__main__':
    unittest.main()
