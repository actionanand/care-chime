package com.actionanand.carechime.app.ui.medicines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.actionanand.carechime.app.domain.model.CourseStatus
import com.actionanand.carechime.app.domain.model.FoodTiming
import com.actionanand.carechime.app.domain.model.Medicine
import com.actionanand.carechime.app.domain.model.MedicineForm
import com.actionanand.carechime.app.domain.model.MedicineRoute
import com.actionanand.carechime.app.domain.validation.MedicineValidator
import com.actionanand.carechime.app.domain.validation.ProfileMedicineValidator
import com.actionanand.carechime.app.ui.AppViewModelProvider
import com.actionanand.carechime.app.ui.components.CareTopBar
import com.actionanand.carechime.app.ui.components.DateInput
import com.actionanand.carechime.app.ui.components.EditorScaffold
import com.actionanand.carechime.app.ui.components.EmptyState
import com.actionanand.carechime.app.ui.components.LoadingBox
import com.actionanand.carechime.app.ui.components.PickerField
import com.actionanand.carechime.app.ui.components.TextInput
import com.actionanand.carechime.app.ui.components.formatDate

private const val REFERENCE_DISCLAIMER =
    "Library entries are your own reference notes. Care Chime never calculates or recommends doses."

private fun Medicine.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return listOfNotNull(name, brandName, genericNames, manufacturer).any { it.contains(q, ignoreCase = true) }
}

@Composable
fun MedicineLibraryScreen(
    onAddMedicine: () -> Unit,
    onEditMedicine: (String) -> Unit,
    viewModel: MedicineLibraryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(
        topBar = { CareTopBar("Medicine library") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMedicine,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add medicine") },
            )
        },
    ) { padding ->
        val list = medicines
        if (list == null) {
            LoadingBox(Modifier.padding(padding))
        } else {
            val filtered = list.filter { it.matches(query) }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(REFERENCE_DISCLAIMER, style = MaterialTheme.typography.bodySmall)
                }
                if (list.isNotEmpty()) {
                    item {
                        TextInput(
                            value = query,
                            onValueChange = { query = it },
                            label = "Search by name, brand, generic or manufacturer",
                            capitalization = KeyboardCapitalization.None,
                        )
                    }
                }
                when {
                    list.isEmpty() -> item {
                        EmptyState("The library is empty. Add the medicines your family uses, as printed on the pack.")
                    }
                    filtered.isEmpty() -> item { EmptyState("No medicines match your search.") }
                }
                items(filtered, key = { it.id }) { medicine ->
                    Card(Modifier.fillMaxWidth().clickable(onClickLabel = "Edit") { onEditMedicine(medicine.id) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(medicine.displayName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(medicine.detailLine(), style = MaterialTheme.typography.bodyMedium)
                            medicine.genericNames?.let { Text("Generic / salt: $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineEditScreen(
    onBack: () -> Unit,
    onSaved: (createdMedicineId: String?) -> Unit,
    viewModel: MedicineEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.finished) {
        if (viewModel.finished) onSaved(viewModel.createdMedicineId)
    }
    val input = viewModel.input
    EditorScaffold(
        title = if (viewModel.isEditing) "Edit medicine" else "Add medicine",
        onBack = onBack,
        isLoading = viewModel.isLoading,
        notFound = viewModel.notFound,
        isSaving = viewModel.isSaving,
        errorMessage = viewModel.errorMessage,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEditing) viewModel::delete else null,
        deleteTitle = "Delete this medicine from the library?",
        deleteMessage = "Medicines still used by a profile cannot be deleted.",
    ) {
        Text(REFERENCE_DISCLAIMER, style = MaterialTheme.typography.bodySmall)
        TextInput(
            value = input.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v) } },
            label = "Medicine name *",
            error = viewModel.error(MedicineValidator.NAME),
            capitalization = KeyboardCapitalization.Words,
        )
        TextInput(
            value = input.brandName,
            onValueChange = { v -> viewModel.update { it.copy(brandName = v) } },
            label = "Brand name",
            error = viewModel.error(MedicineValidator.BRAND_NAME),
            capitalization = KeyboardCapitalization.Words,
        )
        TextInput(
            value = input.genericNames,
            onValueChange = { v -> viewModel.update { it.copy(genericNames = v) } },
            label = "Generic / salt names",
            error = viewModel.error(MedicineValidator.GENERIC_NAMES),
            helper = "Separate multiple salts with commas",
        )
        PickerField(
            label = "Form",
            selected = input.form,
            options = MedicineForm.entries,
            optionLabel = { it.label },
            onSelect = { v -> if (v != null) viewModel.update { it.copy(form = v) } },
        )
        TextInput(
            value = input.strength,
            onValueChange = { v -> viewModel.update { it.copy(strength = v) } },
            label = "Strength",
            error = viewModel.error(MedicineValidator.STRENGTH),
            helper = "As printed, e.g. 500 mg",
        )
        TextInput(
            value = input.concentration,
            onValueChange = { v -> viewModel.update { it.copy(concentration = v) } },
            label = "Concentration",
            error = viewModel.error(MedicineValidator.CONCENTRATION),
            helper = "For liquids or drops, as printed, e.g. 120 mg/5 ml",
        )
        TextInput(
            value = input.ageReferenceNotes,
            onValueChange = { v -> viewModel.update { it.copy(ageReferenceNotes = v) } },
            label = "Age-related reference notes",
            error = viewModel.error(MedicineValidator.AGE_REFERENCE_NOTES),
            helper = "Your own notes from the label or doctor; never used to calculate doses",
            singleLine = false,
            minLines = 2,
        )
        TextInput(
            value = input.manufacturer,
            onValueChange = { v -> viewModel.update { it.copy(manufacturer = v) } },
            label = "Manufacturer",
            error = viewModel.error(MedicineValidator.MANUFACTURER),
            capitalization = KeyboardCapitalization.Words,
        )
        TextInput(
            value = input.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Notes",
            error = viewModel.error(MedicineValidator.NOTES),
            singleLine = false,
            minLines = 3,
        )
    }
}

@Composable
fun ProfileMedicinesScreen(
    onBack: () -> Unit,
    onAddCourse: (profileId: String) -> Unit,
    onEditCourse: (profileId: String, courseId: String) -> Unit,
    viewModel: ProfileMedicinesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val courses by viewModel.items.collectAsStateWithLifecycle()
    val hospitalNames by viewModel.hospitalNames.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { CareTopBar(profileName?.let { "Medicines · $it" } ?: "Medicines", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddCourse(viewModel.profileId) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add medicine") },
            )
        },
    ) { padding ->
        val list = courses
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                "No medicines recorded for this profile. Add them exactly as written on the prescription.",
                Modifier.padding(padding).padding(24.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "Reminders are not available yet. These records do not trigger any alerts.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                items(list, key = { it.course.id }) { entry ->
                    val course = entry.course
                    Card(Modifier.fillMaxWidth().clickable(onClickLabel = "Edit") { onEditCourse(viewModel.profileId, course.id) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                entry.medicine?.displayName() ?: "Medicine no longer in library",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                listOfNotNull(
                                    "${course.doseAmount} ${course.doseUnit}",
                                    course.route?.label,
                                    course.foodTiming.takeIf { it != FoodTiming.NOT_SPECIFIED }?.label,
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                course.endDate?.let { "${formatDate(course.startDate)} – ${formatDate(it)}" }
                                    ?: "From ${formatDate(course.startDate)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                listOfNotNull(
                                    course.status.label,
                                    course.prescribedBy?.let { "Dr: $it" },
                                    course.hospitalId?.let { hospitalNames[it] },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMedicineEditScreen(
    onBack: () -> Unit,
    onCreateMedicine: () -> Unit,
    viewModel: ProfileMedicineEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.finished) {
        if (viewModel.finished) onBack()
    }
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    val hospitals by viewModel.hospitals.collectAsStateWithLifecycle()
    val input = viewModel.input
    EditorScaffold(
        title = if (viewModel.isEditing) "Edit profile medicine" else "Add profile medicine",
        onBack = onBack,
        isLoading = viewModel.isLoading || medicines == null,
        notFound = viewModel.notFound,
        isSaving = viewModel.isSaving,
        errorMessage = viewModel.errorMessage,
        onSave = viewModel::save,
        onDelete = if (viewModel.isEditing) viewModel::delete else null,
        deleteTitle = "Delete this medicine record?",
        deleteMessage = "The record will be permanently removed from this profile.",
    ) {
        Text(
            "Enter the amount, route and timing exactly as prescribed. Care Chime does not suggest doses.",
            style = MaterialTheme.typography.bodySmall,
        )
        val catalog = medicines.orEmpty()
        PickerField(
            label = "Medicine *",
            selected = catalog.firstOrNull { it.id == input.medicineId },
            options = catalog,
            optionLabel = { "${it.displayName()} · ${it.detailLine()}" },
            onSelect = { v -> viewModel.update { it.copy(medicineId = v?.id) } },
            noneLabel = if (catalog.isEmpty()) "Library is empty" else "Choose a medicine",
            error = viewModel.error(ProfileMedicineValidator.MEDICINE),
        )
        OutlinedButton(onClick = onCreateMedicine, modifier = Modifier.fillMaxWidth()) {
            Text("Add a new medicine to the library")
        }
        TextInput(
            value = input.doseAmount,
            onValueChange = { v -> viewModel.update { it.copy(doseAmount = v) } },
            label = "Prescribed amount per dose *",
            error = viewModel.error(ProfileMedicineValidator.DOSE_AMOUNT),
            helper = "From the prescription, e.g. 1 or 0.5",
            keyboardType = KeyboardType.Decimal,
        )
        TextInput(
            value = input.doseUnit,
            onValueChange = { v -> viewModel.update { it.copy(doseUnit = v) } },
            label = "Unit *",
            error = viewModel.error(ProfileMedicineValidator.DOSE_UNIT),
            helper = "e.g. tablet, ml, drops, puff",
            capitalization = KeyboardCapitalization.None,
        )
        PickerField(
            label = "Route",
            selected = input.route,
            options = MedicineRoute.entries,
            optionLabel = { it.label },
            onSelect = { v -> viewModel.update { it.copy(route = v) } },
            noneLabel = "Not specified",
        )
        PickerField(
            label = "Food timing",
            selected = input.foodTiming,
            options = FoodTiming.entries,
            optionLabel = { it.label },
            onSelect = { v -> if (v != null) viewModel.update { it.copy(foodTiming = v) } },
        )
        DateInput(
            value = input.startDate,
            onValueChange = { v -> viewModel.update { it.copy(startDate = v) } },
            label = "Start date *",
            error = viewModel.error(ProfileMedicineValidator.START_DATE),
        )
        DateInput(
            value = input.endDate,
            onValueChange = { v -> viewModel.update { it.copy(endDate = v) } },
            label = "End date",
            error = viewModel.error(ProfileMedicineValidator.END_DATE),
            helper = "Optional · leave empty if ongoing",
        )
        TextInput(
            value = input.prescribedBy,
            onValueChange = { v -> viewModel.update { it.copy(prescribedBy = v) } },
            label = "Prescribing doctor",
            error = viewModel.error(ProfileMedicineValidator.PRESCRIBED_BY),
            capitalization = KeyboardCapitalization.Words,
        )
        PickerField(
            label = "Hospital",
            selected = hospitals.firstOrNull { it.id == input.hospitalId },
            options = hospitals,
            optionLabel = { it.hospitalName },
            onSelect = { v -> viewModel.update { it.copy(hospitalId = v?.id) } },
            noneLabel = "No hospital linked",
        )
        PickerField(
            label = "Status",
            selected = input.status,
            options = CourseStatus.entries,
            optionLabel = { it.label },
            onSelect = { v -> if (v != null) viewModel.update { it.copy(status = v) } },
        )
        TextInput(
            value = input.notes,
            onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
            label = "Notes / instructions",
            error = viewModel.error(ProfileMedicineValidator.NOTES),
            singleLine = false,
            minLines = 3,
        )
    }
}
