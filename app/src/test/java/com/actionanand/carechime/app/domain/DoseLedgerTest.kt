package com.actionanand.carechime.app.domain

import com.actionanand.carechime.app.domain.schedule.DoseActionResult
import com.actionanand.carechime.app.domain.schedule.DoseEvent
import com.actionanand.carechime.app.domain.schedule.DoseEventType
import com.actionanand.carechime.app.domain.schedule.DoseLedger
import com.actionanand.carechime.app.domain.schedule.DoseOccurrence
import com.actionanand.carechime.app.domain.schedule.DoseOccurrenceStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoseLedgerTest {
    private val now = Instant.parse("2026-10-23T08:00:00Z")
    private var counter = 0
    private val ledger = DoseLedger(Clock.fixed(now, ZoneOffset.UTC)) { "event-${++counter}" }
    private val planned = now.minus(Duration.ofHours(1))
    private val occurrence = DoseOccurrence("occ-1", "sched-1", 1, planned, DoseOccurrenceStatus.DUE)

    private fun applied(result: DoseActionResult) =
        result as? DoseActionResult.Applied ?: throw AssertionError("Expected Applied but was $result")

    @Test fun takenRecordsActualTimeSeparatelyFromPlannedAndRecordedTime() {
        val actual = now.minus(Duration.ofMinutes(20))
        val result = applied(ledger.markTaken(occurrence, emptyList(), "tap-1", actual))
        assertEquals(DoseOccurrenceStatus.TAKEN, result.occurrence.status)
        assertEquals(planned, result.occurrence.plannedAt)
        assertEquals(actual, result.event.administeredAt)
        assertEquals(now, result.event.recordedAt)
        assertEquals(DoseEventType.TAKEN, result.event.type)
    }

    @Test fun replayedActionTokenReturnsOriginalEvent() {
        val first = applied(ledger.markTaken(occurrence, emptyList(), "tap-1", now))
        val replay = ledger.markTaken(first.occurrence, listOf(first.event), "tap-1", now)
        assertEquals(DoseActionResult.Duplicate(first.event), replay)
    }

    @Test fun secondTakenTapWithNewTokenIsRejected() {
        val first = applied(ledger.markTaken(occurrence, emptyList(), "tap-1", now))
        val second = ledger.markTaken(first.occurrence, listOf(first.event), "tap-2", now)
        assertTrue(second is DoseActionResult.Rejected)
    }

    @Test fun futureAdministrationTimeIsRejected() {
        val result = ledger.markTaken(occurrence, emptyList(), "tap-1", now.plus(Duration.ofMinutes(5)))
        assertTrue(result is DoseActionResult.Rejected)
    }

    @Test fun missedDoseCanBeRecordedLateButNeverDuplicated() {
        val missed = applied(ledger.markMissed(occurrence, emptyList(), "system-1"))
        assertEquals(DoseOccurrenceStatus.MISSED, missed.occurrence.status)
        val late = applied(ledger.markTaken(missed.occurrence, listOf(missed.event), "tap-1", now))
        assertEquals(DoseOccurrenceStatus.TAKEN, late.occurrence.status)
        assertTrue(ledger.markMissed(late.occurrence, listOf(missed.event, late.event), "system-2") is DoseActionResult.Rejected)
    }

    @Test fun skippedDoseCannotBeTakenWithoutCorrection() {
        val skipped = applied(ledger.skip(occurrence, emptyList(), "skip-1"))
        assertEquals(DoseOccurrenceStatus.SKIPPED, skipped.occurrence.status)
        assertTrue(ledger.markTaken(skipped.occurrence, listOf(skipped.event), "tap-1", now) is DoseActionResult.Rejected)
    }

    @Test fun snoozeKeepsPlannedTime() {
        val until = now.plus(Duration.ofMinutes(10))
        val snoozed = applied(ledger.snooze(occurrence, emptyList(), "snooze-1", until))
        assertEquals(planned, snoozed.occurrence.plannedAt)
        assertEquals(until, snoozed.occurrence.snoozedUntil)
        assertEquals(DoseOccurrenceStatus.DUE, snoozed.occurrence.status)
        val taken = applied(ledger.markTaken(snoozed.occurrence, listOf(snoozed.event), "tap-1", now))
        assertNull(taken.occurrence.snoozedUntil)
        assertTrue(ledger.snooze(occurrence, emptyList(), "snooze-2", now) is DoseActionResult.Rejected)
    }

    @Test fun lastAdministeredAtUsesTakenEventsOnly() {
        val events = listOf(
            DoseEvent("e1", "o1", DoseEventType.TAKEN, "t1", now, administeredAt = now.minus(Duration.ofHours(7))),
            DoseEvent("e2", "o2", DoseEventType.TAKEN, "t2", now, administeredAt = now.minus(Duration.ofHours(1))),
            DoseEvent("e3", "o3", DoseEventType.SKIPPED, "t3", now),
        )
        assertEquals(now.minus(Duration.ofHours(1)), DoseLedger.lastAdministeredAt(events))
        assertNull(DoseLedger.lastAdministeredAt(emptyList()))
    }
}
