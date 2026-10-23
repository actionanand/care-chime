package com.actionanand.carechime.app.data.repository

import androidx.room.withTransaction
import com.actionanand.carechime.app.data.db.AllergyEntity
import com.actionanand.carechime.app.data.db.CareChimeDatabase
import com.actionanand.carechime.app.data.db.ConditionEntity
import com.actionanand.carechime.app.data.db.HistoryEntity
import com.actionanand.carechime.app.data.db.HospitalEntity
import com.actionanand.carechime.app.data.db.MedicineEntity
import com.actionanand.carechime.app.data.db.ProfileEntity
import com.actionanand.carechime.app.data.db.ProfileMedicineEntity
import com.actionanand.carechime.app.domain.model.Allergy
import com.actionanand.carechime.app.domain.model.AllergyDraft
import com.actionanand.carechime.app.domain.model.HospitalDraft
import com.actionanand.carechime.app.domain.model.HospitalRecord
import com.actionanand.carechime.app.domain.model.MedicalCondition
import com.actionanand.carechime.app.domain.model.MedicalConditionDraft
import com.actionanand.carechime.app.domain.model.MedicalHistoryDraft
import com.actionanand.carechime.app.domain.model.MedicalHistoryEntry
import com.actionanand.carechime.app.domain.model.Medicine
import com.actionanand.carechime.app.domain.model.MedicineDraft
import com.actionanand.carechime.app.domain.model.Profile
import com.actionanand.carechime.app.domain.model.ProfileDeletionSummary
import com.actionanand.carechime.app.domain.model.ProfileDraft
import com.actionanand.carechime.app.domain.model.ProfileMedicine
import com.actionanand.carechime.app.domain.model.ProfileMedicineDraft
import com.actionanand.carechime.app.domain.model.ProfileMedicineItem
import com.actionanand.carechime.app.domain.model.RecordedHospital
import com.actionanand.carechime.app.domain.repository.HospitalRepository
import com.actionanand.carechime.app.domain.repository.MedicalRecordRepository
import com.actionanand.carechime.app.domain.repository.MedicineCatalogRepository
import com.actionanand.carechime.app.domain.repository.MedicineInUseException
import com.actionanand.carechime.app.domain.repository.OwnershipException
import com.actionanand.carechime.app.domain.repository.ProfileMedicineRepository
import com.actionanand.carechime.app.domain.repository.ProfileRepository
import com.actionanand.carechime.app.domain.repository.RecordNotFoundException
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

fun newRecordId(): String = UUID.randomUUID().toString()

class RoomProfileRepository(
    private val db: CareChimeDatabase,
    private val clock: Clock,
    private val newId: () -> String = ::newRecordId,
) : ProfileRepository {
    private val dao = db.profileDao()

    override fun observeProfiles(): Flow<List<Profile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeProfile(id: String): Flow<Profile?> = dao.observe(id).map { it?.toDomain() }

    override fun observeProfileCount(): Flow<Int> = dao.observeCount()

    override suspend fun getProfile(id: String): Profile? = dao.get(id)?.toDomain()

    override suspend fun createProfile(draft: ProfileDraft): String {
        val now = clock.millis()
        val id = newId()
        dao.insert(draft.toEntity(id, createdAt = now, updatedAt = now))
        return id
    }

    override suspend fun updateProfile(id: String, draft: ProfileDraft) {
        db.withTransaction {
            val existing = dao.get(id) ?: throw RecordNotFoundException("Profile")
            dao.update(draft.toEntity(id, createdAt = existing.createdAt, updatedAt = clock.millis()))
        }
    }

    override suspend fun deletionSummary(id: String): ProfileDeletionSummary = db.withTransaction {
        ProfileDeletionSummary(
            hospitals = dao.countHospitals(id),
            allergies = dao.countAllergies(id),
            conditions = dao.countConditions(id),
            historyEntries = dao.countHistory(id),
            medicines = dao.countMedicines(id),
        )
    }

    override suspend fun deleteProfile(id: String) {
        if (dao.delete(id) == 0) throw RecordNotFoundException("Profile")
    }

    private fun ProfileDraft.toEntity(id: String, createdAt: Long, updatedAt: Long) = ProfileEntity(
        id = id,
        name = name,
        dateOfBirth = dateOfBirth?.toString(),
        approximateAgeYears = approximateAgeYears,
        approximateAgeRecordedOn = approximateAgeRecordedOn?.toString(),
        relationship = relationship,
        bloodGroup = bloodGroup?.name,
        medicalNotes = medicalNotes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

class RoomHospitalRepository(
    private val db: CareChimeDatabase,
    private val clock: Clock,
    private val newId: () -> String = ::newRecordId,
) : HospitalRepository {
    private val dao = db.hospitalDao()

    override fun observeHospitals(profileId: String): Flow<List<HospitalRecord>> =
        dao.observeForProfile(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun getHospital(profileId: String, hospitalId: String): HospitalRecord? =
        dao.get(profileId, hospitalId)?.toDomain()

    override suspend fun createHospital(profileId: String, draft: HospitalDraft): String = db.withTransaction {
        db.profileDao().get(profileId) ?: throw RecordNotFoundException("Profile")
        val now = clock.millis()
        val id = newId()
        dao.insert(draft.toEntity(id, profileId, now, now))
        id
    }

    override suspend fun updateHospital(profileId: String, hospitalId: String, draft: HospitalDraft) {
        db.withTransaction {
            val existing = dao.get(profileId, hospitalId) ?: throw RecordNotFoundException("Hospital record")
            dao.update(draft.toEntity(hospitalId, profileId, existing.createdAt, clock.millis()))
        }
    }

    override suspend fun deleteHospital(profileId: String, hospitalId: String) {
        if (dao.delete(profileId, hospitalId) == 0) throw RecordNotFoundException("Hospital record")
    }

    private fun HospitalDraft.toEntity(id: String, profileId: String, createdAt: Long, updatedAt: Long) =
        HospitalEntity(
            id = id,
            profileId = profileId,
            hospitalName = hospitalName,
            patientId = patientId,
            placeName = placeName,
            locationDetails = locationDetails,
            mapsUrl = mapsUrl,
            doctorName = doctorName,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}

class RoomMedicalRecordRepository(
    private val db: CareChimeDatabase,
    private val clock: Clock,
    private val newId: () -> String = ::newRecordId,
) : MedicalRecordRepository {
    private val dao = db.medicalRecordDao()

    private suspend fun requireProfile(profileId: String) {
        db.profileDao().get(profileId) ?: throw RecordNotFoundException("Profile")
    }

    override fun observeAllergies(profileId: String): Flow<List<Allergy>> =
        dao.observeAllergies(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllergy(profileId: String, allergyId: String): Allergy? =
        dao.getAllergy(profileId, allergyId)?.toDomain()

    override suspend fun createAllergy(profileId: String, draft: AllergyDraft): String = db.withTransaction {
        requireProfile(profileId)
        val now = clock.millis()
        val id = newId()
        dao.insertAllergy(draft.toEntity(id, profileId, now, now))
        id
    }

    override suspend fun updateAllergy(profileId: String, allergyId: String, draft: AllergyDraft) {
        db.withTransaction {
            val existing = dao.getAllergy(profileId, allergyId) ?: throw RecordNotFoundException("Allergy")
            dao.updateAllergy(draft.toEntity(allergyId, profileId, existing.createdAt, clock.millis()))
        }
    }

    override suspend fun deleteAllergy(profileId: String, allergyId: String) {
        if (dao.deleteAllergy(profileId, allergyId) == 0) throw RecordNotFoundException("Allergy")
    }

    override fun observeConditions(profileId: String): Flow<List<MedicalCondition>> =
        dao.observeConditions(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun getCondition(profileId: String, conditionId: String): MedicalCondition? =
        dao.getCondition(profileId, conditionId)?.toDomain()

    override suspend fun createCondition(profileId: String, draft: MedicalConditionDraft): String =
        db.withTransaction {
            requireProfile(profileId)
            val now = clock.millis()
            val id = newId()
            dao.insertCondition(draft.toEntity(id, profileId, now, now))
            id
        }

    override suspend fun updateCondition(profileId: String, conditionId: String, draft: MedicalConditionDraft) {
        db.withTransaction {
            val existing = dao.getCondition(profileId, conditionId) ?: throw RecordNotFoundException("Condition")
            dao.updateCondition(draft.toEntity(conditionId, profileId, existing.createdAt, clock.millis()))
        }
    }

    override suspend fun deleteCondition(profileId: String, conditionId: String) {
        if (dao.deleteCondition(profileId, conditionId) == 0) throw RecordNotFoundException("Condition")
    }

    override fun observeHistory(profileId: String): Flow<List<MedicalHistoryEntry>> =
        dao.observeHistory(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun getHistoryEntry(profileId: String, entryId: String): MedicalHistoryEntry? =
        dao.getHistory(profileId, entryId)?.toDomain()

    override suspend fun createHistoryEntry(profileId: String, draft: MedicalHistoryDraft): String =
        db.withTransaction {
            requireProfile(profileId)
            val hospital = requireOwnedHospital(db, profileId, draft.hospitalId)
            val now = clock.millis()
            val id = newId()
            dao.insertHistory(draft.toEntity(id, profileId, now, now, hospitalSnapshot(hospital)))
            id
        }

    override suspend fun updateHistoryEntry(profileId: String, entryId: String, draft: MedicalHistoryDraft) {
        db.withTransaction {
            val existing = dao.getHistory(profileId, entryId) ?: throw RecordNotFoundException("History entry")
            val hospital = requireOwnedHospital(db, profileId, draft.hospitalId)
            val snapshot = keptHospitalSnapshot(
                existing.hospitalId, existing.hospitalName, existing.hospitalPlaceName, draft.hospitalId, hospital,
            )
            dao.updateHistory(draft.toEntity(entryId, profileId, existing.createdAt, clock.millis(), snapshot))
        }
    }

    override suspend fun deleteHistoryEntry(profileId: String, entryId: String) {
        if (dao.deleteHistory(profileId, entryId) == 0) throw RecordNotFoundException("History entry")
    }

    private fun AllergyDraft.toEntity(id: String, profileId: String, createdAt: Long, updatedAt: Long) =
        AllergyEntity(
            id = id,
            profileId = profileId,
            category = category.name,
            allergen = allergen,
            reaction = reaction,
            severity = severity.name,
            notedOn = notedOn?.toString(),
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun MedicalConditionDraft.toEntity(id: String, profileId: String, createdAt: Long, updatedAt: Long) =
        ConditionEntity(
            id = id,
            profileId = profileId,
            name = name,
            status = status.name,
            diagnosedOn = diagnosedOn?.toString(),
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun MedicalHistoryDraft.toEntity(
        id: String,
        profileId: String,
        createdAt: Long,
        updatedAt: Long,
        hospital: RecordedHospital?,
    ) =
        HistoryEntity(
            id = id,
            profileId = profileId,
            eventDate = eventDate.toString(),
            title = title,
            category = category.name,
            hospitalId = hospitalId,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            hospitalName = hospital?.hospitalName,
            hospitalPlaceName = hospital?.placeName,
        )
}

/** Rejects links to a hospital record that belongs to a different profile. */
private suspend fun requireOwnedHospital(db: CareChimeDatabase, profileId: String, hospitalId: String?): HospitalEntity? {
    if (hospitalId == null) return null
    return db.hospitalDao().get(profileId, hospitalId) ?: throw OwnershipException("The selected hospital")
}

private fun hospitalSnapshot(hospital: HospitalEntity?): RecordedHospital? =
    hospital?.let { RecordedHospital(it.hospitalName, it.placeName) }

/**
 * Keeps the hospital details recorded with an entry while its link is unchanged, including after the
 * hospital record was deleted (link already null). A newly chosen hospital is captured afresh.
 */
private fun keptHospitalSnapshot(
    existingHospitalId: String?,
    existingName: String?,
    existingPlaceName: String?,
    newHospitalId: String?,
    newHospital: HospitalEntity?,
): RecordedHospital? =
    if (existingHospitalId == newHospitalId && (existingName != null || newHospitalId == null)) {
        recordedHospitalOf(existingName, existingPlaceName)
    } else {
        hospitalSnapshot(newHospital)
    }

class RoomMedicineCatalogRepository(
    private val db: CareChimeDatabase,
    private val clock: Clock,
    private val newId: () -> String = ::newRecordId,
) : MedicineCatalogRepository {
    private val dao = db.medicineDao()

    override fun observeMedicines(): Flow<List<Medicine>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeMedicineCount(): Flow<Int> = dao.observeCount()

    override suspend fun getMedicine(id: String): Medicine? = dao.get(id)?.toDomain()

    override suspend fun createMedicine(draft: MedicineDraft): String {
        val now = clock.millis()
        val id = newId()
        dao.insert(draft.toEntity(id, now, now))
        return id
    }

    override suspend fun updateMedicine(id: String, draft: MedicineDraft) {
        db.withTransaction {
            val existing = dao.get(id) ?: throw RecordNotFoundException("Medicine")
            dao.update(draft.toEntity(id, existing.createdAt, clock.millis()))
        }
    }

    override suspend fun usageCount(id: String): Int = dao.usageCount(id)

    override suspend fun deleteMedicine(id: String) {
        db.withTransaction {
            val usage = dao.usageCount(id)
            if (usage > 0) throw MedicineInUseException(usage)
            if (dao.delete(id) == 0) throw RecordNotFoundException("Medicine")
        }
    }

    private fun MedicineDraft.toEntity(id: String, createdAt: Long, updatedAt: Long) = MedicineEntity(
        id = id,
        name = name,
        brandName = brandName,
        genericNames = genericNames,
        form = form.name,
        strength = strength,
        concentration = concentration,
        ageReferenceNotes = ageReferenceNotes,
        manufacturer = manufacturer,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

class RoomProfileMedicineRepository(
    private val db: CareChimeDatabase,
    private val clock: Clock,
    private val newId: () -> String = ::newRecordId,
) : ProfileMedicineRepository {
    private val dao = db.profileMedicineDao()

    override fun observeProfileMedicines(profileId: String): Flow<List<ProfileMedicineItem>> =
        combine(dao.observeForProfile(profileId), db.medicineDao().observeAll()) { courses, medicines ->
            val byId = medicines.associateBy { it.id }
            courses.map { course -> ProfileMedicineItem(course.toDomain(), byId[course.medicineId]?.toDomain()) }
        }

    override fun observeActiveCourseCount(): Flow<Int> = dao.observeActiveCount()

    override suspend fun getProfileMedicine(profileId: String, courseId: String): ProfileMedicine? =
        dao.get(profileId, courseId)?.toDomain()

    override suspend fun createProfileMedicine(profileId: String, draft: ProfileMedicineDraft): String =
        db.withTransaction {
            db.profileDao().get(profileId) ?: throw RecordNotFoundException("Profile")
            val medicine = requireMedicine(draft)
            val hospital = requireOwnedHospital(db, profileId, draft.hospitalId)
            val now = clock.millis()
            val id = newId()
            dao.insert(draft.toEntity(id, profileId, now, now).withMedicine(medicine).withHospital(hospitalSnapshot(hospital)))
            id
        }

    /**
     * Prescription-time snapshots stay as recorded while the medicine/hospital link is unchanged, so
     * later library or hospital edits never rewrite this record. Choosing a different one re-captures.
     */
    override suspend fun updateProfileMedicine(profileId: String, courseId: String, draft: ProfileMedicineDraft) {
        db.withTransaction {
            val existing = dao.get(profileId, courseId) ?: throw RecordNotFoundException("Profile medicine")
            val medicine = requireMedicine(draft)
            val hospital = requireOwnedHospital(db, profileId, draft.hospitalId)
            val updated = draft.toEntity(courseId, profileId, existing.createdAt, clock.millis())
            val withMedicine = if (existing.medicineId == draft.medicineId && existing.medicineName != null) {
                updated.copy(
                    medicineName = existing.medicineName,
                    medicineBrandName = existing.medicineBrandName,
                    medicineGenericNames = existing.medicineGenericNames,
                    medicineForm = existing.medicineForm,
                    medicineStrength = existing.medicineStrength,
                    medicineConcentration = existing.medicineConcentration,
                    medicineManufacturer = existing.medicineManufacturer,
                )
            } else {
                updated.withMedicine(medicine)
            }
            val snapshot = keptHospitalSnapshot(
                existing.hospitalId, existing.hospitalName, existing.hospitalPlaceName, draft.hospitalId, hospital,
            )
            dao.update(withMedicine.withHospital(snapshot))
        }
    }

    override suspend fun deleteProfileMedicine(profileId: String, courseId: String) {
        if (dao.delete(profileId, courseId) == 0) throw RecordNotFoundException("Profile medicine")
    }

    private suspend fun requireMedicine(draft: ProfileMedicineDraft): MedicineEntity =
        db.medicineDao().get(draft.medicineId) ?: throw RecordNotFoundException("Medicine")

    private fun ProfileMedicineEntity.withMedicine(medicine: MedicineEntity) = copy(
        medicineName = medicine.name,
        medicineBrandName = medicine.brandName,
        medicineGenericNames = medicine.genericNames,
        medicineForm = medicine.form,
        medicineStrength = medicine.strength,
        medicineConcentration = medicine.concentration,
        medicineManufacturer = medicine.manufacturer,
    )

    private fun ProfileMedicineEntity.withHospital(hospital: RecordedHospital?) =
        copy(hospitalName = hospital?.hospitalName, hospitalPlaceName = hospital?.placeName)

    private fun ProfileMedicineDraft.toEntity(id: String, profileId: String, createdAt: Long, updatedAt: Long) =
        ProfileMedicineEntity(
            id = id,
            profileId = profileId,
            medicineId = medicineId,
            doseAmount = doseAmount,
            doseUnit = doseUnit,
            route = route?.name,
            foodTiming = foodTiming.name,
            startDate = startDate.toString(),
            endDate = endDate?.toString(),
            prescribedBy = prescribedBy,
            hospitalId = hospitalId,
            notes = notes,
            status = status.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
