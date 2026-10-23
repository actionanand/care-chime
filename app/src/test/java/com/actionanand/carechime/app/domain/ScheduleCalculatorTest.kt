package com.actionanand.carechime.app.domain

import com.actionanand.carechime.app.domain.model.CourseStatus
import com.actionanand.carechime.app.domain.schedule.CourseWindow
import com.actionanand.carechime.app.domain.schedule.DueBasis
import com.actionanand.carechime.app.domain.schedule.Meal
import com.actionanand.carechime.app.domain.schedule.MealRelation
import com.actionanand.carechime.app.domain.schedule.MealTimes
import com.actionanand.carechime.app.domain.schedule.MedicationSchedule
import com.actionanand.carechime.app.domain.schedule.NextReminder
import com.actionanand.carechime.app.domain.schedule.NoReminderReason
import com.actionanand.carechime.app.domain.schedule.ScheduleCalculator
import com.actionanand.carechime.app.domain.schedule.ScheduleRule
import com.actionanand.carechime.app.domain.schedule.ScheduleRuleValidator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleCalculatorTest {
    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")
    private val newYork: ZoneId = ZoneId.of("America/New_York")

    private fun at(zone: ZoneId, y: Int, m: Int, d: Int, h: Int, min: Int = 0): Instant =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant()

    private fun calculatorAt(instant: Instant) = ScheduleCalculator(Clock.fixed(instant, ist))

    private val now = at(ist, 2026, 10, 23, 8, 0)
    private val calculator = calculatorAt(now)

    private fun schedule(
        rule: ScheduleRule,
        start: LocalDate = LocalDate.of(2026, 10, 1),
        end: LocalDate? = null,
        status: CourseStatus = CourseStatus.ACTIVE,
        zone: ZoneId = ist,
    ) = MedicationSchedule("s1", "c1", rule, CourseWindow(start, end), zone, status, revision = 1)

    private fun due(result: NextReminder): NextReminder.Due =
        result as? NextReminder.Due ?: throw AssertionError("Expected Due but was $result")

    private fun none(result: NextReminder): NextReminder.None =
        result as? NextReminder.None ?: throw AssertionError("Expected None but was $result")

    private val threeTimes = ScheduleRule.FixedTimes(listOf(LocalTime.of(22, 0), LocalTime.of(6, 0), LocalTime.of(14, 0)))

    @Test fun fixedTimeReturnsNextPrescribedTime() {
        val result = due(calculator.next(schedule(threeTimes), lastAdministeredAt = null))
        assertEquals(at(ist, 2026, 10, 23, 14, 0), result.dueAt)
        assertEquals(DueBasis.FIXED_TIME, result.basis)
        assertFalse(result.overdue)
    }

    @Test fun fixedTimeNeverShiftsAfterLateAdministration() {
        val lateDose = at(ist, 2026, 10, 23, 7, 45)
        val result = due(calculator.next(schedule(threeTimes), lastAdministeredAt = lateDose))
        assertEquals(at(ist, 2026, 10, 23, 14, 0), result.dueAt)
    }

    @Test fun fixedTimeRollsToNextDay() {
        val late = calculatorAt(at(ist, 2026, 10, 23, 23, 0))
        assertEquals(at(ist, 2026, 10, 24, 6, 0), due(late.next(schedule(threeTimes), null)).dueAt)
    }

    @Test fun fixedTimeAtExactInstantMovesToFollowingTime() {
        val exact = calculatorAt(at(ist, 2026, 10, 23, 14, 0))
        assertEquals(at(ist, 2026, 10, 23, 22, 0), due(exact.next(schedule(threeTimes), null)).dueAt)
    }

    @Test fun fixedTimeRespectsCourseWindow() {
        assertEquals(
            NoReminderReason.COURSE_ENDED,
            none(calculator.next(schedule(threeTimes, end = LocalDate.of(2026, 10, 22)), null)).reason,
        )
        val lateOnLastDay = calculatorAt(at(ist, 2026, 10, 23, 23, 0))
        assertEquals(
            NoReminderReason.COURSE_ENDED,
            none(lateOnLastDay.next(schedule(threeTimes, end = LocalDate.of(2026, 10, 23)), null)).reason,
        )
        val future = schedule(threeTimes, start = LocalDate.of(2026, 11, 10))
        assertEquals(at(ist, 2026, 11, 10, 6, 0), due(calculator.next(future, null)).dueAt)
    }

    @Test fun inactiveCoursesProduceNoReminder() {
        listOf(CourseStatus.PAUSED, CourseStatus.COMPLETED, CourseStatus.DISCONTINUED).forEach { status ->
            assertEquals(
                NoReminderReason.COURSE_NOT_ACTIVE,
                none(calculator.next(schedule(threeTimes, status = status), null)).reason,
            )
        }
    }

    private fun rolling(hours: Long, confirmed: Boolean = true) = ScheduleRule.RollingInterval(
        interval = Duration.ofHours(hours),
        firstDoseAt = LocalDateTime.of(2026, 10, 23, 6, 0),
        prescriptionConfirmed = confirmed,
    )

    @Test fun rollingSixHoursCountsFromActualAdministration() {
        // Planned 06:00, actually taken 07:00 -> next eligible 13:00 (software example, not medical advice).
        val result = due(calculator.next(schedule(rolling(6)), at(ist, 2026, 10, 23, 7, 0)))
        assertEquals(at(ist, 2026, 10, 23, 13, 0), result.dueAt)
        assertEquals(DueBasis.ACTUAL_ADMINISTRATION, result.basis)
        assertFalse(result.overdue)
    }

    @Test fun rollingTwelveHoursAcrossMidnight() {
        val result = due(calculator.next(schedule(rolling(12)), at(ist, 2026, 10, 22, 21, 30)))
        assertEquals(at(ist, 2026, 10, 23, 9, 30), result.dueAt)
    }

    @Test fun rollingWithoutAdministrationUsesPlannedFirstDoseOnce() {
        val result = due(calculator.next(schedule(rolling(6)), null))
        assertEquals(at(ist, 2026, 10, 23, 6, 0), result.dueAt)
        assertEquals(DueBasis.PLANNED_FIRST_DOSE, result.basis)
        assertTrue(result.overdue)
    }

    @Test fun rollingAfterLongGapReturnsSingleOverdueDoseNoCatchUp() {
        val lastDose = at(ist, 2026, 10, 20, 8, 0)
        val result = due(calculator.next(schedule(rolling(6)), lastDose))
        assertEquals(lastDose.plus(Duration.ofHours(6)), result.dueAt)
        assertTrue(result.overdue)
    }

    @Test fun rollingRequiresPrescriptionConfirmation() {
        assertEquals(NoReminderReason.INVALID_RULE, none(calculator.next(schedule(rolling(6, confirmed = false)), null)).reason)
        assertTrue(ScheduleRuleValidator.validate(rolling(6, confirmed = false)).isNotEmpty())
    }

    @Test fun rollingStopsAfterCourseEnd() {
        val result = calculator.next(schedule(rolling(12), end = LocalDate.of(2026, 10, 23)), at(ist, 2026, 10, 23, 14, 0))
        assertEquals(NoReminderReason.COURSE_ENDED, none(result).reason)
    }

    private val meals = MealTimes(LocalTime.of(8, 30), LocalTime.of(13, 0), LocalTime.of(20, 0))

    @Test fun mealRelativeUsesConfiguredMealTimes() {
        val rule = ScheduleRule.MealRelative(setOf(Meal.BREAKFAST, Meal.DINNER), MealRelation.BEFORE, Duration.ofMinutes(30))
        // Breakfast reminder (08:00) equals "now", so the next one is before dinner.
        val result = due(calculator.next(schedule(rule), null, meals))
        assertEquals(at(ist, 2026, 10, 23, 19, 30), result.dueAt)
        assertEquals(DueBasis.MEAL_TIME, result.basis)
    }

    @Test fun mealRelativeAcrossMidnight() {
        val lateDinner = MealTimes(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(23, 45))
        val rule = ScheduleRule.MealRelative(setOf(Meal.DINNER), MealRelation.AFTER, Duration.ofMinutes(30))
        val calc = calculatorAt(at(ist, 2026, 10, 24, 0, 5))
        assertEquals(at(ist, 2026, 10, 24, 0, 15), due(calc.next(schedule(rule), null, lateDinner)).dueAt)
    }

    @Test fun mealRelativeNeedsMealTimes() {
        val rule = ScheduleRule.MealRelative(setOf(Meal.LUNCH), MealRelation.WITH, Duration.ZERO)
        assertEquals(NoReminderReason.MEAL_TIMES_NOT_SET, none(calculator.next(schedule(rule), null)).reason)
        assertEquals(at(ist, 2026, 10, 23, 13, 0), due(calculator.next(schedule(rule), null, meals)).dueAt)
    }

    @Test fun oneTimeReminder() {
        val future = ScheduleRule.OneTime(LocalDateTime.of(2026, 10, 25, 9, 0))
        assertEquals(at(ist, 2026, 10, 25, 9, 0), due(calculator.next(schedule(future), null)).dueAt)
        val past = ScheduleRule.OneTime(LocalDateTime.of(2026, 10, 22, 9, 0))
        assertEquals(NoReminderReason.ONE_TIME_PASSED, none(calculator.next(schedule(past), null)).reason)
        assertEquals(
            NoReminderReason.ALREADY_RECORDED,
            none(calculator.next(schedule(future), at(ist, 2026, 10, 23, 7, 0))).reason,
        )
    }

    @Test fun asNeededNeverSchedulesAReminder() {
        val rule = ScheduleRule.AsNeeded(Duration.ofHours(4))
        val lastDose = at(ist, 2026, 10, 23, 7, 0)
        val result = none(calculator.next(schedule(rule), lastDose))
        assertEquals(NoReminderReason.AS_NEEDED, result.reason)
        assertEquals(lastDose.plus(Duration.ofHours(4)), result.earliestEligibleAt)
        assertNull(none(calculator.next(schedule(rule), null)).earliestEligibleAt)
    }

    @Test fun daylightSavingGapShiftsForwardOnce() {
        val calc = ScheduleCalculator(Clock.fixed(at(newYork, 2026, 3, 8, 0, 0), newYork))
        val rule = ScheduleRule.FixedTimes(listOf(LocalTime.of(2, 30)))
        val result = due(calc.next(schedule(rule, zone = newYork), null))
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), result.dueAt)
    }

    @Test fun daylightSavingOverlapDoesNotRepeatTheDose() {
        val rule = ScheduleRule.FixedTimes(listOf(LocalTime.of(1, 30)))
        val beforeFirst = ScheduleCalculator(Clock.fixed(Instant.parse("2026-11-01T04:00:00Z"), newYork))
        assertEquals(Instant.parse("2026-11-01T05:30:00Z"), due(beforeFirst.next(schedule(rule, zone = newYork), null)).dueAt)
        val afterFirst = ScheduleCalculator(Clock.fixed(Instant.parse("2026-11-01T05:45:00Z"), newYork))
        assertEquals(Instant.parse("2026-11-02T06:30:00Z"), due(afterFirst.next(schedule(rule, zone = newYork), null)).dueAt)
    }

    @Test fun ruleValidation() {
        assertTrue(ScheduleRuleValidator.validate(ScheduleRule.FixedTimes(emptyList())).isNotEmpty())
        assertTrue(ScheduleRuleValidator.validate(ScheduleRule.FixedTimes(listOf(LocalTime.NOON, LocalTime.NOON))).isNotEmpty())
        assertTrue(
            ScheduleRuleValidator.validate(
                ScheduleRule.MealRelative(setOf(Meal.LUNCH), MealRelation.WITH, Duration.ofMinutes(10)),
            ).isNotEmpty(),
        )
        assertTrue(ScheduleRuleValidator.validate(ScheduleRule.AsNeeded(Duration.ZERO)).isNotEmpty())
        assertTrue(ScheduleRuleValidator.validate(threeTimes).isEmpty())
        assertTrue(ScheduleRuleValidator.validate(rolling(6)).isEmpty())
    }
}
