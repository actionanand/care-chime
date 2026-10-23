# Care Chime — Coding agent instructions

These instructions govern Codex, GitHub Copilot and other coding assistants working in this repo. **Read `docs/ANDROID.md`, `docs/ARCHITECTURE.md`, `docs/DATA_MODEL.md`, `docs/REMINDER_ENGINE.md`, `docs/SECURITY.md` and `docs/ROADMAP.md` before making implementation changes.**

## Immutable project facts

- Name: **Care Chime**. Tagline: **Care, right on time.**
- Repository: `actionanand/care-chime`
- Production applicationId and Kotlin package: **`com.actionanand.carechime.app`**.
- Native Android Kotlin + Jetpack Compose, offline-first; Angular web is later.
- `main-android` is the CI release branch.
- Keep `app-config.json`, `android-version.json`, `android-sdk.properties`, `releases/`, `branding/` and `docs/ANDROID.md` consistent.

## Implementation discipline

1. **Never copy LocalTell's domain-specific code**. Use LocalTell's release mechanics as a reference only. Do not import its location, tower, Dropbox or journey features.
2. Use pure domain logic for scheduling and Room for durable medical data. Add tests and stable migration paths before schemas change.
3. Maintain an auditable, idempotent log of dose occurrences and actual administrations; never silently rewrite dose history.
4. Treat fixed schedule, rolling interval, meal-based and PRN reminders distinctly. Do not recommend medication or dose amounts.
5. Add Android exact alarm and notification permissions only when implementing those features; handle denial, restarts, time changes and device limits.
6. No fake buttons or placeholder success states; every exposed control must work as described.
7. No internet/server dependency by default. Encrypted backup and web interoperability must have a versioned spec and tests.
8. Never log personal medical content or commit keystores, passwords, user backup files or database dumps.
9. Ensure GitHub workflow only releases successful builds, signs when secrets exist, and auto-increments versionCode on `main-android` without changing versionName.
10. Update README, docs/ANDROID.md, related design docs and tests **in the same change** whenever behavior or release steps change.

## Definition of done for any feature

- Works offline where applicable, has tests for intended and edge-case behavior, no misleading clinical claims.
- Validates persisted data, handles errors, accessibility and state restoration.
- Gradle lint/unit tests pass in GitHub Actions (`android-ci.yml`); relevant Android device testing performed if alarm-related.
- `main-android` CI remains valid; no accidental application ID or signing key change.

## Build verification policy (permanent)

1. Coding agents (Codex, Copilot, etc.) may implement features **without local Android build tools**. A missing Android SDK, Android Studio, Gradle 9.5 or JDK 21 is **not** a reason to stop development or to spend time installing them.
2. Android compilation, Android Lint, Android unit tests (including Robolectric Room tests), and APK/AAB generation are validated by **GitHub Actions**: `.github/workflows/android-ci.yml` for feature branches/PRs (no version bump, no release) and `.github/workflows/android-build.yml` for `main-android` releases.
3. Locally, run every check that does not need the Android SDK: `python3 scripts/validate-config.py`, `python3 -m unittest discover -s scripts/tests -v`, `python3 scripts/check-source-policy.py`, JSON/XML/YAML parsing and script syntax checks.
4. Keep both workflows fully operational and able to fail on real compile, lint, test or packaging errors. **Never remove or weaken build checks** to make a run pass.
5. Never report a feature as tested, compiled or working without evidence. Report Android checks that did not run locally as **"Not locally verified — pending GitHub Actions"**, and uncompiled code as uncompiled.
6. Update documentation in the same change whenever implementation logic, schema, build or release behavior changes.

## First development sequence

1. Verify builds in GitHub Actions (`android-ci.yml`) and on a physical Android device using the CI debug APK; fix any release tool incompatibilities.
2. ~~Implement Profile/Hospital/Condition/Allergy Room schema and CRUD screens.~~ Done (schema v1).
3. ~~Implement a medication catalog and profile-specific course model.~~ Done (schema v1).
4. Schedule rule engine + unit tests are implemented as pure domain code; next persist schedules/dose events (schema v2 + migration) before OS alarm plumbing.
5. Add secure backup format after data model foundations are stable.
