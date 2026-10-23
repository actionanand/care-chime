package com.actionanand.carechime.app.data.repository

import com.actionanand.carechime.app.data.db.AllergyEntity
import com.actionanand.carechime.app.data.db.ConditionEntity
import com.actionanand.carechime.app.data.db.HistoryEntity
import com.actionanand.carechime.app.data.db.HospitalEntity
import com.actionanand.carechime.app.data.db.MedicineEntity
import com.actionanand.carechime.app.data.db.ProfileEntity
import com.actionanand.carechime.app.data.db.ProfileMedicineEntity
import com.actionanand.carechime.app.domain.model.Allergy
import com.actionanand.carechime.app.domain.model.AllergyCategory
import com.actionanand.carechime.app.domain.model.AllergySeverity
import com.actionanand.carechime.app.domain.model.BloodGroup
import com.actionanand.carechime.app.domain.model.ConditionStatus
import com.actionanand.carechime.app.domain.model.CourseStatus
import com.actionanand.carechime.app.domain.model.FoodTiming
import com.actionanand.carechime.app.domain.model.HistoryCategory
import com.actionanand.carechime.app.domain.model.HospitalRecord
import com.actionanand.carechime.app.domain.model.MedicalCondition
import com.actionanand.carechime.app.domain.model.MedicalHistoryEntry
import com.actionanand.carechime.app.domain.model.Medicine
import com.actionanand.carechime.app.domain.model.MedicineForm
import com.actionanand.carechime.app.domain.model.MedicineRoute
import com.actionanand.carechime.app.domain.model.Profile
import com.actionanand.carechime.app.domain.model.ProfileMedicine
import java.time.Instant
import java.time.LocalDate

internal inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    name?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

internal inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T = enumOrNull<T>(name) ?: fallback

internal fun String?.toLocalDateOrNull(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

internal fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

internal fun ProfileEntity.toDomain() = Profile(
    id = id,
    name = name,
    dateOfBirth = dateOfBirth.toLocalDateOrNull(),
    approximateAgeYears = approximateAgeYears,
    approximateAgeRecordedOn = approximateAgeRecordedOn.toLocalDateOrNull(),
    relationship = relationship,
    bloodGroup = enumOrNull<BloodGroup>(bloodGroup),
    medicalNotes = medicalNotes,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)

internal fun HospitalEntity.toDomain() = HospitalRecord(
    id = id,
    profileId = profileId,
    hospitalName = hospitalName,
    patientId = patientId,
    placeName = placeName,
    locationDetails = locationDetails,
    mapsUrl = mapsUrl,
    doctorName = doctorName,
    notes = notes,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)

internal fun AllergyEntity.toDomain() = Allergy(
    id = id,
    profileId = profileId,
    category = enumOr(category, AllergyCategory.OTHER),
    allergen = allergen,
    reaction = reaction,
    severity = enumOr(severity, AllergySeverity.UNKNOWN),
    notedOn = notedOn.toLocalDateOrNull(),
    notes = notes,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)

internal fun ConditionEntity.toDomain() = MedicalCondition(
    id = id,
    profileId = profileId,
    name = name,
    status = enumOr(status, ConditionStatus.UNKNOWN),
    diagnosedOn = diagnosedOn.toLocalDateOrNull(),
    notes = notes,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)

internal fun HistoryEntity.toDomain() = MedicalHistoryEntry(
    id = id,
    profileId = profileId,
    eventDate = LocalDate.parse(eventDate),
    title = title,
    category = enumOr(category, HistoryCategory.OTHER),
    hospitalId = hospitalId,
    notes = notes,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)

internal fun MedicineEntity.toDomain() = Medicine(
    id = id,
    name = name,
    brandName = brandName,
    genericNames = genericNames,
    form = enumOr(form, MedicineForm.OTHER),
    strength = strength,
    concentration = concentration,
    ageReferenceNotes = ageReferenceNotes,
    manufacturer = manufacturer,
    notes = notes,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)

internal fun ProfileMedicineEntity.toDomain() = ProfileMedicine(
    id = id,
    profileId = profileId,
    medicineId = medicineId,
    doseAmount = doseAmount,
    doseUnit = doseUnit,
    route = enumOrNull<MedicineRoute>(route),
    foodTiming = enumOr(foodTiming, FoodTiming.NOT_SPECIFIED),
    startDate = LocalDate.parse(startDate),
    endDate = endDate.toLocalDateOrNull(),
    prescribedBy = prescribedBy,
    hospitalId = hospitalId,
    notes = notes,
    // Unknown future values fall back to a non-reminding state.
    status = enumOr(status, CourseStatus.PAUSED),
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)
