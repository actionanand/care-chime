package com.actionanand.carechime.app.ui.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.actionanand.carechime.app.domain.age.AgeCalculator
import com.actionanand.carechime.app.domain.model.BloodGroup
import com.actionanand.carechime.app.domain.model.Profile
import com.actionanand.carechime.app.domain.validation.ProfileValidator
import com.actionanand.carechime.app.ui.AppViewModelProvider
import com.actionanand.carechime.app.ui.Loadable
import com.actionanand.carechime.app.ui.components.CareTopBar
import com.actionanand.carechime.app.ui.components.ConfirmDeleteDialog
import com.actionanand.carechime.app.ui.components.DateInput
import com.actionanand.carechime.app.ui.components.EditorScaffold
import com.actionanand.carechime.app.ui.components.EmptyState
import com.actionanand.carechime.app.ui.components.InfoRow
import com.actionanand.carechime.app.ui.components.LoadingBox
import com.actionanand.carechime.app.ui.components.PickerField
import com.actionanand.carechime.app.ui.components.TextInput
import com.actionanand.carechime.app.ui.components.formatDate
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalDate

fun profileSubtitle(profile: Profile, today: LocalDate): String = listOfNotNull(
    profile.relationship,
    AgeCalculator.forProfile(profile, today)?.describe(),
    profile.bloodGroup?.label,
).joinToString(" · ")

@Composable
fun ProfilesScreen(
    onAddProfile: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: ProfilesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { CareTopBar("Family profiles") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProfile,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add profile") },
            )
        },
    ) { padding ->
        val list = profiles
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                "No profiles yet. Add yourself or a family member to start keeping records on this device.",
                Modifier.padding(padding).padding(24.dp),
            )
            else -> {
                val today = viewModel.today()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(list, key = { it.id }) { profile ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenProfile(profile.id) },
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val subtitle = profileSubtitle(profile, today)
                                if (subtitle.isNotEmpty()) {
                                    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenHospitals: (String) -> Unit,
    onOpenMedicalRecords: (String) -> Unit,
    onOpenMedicines: (String) -> Unit,
    viewModel: ProfileDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.overview.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel.deleted) {
        if (viewModel.deleted) onBack()
    }
    val readyProfile = (state as? Loadable.Ready<ProfileOverview>)?.value?.profile
    Scaffold(
        topBar = {
            CareTopBar(readyProfile?.name ?: "Profile", onBack = onBack) {
                if (readyProfile != null) {
                    IconButton(onClick = { onEdit(viewModel.profileId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                    }
                    IconButton(onClick = viewModel::requestDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete profile")
                    }
                }
            }
        },
    ) { padding ->
        when (val current = state) {
            Loadable.Loading -> LoadingBox(Modifier.padding(padding))
            Loadable.Missing -> EmptyState("This profile no longer exists.", Modifier.padding(padding).padding(24.dp))
            is Loadable.Ready -> {
                val overview = current.value
                val profile = overview.profile
                val today = viewModel.today()
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            InfoRow("Age", AgeCalculator.forProfile(profile, today)?.describe() ?: "Not recorded")
                            InfoRow("Date of birth", profile.dateOfBirth?.let(::formatDate))
                            InfoRow("Relationship", profile.relationship)
                            InfoRow("Blood group", profile.bloodGroup?.label)
                            InfoRow("Medical notes", profile.medicalNotes)
                        }
                    }
                    NavigationCard(
                        title = "Hospitals",
                        summary = countText(overview.hospitalCount, "hospital record"),
                        onClick = { onOpenHospitals(profile.id) },
                    )
                    NavigationCard(
                        title = "Allergies, conditions & history",
                        summary = listOf(
                            countText(overview.allergyCount, "allergy", "allergies"),
                            countText(overview.conditionCount, "condition"),
                            countText(overview.historyCount, "history entry", "history entries"),
                        ).joinToString(" · "),
                        onClick = { onOpenMedicalRecords(profile.id) },
                    )
                    NavigationCard(
                        title = "Medicines",
                        summary = countText(overview.medicineCount, "medicine record"),
                        onClick = { onOpenMedicines(profile.id) },
                    )
                    viewModel.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    viewModel.pendingDeletion?.let { summary ->
        val details = if (summary.total == 0) {
            "No other records are attached."
        } else {
            "This also permanently deletes ${countText(summary.hospitals, "hospital record")}, " +
                "${countText(summary.allergies, "allergy", "allergies")}, " +
                "${countText(summary.conditions, "condition")}, " +
                "${countText(summary.historyEntries, "history entry", "history entries")} and " +
                "${countText(summary.medicines, "medicine record")}."
        }
        ConfirmDeleteDialog(
            title = "Delete this profile?",
            message = "$details This cannot be undone.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete,
            confirmLabel = "Delete permanently",
        )
    }
}

fun countText(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"

@Composable
fun NavigationCard(title: String, summary: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    onSaved: (createdProfileId: String?) -> Unit,
    viewModel: ProfileEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.finished) {
        if (viewModel.finished) onSaved(viewModel.createdProfileId)
    }
    val input = viewModel.input
    EditorScaffold(
        title = if (viewModel.isEditing) "Edit profile" else "Add profile",
        onBack = onBack,
        isLoading = viewModel.isLoading,
        notFound = viewModel.notFound,
        isSaving = viewModel.isSaving,
        errorMessage = viewModel.errorMessage,
        onSave = viewModel::save,
        onDelete = null,
        deleteTitle = "",
        deleteMessage = "",
    ) {
        TextInput(
            value = input.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v) } },
            label = "Name *",
            error = viewModel.error(ProfileValidator.NAME),
            capitalization = KeyboardCapitalization.Words,
        )
        DateInput(
            value = input.dateOfBirth,
            onValueChange = { v -> viewModel.update { it.copy(dateOfBirth = v) } },
            label = "Date of birth",
            error = viewModel.error(ProfileValidator.DATE_OF_BIRTH),
            helper = "Optional · YYYY-MM-DD · age is calculated automatically",
        )
        TextInput(
            value = input.approximateAge,
            onValueChange = { v -> viewModel.update { it.copy(approximateAge = v) } },
            label = "Approximate age (years)",
            error = viewModel.error(ProfileValidator.APPROXIMATE_AGE),
            helper = "Only if the date of birth is unknown",
            keyboardType = KeyboardType.Number,
        )
        TextInput(
            value = input.relationship,
            onValueChange = { v -> viewModel.update { it.copy(relationship = v) } },
            label = "Relationship",
            error = viewModel.error(ProfileValidator.RELATIONSHIP),
            helper = "Optional, e.g. Self, Mother, Son",
            capitalization = KeyboardCapitalization.Words,
        )
        PickerField(
            label = "Blood group",
            selected = input.bloodGroup,
            options = BloodGroup.entries,
            optionLabel = { it.label },
            onSelect = { v -> viewModel.update { it.copy(bloodGroup = v) } },
            noneLabel = "Not recorded",
        )
        TextInput(
            value = input.medicalNotes,
            onValueChange = { v -> viewModel.update { it.copy(medicalNotes = v) } },
            label = "Medical notes",
            error = viewModel.error(ProfileValidator.MEDICAL_NOTES),
            singleLine = false,
            minLines = 3,
        )
    }
}
