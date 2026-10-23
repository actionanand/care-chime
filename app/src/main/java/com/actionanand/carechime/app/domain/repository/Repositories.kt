package com.actionanand.carechime.app.domain.repository

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
import kotlinx.coroutines.flow.Flow

/** Domain failures carry user-safe messages that never include entered medical details. */
open class DomainException(message: String) : Exception(message)

class RecordNotFoundException(what: String) : DomainException("$what was not found. It may have been deleted.")

class OwnershipException(what: String) : DomainException("$what does not belong to this profile.")

class MedicineInUseException(val courseCount: Int) : DomainException(
    "This medicine is used by $courseCount profile medicine record(s). Remove those records first.",
)

/*
 * Every child record is addressed by (profileId, recordId) so that a record can never be read
 * or modified through a different profile.
 */

interface ProfileRepository {
    fun observeProfiles(): Flow<List<Profile>>
    fun observeProfile(id: String): Flow<Profile?>
    fun observeProfileCount(): Flow<Int>
    suspend fun getProfile(id: String): Profile?
    suspend fun createProfile(draft: ProfileDraft): String
    suspend fun updateProfile(id: String, draft: ProfileDraft)
    suspend fun deletionSummary(id: String): ProfileDeletionSummary
    suspend fun deleteProfile(id: String)
}

interface HospitalRepository {
    fun observeHospitals(profileId: String): Flow<List<HospitalRecord>>
    suspend fun getHospital(profileId: String, hospitalId: String): HospitalRecord?
    suspend fun createHospital(profileId: String, draft: HospitalDraft): String
    suspend fun updateHospital(profileId: String, hospitalId: String, draft: HospitalDraft)
    suspend fun deleteHospital(profileId: String, hospitalId: String)
}

interface MedicalRecordRepository {
    fun observeAllergies(profileId: String): Flow<List<Allergy>>
    suspend fun getAllergy(profileId: String, allergyId: String): Allergy?
    suspend fun createAllergy(profileId: String, draft: AllergyDraft): String
    suspend fun updateAllergy(profileId: String, allergyId: String, draft: AllergyDraft)
    suspend fun deleteAllergy(profileId: String, allergyId: String)

    fun observeConditions(profileId: String): Flow<List<MedicalCondition>>
    suspend fun getCondition(profileId: String, conditionId: String): MedicalCondition?
    suspend fun createCondition(profileId: String, draft: MedicalConditionDraft): String
    suspend fun updateCondition(profileId: String, conditionId: String, draft: MedicalConditionDraft)
    suspend fun deleteCondition(profileId: String, conditionId: String)

    fun observeHistory(profileId: String): Flow<List<MedicalHistoryEntry>>
    suspend fun getHistoryEntry(profileId: String, entryId: String): MedicalHistoryEntry?
    suspend fun createHistoryEntry(profileId: String, draft: MedicalHistoryDraft): String
    suspend fun updateHistoryEntry(profileId: String, entryId: String, draft: MedicalHistoryDraft)
    suspend fun deleteHistoryEntry(profileId: String, entryId: String)
}

interface MedicineCatalogRepository {
    fun observeMedicines(): Flow<List<Medicine>>
    fun observeMedicineCount(): Flow<Int>
    suspend fun getMedicine(id: String): Medicine?
    suspend fun createMedicine(draft: MedicineDraft): String
    suspend fun updateMedicine(id: String, draft: MedicineDraft)
    suspend fun usageCount(id: String): Int

    /** @throws MedicineInUseException when profile medicine records still reference it. */
    suspend fun deleteMedicine(id: String)
}

interface ProfileMedicineRepository {
    fun observeProfileMedicines(profileId: String): Flow<List<ProfileMedicineItem>>
    fun observeActiveCourseCount(): Flow<Int>
    suspend fun getProfileMedicine(profileId: String, courseId: String): ProfileMedicine?
    suspend fun createProfileMedicine(profileId: String, draft: ProfileMedicineDraft): String
    suspend fun updateProfileMedicine(profileId: String, courseId: String, draft: ProfileMedicineDraft)
    suspend fun deleteProfileMedicine(profileId: String, courseId: String)
}
