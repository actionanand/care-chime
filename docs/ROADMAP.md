# Phased implementation roadmap

## Phase 0 — This starter
- [x] App identity, Kotlin Compose shell and branding draft
- [x] Root config, versioning script and basic tests
- [x] `main-android` APK/AAB workflow and signing scripts
- [x] Android build documentation and AI instructions
- [ ] GitHub Actions execution / APK smoke test (requires Android SDK CI)

## Phase 1 — Family records
- [ ] Room database, migrations and profile CRUD
- [ ] DOB/approximate age, optional relationship and blood group
- [ ] Multiple hospitals per profile; hospital IDs, map links
- [ ] Conditions, allergies, medicine allergies and medical history
- [ ] Generic medicine library, profile medicine courses

## Phase 2 — Reminder reliability
- [ ] Pure recurrence and interval engine with tests
- [ ] Fixed, rolling, PRN and meal-time schedule rules
- [ ] AlarmManager/receivers/channels and permissions
- [ ] Actual-dose event, snooze, skip, stop/pause
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
