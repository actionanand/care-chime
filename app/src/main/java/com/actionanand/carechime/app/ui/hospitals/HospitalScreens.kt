package com.actionanand.carechime.app.ui.hospitals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.actionanand.carechime.app.domain.model.HospitalDraft
import com.actionanand.carechime.app.domain.model.HospitalRecord
import com.actionanand.carechime.app.domain.repository.HospitalRepository
import com.actionanand.carechime.app.domain.repository.ProfileRepository
import com.actionanand.carechime.app.domain.validation.FormResult
import com.actionanand.carechime.app.domain.validation.HospitalInput
import com.actionanand.carechime.app.domain.validation.HospitalValidator
import com.actionanand.carechime.app.ui.AppViewModelProvider
import com.actionanand.carechime.app.ui.NEW_RECORD_ID
import com.actionanand.carechime.app.ui.RecordEditorViewModel
import com.actionanand.carechime.app.ui.components.CareTopBar
import com.actionanand.carechime.app.ui.components.EditorScaffold
import com.actionanand.carechime.app.ui.components.EmptyState
import com.actionanand.carechime.app.ui.components.InfoRow
import com.actionanand.carechime.app.ui.components.LoadingBox
import com.actionanand.carechime.app.ui.components.TextInput
import com.actionanand.carechime.app.ui.navigation.Routes
import com.actionanand.carechime.app.ui.stateWhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class HospitalsViewModel(
    handle: SavedStateHandle,
    profiles: ProfileRepository,
    hospitals: HospitalRepository,
) : ViewModel() {
    val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    val profileName: StateFlow<String?> = profiles.observeProfile(profileId).map { it?.name }.stateWhileSubscribed(this, null)
    val hospitals: StateFlow<List<HospitalRecord>?> = hospitals.observeHospitals(profileId).stateWhileSubscribed(this, null)
}

class HospitalEditViewModel(
    handle: SavedStateHandle,
    private val hospitals: HospitalRepository,
) : RecordEditorViewModel<HospitalInput, HospitalDraft>(HospitalInput()) {
    private val profileId: String = checkNotNull(handle.get<String>(Routes.ARG_PROFILE_ID))
    private val hospitalId: String = checkNotNull(handle.get<String>(Routes.ARG_RECORD_ID))
    override val isEditing: Boolean = hospitalId != NEW_RECORD_ID

    init {
        if (isEditing) {
            loadExisting {
                hospitals.getHospital(profileId, hospitalId)?.let {
                    HospitalInput(
                        hospitalName = it.hospitalName,
                        patientId = it.patientId,
                        placeName = it.placeName.orEmpty(),
                        locationDetails = it.locationDetails.orEmpty(),
                        mapsUrl = it.mapsUrl.orEmpty(),
                        doctorName = it.doctorName.orEmpty(),
                        notes = it.notes.orEmpty(),
                    )
                }
            }
        }
    }

    override fun validate(input: HospitalInput): FormResult<HospitalDraft> = HospitalValidator.validate(input)

    override suspend fun persist(draft: HospitalDraft) {
        if (isEditing) hospitals.updateHospital(profileId, hospitalId, draft) else hospitals.createHospital(profileId, draft)
    }

    override suspend fun remove() {
        hospitals.deleteHospital(profileId, hospitalId)
    }
}

@Composable
fun HospitalsScreen(
    onBack: () -> Unit,
    onAddHospital: (profileId: String) -> Unit,
    onEditHospital: (profileId: String, hospitalId: String) -> Unit,
    viewModel: HospitalsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val hospitals by viewModel.hospitals.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = { CareTopBar(profileName?.let { "Hospitals · $it" } ?: "Hospitals", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddHospital(viewModel.profileId) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add hospital") },
            )
        },
    ) { padding ->
        val list = hospitals
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                "No hospitals yet. Add each hospital with the patient ID it issued.",
                Modifier.padding(padding).padding(24.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                linkError?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
                items(list, key = { it.id }) { hospital ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    hospital.hospitalName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { onEditHospital(viewModel.profileId, hospital.id) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit ${hospital.hospitalName}")
                                }
                            }
                            InfoRow("Patient / hospital ID", hospital.patientId)
                            InfoRow("Place", hospital.placeName)
                            InfoRow("Location details", hospital.locationDetails)
                            InfoRow("Doctor", hospital.doctorName)
                            InfoRow("Notes", hospital.notes)
                            hospital.mapsUrl?.let { url ->
                                TextButton(
                                    onClick = {
                                        linkError = try {
                                            uriHandler.openUri(url)
                                            null
                                        } catch (e: Exception) {
                                            "No app is available to open this map link."
                                        }
                                    },
                                ) { Text("Open in Google Maps") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HospitalEditScreen(
    onBack: () -> Unit,
    viewModel: HospitalEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.finished) {
        if (viewModel.finished) onBack()
    }
    val input = viewModel.input
    EditorScaffold(
        title = if (viewModel.isEditing) "Edit hospital" else "Add hospital",
        onBack = onBack,
        isLoading = viewModel.isLoading,
        notFound = viewModel.notFound,
        isSaving = viewModel.isSaving,
        errorMessage = viewModel.errorMessage,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEditing) viewModel::delete else null,
        deleteTitle = "Delete this hospital record?",
        deleteMessage = "History entries and medicines linked to it are kept and still show the hospital name and place recorded with them. The patient ID is not kept; note it first if you may need it.",
    ) {
        TextInput(
            value = input.hospitalName,
            onValueChange = { v -> viewModel.update { it.copy(hospitalName = v) } },
            label = "Hospital name *",
            error = viewModel.error(HospitalValidator.HOSPITAL_NAME),
            capitalization = KeyboardCapitalization.Words,
        )
        TextInput(
            value = input.patientId,
            onValueChange = { v -> viewModel.update { it.copy(patientId = v) } },
            label = "Patient / hospital ID *",
            error = viewModel.error(HospitalValidator.PATIENT_ID),
            capitalization = KeyboardCapitalization.Characters,
        )
        TextInput(
            value = input.placeName,
            onValueChange = { v -> viewModel.update { it.copy(placeName = v) } },
            label = "Place name",
            error = viewModel.error(HospitalValidator.PLACE_NAME),
            capitalization = KeyboardCapitalization.Words,
        )
        TextInput(
            value = input.locationDetails,
            onValueChange = { v -> viewModel.update { it.copy(locationDetails = v) } },
            label = "Location details",
            error = viewModel.error(HospitalValidator.LOCATION_DETAILS),
            helper = "Optional address, block, floor or directions",
            singleLine = false,
            minLines = 2,
        )
        TextInput(
            value = input.mapsUrl,
            onValueChange = { v -> viewModel.update { it.copy(mapsUrl = v) } },
            label = "Google Maps link",
            error = viewModel.error(HospitalValidator.MAPS_URL),
            helper = "Optional https link copied from Google Maps",
            keyboardType = KeyboardType.Uri,
            capitalization = KeyboardCapitalization.None,
        )
        TextInput(
            value = input.doctorName,
            onValueChange = { v -> viewModel.update { it.copy(doctorName = v) } },
            label = "Doctor name",
            error = viewModel.error(HospitalValidator.DOCTOR_NAME),
            capitalization = KeyboardCapitalization.Words,
        )
        TextInput(
            value = input.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Notes",
            error = viewModel.error(HospitalValidator.NOTES),
            singleLine = false,
            minLines = 3,
        )
    }
}
