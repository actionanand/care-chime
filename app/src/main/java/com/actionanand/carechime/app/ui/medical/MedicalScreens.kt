package com.actionanand.carechime.app.ui.medical

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.actionanand.carechime.app.domain.model.Allergy
import com.actionanand.carechime.app.domain.model.AllergyCategory
import com.actionanand.carechime.app.domain.model.AllergySeverity
import com.actionanand.carechime.app.domain.model.ConditionStatus
import com.actionanand.carechime.app.domain.model.HistoryCategory
import com.actionanand.carechime.app.domain.validation.AllergyValidator
import com.actionanand.carechime.app.domain.validation.ConditionValidator
import com.actionanand.carechime.app.domain.validation.HistoryValidator
import com.actionanand.carechime.app.ui.AppViewModelProvider
import com.actionanand.carechime.app.ui.components.CareTopBar
import com.actionanand.carechime.app.ui.components.DateInput
import com.actionanand.carechime.app.ui.components.EditorScaffold
import com.actionanand.carechime.app.ui.components.EmptyState
import com.actionanand.carechime.app.ui.components.LoadingBox
import com.actionanand.carechime.app.ui.components.PickerField
import com.actionanand.carechime.app.ui.components.SectionHeader
import com.actionanand.carechime.app.ui.components.TextInput
import com.actionanand.carechime.app.ui.components.formatDate

enum class MedicalRecordKind { ALLERGY, CONDITION, HISTORY }

@Composable
fun MedicalRecordsScreen(
    onBack: () -> Unit,
    onOpenRecord: (profileId: String, kind: MedicalRecordKind, recordId: String?) -> Unit,
    viewModel: MedicalRecordsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val profileId = viewModel.profileId
    Scaffold(
        topBar = { CareTopBar(profileName?.let { "Medical records · $it" } ?: "Medical records", onBack = onBack) },
    ) { padding ->
        val data = records
        if (data == null) {
            LoadingBox(Modifier.padding(padding))
        } else {
            val medicationAllergies = data.allergies.filter { it.category == AllergyCategory.MEDICATION }
            val otherAllergies = data.allergies.filter { it.category != AllergyCategory.MEDICATION }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "Records are entered by you for reference. Care Chime does not diagnose or interpret them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    SectionHeader("Medication allergies", "Add") {
                        onOpenRecord(profileId, MedicalRecordKind.ALLERGY, null)
                    }
                }
                allergyItems(medicationAllergies, "No medication allergies recorded.") {
                    onOpenRecord(profileId, MedicalRecordKind.ALLERGY, it)
                }
                item {
                    SectionHeader("Other allergies (food, environmental)", "Add") {
                        onOpenRecord(profileId, MedicalRecordKind.ALLERGY, null)
                    }
                }
                allergyItems(otherAllergies, "No other allergies recorded.") {
                    onOpenRecord(profileId, MedicalRecordKind.ALLERGY, it)
                }
                item {
                    SectionHeader("Existing illnesses & conditions", "Add") {
                        onOpenRecord(profileId, MedicalRecordKind.CONDITION, null)
                    }
                }
                if (data.conditions.isEmpty()) {
                    item { EmptyState("No conditions recorded.") }
                }
                items(data.conditions, key = { "condition-${it.id}" }) { condition ->
                    RecordCard(
                        title = condition.name,
                        lines = listOfNotNull(
                            condition.status.label,
                            condition.diagnosedOn?.let { "Diagnosed ${formatDate(it)}" },
                            condition.notes,
                        ),
                        onClick = { onOpenRecord(profileId, MedicalRecordKind.CONDITION, condition.id) },
                    )
                }
                item {
                    SectionHeader("Medical history", "Add") {
                        onOpenRecord(profileId, MedicalRecordKind.HISTORY, null)
                    }
                }
                if (data.history.isEmpty()) {
                    item { EmptyState("No dated history recorded.") }
                }
                items(data.history, key = { "history-${it.id}" }) { entry ->
                    RecordCard(
                        title = "${formatDate(entry.eventDate)} · ${entry.title}",
                        lines = listOfNotNull(
                            entry.category.label,
                            entry.hospitalId?.let { data.hospitalNames[it] },
                            entry.notes,
                        ),
                        onClick = { onOpenRecord(profileId, MedicalRecordKind.HISTORY, entry.id) },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.allergyItems(allergies: List<Allergy>, emptyText: String, onOpen: (String) -> Unit) {
    if (allergies.isEmpty()) {
        item { EmptyState(emptyText) }
    }
    items(allergies, key = { "allergy-${it.id}" }) { allergy ->
        RecordCard(
            title = allergy.allergen,
            lines = listOfNotNull(
                allergy.category.label.takeIf { allergy.category != AllergyCategory.MEDICATION },
                allergy.severity.takeIf { it != AllergySeverity.UNKNOWN }?.let { "Severity: ${it.label}" },
                allergy.reaction?.let { "Reaction: $it" },
                allergy.notedOn?.let { "Noted ${formatDate(it)}" },
                allergy.notes,
            ),
            onClick = { onOpen(allergy.id) },
        )
    }
}

@Composable
private fun RecordCard(title: String, lines: List<String>, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClickLabel = "Edit", onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun AllergyEditScreen(
    onBack: () -> Unit,
    viewModel: AllergyEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.finished) {
        if (viewModel.finished) onBack()
    }
    val input = viewModel.input
    EditorScaffold(
        title = if (viewModel.isEditing) "Edit allergy" else "Add allergy",
        onBack = onBack,
        isLoading = viewModel.isLoading,
        notFound = viewModel.notFound,
        isSaving = viewModel.isSaving,
        errorMessage = viewModel.errorMessage,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEditing) viewModel::delete else null,
        deleteTitle = "Delete this allergy?",
        deleteMessage = "The allergy record will be permanently removed from this device.",
    ) {
        Text(
            "Record substances that cause a reaction. Medicines taken to treat allergies belong in the profile's medicines.",
            style = MaterialTheme.typography.bodySmall,
        )
        PickerField(
            label = "Type",
            selected = input.category,
            options = AllergyCategory.entries,
            optionLabel = { it.label },
            onSelect = { v -> if (v != null) viewModel.update { it.copy(category = v) } },
        )
        TextInput(
            value = input.allergen,
            onValueChange = { v -> viewModel.update { it.copy(allergen = v) } },
            label = "Allergen *",
            error = viewModel.error(AllergyValidator.ALLERGEN),
            helper = "e.g. the medicine, food or substance",
        )
        TextInput(
            value = input.reaction,
            onValueChange = { v -> viewModel.update { it.copy(reaction = v) } },
            label = "Reaction",
            error = viewModel.error(AllergyValidator.REACTION),
        )
        PickerField(
            label = "Severity",
            selected = input.severity,
            options = AllergySeverity.entries,
            optionLabel = { it.label },
            onSelect = { v -> if (v != null) viewModel.update { it.copy(severity = v) } },
        )
        DateInput(
            value = input.notedOn,
            onValueChange = { v -> viewModel.update { it.copy(notedOn = v) } },
            label = "Date noted",
            error = viewModel.error(AllergyValidator.NOTED_ON),
            helper = "Optional · YYYY-MM-DD",
        )
        TextInput(
            value = input.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Notes",
            error = viewModel.error(AllergyValidator.NOTES),
            singleLine = false,
            minLines = 3,
        )
    }
}

@Composable
fun ConditionEditScreen(
    onBack: () -> Unit,
    viewModel: ConditionEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.finished) {
        if (viewModel.finished) onBack()
    }
    val input = viewModel.input
    EditorScaffold(
        title = if (viewModel.isEditing) "Edit condition" else "Add condition",
        onBack = onBack,
        isLoading = viewModel.isLoading,
        notFound = viewModel.notFound,
        isSaving = viewModel.isSaving,
        errorMessage = viewModel.errorMessage,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEditing) viewModel::delete else null,
        deleteTitle = "Delete this condition?",
        deleteMessage = "The condition record will be permanently removed from this device.",
    ) {
        TextInput(
            value = input.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v) } },
            label = "Illness or condition *",
            error = viewModel.error(ConditionValidator.NAME),
            helper = "As told by your doctor",
            capitalization = KeyboardCapitalization.Words,
        )
        PickerField(
            label = "Status",
            selected = input.status,
            options = ConditionStatus.entries,
            optionLabel = { it.label },
            onSelect = { v -> if (v != null) viewModel.update { it.copy(status = v) } },
        )
        DateInput(
            value = input.diagnosedOn,
            onValueChange = { v -> viewModel.update { it.copy(diagnosedOn = v) } },
            label = "Diagnosis date",
            error = viewModel.error(ConditionValidator.DIAGNOSED_ON),
            helper = "Optional · YYYY-MM-DD",
        )
        TextInput(
            value = input.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Notes",
            error = viewModel.error(ConditionValidator.NOTES),
            singleLine = false,
            minLines = 3,
        )
    }
}

@Composable
fun HistoryEditScreen(
    onBack: () -> Unit,
    viewModel: HistoryEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.finished) {
        if (viewModel.finished) onBack()
    }
    val hospitals by viewModel.hospitals.collectAsStateWithLifecycle()
    val input = viewModel.input
    EditorScaffold(
        title = if (viewModel.isEditing) "Edit history entry" else "Add history entry",
        onBack = onBack,
        isLoading = viewModel.isLoading,
        notFound = viewModel.notFound,
        isSaving = viewModel.isSaving,
        errorMessage = viewModel.errorMessage,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEditing) viewModel::delete else null,
        deleteTitle = "Delete this history entry?",
        deleteMessage = "The entry will be permanently removed from this device.",
    ) {
        DateInput(
            value = input.eventDate,
            onValueChange = { v -> viewModel.update { it.copy(eventDate = v) } },
            label = "Date *",
            error = viewModel.error(HistoryValidator.EVENT_DATE),
        )
        TextInput(
            value = input.title,
            onValueChange = { v -> viewModel.update { it.copy(title = v) } },
            label = "Title *",
            error = viewModel.error(HistoryValidator.TITLE),
            helper = "e.g. Follow-up visit, Blood test, Surgery",
        )
        PickerField(
            label = "Category",
            selected = input.category,
            options = HistoryCategory.entries,
            optionLabel = { it.label },
            onSelect = { v -> if (v != null) viewModel.update { it.copy(category = v) } },
        )
        PickerField(
            label = "Hospital",
            selected = hospitals.firstOrNull { it.id == input.hospitalId },
            options = hospitals,
            optionLabel = { it.hospitalName },
            onSelect = { v -> viewModel.update { it.copy(hospitalId = v?.id) } },
            noneLabel = "No hospital linked",
        )
        TextInput(
            value = input.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Notes",
            error = viewModel.error(HistoryValidator.NOTES),
            singleLine = false,
            minLines = 4,
        )
    }
}
