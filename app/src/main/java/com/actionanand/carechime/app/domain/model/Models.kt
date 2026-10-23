package com.actionanand.carechime.app.domain.model

import java.time.Instant
import java.time.LocalDate

/*
 * Domain models are framework-free so they can be validated in JVM tests and mirrored by the
 * future versioned export format. Enum constant names are persisted: never rename them; add new
 * constants and migrate instead.
 */

interface LabeledOption {
    val label: String
}

enum class BloodGroup(override val label: String) : LabeledOption {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-"),
    OTHER("Other / rare"),
}

data class Profile(
    val id: String,
    val name: String,
    val dateOfBirth: LocalDate?,
    val approximateAgeYears: Int?,
    /** Date the approximate age was entered, so the displayed age keeps advancing. */
    val approximateAgeRecordedOn: LocalDate?,
    val relationship: String?,
    val bloodGroup: BloodGroup?,
    val medicalNotes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ProfileDraft(
    val name: String,
    val dateOfBirth: LocalDate?,
    val approximateAgeYears: Int?,
    val approximateAgeRecordedOn: LocalDate?,
    val relationship: String?,
    val bloodGroup: BloodGroup?,
    val medicalNotes: String?,
)

data class ProfileDeletionSummary(
    val hospitals: Int,
    val allergies: Int,
    val conditions: Int,
    val historyEntries: Int,
    val medicines: Int,
) {
    val total: Int get() = hospitals + allergies + conditions + historyEntries + medicines
}

data class HospitalRecord(
    val id: String,
    val profileId: String,
    val hospitalName: String,
    val patientId: String,
    val placeName: String?,
    val locationDetails: String?,
    val mapsUrl: String?,
    val doctorName: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class HospitalDraft(
    val hospitalName: String,
    val patientId: String,
    val placeName: String?,
    val locationDetails: String?,
    val mapsUrl: String?,
    val doctorName: String?,
    val notes: String?,
)

/** Hospital details captured when a record was linked; kept if the hospital record is edited or deleted. */
data class RecordedHospital(
    val hospitalName: String,
    val placeName: String?,
)

enum class AllergyCategory(override val label: String) : LabeledOption {
    MEDICATION("Medication"),
    FOOD("Food"),
    ENVIRONMENTAL("Environmental"),
    OTHER("Other"),
}

enum class AllergySeverity(override val label: String) : LabeledOption {
    UNKNOWN("Not recorded"),
    MILD("Mild"),
    MODERATE("Moderate"),
    SEVERE("Severe"),
}

/** A substance the person reacts to. Medicines prescribed to treat allergies are ProfileMedicines. */
data class Allergy(
    val id: String,
    val profileId: String,
    val category: AllergyCategory,
    val allergen: String,
    val reaction: String?,
    val severity: AllergySeverity,
    val notedOn: LocalDate?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AllergyDraft(
    val category: AllergyCategory,
    val allergen: String,
    val reaction: String?,
    val severity: AllergySeverity,
    val notedOn: LocalDate?,
    val notes: String?,
)

enum class ConditionStatus(override val label: String) : LabeledOption {
    ACTIVE("Active"),
    MANAGED("Managed"),
    RESOLVED("Resolved"),
    UNKNOWN("Not recorded"),
}

/** User-entered existing illness or condition; the app never derives diagnoses. */
data class MedicalCondition(
    val id: String,
    val profileId: String,
    val name: String,
    val status: ConditionStatus,
    val diagnosedOn: LocalDate?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class MedicalConditionDraft(
    val name: String,
    val status: ConditionStatus,
    val diagnosedOn: LocalDate?,
    val notes: String?,
)

enum class HistoryCategory(override val label: String) : LabeledOption {
    CONSULTATION("Consultation"),
    TEST("Test / investigation"),
    PROCEDURE("Procedure"),
    HOSPITAL_ADMISSION("Hospital admission"),
    SURGERY("Surgery"),
    OTHER("Other"),
}

data class MedicalHistoryEntry(
    val id: String,
    val profileId: String,
    val eventDate: LocalDate,
    val title: String,
    val category: HistoryCategory,
    val hospitalId: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val recordedHospital: RecordedHospital? = null,
)

data class MedicalHistoryDraft(
    val eventDate: LocalDate,
    val title: String,
    val category: HistoryCategory,
    val hospitalId: String?,
    val notes: String?,
)

enum class MedicineForm(override val label: String) : LabeledOption {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    SYRUP("Syrup"),
    SUSPENSION("Suspension"),
    DROPS("Drops"),
    INJECTION("Injection"),
    INHALER("Inhaler"),
    CREAM_OINTMENT("Cream / ointment"),
    POWDER("Powder / sachet"),
    OTHER("Other"),
}

/** Reference-only catalog entry. It is never used to calculate or recommend a dose. */
data class Medicine(
    val id: String,
    val name: String,
    val brandName: String?,
    val genericNames: String?,
    val form: MedicineForm,
    val strength: String?,
    val concentration: String?,
    val ageReferenceNotes: String?,
    val manufacturer: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class MedicineDraft(
    val name: String,
    val brandName: String?,
    val genericNames: String?,
    val form: MedicineForm,
    val strength: String?,
    val concentration: String?,
    val ageReferenceNotes: String?,
    val manufacturer: String?,
    val notes: String?,
)

enum class MedicineRoute(override val label: String) : LabeledOption {
    ORAL("By mouth"),
    SUBLINGUAL("Under the tongue"),
    TOPICAL("On the skin"),
    EYE("Eye"),
    EAR("Ear"),
    NASAL("Nose"),
    INHALED("Inhaled"),
    INJECTION("Injection"),
    OTHER("Other"),
}

enum class FoodTiming(override val label: String) : LabeledOption {
    NOT_SPECIFIED("Not specified"),
    BEFORE_FOOD("Before food"),
    WITH_FOOD("With food"),
    AFTER_FOOD("After food"),
}

enum class CourseStatus(override val label: String) : LabeledOption {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    DISCONTINUED("Discontinued"),
}

/** A profile-specific medicine course, entered from a prescription. Schedules attach to this later. */
data class ProfileMedicine(
    val id: String,
    val profileId: String,
    val medicineId: String,
    /** Exactly as entered from the prescription, normalised decimal text (e.g. "0.5"). */
    val doseAmount: String,
    val doseUnit: String,
    val route: MedicineRoute?,
    val foodTiming: FoodTiming,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val prescribedBy: String?,
    val hospitalId: String?,
    val notes: String?,
    val status: CourseStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val prescribedMedicine: PrescribedMedicine? = null,
    val recordedHospital: RecordedHospital? = null,
)

/** Library details captured when the prescription was recorded; later library edits never change them. */
data class PrescribedMedicine(
    val name: String,
    val brandName: String?,
    val genericNames: String?,
    val form: MedicineForm,
    val strength: String?,
    val concentration: String?,
    val manufacturer: String?,
)

data class ProfileMedicineDraft(
    val medicineId: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: MedicineRoute?,
    val foodTiming: FoodTiming,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val prescribedBy: String?,
    val hospitalId: String?,
    val notes: String?,
    val status: CourseStatus,
)

data class ProfileMedicineItem(
    val course: ProfileMedicine,
    val medicine: Medicine?,
)
