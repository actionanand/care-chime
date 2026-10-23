# Architecture and module boundaries

## Release 0: Bootstrap

Native Android Kotlin + Jetpack Compose shell under `app/`, Gradle at repo root. No cloud backend, login, patient storage, alarms or backup implementation yet. The generated UI intentionally identifies itself as a starter.

## Planned Android layers

- `ui/`: screens, composables, navigation and accessibility.
- `domain/`: validated scheduling use cases and **pure** decision logic; no Android framework dependencies.
- `data/`: Room entities/DAOs, migrations, repository interfaces and settings.
- `reminders/`: AlarmManager scheduling, BroadcastReceivers, notification channels/actions and rescheduling.
- `backup/`: versioned portable encrypted export/import, password handling and transactional restore.
- `security/`: local PIN/biometric gate, private local storage and safe logs.

Use interfaces to keep scheduling decision logic testable without device clocks. Inject `Clock` and timezone references. Use lifecycle-aware Compose flows and avoid an application-wide mutable singleton medical database.

## Offline-first

Android is authoritative for alarm delivery and dose events when offline. Preserve original occurrence IDs, planned due time, actual time, timezone, user confirmation time and status. WorkManager is for reconciliation, not precise medicine alarm dispatch.

## Companion web

The future Angular app should use a shared, **documented versioned data schema**; do not open Android's private Room DB directly. Encrypted backup import/export can work without an account. Real-time multi-device sync is a separate later project requiring authentication, end-to-end cryptography decisions, reconciliation and delivery acknowledgement.

## Future tests

Room migration tests, property-based recurrence tests, notification idempotency, boot/permission/timezone transitions, concurrent dose logging, app upgrade and recovery, integrity tests and encrypted backup roundtrips with test fixtures.
