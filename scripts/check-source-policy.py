#!/usr/bin/env python3
"""Static policy and structure checks that run without the Android SDK.

These complement (never replace) Gradle lint/compile/tests in GitHub Actions:
- privacy/safety policy: no logging APIs, no INTERNET permission, backups disabled,
  no destructive Room migration, no REPLACE conflict strategy (it cascades deletes);
- structure: Kotlin package matches its directory, delimiters are balanced, and
  imports of this project's own symbols resolve to a declaration.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP_PACKAGE = 'com.actionanand.carechime.app'
GENERATED_SYMBOLS = {'R', 'BuildConfig'}

FORBIDDEN_MAIN_PATTERNS = {
    r'\bandroid\.util\.Log\b': 'Android logging (may leak medical data)',
    r'(?<![\w.])Log\.[dviwe]\(': 'Android logging (may leak medical data)',
    r'\bprintln\(': 'stdout logging (may leak medical data)',
    r'\.printStackTrace\(': 'stack trace logging',
    r'fallbackToDestructiveMigration': 'destructive Room migration (would erase medical history)',
    r'OnConflictStrategy\.REPLACE': 'REPLACE deletes rows and fires ON DELETE CASCADE',
}

DECLARATION = re.compile(
    r'^\s*(?:(?:private|internal|public|protected|data|sealed|enum|abstract|open|inline|value|annotation|'
    r'const|override|lateinit|suspend|operator|infix|tailrec|fun\s+interface)\s+)*'
    r'(?:class|interface|object|fun|val|var|typealias)\s+'
    r'(?:<[^>]*>\s*)?(?:[\w.]+(?:<[^>]*>)?\??\.)?`?(\w+)`?',
    re.MULTILINE,
)


def strip_strings_and_comments(text: str) -> str:
    out = []
    i, n = 0, len(text)
    while i < n:
        if text.startswith('//', i):
            j = text.find('\n', i)
            i = n if j == -1 else j
        elif text.startswith('/*', i):
            j = text.find('*/', i + 2)
            i = n if j == -1 else j + 2
        elif text.startswith('"""', i):
            j = text.find('"""', i + 3)
            out.append('""')
            i = n if j == -1 else j + 3
        elif text[i] == '"':
            i += 1
            depth = 0
            while i < n:
                c = text[i]
                if c == '\\':
                    i += 2
                    continue
                if text.startswith('${', i):
                    depth += 1
                    i += 2
                    continue
                if c == '}' and depth:
                    depth -= 1
                elif c == '"' and not depth:
                    break
                i += 1
            out.append('""')
            i += 1
        elif text[i] == "'":
            j = i + 1
            while j < n and text[j] != "'":
                j += 2 if text[j] == '\\' else 1
            out.append("''")
            i = j + 1
        else:
            out.append(text[i])
            i += 1
    return ''.join(out)


def check_balance(path: Path, code: str, errors: list) -> None:
    pairs = {')': '(', ']': '[', '}': '{'}
    stack = []
    for line_no, line in enumerate(code.splitlines(), 1):
        for c in line:
            if c in '([{':
                stack.append((c, line_no))
            elif c in pairs:
                if not stack or stack[-1][0] != pairs[c]:
                    errors.append(f'{path}:{line_no}: unbalanced {c!r}')
                    return
                stack.pop()
    if stack:
        errors.append(f'{path}:{stack[-1][1]}: unclosed {stack[-1][0]!r}')


def main() -> int:
    errors = []
    main_src = ROOT / 'app/src/main/java'
    kotlin_files = sorted((ROOT / 'app/src').rglob('*.kt'))
    declared = {}
    sources = {}
    for path in kotlin_files:
        text = path.read_text(encoding='utf-8')
        code = strip_strings_and_comments(text)
        sources[path] = (text, code)
        match = re.search(r'^package\s+([\w.]+)', text, re.MULTILINE)
        if not match:
            errors.append(f'{path}: missing package declaration')
            continue
        package = match.group(1)
        source_root = next((p for p in path.parents if p.name in ('java', 'kotlin')), None)
        if source_root is not None:
            expected = '.'.join(path.parent.relative_to(source_root).parts)
            if package != expected:
                errors.append(f'{path}: package {package} does not match directory {expected}')
        declared.setdefault(package, set()).update(DECLARATION.findall(code))
        for constant in re.findall(r'^\s*(\w+)\s*(?:\(|,|$)', code, re.MULTILINE):
            declared[package].add(constant)  # enum entries

    for path, (text, code) in sources.items():
        check_balance(path, code, errors)
        for imported in re.findall(r'^import\s+([\w.]+)', text, re.MULTILINE):
            if not imported.startswith(APP_PACKAGE + '.') and imported != APP_PACKAGE:
                continue
            package, _, symbol = imported.rpartition('.')
            if symbol in GENERATED_SYMBOLS:
                continue
            if symbol not in declared.get(package, set()):
                errors.append(f'{path}: unresolved project import {imported}')
        if main_src in path.parents:
            for pattern, reason in FORBIDDEN_MAIN_PATTERNS.items():
                for match in re.finditer(pattern, code):
                    line = code.count('\n', 0, match.start()) + 1
                    errors.append(f'{path}:{line}: forbidden {reason}')

    manifest = (ROOT / 'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
    if 'android.permission.INTERNET' in manifest:
        errors.append('AndroidManifest.xml: INTERNET permission is not allowed without a documented opt-in feature')
    if 'android:allowBackup="false"' not in manifest:
        errors.append('AndroidManifest.xml: android:allowBackup must stay false')

    if errors:
        print('\n'.join(errors), file=sys.stderr)
        print(f'Source policy check failed with {len(errors)} problem(s).', file=sys.stderr)
        return 1
    print(f'Source policy check passed for {len(kotlin_files)} Kotlin files.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
