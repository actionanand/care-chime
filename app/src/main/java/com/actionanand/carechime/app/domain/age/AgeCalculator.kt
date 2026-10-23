package com.actionanand.carechime.app.domain.age

import com.actionanand.carechime.app.domain.model.Profile
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Age is always derived at display time; it is never persisted as a fixed number. */
data class Age(
    val years: Int,
    val months: Int,
    val days: Int,
    val approximate: Boolean,
) {
    fun describe(): String {
        val text = when {
            years >= 2 -> plural(years, "year")
            years == 1 -> if (months == 0) "1 year" else "1 year ${plural(months, "month")}"
            months >= 1 -> plural(months, "month")
            else -> plural(days, "day")
        }
        return if (approximate) "About $text" else text
    }

    private fun plural(value: Int, unit: String) = if (value == 1) "1 $unit" else "$value ${unit}s"
}

object AgeCalculator {
    fun fromDateOfBirth(dateOfBirth: LocalDate, today: LocalDate): Age? {
        if (dateOfBirth.isAfter(today)) return null
        val period = Period.between(dateOfBirth, today)
        return Age(period.years, period.months, period.days, approximate = false)
    }

    /** Advances an approximate age by the full years elapsed since it was recorded. */
    fun fromApproximate(ageYears: Int, recordedOn: LocalDate?, today: LocalDate): Age {
        val elapsed = recordedOn
            ?.takeIf { !it.isAfter(today) }
            ?.let { ChronoUnit.YEARS.between(it, today).toInt() }
            ?: 0
        return Age(ageYears + elapsed, 0, 0, approximate = true)
    }

    fun currentApproximateYears(ageYears: Int, recordedOn: LocalDate?, today: LocalDate): Int =
        fromApproximate(ageYears, recordedOn, today).years

    fun forProfile(profile: Profile, today: LocalDate): Age? {
        profile.dateOfBirth?.let { return fromDateOfBirth(it, today) }
        val approximate = profile.approximateAgeYears ?: return null
        return fromApproximate(approximate, profile.approximateAgeRecordedOn, today)
    }
}
