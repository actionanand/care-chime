package com.actionanand.carechime.app.ui.profiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actionanand.carechime.app.domain.model.Profile
import com.actionanand.carechime.app.domain.model.ProfileDeletionSummary
import com.actionanand.carechime.app.domain.model.ProfileDraft
import com.actionanand.carechime.app.domain.repository.DomainException
import com.actionanand.carechime.app.domain.repository.HospitalRepository
import com.actionanand.carechime.app.domain.repository.MedicalRecordRepository
import com.actionanand.carechime.app.domain.repository.ProfileMedicineRepository
import com.actionanand.carechime.app.domain.repository.ProfileRepository
import com.actionanand.carechime.app.domain.validation.FormResult
import com.actionanand.carechime.app.domain.validation.ProfileInput
import com.actionanand.carechime.app.domain.validation.ProfileValidator
import com.actionanand.carechime.app.ui.Loadable
import com.actionanand.carechime.app.ui.NEW_RECORD_ID
import com.actionanand.carechime.app.ui.RecordEditorViewModel
import com.actionanand.carechime.app.ui.navigation.Routes
import com.actionanand.carechime.app.ui.stateWhileSubscribed
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProfilesViewModel(profiles: ProfileRepository, private val clock: Clock) : ViewModel() {
    val profiles: StateFlow<List<Profile>?> = profiles.observeProfiles().stateWhileSubscribed(this, null)

    fun today(): LocalDate = LocalDate.now(clock)
}

data class ProfileOverview(
    val profile: Profile,
    val hospitalCount: Int,
    val allergyCount: Int,
    val conditionCount: Int,
    val historyCount: Int,
    val medicineCount: Int,
)

class ProfileDetailViewModel(
    handle: SavedStateHandle,
    private val profiles: ProfileRepository,
    hospitals: HospitalRepository,
    medicalRecords: MedicalRecordRepository,
    profileMedicines: ProfileMedicineRepository,
    private val clock: Clock,
) : ViewModel() {
    val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))

    val overview: StateFlow<Loadable<ProfileOverview>> = combine(
        profiles.observeProfile(profileId),
        hospitals.observeHospitals(profileId).map { it.size },
        medicalRecords.observeAllergies(profileId).map { it.size },
        medicalRecords.observeConditions(profileId).map { it.size },
        combine(
            medicalRecords.observeHistory(profileId).map { it.size },
            profileMedicines.observeProfileMedicines(profileId).map { it.size },
        ) { history, medicines -> history to medicines },
    ) { profile, hospitalCount, allergyCount, conditionCount, counts ->
        if (profile == null) {
            Loadable.Missing
        } else {
            Loadable.Ready(
                ProfileOverview(profile, hospitalCount, allergyCount, conditionCount, counts.first, counts.second),
            )
        }
    }.stateWhileSubscribed(this, Loadable.Loading)

    var pendingDeletion by mutableStateOf<ProfileDeletionSummary?>(null)
        private set
    var deleted by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun today(): LocalDate = LocalDate.now(clock)

    fun requestDelete() {
        viewModelScope.launch {
            try {
                pendingDeletion = profiles.deletionSummary(profileId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Could not prepare deletion. Please try again."
            }
        }
    }

    fun cancelDelete() {
        pendingDeletion = null
    }

    fun confirmDelete() {
        pendingDeletion = null
        viewModelScope.launch {
            try {
                profiles.deleteProfile(profileId)
                deleted = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: DomainException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Could not delete this profile. Please try again."
            }
        }
    }
}

class ProfileEditViewModel(
    handle: SavedStateHandle,
    private val profiles: ProfileRepository,
    private val clock: Clock,
) : RecordEditorViewModel<ProfileInput, ProfileDraft>(ProfileInput()) {
    private val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    override val isEditing: Boolean = profileId != NEW_RECORD_ID

    /** Set after creating a profile so the screen can open its detail page. */
    var createdProfileId: String? = null
        private set

    init {
        if (isEditing) {
            loadExisting {
                profiles.getProfile(profileId)?.let { profile ->
                    ProfileInput(
                        name = profile.name,
                        dateOfBirth = profile.dateOfBirth?.toString().orEmpty(),
                        approximateAge = ProfileValidator.currentApproximateAgeText(
                            profile.approximateAgeYears,
                            profile.approximateAgeRecordedOn,
                            LocalDate.now(clock),
                        ),
                        relationship = profile.relationship.orEmpty(),
                        bloodGroup = profile.bloodGroup,
                        medicalNotes = profile.medicalNotes.orEmpty(),
                    )
                }
            }
        }
    }

    override fun validate(input: ProfileInput): FormResult<ProfileDraft> =
        ProfileValidator.validate(input, LocalDate.now(clock))

    override suspend fun persist(draft: ProfileDraft) {
        if (isEditing) {
            profiles.updateProfile(profileId, draft)
        } else {
            createdProfileId = profiles.createProfile(draft)
        }
    }
}
