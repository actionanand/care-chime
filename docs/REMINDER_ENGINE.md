# Reminder scheduling model (design, not yet implemented)

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
