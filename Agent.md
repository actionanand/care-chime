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
- Gradle lint/unit tests pass; relevant Android device testing performed if alarm-related.
- `main-android` CI remains valid; no accidental application ID or signing key change.

## First development sequence

1. Verify starter builds in GitHub Actions / physical Android device; fix any release tool incompatibilities.
2. Implement Profile/Hospital/Condition/Allergy Room schema and CRUD screens.
3. Implement a medication catalog and profile-specific course model.
4. Implement schedule rule engine + unit tests before OS alarm plumbing.
5. Add secure backup format after data model foundations are stable.
