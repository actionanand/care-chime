# Reminder scheduling model

## Implementation status

Implemented as **pure Kotlin domain code** in `app/src/main/java/.../domain/schedule` with JVM unit tests (`ScheduleCalculatorTest`, `DoseLedgerTest`). Nothing is persisted or scheduled with Android yet: there are **no alarms, receivers, notifications or permissions** in this build.

- `ScheduleRule` (`FixedTimes`, `RollingInterval`, `MealRelative`, `OneTime`, `AsNeeded`) with `RULE_FORMAT_VERSION = 1` for the future versioned JSON form shared with the Angular app.
- `ScheduleRuleValidator`: technical consistency only (non-empty/unique times, rolling must be `prescriptionConfirmed`, meal offset 0–3 h and 0 for "with meal", positive PRN interval). It does not validate clinical correctness.
- `ScheduleCalculator(clock)`: returns at most **one** `NextReminder.Due(dueAt, basis, overdue)` or `NextReminder.None(reason, earliestEligibleAt?)`. Time zone comes from the schedule; the clock is injected.
  - Fixed and meal-relative: next occurrence strictly after now inside the course window; late or early administration never shifts them. DST gaps shift forward once; DST overlaps fire once.
  - Rolling: `lastAdministeredAt + interval` (from the actual confirmed time), else the planned first dose. A long gap yields a single overdue due time — never multiple or catch-up doses.
  - One-time: due if in the future and not already recorded.
  - PRN: never a reminder; `earliestEligibleAt` is an informational guardrail only.
  - Paused, completed or discontinued courses produce no reminder.
- `DoseLedger(clock, idGenerator)`: append-only `DoseEvent`s with separate `administeredAt` and `recordedAt`; `actionToken` makes every action idempotent; a second Taken on a taken dose is rejected; skipped doses need a correction (future) rather than a silent change; snoozing never changes `plannedAt`; future administration times are rejected.

Next phase: persist schedules, occurrences and events (Room v2 + migration), then AlarmManager/receivers/notification channels with permission handling and device testing.

## Separate schedule types

- **Fixed clock schedule**: prescribed times do not shift simply because confirmation is late.
- **Rolling interval**: a configured dose may calculate next eligible alarm from **actual last dose administration time**; use only if instructions allow this schedule.
- **Meal-relative**: use configured meal times and prescribed offsets, not assumptions.
- **As-needed (PRN)**: earliest next permitted time is a guardrail, **not** a required or automatic dose reminder.
- **One-off/course schedule**: specific dates, range, end date, pause and stop rules.

Never convert every 6/12 hour medication into rolling mode by default. Missed doses, maximum daily doses, and clinical contraindications depend on the prescription and medicine; the app must not suggest clinical actions on its own.

## Dose occurrence lifecycle

`PLANNED -> DUE -> (TAKEN | SKIPPED | MISSED)` plus separate `SNOOZED` reminder state; a stopped schedule cancels future alarms but **does not erase history**. A late dose entered retrospectively has separate `administeredAt` vs `recordedAt` timestamps. Correction creates an audit event rather than silently changing history.

A `Taken` notification action must be idempotent: same occurrence ID and action token cannot record two administrations. Every schedule edit changes a revision number, atomically cancels obsolete pending intents and reschedules only current occurrences.

## Rolling example

Prescription-configured rolling interval: 6 hours. Planned 06:00, actual administration 07:00, next calculated reminder 13:00. This is a software example, **not advice to give paracetamol or any other medicine every six hours**. Prefer data representing interval duration plus prescription-specific daily cap and clinical review fields; do not infer safe doses.

## Recovery

- After confirmed dose: persist event **first** in a transaction, compute the next occurrence, persist schedule state, then apply Android OS alarms.
- After reboot/update/timezone/permission changes: reconcile from stored records; avoid duplicate alarms and retain missed timestamps.
- After force-stop, powered-off phone or permission denial: do not claim a guaranteed alarm. Inform the user when reliability settings are missing.
- Notification actions: Taken, Snooze, Skip, Pause/Stop with explicit confirmations for destructive or course-ending actions.
- Tests: midnight rollover, daylight saving, travel, dose entries in past, skipped dose, double taps, exact alarm disabled, notification permission denied, doze, restart, restore.

## Platform

Android `AlarmManager` for OS-scheduled due alarms where permitted; `BroadcastReceiver` for delivery; notification permission and exact-alarm permission handled with alternatives. WorkManager for non-urgent reconciliation and maintenance. Investigate policy constraints around full-screen alarms; don't claim always-on display behavior.
