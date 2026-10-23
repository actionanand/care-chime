package com.actionanand.carechime.app.domain

import com.actionanand.carechime.app.domain.age.AgeCalculator
import com.actionanand.carechime.app.domain.model.Profile
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgeCalculatorTest {
    private val today = LocalDate.of(2026, 10, 23)

    @Test fun exactAgeBeforeAndAfterBirthday() {
        assertEquals(39, AgeCalculator.fromDateOfBirth(LocalDate.of(1986, 10, 24), today)!!.years)
        assertEquals(40, AgeCalculator.fromDateOfBirth(LocalDate.of(1986, 10, 23), today)!!.years)
    }

    @Test fun infantAgeUsesMonthsAndDays() {
        assertEquals("5 months", AgeCalculator.fromDateOfBirth(LocalDate.of(2026, 5, 20), today)!!.describe())
        assertEquals("12 days", AgeCalculator.fromDateOfBirth(LocalDate.of(2026, 10, 11), today)!!.describe())
        assertEquals("1 year 2 months", AgeCalculator.fromDateOfBirth(LocalDate.of(2025, 8, 1), today)!!.describe())
        assertEquals("0 days", AgeCalculator.fromDateOfBirth(today, today)!!.describe())
    }

    @Test fun leapDayBirthday() {
        assertEquals(0, AgeCalculator.fromDateOfBirth(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28))!!.years)
        assertEquals(1, AgeCalculator.fromDateOfBirth(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 3, 1))!!.years)
    }

    @Test fun futureBirthDateHasNoAge() {
        assertNull(AgeCalculator.fromDateOfBirth(today.plusDays(1), today))
    }

    @Test fun approximateAgeAdvancesWithTime() {
        val age = AgeCalculator.fromApproximate(40, LocalDate.of(2024, 6, 1), today)
        assertEquals(42, age.years)
        assertTrue(age.approximate)
        assertEquals("About 42 years", age.describe())
        assertEquals(40, AgeCalculator.fromApproximate(40, null, today).years)
    }

    @Test fun profilePrefersDateOfBirth() {
        val profile = Profile(
            id = "p1", name = "Test Person", dateOfBirth = LocalDate.of(2000, 1, 1),
            approximateAgeYears = 5, approximateAgeRecordedOn = today, relationship = null, bloodGroup = null,
            medicalNotes = null, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        )
        assertEquals(26, AgeCalculator.forProfile(profile, today)!!.years)
        assertNull(AgeCalculator.forProfile(profile.copy(dateOfBirth = null, approximateAgeYears = null), today))
    }
}
