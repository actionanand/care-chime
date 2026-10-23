package com.actionanand.carechime.app.ui.medical

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.actionanand.carechime.app.domain.model.Allergy
import com.actionanand.carechime.app.domain.model.AllergyDraft
import com.actionanand.carechime.app.domain.model.HospitalRecord
import com.actionanand.carechime.app.domain.model.MedicalCondition
import com.actionanand.carechime.app.domain.model.MedicalConditionDraft
import com.actionanand.carechime.app.domain.model.MedicalHistoryDraft
import com.actionanand.carechime.app.domain.model.MedicalHistoryEntry
import com.actionanand.carechime.app.domain.repository.HospitalRepository
import com.actionanand.carechime.app.domain.repository.MedicalRecordRepository
import com.actionanand.carechime.app.domain.repository.ProfileRepository
import com.actionanand.carechime.app.domain.validation.AllergyInput
import com.actionanand.carechime.app.domain.validation.AllergyValidator
import com.actionanand.carechime.app.domain.validation.ConditionInput
import com.actionanand.carechime.app.domain.validation.ConditionValidator
import com.actionanand.carechime.app.domain.validation.FormResult
import com.actionanand.carechime.app.domain.validation.HistoryInput
import com.actionanand.carechime.app.domain.validation.HistoryValidator
import com.actionanand.carechime.app.ui.NEW_RECORD_ID
import com.actionanand.carechime.app.ui.RecordEditorViewModel
import com.actionanand.carechime.app.ui.navigation.Routes
import com.actionanand.carechime.app.ui.stateWhileSubscribed
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class MedicalRecords(
    val allergies: List<Allergy>,
    val conditions: List<MedicalCondition>,
    val history: List<MedicalHistoryEntry>,
    val hospitalNames: Map<String, String>,
)

class MedicalRecordsViewModel(
    handle: SavedStateHandle,
    profiles: ProfileRepository,
    medicalRecords: MedicalRecordRepository,
    hospitals: HospitalRepository,
) : ViewModel() {
    val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    val profileName: StateFlow<String?> = profiles.observeProfile(profileId).map { it?.name }.stateWhileSubscribed(this, null)

    val records: StateFlow<MedicalRecords?> = combine(
        medicalRecords.observeAllergies(profileId),
        medicalRecords.observeConditions(profileId),
        medicalRecords.observeHistory(profileId),
        hospitals.observeHospitals(profileId),
    ) { allergies, conditions, history, hospitalList ->
        MedicalRecords(allergies, conditions, history, hospitalList.associate { it.id to it.hospitalName })
    }.stateWhileSubscribed(this, null)
}

class AllergyEditViewModel(
    handle: SavedStateHandle,
    private val repository: MedicalRecordRepository,
    private val clock: Clock,
) : RecordEditorViewModel<AllergyInput, AllergyDraft>(AllergyInput()) {
    private val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    private val recordId: String = checkNotNull(handle.get<String>(Routes.ARG_RECORD_ID))
    override val isEditing: Boolean = recordId != NEW_RECORD_ID

    init {
        if (isEditing) {
            loadExisting {
                repository.getAllergy(profileId, recordId)?.let {
                    AllergyInput(
                        category = it.category,
                        allergen = it.allergen,
                        reaction = it.reaction.orEmpty(),
                        severity = it.severity,
                        notedOn = it.notedOn?.toString().orEmpty(),
                        notes = it.notes.orEmpty(),
                    )
                }
            }
        }
    }

    override fun validate(input: AllergyInput): FormResult<AllergyDraft> =
        AllergyValidator.validate(input, LocalDate.now(clock))

    override suspend fun persist(draft: AllergyDraft) {
        if (isEditing) repository.updateAllergy(profileId, recordId, draft) else repository.createAllergy(profileId, draft)
    }

    override suspend fun remove() {
        repository.deleteAllergy(profileId, recordId)
    }
}

class ConditionEditViewModel(
    handle: SavedStateHandle,
    private val repository: MedicalRecordRepository,
    private val clock: Clock,
) : RecordEditorViewModel<ConditionInput, MedicalConditionDraft>(ConditionInput()) {
    private val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    private val recordId: String = checkNotNull(handle.get<String>(Routes.ARG_RECORD_ID))
    override val isEditing: Boolean = recordId != NEW_RECORD_ID

    init {
        if (isEditing) {
            loadExisting {
                repository.getCondition(profileId, recordId)?.let {
                    ConditionInput(
                        name = it.name,
                        status = it.status,
                        diagnosedOn = it.diagnosedOn?.toString().orEmpty(),
                        notes = it.notes.orEmpty(),
                    )
                }
            }
        }
    }

    override fun validate(input: ConditionInput): FormResult<MedicalConditionDraft> =
        ConditionValidator.validate(input, LocalDate.now(clock))

    override suspend fun persist(draft: MedicalConditionDraft) {
        if (isEditing) repository.updateCondition(profileId, recordId, draft) else repository.createCondition(profileId, draft)
    }

    override suspend fun remove() {
        repository.deleteCondition(profileId, recordId)
    }
}

class HistoryEditViewModel(
    handle: SavedStateHandle,
    private val repository: MedicalRecordRepository,
    hospitals: HospitalRepository,
    private val clock: Clock,
) : RecordEditorViewModel<HistoryInput, MedicalHistoryDraft>(HistoryInput()) {
    private val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    private val recordId: String = checkNotNull(handle.get<String>(Routes.ARG_RECORD_ID))
    override val isEditing: Boolean = recordId != NEW_RECORD_ID

    val hospitals: StateFlow<List<HospitalRecord>> =
        hospitals.observeHospitals(profileId).stateWhileSubscribed(this, emptyList())

    init {
        if (isEditing) {
            loadExisting {
                repository.getHistoryEntry(profileId, recordId)?.let {
                    HistoryInput(
                        eventDate = it.eventDate.toString(),
                        title = it.title,
                        category = it.category,
                        hospitalId = it.hospitalId,
                        notes = it.notes.orEmpty(),
                    )
                }
            }
        }
    }

    override fun validate(input: HistoryInput): FormResult<MedicalHistoryDraft> =
        HistoryValidator.validate(input, LocalDate.now(clock))

    override suspend fun persist(draft: MedicalHistoryDraft) {
        if (isEditing) {
            repository.updateHistoryEntry(profileId, recordId, draft)
        } else {
            repository.createHistoryEntry(profileId, draft)
        }
    }

    override suspend fun remove() {
        repository.deleteHistoryEntry(profileId, recordId)
    }
}
