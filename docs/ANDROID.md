# Care Chime Android build, signing, and release guide

This file is the **authoritative Android build guide** and must be updated with each build-system change. Release conventions are adapted from [LocalTell's `docs/ANDROID.md`](https://github.com/actionanand/local-tell/blob/master/docs/ANDROID.md).

## Identity and build configuration

| File | Purpose |
| --- | --- |
| `app-config.json` | `applicationId`, app name, tagline, release file prefix |
| `android-version.json` | Android `versionCode` and public `versionName` |
| `android-sdk.properties` | Android SDK and build tools versions |
| `app/build.gradle.kts` | Native Compose app using those root files; Room via KSP |
| `gradle/libs.versions.toml` | Pinned library versions (Room, Navigation, Lifecycle, Robolectric, …) |
| `app/schemas/` | Exported Room schema JSON per database version (migration baseline) |
| `.github/workflows/android-ci.yml` | Feature-branch/PR verification: lint, tests, debug + unsigned release builds; never releases |
| `.github/workflows/android-build.yml` | `main-android` only: validate, test, build, sign, publish, commit artifacts |
| `scripts/bump-android-version.py` | Safe code increment; optionally update name |
| `scripts/check-source-policy.py` | SDK-free static checks: privacy policy, package/imports, delimiters |
| `scripts/sign-release.sh` | Sign and verify APK and AAB |
| `scripts/generate-keystore.mjs` | Generate PKCS12 signing material locally |
| `scripts/detect-keystore-format.mjs` | Inspect keystore with `keytool` |
| `scripts/build-android.sh` | Local Gradle build helper |

Application ID: **`com.actionanand.carechime.app`**. Do not change after publication. Debug builds use suffix `.debug` and can be installed alongside a release build.

The starter SDK values follow the current LocalTell reference: min SDK 26, compile SDK 37, target SDK 36, build tools 36.0.0. Confirm installed SDK availability and Play policies at release time; update **only** through `android-sdk.properties`.

## Toolchain and dependencies

| Component | Version | Notes |
| --- | --- | --- |
| Android Gradle Plugin | 9.3.1 | Built-in Kotlin (no `kotlin-android` plugin), same as LocalTell |
| Kotlin / Compose compiler plugin | 2.4.20 | KGP pinned through `buildscript` classpath |
| KSP | 2.3.12 | Versioned independently of Kotlin; supports AGP 9 built-in Kotlin |
| Room | 2.8.5 | `room-runtime`, `room-ktx`, `room-compiler` (KSP) |
| Navigation Compose | 2.9.8 | String routes |
| Lifecycle | 2.11.0 | ViewModel Compose, SavedStateHandle, `collectAsStateWithLifecycle` |
| Compose BOM | 2026.09.00 | Material 3 + `material-icons-core` |
| Robolectric | 4.17 | JVM Room tests; SDK pinned to 35 in `app/src/test/resources/robolectric.properties` |

Room exports its schema to `app/schemas/<database class>/<version>.json` (`ksp { arg("room.schemaLocation", …) }`). Commit each new schema file. Never edit a committed schema: bump `CareChimeDatabase.VERSION`, add an explicit `Migration` to `CareChimeMigrations.ALL` and a migration test. Destructive fallback migration is forbidden (enforced by `check-source-policy.py`).

## Build verification policy

Coding agents may implement features without a local Android SDK. **Android compilation, lint, unit tests and APK/AAB generation are verified in GitHub Actions**; missing local tooling is not a reason to stop development. Locally, run the SDK-free checks:

```bash
python3 scripts/validate-config.py
python3 -m unittest discover -s scripts/tests -v
python3 scripts/check-source-policy.py
```

Report Android checks that could not run locally as "Not locally verified — pending GitHub Actions". Never weaken workflow checks to obtain a green run.

## Local builds (optional)

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

## Feature-branch CI (`android-ci.yml`)

Runs on every push to a branch other than `main-android`, on pull requests (including PRs into `main-android`) and on manual dispatch. It has read-only repository permission and **never** bumps `versionCode`, signs, commits or writes to `releases/`.

1. Set up JDK 21, Android SDK (command-line tools + Android CLI, see below) and Gradle 9.5.0.
2. Validate config, run script tests and `check-source-policy.py`.
3. `gradle lint testDebugUnitTest` (domain tests + Robolectric Room tests).
4. `gradle assembleDebug assembleRelease bundleRelease`.
5. Fail if a committed Room schema changed; warn if a new schema file was generated but not committed.
6. Upload the Room schemas, the **debug APK** (debug-signed, for device testing only — not a release) and lint/test reports.

## Android SDK installation in CI

`android-actions/setup-android@v4` is called with `packages: ''` because the legacy SDK `tools` package was retired; the exact `platforms/android-<COMPILE_SDK_VERSION>`, `build-tools/<BUILD_TOOLS_VERSION>` and `platform-tools` are then installed with Google's Android CLI (`https://dl.google.com/android/cli/latest/linux_x86_64/install.sh`). This mirrors LocalTell's working workflow and replaces the scaffold's original `setup-android@v3` + `sdkmanager` step.

## `main-android` branch automation

The workflow runs on pushes to `main-android` or manual dispatch **from that branch**.

1. Check out `main-android`, prepare JDK 21, Android SDK (Android CLI, as above), Gradle 9.5.0.
2. Run config checks, script unit tests, `check-source-policy.py`, Android lint and unit tests.
3. Increment `versionCode` **for the planned build** (leave `versionName` unchanged).
4. Build the new version of both release APK and AAB.
5. Replace older APK/AAB artifacts in `releases/` (retain latest files only (Git history still grows)).
6. If all required secrets exist, sign and verify APK and AAB. If none exist, leave files explicitly `-unsigned`. Incomplete signing configuration fails.
7. Upload to Actions artifacts and then commit new `android-version.json` and `releases/` files in **one `[skip ci]` commit**. Lint/test reports are uploaded when a run fails.
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

## Launcher icon

The launcher uses an **adaptive icon** (`res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`; min SDK 26 means every device uses it). The foreground `drawable-nodpi/ic_launcher_foreground.png` is the 400 px centre crop of `branding/app-icon.png` placed on a transparent 600 px canvas, so the artwork exactly fills the visible 72 dp mask and the bell stays within the 66 dp safe zone. The background is the solid brand blue `@color/ic_launcher_background` (`#3878F7`). This removes the white corners the original square PNG showed under circular/squircle masks. `drawable-nodpi/care_chime_mark.png` (same crop) is the in-app logo. `branding/` source files are unchanged.

## Known limitations of this build

- No medication reminder scheduling, alarms or notifications exist yet; the reminder rules are pure domain code with unit tests only.
- Workflows and Kotlin code introduced on `feature/1-basic-setup` were written without a local Android SDK; their compile/lint/test status is established only by the GitHub Actions `android-ci.yml` run.
- The first Room schema JSON (`app/schemas/.../1.json`) is generated by CI and must be downloaded from the `room-schemas` artifact and committed.
- A monochrome (themed) launcher icon layer is not provided yet.
- Generated branding assets are conceptual and should be approved for public branding before release.
