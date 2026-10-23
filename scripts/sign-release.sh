#!/usr/bin/env bash
# Sign and VERIFY an already-built release APK and AAB. Does not bump version.
set -euo pipefail
if [[ $# -ne 3 ]]; then
  echo 'Usage: scripts/sign-release.sh <unsigned.apk> <unsigned.aab> <output-prefix>' >&2
  exit 2
fi
APK="$1"; AAB="$2"; PREFIX="$3"
: "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD}"
: "${KEY_ALIAS:?Set KEY_ALIAS}"
KEY_PASSWORD="${KEY_PASSWORD:-$KEYSTORE_PASSWORD}"
export KEY_PASSWORD
if [[ ! -s "$APK" || ! -s "$AAB" ]]; then echo 'Missing unsigned APK or AAB' >&2; exit 1; fi
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_TOOLS_VERSION="$(sed -n 's/^BUILD_TOOLS_VERSION=//p' "$ROOT/android-sdk.properties")"
ANDROID_SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_HOME or ANDROID_SDK_ROOT}"
TOOLS="$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION"
test -x "$TOOLS/zipalign" && test -x "$TOOLS/apksigner" || { echo 'Missing build tools' >&2; exit 1; }
mkdir -p "$(dirname "$PREFIX")"
SIGNED_APK="${PREFIX}.apk"
SIGNED_AAB="${PREFIX}.aab"
tmp_keystore=''
cleanup() {
  rc=$?
  if [[ -n "$tmp_keystore" ]]; then rm -f "$tmp_keystore"; fi
  if (( rc != 0 )); then rm -f "$SIGNED_APK" "$SIGNED_AAB" "${SIGNED_APK}.idsig"; fi
}
trap cleanup EXIT
if [[ -n "${KEYSTORE_BASE64:-}" ]]; then
  tmp_keystore="$(mktemp)"
  chmod 600 "$tmp_keystore"
  printf '%s' "$KEYSTORE_BASE64" | base64 --decode > "$tmp_keystore"
  KS="$tmp_keystore"
else
  KS="${KEYSTORE_FILE:-$ROOT/release-keystore.jks}"
fi
[[ -s "$KS" ]] || { echo 'No keystore available' >&2; exit 1; }
"$TOOLS/zipalign" -f -p 4 "$APK" "$SIGNED_APK"
"$TOOLS/apksigner" sign \
  --ks "$KS" --ks-pass env:KEYSTORE_PASSWORD \
  --ks-key-alias "$KEY_ALIAS" --key-pass env:KEY_PASSWORD \
  "$SIGNED_APK"
"$TOOLS/apksigner" verify --verbose "$SIGNED_APK"
jarsigner -keystore "$KS" -storepass:env KEYSTORE_PASSWORD -keypass:env KEY_PASSWORD \
  -signedjar "$SIGNED_AAB" "$AAB" "$KEY_ALIAS"
jarsigner -verify -verbose -certs "$SIGNED_AAB" >/dev/null
rm -f "${SIGNED_APK}.idsig"
echo "Verified signed outputs: $SIGNED_APK and $SIGNED_AAB"
