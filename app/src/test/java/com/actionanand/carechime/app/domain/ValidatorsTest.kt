package com.actionanand.carechime.app.domain

import com.actionanand.carechime.app.domain.model.AllergyCategory
import com.actionanand.carechime.app.domain.model.BloodGroup
import com.actionanand.carechime.app.domain.model.CourseStatus
import com.actionanand.carechime.app.domain.model.MedicineForm
import com.actionanand.carechime.app.domain.validation.AllergyInput
import com.actionanand.carechime.app.domain.validation.AllergyValidator
import com.actionanand.carechime.app.domain.validation.ConditionInput
import com.actionanand.carechime.app.domain.validation.ConditionValidator
import com.actionanand.carechime.app.domain.validation.DoseAmountParser
import com.actionanand.carechime.app.domain.validation.FormResult
import com.actionanand.carechime.app.domain.validation.HistoryInput
import com.actionanand.carechime.app.domain.validation.HistoryValidator
import com.actionanand.carechime.app.domain.validation.HospitalInput
import com.actionanand.carechime.app.domain.validation.HospitalValidator
import com.actionanand.carechime.app.domain.validation.MapsUrlValidator
import com.actionanand.carechime.app.domain.validation.MedicineInput
import com.actionanand.carechime.app.domain.validation.MedicineValidator
import com.actionanand.carechime.app.domain.validation.ProfileInput
import com.actionanand.carechime.app.domain.validation.ProfileMedicineInput
import com.actionanand.carechime.app.domain.validation.ProfileMedicineValidator
import com.actionanand.carechime.app.domain.validation.ProfileValidator
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun <T> FormResult<T>.valid(): T = (this as? FormResult.Valid<T>)?.value
    ?: throw AssertionError("Expected valid result but got $this")

private fun <T> FormResult<T>.errors(): Map<String, String> = (this as? FormResult.Invalid)?.errors
    ?: throw AssertionError("Expected invalid result but got $this")

class ProfileValidatorTest {
    private val today = LocalDate.of(2026, 10, 23)

    @Test fun nameIsRequiredAndTrimmed() {
        assertTrue(ProfileValidator.NAME in ProfileValidator.validate(ProfileInput(name = "   "), today).errors())
        val draft = ProfileValidator.validate(ProfileInput(name = "  Test Person  ", bloodGroup = BloodGroup.O_POSITIVE), today).valid()
        assertEquals("Test Person", draft.name)
        assertEquals(BloodGroup.O_POSITIVE, draft.bloodGroup)
        assertNull(draft.dateOfBirth)
        assertNull(draft.relationship)
    }

    @Test fun dateOfBirthMustBeValidAndNotFuture() {
        val bad = ProfileValidator.validate(ProfileInput(name = "A", dateOfBirth = "2026-13-01"), today).errors()
        assertTrue(ProfileValidator.DATE_OF_BIRTH in bad)
        val future = ProfileValidator.validate(ProfileInput(name = "A", dateOfBirth = "2026-10-24"), today).errors()
        assertTrue(ProfileValidator.DATE_OF_BIRTH in future)
        val tooOld = ProfileValidator.validate(ProfileInput(name = "A", dateOfBirth = "1890-01-01"), today).errors()
        assertTrue(ProfileValidator.DATE_OF_BIRTH in tooOld)
        val ok = ProfileValidator.validate(ProfileInput(name = "A", dateOfBirth = "2026-10-23"), today).valid()
        assertEquals(today, ok.dateOfBirth)
    }

    @Test fun approximateAgeIsAnchoredToToday() {
        val draft = ProfileValidator.validate(ProfileInput(name = "A", approximateAge = "70"), today).valid()
        assertEquals(70, draft.approximateAgeYears)
        assertEquals(today, draft.approximateAgeRecordedOn)
    }

    @Test fun approximateAgeRejectedWithDobOrOutOfRange() {
        val both = ProfileValidator.validate(ProfileInput(name = "A", dateOfBirth = "2000-01-01", approximateAge = "20"), today)
        assertTrue(ProfileValidator.APPROXIMATE_AGE in both.errors())
        listOf("-1", "131", "4.5", "abc").forEach { value ->
            val result = ProfileValidator.validate(ProfileInput(name = "A", approximateAge = value), today)
            assertTrue(value, ProfileValidator.APPROXIMATE_AGE in result.errors())
        }
    }

    @Test fun editFormShowsAdvancedApproximateAge() {
        assertEquals("42", ProfileValidator.currentApproximateAgeText(40, LocalDate.of(2024, 1, 1), today))
        assertEquals("", ProfileValidator.currentApproximateAgeText(null, null, today))
    }

    @Test fun lengthLimitsAreEnforced() {
        val result = ProfileValidator.validate(ProfileInput(name = "x".repeat(81)), today)
        assertTrue(ProfileValidator.NAME in result.errors())
    }
}

class HospitalValidatorTest {
    private val valid = HospitalInput(hospitalName = "City Hospital", patientId = "MRN-001")

    @Test fun nameAndPatientIdRequired() {
        val errors = HospitalValidator.validate(HospitalInput()).errors()
        assertTrue(HospitalValidator.HOSPITAL_NAME in errors)
        assertTrue(HospitalValidator.PATIENT_ID in errors)
        val draft = HospitalValidator.validate(valid.copy(placeName = " ")).valid()
        assertNull(draft.placeName)
    }

    @Test fun mapsUrlMustBeGoogleMapsHttps() {
        assertTrue(HospitalValidator.MAPS_URL in HospitalValidator.validate(valid.copy(mapsUrl = "http://maps.google.com/x")).errors())
        val draft = HospitalValidator.validate(valid.copy(mapsUrl = "https://maps.app.goo.gl/abc123")).valid()
        assertEquals("https://maps.app.goo.gl/abc123", draft.mapsUrl)
    }

    @Test fun mapsUrlValidatorCases() {
        listOf(
            "https://maps.app.goo.gl/AbC123",
            "https://www.google.com/maps/place/Somewhere/@12.9,77.5,15z",
            "https://google.co.in/maps?q=12.9,77.5",
            "https://maps.google.com/?q=hospital",
            "https://goo.gl/maps/xyz",
        ).forEach { assertTrue(it, MapsUrlValidator.isValid(it)) }
        listOf(
            "http://maps.google.com/?q=x",
            "https://evil.example/maps",
            "https://www.google.com/search?q=maps",
            "https://maps.google.com@evil.example/",
            "https://user:pw@maps.google.com/",
            "https://www.google.com.evil.example/maps",
            "https://maps.google.com:8443/",
            "javascript:alert(1)",
            "not a url",
            "",
        ).forEach { assertFalse(it, MapsUrlValidator.isValid(it)) }
    }
}

class MedicalRecordValidatorTest {
    private val today = LocalDate.of(2026, 10, 23)

    @Test fun allergyRequiresAllergenAndPastDate() {
        assertTrue(AllergyValidator.ALLERGEN in AllergyValidator.validate(AllergyInput(), today).errors())
        val future = AllergyValidator.validate(AllergyInput(allergen = "Penicillin", notedOn = "2027-01-01"), today)
        assertTrue(AllergyValidator.NOTED_ON in future.errors())
        val draft = AllergyValidator.validate(AllergyInput(category = AllergyCategory.FOOD, allergen = "Peanut"), today).valid()
        assertEquals(AllergyCategory.FOOD, draft.category)
        assertNull(draft.reaction)
    }

    @Test fun conditionRequiresName() {
        assertTrue(ConditionValidator.NAME in ConditionValidator.validate(ConditionInput(), today).errors())
        assertEquals("Asthma", ConditionValidator.validate(ConditionInput(name = " Asthma "), today).valid().name)
    }

    @Test fun historyRequiresDateAndTitle() {
        val errors = HistoryValidator.validate(HistoryInput(), today).errors()
        assertTrue(HistoryValidator.EVENT_DATE in errors)
        assertTrue(HistoryValidator.TITLE in errors)
        val draft = HistoryValidator.validate(HistoryInput(eventDate = "2025-01-05", title = "Check-up"), today).valid()
        assertEquals(LocalDate.of(2025, 1, 5), draft.eventDate)
    }
}

class MedicineValidatorTest {
    private val today = LocalDate.of(2026, 10, 23)

    @Test fun catalogMedicineRequiresName() {
        assertTrue(MedicineValidator.NAME in MedicineValidator.validate(MedicineInput()).errors())
        val draft = MedicineValidator.validate(
            MedicineInput(name = "Example Syrup", form = MedicineForm.SYRUP, concentration = "120 mg/5 ml"),
        ).valid()
        assertEquals(MedicineForm.SYRUP, draft.form)
        assertEquals("120 mg/5 ml", draft.concentration)
        assertNull(draft.brandName)
    }

    @Test fun doseAmountParsing() {
        assertEquals("0.5", DoseAmountParser.normalise("0.50"))
        assertEquals("2.5", DoseAmountParser.normalise("2,5"))
        assertEquals("100", DoseAmountParser.normalise("100"))
        assertEquals("0.125", DoseAmountParser.normalise("0.125"))
        listOf("0", "-1", "abc", "1.2345", "1e3", "", "100001", ".5").forEach {
            assertNull(it, DoseAmountParser.normalise(it))
        }
    }

    @Test fun profileMedicineValidation() {
        val errors = ProfileMedicineValidator.validate(ProfileMedicineInput(startDate = ""), today).errors()
        assertTrue(ProfileMedicineValidator.MEDICINE in errors)
        assertTrue(ProfileMedicineValidator.DOSE_AMOUNT in errors)
        assertTrue(ProfileMedicineValidator.DOSE_UNIT in errors)
        assertTrue(ProfileMedicineValidator.START_DATE in errors)

        val endBeforeStart = ProfileMedicineInput(
            medicineId = "m1", doseAmount = "1", doseUnit = "tablet", startDate = "2026-10-20", endDate = "2026-10-19",
        )
        assertTrue(ProfileMedicineValidator.END_DATE in ProfileMedicineValidator.validate(endBeforeStart, today).errors())

        val draft = ProfileMedicineValidator.validate(
            endBeforeStart.copy(doseAmount = "0.50", endDate = "", status = CourseStatus.PAUSED, startDate = "2026-11-01"),
            today,
        ).valid()
        assertEquals("0.5", draft.doseAmount)
        assertNull(draft.endDate)
        assertEquals(CourseStatus.PAUSED, draft.status)
        assertEquals(LocalDate.of(2026, 11, 1), draft.startDate)
    }
}
