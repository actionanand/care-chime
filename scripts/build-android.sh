#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
variant="${1:-debug}"
python3 scripts/validate-config.py
case "$variant" in
  debug)
    "${GRADLE_BIN:-gradle}" lint testDebugUnitTest assembleDebug
    echo 'Debug APK: app/build/outputs/apk/debug/'
    ;;
  release)
    "${GRADLE_BIN:-gradle}" lint testDebugUnitTest assembleRelease bundleRelease
    echo 'Unsigned release APK/AAB: app/build/outputs/ (use sign-release.sh to sign)'
    ;;
  *) echo 'Usage: scripts/build-android.sh [debug|release]' >&2; exit 2 ;;
esac
