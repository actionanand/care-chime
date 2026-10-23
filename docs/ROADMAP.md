# Phased implementation roadmap

## Phase 0 — This starter
- [x] App identity, Kotlin Compose shell and branding draft
- [x] Root config, versioning script and basic tests
- [x] `main-android` APK/AAB workflow and signing scripts
- [x] Android build documentation and AI instructions
- [x] Feature-branch CI workflow (`android-ci.yml`), LocalTell-style SDK installation, source policy checks
- [x] Adaptive launcher icon without white corners
- [ ] GitHub Actions execution / APK smoke test on a physical device (pending first CI run)

## Phase 1 — Family records (implemented; pending CI verification)
- [x] Room database (schema v1), explicit migration list and profile CRUD
- [x] Schema v2: prescription-time medicine and hospital snapshots (additive migration 1→2 + tests)
- [x] DOB/approximate age (years and months under five), optional relationship and blood group
- [x] Multiple hospitals per profile; hospital IDs, doctor, map links
- [x] Conditions, allergies, medicine allergies and medical history
- [x] Generic medicine library, profile medicines
- [ ] Commit `app/schemas` 1.json (CI artifact from run on `78d13f7`) and 2.json (artifact from the v2 commit); CI fails until both are committed

## Phase 2 — Reminder reliability
- [x] Pure recurrence and interval engine with tests (domain only)
- [x] Fixed, rolling, PRN, one-time and meal-time schedule rules (domain only)
- [x] Idempotent dose event ledger with actual vs recorded time (domain only)
- [ ] Persist schedules, occurrences and dose events (next Room schema version + migration tests)
- [ ] AlarmManager/receivers/channels and permissions
- [ ] Actual-dose UI, snooze, skip, stop/pause actions
- [ ] Boot/timezone/permission/restore recovery tests

## Phase 3 — Extended care
- [ ] Water tracking and optional user-set targets
- [ ] Vaccination history, manual scheduling and child workflow
- [ ] Prescription attachments, appointments and stock warnings

## Phase 4 — Portability and backup
- [ ] Portable versioned export contract and cross-platform test vectors
- [ ] Password-derived authenticated encryption
- [ ] Atomic, validated import and recovery tests

## Phase 5 — Angular web companion
- [ ] Client-side encrypted file import/edit/export
- [ ] Optional future synchronization design; never require cloud accounts for native alarms
