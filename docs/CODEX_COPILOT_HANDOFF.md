# Ready-to-use Codex / GitHub Copilot handoff prompt

You are continuing a new project called **Care Chime** from an initial, build-oriented starter scaffold.

Project identity (immutable):
- Repo: `https://github.com/actionanand/care-chime`
- App name: `Care Chime`
- Tagline: `Care, right on time.`
- Android application ID: `com.actionanand.carechime.app`
- Release branch: `main-android`
- LocalTell reference: `https://github.com/actionanand/local-tell`
- Build reference: `https://github.com/actionanand/local-tell/blob/master/docs/ANDROID.md`

First read `Agent.md`, `AGENTS.md`, README and every `docs/*.md`, particularly `docs/ANDROID.md`, `docs/REMINDER_ENGINE.md`, `docs/DATA_MODEL.md` and `docs/SECURITY.md`.

**Task 1 — Verify the scaffold, don't jump ahead:**
1. Compare the starter against existing care-chime repo, preserve the user's own commits and branch conventions, and migrate/import scaffold carefully. Do not overwrite existing work.
2. Confirm project compiles on JDK 21 / Gradle 9.5 / SDK defined in `android-sdk.properties`. Run unit tests, lint, assembleDebug, assembleRelease and bundleRelease. Fix actual compatibility problems if necessary.
3. Verify `main-android` GitHub Actions: successful APK/AAB generation, source-of-truth JSON configs, atomic versionCode+artifact commit only after success, versionName unchanged, verified keystore signing and correct `releases/` contents. If signing secrets aren't set, publish explicit `-unsigned` outputs only.
4. Verify branded PNGs display correctly in app icon and README. Refine adaptive launcher icon/round assets if needed; avoid a white border.
5. Update `docs/ANDROID.md` with every build/signing/CI change.

**Task 2 — Implement small end-to-end vertical slice:**
1. Set up Room schema, migrations, repository interfaces and Compose navigation for multiple named profiles.
2. Profile fields: DOB optional and calculated age if supplied, optional approximate age, relationship, blood group; unlimited hospitals with hospital name, patient ID, place name and optional Google Maps URL.
3. Add allergies, pre-existing illnesses/conditions and dated medical history (data entry only, no diagnoses).
4. Implement a general medicine catalog independent of profiles, and separate profile-specific medication courses (tablet/drops/syrup/injection, generic/salt info, concentration, dose entered from prescription, route, notes).
5. Do NOT implement unreviewed dosing recommendations, fake notifications or cloud data collection.
6. Add migration/unit tests and user-friendly validation; keep all data on device.

**Task 3 — Reminders only after domain model is tested:**
1. Implement pure fixed-time, rolling-interval, meal-relative, one-time and PRN rule calculations with clock injection and strong tests.
2. Separate due time and actual administered time; preserve immutable dose events and prevent duplicate Taken taps.
3. Add Android AlarmManager + receivers + notification channels/actions, exact alarm and notification permissions, boot/timezone recovery, permission warning, and cancel/pause/stop.
4. Shift the next reminder from actual taken time **only for prescription-configured rolling schedules**. Fixed times never shift automatically. Do not double/catch-up-dose after missed medication.
5. Test real devices and restricted battery modes, and document known Android limitations.

**Task 4 — Further features:**
Water reminder and logging; vaccination history and under-five UI; secure versioned authenticated encrypted import/export portable to future Angular web app; prescription attachments; future web schemas.

Before each change: state which files will change, implement with tests, and report exactly what passed and what couldn't be verified. Never claim unimplemented medication reminders work. Keep release scripts, docs and configs in sync.
