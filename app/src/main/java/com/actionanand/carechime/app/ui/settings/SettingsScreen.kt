package com.actionanand.carechime.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.actionanand.carechime.app.BuildConfig
import com.actionanand.carechime.app.R
import com.actionanand.carechime.app.ui.components.CareTopBar
import com.actionanand.carechime.app.ui.components.InfoRow

/** Informational only: every statement here describes behaviour that exists in this build. */
@Composable
fun SettingsScreen() {
    Scaffold(topBar = { CareTopBar("Settings") }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsCard("About") {
                InfoRow("App", "${stringResource(R.string.app_name)} — ${stringResource(R.string.app_tagline)}")
                InfoRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoRow("Application ID", BuildConfig.APPLICATION_ID)
            }
            SettingsCard("Privacy & storage") {
                Text(
                    "All profiles, hospitals, medical records and medicines are stored only in this app's private " +
                        "storage on this device. Care Chime has no internet permission, no account and no analytics.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Android cloud backup and device-to-device transfer are disabled for this app, so uninstalling " +
                        "it or clearing its data permanently deletes these records.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SettingsCard("Not available yet") {
                Text(
                    "Medicine and water reminders, encrypted backup/export and vaccination records are planned " +
                        "but not implemented in this version.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SettingsCard("Medical safety") {
                Text(
                    "Care Chime records information you enter. It does not diagnose, recommend medicines or " +
                        "calculate doses. Always follow your doctor's prescription.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
