package com.actionanand.carechime.app.domain.schedule

import com.actionanand.carechime.app.domain.model.CourseStatus
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/*
 * Reminder-engine domain contracts (pure Kotlin, no Android APIs). Persisted rule JSON will carry
 * RULE_FORMAT_VERSION so the Angular companion can reproduce the same calculations.
 */

const val RULE_FORMAT_VERSION = 1

enum class ScheduleType { FIXED_TIMES, ROLLING_INTERVAL, MEAL_RELATIVE, ONE_TIME, AS_NEEDED }

enum class Meal { BREAKFAST, LUNCH, DINNER }

enum class MealRelation { BEFORE, WITH, AFTER }

data class MealTimes(
    val breakfast: LocalTime,
    val lunch: LocalTime,
    val dinner: LocalTime,
) {
    fun timeOf(meal: Meal): LocalTime = when (meal) {
        Meal.BREAKFAST -> breakfast
        Meal.LUNCH -> lunch
        Meal.DINNER -> dinner
    }
}

sealed interface ScheduleRule {
    val type: ScheduleType

    /** Prescribed wall-clock times. Never shifted by late or early confirmation. */
    data class FixedTimes(val times: List<LocalTime>) : ScheduleRule {
        override val type get() = ScheduleType.FIXED_TIMES
    }

    /**
     * Next dose becomes eligible a fixed interval after the actual confirmed administration.
     * Only valid when the prescription explicitly allows interval-from-last-dose timing.
     */
    data class RollingInterval(
        val interval: Duration,
        val firstDoseAt: LocalDateTime,
        val prescriptionConfirmed: Boolean,
    ) : ScheduleRule {
        override val type get() = ScheduleType.ROLLING_INTERVAL
    }

    /** Times derived from user-configured meal times and the prescribed offset. */
    data class MealRelative(
        val meals: Set<Meal>,
        val relation: MealRelation,
        val offset: Duration,
    ) : ScheduleRule {
        override val type get() = ScheduleType.MEAL_RELATIVE
    }

    data class OneTime(val at: LocalDateTime) : ScheduleRule {
        override val type get() = ScheduleType.ONE_TIME
    }

    /** As-needed (PRN): never produces a reminder; the minimum interval is only a guardrail. */
    data class AsNeeded(val minimumInterval: Duration?) : ScheduleRule {
        override val type get() = ScheduleType.AS_NEEDED
    }
}

data class CourseWindow(val startDate: LocalDate, val endDateInclusive: LocalDate?) {
    fun contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && (endDateInclusive == null || !date.isAfter(endDateInclusive))
}

data class MedicationSchedule(
    val id: String,
    val profileMedicineId: String,
    val rule: ScheduleRule,
    val window: CourseWindow,
    val zoneId: ZoneId,
    val status: CourseStatus,
    /** Incremented on every edit; alarms from older revisions must be cancelled, never delivered. */
    val revision: Int,
)

object ScheduleRuleValidator {
    val MIN_ROLLING_INTERVAL: Duration = Duration.ofMinutes(1)
    val MAX_ROLLING_INTERVAL: Duration = Duration.ofDays(30)
    val MAX_MEAL_OFFSET: Duration = Duration.ofHours(3)
    const val MAX_FIXED_TIMES = 24

    /** Technical consistency checks only; this is not clinical validation of any prescription. */
    fun validate(rule: ScheduleRule): List<String> = buildList {
        when (rule) {
            is ScheduleRule.FixedTimes -> {
                if (rule.times.isEmpty()) add("Add at least one time.")
                if (rule.times.size > MAX_FIXED_TIMES) add("Use at most $MAX_FIXED_TIMES times per day.")
                if (rule.times.toSet().size != rule.times.size) add("Each time can only be listed once.")
            }
            is ScheduleRule.RollingInterval -> {
                if (!rule.prescriptionConfirmed) {
                    add("Interval-from-last-dose timing must be confirmed against the prescription.")
                }
                if (rule.interval < MIN_ROLLING_INTERVAL || rule.interval > MAX_ROLLING_INTERVAL) {
                    add("Interval must be between 1 minute and 30 days.")
                }
            }
            is ScheduleRule.MealRelative -> {
                if (rule.meals.isEmpty()) add("Choose at least one meal.")
                if (rule.offset.isNegative || rule.offset > MAX_MEAL_OFFSET) {
                    add("Meal offset must be between 0 and 3 hours.")
                }
                if (rule.relation == MealRelation.WITH && !rule.offset.isZero) {
                    add("Use no offset for doses taken with a meal.")
                }
            }
            is ScheduleRule.OneTime -> Unit
            is ScheduleRule.AsNeeded -> {
                val minimum = rule.minimumInterval
                if (minimum != null && (minimum.isNegative || minimum.isZero)) {
                    add("Minimum interval must be positive.")
                }
            }
        }
    }
}
