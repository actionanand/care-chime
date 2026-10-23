package com.actionanand.carechime.app.domain.schedule

import com.actionanand.carechime.app.domain.model.CourseStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class DueBasis { FIXED_TIME, ACTUAL_ADMINISTRATION, PLANNED_FIRST_DOSE, MEAL_TIME, ONE_TIME }

enum class NoReminderReason {
    COURSE_NOT_ACTIVE,
    COURSE_ENDED,
    AS_NEEDED,
    ONE_TIME_PASSED,
    ALREADY_RECORDED,
    INVALID_RULE,
    MEAL_TIMES_NOT_SET,
}

sealed interface NextReminder {
    /**
     * A single next due time. When [overdue] is true the alarm layer shows one "due" reminder;
     * it must never create extra or catch-up doses for missed time.
     */
    data class Due(val dueAt: Instant, val basis: DueBasis, val overdue: Boolean) : NextReminder

    /** No reminder. [earliestEligibleAt] is informational only (e.g. PRN guardrail), never a prompt. */
    data class None(val reason: NoReminderReason, val earliestEligibleAt: Instant? = null) : NextReminder
}

class ScheduleCalculator(private val clock: Clock) {

    /**
     * @param lastAdministeredAt actual time of the most recent confirmed administration for this
     *   schedule (from immutable dose events), or null when none was recorded.
     */
    fun next(
        schedule: MedicationSchedule,
        lastAdministeredAt: Instant?,
        mealTimes: MealTimes? = null,
    ): NextReminder {
        if (schedule.status != CourseStatus.ACTIVE) return NextReminder.None(NoReminderReason.COURSE_NOT_ACTIVE)
        if (ScheduleRuleValidator.validate(schedule.rule).isNotEmpty()) {
            return NextReminder.None(NoReminderReason.INVALID_RULE)
        }
        val now = clock.instant()
        val zone = schedule.zoneId
        return when (val rule = schedule.rule) {
            is ScheduleRule.FixedTimes -> nextDaily(schedule.window, zone, now, DueBasis.FIXED_TIME) { date ->
                rule.times.map { LocalDateTime.of(date, it) }
            }
            is ScheduleRule.MealRelative -> {
                if (mealTimes == null) return NextReminder.None(NoReminderReason.MEAL_TIMES_NOT_SET)
                nextDaily(schedule.window, zone, now, DueBasis.MEAL_TIME) { date ->
                    rule.meals.map { meal ->
                        val mealAt = LocalDateTime.of(date, mealTimes.timeOf(meal))
                        when (rule.relation) {
                            MealRelation.BEFORE -> mealAt.minus(rule.offset)
                            MealRelation.WITH -> mealAt
                            MealRelation.AFTER -> mealAt.plus(rule.offset)
                        }
                    }
                }
            }
            is ScheduleRule.RollingInterval -> nextRolling(rule, schedule.window, zone, now, lastAdministeredAt)
            is ScheduleRule.OneTime -> {
                if (lastAdministeredAt != null) return NextReminder.None(NoReminderReason.ALREADY_RECORDED)
                val dueAt = rule.at.atZone(zone).toInstant()
                if (dueAt.isAfter(now)) {
                    NextReminder.Due(dueAt, DueBasis.ONE_TIME, overdue = false)
                } else {
                    NextReminder.None(NoReminderReason.ONE_TIME_PASSED)
                }
            }
            is ScheduleRule.AsNeeded -> NextReminder.None(
                NoReminderReason.AS_NEEDED,
                earliestEligibleAt = rule.minimumInterval?.let { interval -> lastAdministeredAt?.plus(interval) },
            )
        }
    }

    private fun nextRolling(
        rule: ScheduleRule.RollingInterval,
        window: CourseWindow,
        zone: ZoneId,
        now: Instant,
        lastAdministeredAt: Instant?,
    ): NextReminder {
        val (dueAt, basis) = if (lastAdministeredAt != null) {
            lastAdministeredAt.plus(rule.interval) to DueBasis.ACTUAL_ADMINISTRATION
        } else {
            rule.firstDoseAt.atZone(zone).toInstant() to DueBasis.PLANNED_FIRST_DOSE
        }
        val dueDate = dueAt.atZone(zone).toLocalDate()
        val end = window.endDateInclusive
        if (end != null && dueDate.isAfter(end)) return NextReminder.None(NoReminderReason.COURSE_ENDED)
        return NextReminder.Due(dueAt, basis, overdue = dueAt.isBefore(now))
    }

    /** Earliest wall-clock occurrence strictly after [now] whose calendar day is inside the course. */
    private fun nextDaily(
        window: CourseWindow,
        zone: ZoneId,
        now: Instant,
        basis: DueBasis,
        occurrencesOn: (LocalDate) -> List<LocalDateTime>,
    ): NextReminder {
        val today = now.atZone(zone).toLocalDate()
        val end = window.endDateInclusive
        // Start one day early so meal offsets that cross midnight are not skipped.
        val yesterday = today.minusDays(1)
        val firstDate = if (window.startDate.isAfter(yesterday)) window.startDate else yesterday
        val lastDate = firstDate.plusDays(3)
        val candidates = mutableListOf<Instant>()
        var date = firstDate
        while (!date.isAfter(lastDate)) {
            if (end != null && date.isAfter(end)) break
            occurrencesOn(date)
                .map { it.atZone(zone).toInstant() }
                .filterTo(candidates) { it.isAfter(now) }
            date = date.plusDays(1)
        }
        val next = candidates.minOrNull() ?: return NextReminder.None(NoReminderReason.COURSE_ENDED)
        return NextReminder.Due(next, basis, overdue = false)
    }
}
