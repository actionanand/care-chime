package com.actionanand.carechime.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.actionanand.carechime.app.domain.validation.FieldValidator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun formatDate(date: LocalDate): String = displayDateFormatter.format(date)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = { actions() },
    )
}

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val supporting = error ?: helper
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = if (supporting != null) {
            { Text(supporting) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType),
        trailingIcon = trailingIcon,
    )
}

/** ISO date text field with an accessible calendar picker; typed input is validated by the domain layer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    helper: String = "YYYY-MM-DD",
) {
    var showPicker by remember { mutableStateOf(false) }
    TextInput(
        value = value,
        onValueChange = onValueChange,
        label = label,
        error = error,
        helper = helper,
        keyboardType = KeyboardType.Number,
        capitalization = KeyboardCapitalization.None,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Choose $label from calendar")
            }
        },
    )
    if (showPicker) {
        val initial = FieldValidator.parseDate(value)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onValueChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                        }
                        showPicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Accessible single-choice picker. [noneLabel] adds an explicit "not set" option. */
@Composable
fun <T> PickerField(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T?) -> Unit,
    noneLabel: String? = null,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = selected?.let(optionLabel) ?: noneLabel ?: "Choose"
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "$label: $selectedText. Double tap to change." },
            ) {
                Text(selectedText, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (noneLabel != null) {
                    DropdownMenuItem(text = { Text(noneLabel) }, onClick = { onSelect(null); expanded = false })
                }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = { onSelect(option); expanded = false },
                    )
                }
            }
        }
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Delete",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Shared create/edit layout: scrollable form, save button, error text and confirmed deletion. */
@Composable
fun EditorScaffold(
    title: String,
    onBack: () -> Unit,
    isLoading: Boolean,
    notFound: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    deleteTitle: String,
    deleteMessage: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CareTopBar(title = title, onBack = onBack) {
                if (onDelete != null && !isLoading && !notFound) {
                    IconButton(onClick = { confirmDelete = true }, enabled = !isSaving) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        },
    ) { padding ->
        when {
            isLoading -> LoadingBox(Modifier.padding(padding))
            notFound -> EmptyState(
                "This record no longer exists.",
                Modifier.padding(padding).padding(24.dp),
            )
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isSaving) "Saving…" else "Save")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmDelete && onDelete != null) {
        ConfirmDeleteDialog(
            title = deleteTitle,
            message = deleteMessage,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
