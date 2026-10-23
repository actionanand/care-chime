# Care Chime Android build, signing, and release guide

This file is the **authoritative Android build guide** and must be updated with each build-system change. Release conventions are adapted from [LocalTell's `docs/ANDROID.md`](https://github.com/actionanand/local-tell/blob/master/docs/ANDROID.md).

## Identity and build configuration

| File | Purpose |
| --- | --- |
| `app-config.json` | `applicationId`, app name, tagline, release file prefix |
| `android-version.json` | Android `versionCode` and public `versionName` |
| `android-sdk.properties` | Android SDK and build tools versions |
| `app/build.gradle.kts` | Native Compose app using those root files |
| `.github/workflows/android-build.yml` | Validate, test, build, sign, publish, commit artifacts |
| `scripts/bump-android-version.py` | Safe code increment; optionally update name |
| `scripts/sign-release.sh` | Sign and verify APK and AAB |
| `scripts/generate-keystore.mjs` | Generate PKCS12 signing material locally |
| `scripts/detect-keystore-format.mjs` | Inspect keystore with `keytool` |
| `scripts/build-android.sh` | Local Gradle build helper |

Application ID: **`com.actionanand.carechime.app`**. Do not change after publication. Debug builds use suffix `.debug` and can be installed alongside a release build.

The starter SDK values follow the current LocalTell reference: min SDK 26, compile SDK 37, target SDK 36, build tools 36.0.0. Confirm installed SDK availability and Play policies at release time; update **only** through `android-sdk.properties`.

## Local builds

Install **JDK 21**, **Gradle 9.5.0**, Android SDK platforms and build tools listed above. Android Studio can open the repository root.

```bash
python3 scripts/validate-config.py
python3 -m unittest discover -s scripts/tests -v
bash scripts/build-android.sh debug
bash scripts/build-android.sh release
```

Equivalent Gradle tasks:

```bash
gradle lint testDebugUnitTest
gradle clean assembleRelease bundleRelease
```

Release compilation produces **unsigned** APK/AAB under `app/build/outputs/`; they are not a substitute for a properly signed distribution. A Gradle wrapper is not included, matching the LocalTell reference convention.

## `main-android` branch automation

The workflow runs on pushes to `main-android` or manual dispatch **from that branch**.

1. Check out `main-android`, prepare JDK 21, Android SDK, Gradle 9.5.0.
2. Run config checks, script unit tests, Android lint and unit tests.
3. Increment `versionCode` **for the planned build** (leave `versionName` unchanged).
4. Build the new version of both release APK and AAB.
5. Replace older APK/AAB artifacts in `releases/` (retain latest files only (Git history still grows)).
6. If all required secrets exist, sign and verify APK and AAB. If none exist, leave files explicitly `-unsigned`. Incomplete signing configuration fails.
7. Upload to Actions artifacts and then commit new `android-version.json` and `releases/` files in **one `[skip ci]` commit**.
8. Push without force. If branch changed mid-run, fail instead of attaching stale binaries to newer code.

A failed build **does not commit** a version increment. `versionCode` only persists after a completed build and validation. Manual builds do not auto-increment; run `python3 scripts/bump-android-version.py` first when preparing local releases that need a higher code.

Create the branch once if it does not yet exist:

```bash
git clone https://github.com/actionanand/care-chime.git
cd care-chime
git checkout -b main-android
git push -u origin main-android
```

If `main-android` already exists, switch to it instead; do not overwrite it. Repository Settings → Actions → General must allow the workflow to write repository contents. Branch protection/rulesets must explicitly permit the intended bot commit workflow or switch to a PR-based release process.

## Generate signing keystore

On a **trusted machine** with Node.js and OpenSSL installed, choose a strong keystore password. Do not place a production password in shell command arguments or shell history.

```bash
# Set KEYSTORE_PASSWORD via your secure shell secret mechanism (do not commit it).
npm run generate-keystore
npm run keystore:type
```

This generates `release-keystore.jks` (PKCS12, alias **`carechime`**) and `keystore.b64.txt` (single-line Base64 encoding). Both are gitignored. Securely back up the keystore and its password offline; the Base64 file is **not** encrypted and must be treated as secret.

Set these under **GitHub repo → Settings → Secrets and variables → Actions**:

| Secret | Required value |
| --- | --- |
| `KEYSTORE_BASE64` | Complete one-line contents of `keystore.b64.txt` |
| `KEYSTORE_PASSWORD` | Keystore store password |
| `KEY_ALIAS` | `carechime` |
| `KEY_PASSWORD` | Usually same as store password for this PKCS12 key; if absent, script defaults to store password |

Missing **all** secrets = explicitly unsigned build. Missing **some** required secrets = failed build. Never upload or check in a private key, `.jks`, signing password or Base64 secret.

## Sign manually

After a successful local release build:

```bash
export KEYSTORE_FILE="$PWD/release-keystore.jks"
export KEY_ALIAS=carechime
# Set KEYSTORE_PASSWORD and KEY_PASSWORD through secure means.
# This example assumes the default filenames produced by Gradle:
bash scripts/sign-release.sh   app/build/outputs/apk/release/app-release-unsigned.apk   app/build/outputs/bundle/release/app-release.aab   releases/CareChime-0.1.0-local
```

The sign script uses Android `zipalign`, `apksigner` and Java `jarsigner`, then verifies both signatures. Never assume unsigned APK/AAB files can be distributed or installed as release builds.

## Initial release checklist

- Validate production keystore backups and signer alias.
- Test a signed APK installation on a physical device, then an **upgrade** from a previously signed build with the same key.
- Confirm `applicationId`, version name/code and compiled SDK versions.
- Ensure medication alarms are described as unavailable until implementation and testing.
- Review Play permissions and health-app policies separately before public distribution.
- Never commit real patient data, backup exports or personal medical attachments.

## Known limitations of this scaffold

- No real medical reminder scheduling has been implemented or tested.
- The APK/AAB CI steps are structured but **have not been executed in this environment**; validate on GitHub Actions and adjust dependencies/SDK where required.
- Generated branding assets are conceptual and should be approved for public branding before release.
