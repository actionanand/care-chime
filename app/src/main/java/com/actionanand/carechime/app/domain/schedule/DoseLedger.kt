package com.actionanand.carechime.app.domain.schedule

import java.time.Clock
import java.time.Duration
import java.time.Instant

enum class DoseOccurrenceStatus { PLANNED, DUE, TAKEN, SKIPPED, MISSED }

enum class DoseEventType { TAKEN, SKIPPED, SNOOZED, MARKED_MISSED }

/** One planned dose. [plannedAt] never changes after creation; snoozing only sets [snoozedUntil]. */
data class DoseOccurrence(
    val id: String,
    val scheduleId: String,
    val scheduleRevision: Int,
    val plannedAt: Instant,
    val status: DoseOccurrenceStatus,
    val snoozedUntil: Instant? = null,
)

/** Append-only audit event. [administeredAt] (actual) is distinct from [recordedAt] (entry time). */
data class DoseEvent(
    val id: String,
    val occurrenceId: String,
    val type: DoseEventType,
    /** Idempotency key, e.g. one per notification action; replays return the original event. */
    val actionToken: String,
    val recordedAt: Instant,
    val administeredAt: Instant? = null,
    val snoozedUntil: Instant? = null,
)

sealed interface DoseActionResult {
    data class Applied(val occurrence: DoseOccurrence, val event: DoseEvent) : DoseActionResult
    data class Duplicate(val existing: DoseEvent) : DoseActionResult
    data class Rejected(val reason: String) : DoseActionResult
}

/**
 * Pure state transitions for dose occurrences. Persisting the returned event and occurrence
 * atomically (and only then rescheduling alarms) is the data layer's responsibility.
 */
class DoseLedger(
    private val clock: Clock,
    private val newId: () -> String,
) {
    fun markTaken(
        occurrence: DoseOccurrence,
        history: List<DoseEvent>,
        actionToken: String,
        administeredAt: Instant,
    ): DoseActionResult {
        duplicateOf(occurrence, history, actionToken)?.let { return DoseActionResult.Duplicate(it) }
        val now = clock.instant()
        return when {
            occurrence.status == DoseOccurrenceStatus.TAKEN ->
                DoseActionResult.Rejected("This dose is already recorded as taken.")
            occurrence.status == DoseOccurrenceStatus.SKIPPED ->
                DoseActionResult.Rejected("This dose was recorded as skipped. Add a correction instead.")
            administeredAt.isAfter(now.plus(FUTURE_TOLERANCE)) ->
                DoseActionResult.Rejected("The administration time cannot be in the future.")
            else -> transition(occurrence, DoseOccurrenceStatus.TAKEN, event(occurrence, DoseEventType.TAKEN, actionToken, now, administeredAt = administeredAt))
        }
    }

    fun skip(occurrence: DoseOccurrence, history: List<DoseEvent>, actionToken: String): DoseActionResult {
        duplicateOf(occurrence, history, actionToken)?.let { return DoseActionResult.Duplicate(it) }
        return when (occurrence.status) {
            DoseOccurrenceStatus.TAKEN -> DoseActionResult.Rejected("A taken dose cannot be skipped.")
            DoseOccurrenceStatus.SKIPPED -> DoseActionResult.Rejected("This dose is already skipped.")
            else -> transition(occurrence, DoseOccurrenceStatus.SKIPPED, event(occurrence, DoseEventType.SKIPPED, actionToken, clock.instant()))
        }
    }

    fun snooze(
        occurrence: DoseOccurrence,
        history: List<DoseEvent>,
        actionToken: String,
        until: Instant,
    ): DoseActionResult {
        duplicateOf(occurrence, history, actionToken)?.let { return DoseActionResult.Duplicate(it) }
        val now = clock.instant()
        if (occurrence.status != DoseOccurrenceStatus.PLANNED && occurrence.status != DoseOccurrenceStatus.DUE) {
            return DoseActionResult.Rejected("Only an upcoming or due dose can be snoozed.")
        }
        if (!until.isAfter(now)) return DoseActionResult.Rejected("Snooze time must be in the future.")
        val snoozeEvent = event(occurrence, DoseEventType.SNOOZED, actionToken, now, snoozedUntil = until)
        return DoseActionResult.Applied(occurrence.copy(snoozedUntil = until), snoozeEvent)
    }

    /** System transition after the reminder window passes. Never creates a replacement dose. */
    fun markMissed(occurrence: DoseOccurrence, history: List<DoseEvent>, actionToken: String): DoseActionResult {
        duplicateOf(occurrence, history, actionToken)?.let { return DoseActionResult.Duplicate(it) }
        if (occurrence.status != DoseOccurrenceStatus.PLANNED && occurrence.status != DoseOccurrenceStatus.DUE) {
            return DoseActionResult.Rejected("Only an upcoming or due dose can be marked missed.")
        }
        return transition(occurrence, DoseOccurrenceStatus.MISSED, event(occurrence, DoseEventType.MARKED_MISSED, actionToken, clock.instant()))
    }

    private fun duplicateOf(occurrence: DoseOccurrence, history: List<DoseEvent>, token: String): DoseEvent? =
        history.firstOrNull { it.occurrenceId == occurrence.id && it.actionToken == token }

    private fun transition(occurrence: DoseOccurrence, status: DoseOccurrenceStatus, event: DoseEvent) =
        DoseActionResult.Applied(occurrence.copy(status = status, snoozedUntil = null), event)

    private fun event(
        occurrence: DoseOccurrence,
        type: DoseEventType,
        token: String,
        recordedAt: Instant,
        administeredAt: Instant? = null,
        snoozedUntil: Instant? = null,
    ) = DoseEvent(newId(), occurrence.id, type, token, recordedAt, administeredAt, snoozedUntil)

    companion object {
        val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(1)

        /** Latest actual administration, used as the anchor for rolling-interval schedules. */
        fun lastAdministeredAt(events: List<DoseEvent>): Instant? =
            events.filter { it.type == DoseEventType.TAKEN }.mapNotNull { it.administeredAt }.maxOrNull()
    }
}
