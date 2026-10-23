package com.actionanand.carechime.app.ui.medicines

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actionanand.carechime.app.domain.model.HospitalRecord
import com.actionanand.carechime.app.domain.model.Medicine
import com.actionanand.carechime.app.domain.model.MedicineDraft
import com.actionanand.carechime.app.domain.model.PrescribedMedicine
import com.actionanand.carechime.app.domain.model.ProfileMedicineDraft
import com.actionanand.carechime.app.domain.model.ProfileMedicineItem
import com.actionanand.carechime.app.domain.repository.HospitalRepository
import com.actionanand.carechime.app.domain.repository.MedicineCatalogRepository
import com.actionanand.carechime.app.domain.repository.ProfileMedicineRepository
import com.actionanand.carechime.app.domain.repository.ProfileRepository
import com.actionanand.carechime.app.domain.validation.FormResult
import com.actionanand.carechime.app.domain.validation.MedicineInput
import com.actionanand.carechime.app.domain.validation.MedicineValidator
import com.actionanand.carechime.app.domain.validation.ProfileMedicineInput
import com.actionanand.carechime.app.domain.validation.ProfileMedicineValidator
import com.actionanand.carechime.app.ui.NEW_RECORD_ID
import com.actionanand.carechime.app.ui.RecordEditorViewModel
import com.actionanand.carechime.app.ui.navigation.Routes
import com.actionanand.carechime.app.ui.stateWhileSubscribed
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** SavedStateHandle key used to hand a newly created catalog medicine back to the course editor. */
const val CREATED_MEDICINE_ID_KEY = "createdMedicineId"

fun Medicine.displayName(): String = if (brandName.isNullOrBlank() || brandName == name) name else "$name ($brandName)"

fun Medicine.detailLine(): String = listOfNotNull(form.label, strength, concentration).joinToString(" · ")

fun PrescribedMedicine.displayName(): String =
    if (brandName.isNullOrBlank() || brandName == name) name else "$name ($brandName)"

fun PrescribedMedicine.detailLine(): String = listOfNotNull(form.label, strength, concentration).joinToString(" · ")

/** True when the library entry no longer matches what was recorded with the prescription. */
fun PrescribedMedicine.differsFrom(medicine: Medicine): Boolean =
    this != PrescribedMedicine(
        medicine.name, medicine.brandName, medicine.genericNames, medicine.form,
        medicine.strength, medicine.concentration, medicine.manufacturer,
    )

class MedicineLibraryViewModel(medicines: MedicineCatalogRepository) : ViewModel() {
    val medicines: StateFlow<List<Medicine>?> = medicines.observeMedicines().stateWhileSubscribed(this, null)
}

class MedicineEditViewModel(
    handle: SavedStateHandle,
    private val medicines: MedicineCatalogRepository,
) : RecordEditorViewModel<MedicineInput, MedicineDraft>(MedicineInput()) {
    private val medicineId: String = checkNotNull(handle.get<String>(Routes.ARG_RECORD_ID))
    override val isEditing: Boolean = medicineId != NEW_RECORD_ID

    var createdMedicineId: String? = null
        private set

    init {
        if (isEditing) {
            loadExisting {
                medicines.getMedicine(medicineId)?.let {
                    MedicineInput(
                        name = it.name,
                        brandName = it.brandName.orEmpty(),
                        genericNames = it.genericNames.orEmpty(),
                        form = it.form,
                        strength = it.strength.orEmpty(),
                        concentration = it.concentration.orEmpty(),
                        ageReferenceNotes = it.ageReferenceNotes.orEmpty(),
                        manufacturer = it.manufacturer.orEmpty(),
                        notes = it.notes.orEmpty(),
                    )
                }
            }
        }
    }

    override fun validate(input: MedicineInput): FormResult<MedicineDraft> = MedicineValidator.validate(input)

    override suspend fun persist(draft: MedicineDraft) {
        if (isEditing) {
            medicines.updateMedicine(medicineId, draft)
        } else {
            createdMedicineId = medicines.createMedicine(draft)
        }
    }

    override suspend fun remove() {
        medicines.deleteMedicine(medicineId)
    }
}

class ProfileMedicinesViewModel(
    handle: SavedStateHandle,
    profiles: ProfileRepository,
    profileMedicines: ProfileMedicineRepository,
    hospitals: HospitalRepository,
) : ViewModel() {
    val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    val profileName: StateFlow<String?> = profiles.observeProfile(profileId).map { it?.name }.stateWhileSubscribed(this, null)
    val items: StateFlow<List<ProfileMedicineItem>?> =
        profileMedicines.observeProfileMedicines(profileId).stateWhileSubscribed(this, null)
    val hospitalNames: StateFlow<Map<String, String>> = hospitals.observeHospitals(profileId)
        .map { list -> list.associate { it.id to it.hospitalName } }
        .stateWhileSubscribed(this, emptyMap())
}

class ProfileMedicineEditViewModel(
    handle: SavedStateHandle,
    private val profileMedicines: ProfileMedicineRepository,
    medicines: MedicineCatalogRepository,
    hospitals: HospitalRepository,
    private val clock: Clock,
) : RecordEditorViewModel<ProfileMedicineInput, ProfileMedicineDraft>(
    ProfileMedicineInput(startDate = LocalDate.now(clock).toString()),
) {
    val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    private val courseId: String = checkNotNull(handle.get<String>(Routes.ARG_RECORD_ID))
    override val isEditing: Boolean = courseId != NEW_RECORD_ID

    val medicines: StateFlow<List<Medicine>?> = medicines.observeMedicines().stateWhileSubscribed(this, null)
    val hospitals: StateFlow<List<HospitalRecord>> =
        hospitals.observeHospitals(profileId).stateWhileSubscribed(this, emptyList())

    /** Medicine details stored with this record; kept on save unless a different medicine is chosen. */
    var recordedMedicine by mutableStateOf<Pair<String, PrescribedMedicine>?>(null)
        private set

    init {
        if (isEditing) {
            loadExisting {
                profileMedicines.getProfileMedicine(profileId, courseId)?.let {
                    recordedMedicine = it.prescribedMedicine?.let { recorded -> it.medicineId to recorded }
                    ProfileMedicineInput(
                        medicineId = it.medicineId,
                        doseAmount = it.doseAmount,
                        doseUnit = it.doseUnit,
                        route = it.route,
                        foodTiming = it.foodTiming,
                        startDate = it.startDate.toString(),
                        endDate = it.endDate?.toString().orEmpty(),
                        prescribedBy = it.prescribedBy.orEmpty(),
                        hospitalId = it.hospitalId,
                        notes = it.notes.orEmpty(),
                        status = it.status,
                    )
                }
            }
        }
        viewModelScope.launch {
            handle.getStateFlow<String?>(CREATED_MEDICINE_ID_KEY, null).filterNotNull().collect { createdId ->
                update { it.copy(medicineId = createdId) }
                handle.set<String>(CREATED_MEDICINE_ID_KEY, null)
            }
        }
    }

    override fun validate(input: ProfileMedicineInput): FormResult<ProfileMedicineDraft> =
        ProfileMedicineValidator.validate(input, LocalDate.now(clock))

    override suspend fun persist(draft: ProfileMedicineDraft) {
        if (isEditing) {
            profileMedicines.updateProfileMedicine(profileId, courseId, draft)
        } else {
            profileMedicines.createProfileMedicine(profileId, draft)
        }
    }

    override suspend fun remove() {
        profileMedicines.deleteProfileMedicine(profileId, courseId)
    }
}
