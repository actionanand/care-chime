package com.actionanand.carechime.app.domain.validation

import com.actionanand.carechime.app.domain.age.AgeCalculator
import com.actionanand.carechime.app.domain.model.AllergyCategory
import com.actionanand.carechime.app.domain.model.AllergyDraft
import com.actionanand.carechime.app.domain.model.AllergySeverity
import com.actionanand.carechime.app.domain.model.BloodGroup
import com.actionanand.carechime.app.domain.model.ConditionStatus
import com.actionanand.carechime.app.domain.model.CourseStatus
import com.actionanand.carechime.app.domain.model.FoodTiming
import com.actionanand.carechime.app.domain.model.HistoryCategory
import com.actionanand.carechime.app.domain.model.HospitalDraft
import com.actionanand.carechime.app.domain.model.MedicalConditionDraft
import com.actionanand.carechime.app.domain.model.MedicalHistoryDraft
import com.actionanand.carechime.app.domain.model.MedicineDraft
import com.actionanand.carechime.app.domain.model.MedicineForm
import com.actionanand.carechime.app.domain.model.MedicineRoute
import com.actionanand.carechime.app.domain.model.ProfileDraft
import com.actionanand.carechime.app.domain.model.ProfileMedicineDraft
import java.time.LocalDate

/* Form inputs hold raw text exactly as typed; validators turn them into typed drafts. */

data class ProfileInput(
    val name: String = "",
    val dateOfBirth: String = "",
    val approximateAge: String = "",
    val relationship: String = "",
    val bloodGroup: BloodGroup? = null,
    val medicalNotes: String = "",
)

object ProfileValidator {
    const val NAME = "name"
    const val DATE_OF_BIRTH = "dateOfBirth"
    const val APPROXIMATE_AGE = "approximateAge"
    const val RELATIONSHIP = "relationship"
    const val MEDICAL_NOTES = "medicalNotes"
    const val MAX_AGE_YEARS = 130

    fun validate(input: ProfileInput, today: LocalDate): FormResult<ProfileDraft> {
        val v = FieldValidator()
        val name = v.required(NAME, input.name, "Name", 80)
        val dob = v.optionalDate(DATE_OF_BIRTH, input.dateOfBirth, "Date of birth", today)
        if (dob != null && !v.hasError(DATE_OF_BIRTH) && dob.isBefore(today.minusYears(MAX_AGE_YEARS.toLong()))) {
            v.error(DATE_OF_BIRTH, "Date of birth must be within the last $MAX_AGE_YEARS years.")
        }
        val approxText = input.approximateAge.trim()
        val approximate: Int? = approxText.toIntOrNull()
        if (approxText.isNotEmpty()) {
            when {
                dob != null -> v.error(APPROXIMATE_AGE, "Use either date of birth or approximate age, not both.")
                approximate == null || approximate < 0 || approximate > MAX_AGE_YEARS ->
                    v.error(APPROXIMATE_AGE, "Approximate age must be a whole number from 0 to $MAX_AGE_YEARS.")
            }
        }
        val relationship = v.optional(RELATIONSHIP, input.relationship, "Relationship", 40)
        val notes = v.optional(MEDICAL_NOTES, input.medicalNotes, "Medical notes", 2000)
        return v.result {
            ProfileDraft(
                name = name,
                dateOfBirth = dob,
                approximateAgeYears = approximate,
                approximateAgeRecordedOn = approximate?.let { today },
                relationship = relationship,
                bloodGroup = input.bloodGroup,
                medicalNotes = notes,
            )
        }
    }

    /** Edit forms show the current approximate age so saving re-anchors it to today without drift. */
    fun currentApproximateAgeText(ageYears: Int?, recordedOn: LocalDate?, today: LocalDate): String =
        ageYears?.let { AgeCalculator.currentApproximateYears(it, recordedOn, today).toString() }.orEmpty()
}

data class HospitalInput(
    val hospitalName: String = "",
    val patientId: String = "",
    val placeName: String = "",
    val locationDetails: String = "",
    val mapsUrl: String = "",
    val doctorName: String = "",
    val notes: String = "",
)

object HospitalValidator {
    const val HOSPITAL_NAME = "hospitalName"
    const val PATIENT_ID = "patientId"
    const val PLACE_NAME = "placeName"
    const val LOCATION_DETAILS = "locationDetails"
    const val MAPS_URL = "mapsUrl"
    const val DOCTOR_NAME = "doctorName"
    const val NOTES = "notes"

    fun validate(input: HospitalInput): FormResult<HospitalDraft> {
        val v = FieldValidator()
        val name = v.required(HOSPITAL_NAME, input.hospitalName, "Hospital name", 120)
        val patientId = v.required(PATIENT_ID, input.patientId, "Patient / hospital ID", 60)
        val place = v.optional(PLACE_NAME, input.placeName, "Place name", 120)
        val location = v.optional(LOCATION_DETAILS, input.locationDetails, "Location details", 500)
        val maps = v.optional(MAPS_URL, input.mapsUrl, "Google Maps link", 500)
        if (maps != null && !v.hasError(MAPS_URL) && !MapsUrlValidator.isValid(maps)) {
            v.error(MAPS_URL, "Enter an https Google Maps link (maps.google.com, google.com/maps or maps.app.goo.gl).")
        }
        val doctor = v.optional(DOCTOR_NAME, input.doctorName, "Doctor name", 120)
        val notes = v.optional(NOTES, input.notes, "Notes", 2000)
        return v.result { HospitalDraft(name, patientId, place, location, maps, doctor, notes) }
    }
}

data class AllergyInput(
    val category: AllergyCategory = AllergyCategory.MEDICATION,
    val allergen: String = "",
    val reaction: String = "",
    val severity: AllergySeverity = AllergySeverity.UNKNOWN,
    val notedOn: String = "",
    val notes: String = "",
)

object AllergyValidator {
    const val ALLERGEN = "allergen"
    const val REACTION = "reaction"
    const val NOTED_ON = "notedOn"
    const val NOTES = "notes"

    fun validate(input: AllergyInput, today: LocalDate): FormResult<AllergyDraft> {
        val v = FieldValidator()
        val allergen = v.required(ALLERGEN, input.allergen, "Allergen", 120)
        val reaction = v.optional(REACTION, input.reaction, "Reaction", 300)
        val notedOn = v.optionalDate(NOTED_ON, input.notedOn, "Date noted", today)
        val notes = v.optional(NOTES, input.notes, "Notes", 2000)
        return v.result { AllergyDraft(input.category, allergen, reaction, input.severity, notedOn, notes) }
    }
}

data class ConditionInput(
    val name: String = "",
    val status: ConditionStatus = ConditionStatus.ACTIVE,
    val diagnosedOn: String = "",
    val notes: String = "",
)

object ConditionValidator {
    const val NAME = "name"
    const val DIAGNOSED_ON = "diagnosedOn"
    const val NOTES = "notes"

    fun validate(input: ConditionInput, today: LocalDate): FormResult<MedicalConditionDraft> {
        val v = FieldValidator()
        val name = v.required(NAME, input.name, "Condition", 120)
        val diagnosedOn = v.optionalDate(DIAGNOSED_ON, input.diagnosedOn, "Diagnosis date", today)
        val notes = v.optional(NOTES, input.notes, "Notes", 2000)
        return v.result { MedicalConditionDraft(name, input.status, diagnosedOn, notes) }
    }
}

data class HistoryInput(
    val eventDate: String = "",
    val title: String = "",
    val category: HistoryCategory = HistoryCategory.CONSULTATION,
    val hospitalId: String? = null,
    val notes: String = "",
)

object HistoryValidator {
    const val EVENT_DATE = "eventDate"
    const val TITLE = "title"
    const val NOTES = "notes"

    fun validate(input: HistoryInput, today: LocalDate): FormResult<MedicalHistoryDraft> {
        val v = FieldValidator()
        val date = v.requiredDate(EVENT_DATE, input.eventDate, "Date", today)
        val title = v.required(TITLE, input.title, "Title", 120)
        val notes = v.optional(NOTES, input.notes, "Notes", 4000)
        return v.result { MedicalHistoryDraft(date!!, title, input.category, input.hospitalId, notes) }
    }
}

data class MedicineInput(
    val name: String = "",
    val brandName: String = "",
    val genericNames: String = "",
    val form: MedicineForm = MedicineForm.TABLET,
    val strength: String = "",
    val concentration: String = "",
    val ageReferenceNotes: String = "",
    val manufacturer: String = "",
    val notes: String = "",
)

object MedicineValidator {
    const val NAME = "name"
    const val BRAND_NAME = "brandName"
    const val GENERIC_NAMES = "genericNames"
    const val STRENGTH = "strength"
    const val CONCENTRATION = "concentration"
    const val AGE_REFERENCE_NOTES = "ageReferenceNotes"
    const val MANUFACTURER = "manufacturer"
    const val NOTES = "notes"

    fun validate(input: MedicineInput): FormResult<MedicineDraft> {
        val v = FieldValidator()
        val name = v.required(NAME, input.name, "Medicine name", 120)
        val brand = v.optional(BRAND_NAME, input.brandName, "Brand name", 120)
        val generic = v.optional(GENERIC_NAMES, input.genericNames, "Generic / salt names", 300)
        val strength = v.optional(STRENGTH, input.strength, "Strength", 60)
        val concentration = v.optional(CONCENTRATION, input.concentration, "Concentration", 60)
        val ageNotes = v.optional(AGE_REFERENCE_NOTES, input.ageReferenceNotes, "Age reference notes", 1000)
        val manufacturer = v.optional(MANUFACTURER, input.manufacturer, "Manufacturer", 120)
        val notes = v.optional(NOTES, input.notes, "Notes", 2000)
        return v.result {
            MedicineDraft(name, brand, generic, input.form, strength, concentration, ageNotes, manufacturer, notes)
        }
    }
}

data class ProfileMedicineInput(
    val medicineId: String? = null,
    val doseAmount: String = "",
    val doseUnit: String = "",
    val route: MedicineRoute? = null,
    val foodTiming: FoodTiming = FoodTiming.NOT_SPECIFIED,
    val startDate: String = "",
    val endDate: String = "",
    val prescribedBy: String = "",
    val hospitalId: String? = null,
    val notes: String = "",
    val status: CourseStatus = CourseStatus.ACTIVE,
)

object ProfileMedicineValidator {
    const val MEDICINE = "medicine"
    const val DOSE_AMOUNT = "doseAmount"
    const val DOSE_UNIT = "doseUnit"
    const val START_DATE = "startDate"
    const val END_DATE = "endDate"
    const val PRESCRIBED_BY = "prescribedBy"
    const val NOTES = "notes"

    fun validate(input: ProfileMedicineInput, today: LocalDate): FormResult<ProfileMedicineDraft> {
        val v = FieldValidator()
        if (input.medicineId.isNullOrBlank()) v.error(MEDICINE, "Choose a medicine from the library.")
        val amount = DoseAmountParser.normalise(input.doseAmount)
        if (input.doseAmount.isBlank()) {
            v.error(DOSE_AMOUNT, "Enter the prescribed amount.")
        } else if (amount == null) {
            v.error(DOSE_AMOUNT, "Enter a positive number with up to 3 decimal places.")
        }
        val unit = v.required(DOSE_UNIT, input.doseUnit, "Unit", 30)
        val start = v.requiredDate(START_DATE, input.startDate, "Start date", today, allowFuture = true)
        val end = v.optionalDate(END_DATE, input.endDate, "End date", today, allowFuture = true)
        if (start != null && end != null && end.isBefore(start)) {
            v.error(END_DATE, "End date cannot be before the start date.")
        }
        val prescribedBy = v.optional(PRESCRIBED_BY, input.prescribedBy, "Doctor", 120)
        val notes = v.optional(NOTES, input.notes, "Notes", 2000)
        return v.result {
            ProfileMedicineDraft(
                medicineId = input.medicineId!!,
                doseAmount = amount!!,
                doseUnit = unit,
                route = input.route,
                foodTiming = input.foodTiming,
                startDate = start!!,
                endDate = end,
                prescribedBy = prescribedBy,
                hospitalId = input.hospitalId,
                notes = notes,
                status = input.status,
            )
        }
    }
}
