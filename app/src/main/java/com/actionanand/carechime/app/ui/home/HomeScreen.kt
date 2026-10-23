package com.actionanand.carechime.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.actionanand.carechime.app.R
import com.actionanand.carechime.app.domain.repository.MedicineCatalogRepository
import com.actionanand.carechime.app.domain.repository.ProfileMedicineRepository
import com.actionanand.carechime.app.domain.repository.ProfileRepository
import com.actionanand.carechime.app.ui.AppViewModelProvider
import com.actionanand.carechime.app.ui.stateWhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

data class DashboardCounts(val profiles: Int, val libraryMedicines: Int, val activeCourses: Int)

class HomeViewModel(
    profiles: ProfileRepository,
    medicines: MedicineCatalogRepository,
    profileMedicines: ProfileMedicineRepository,
) : ViewModel() {
    val counts: StateFlow<DashboardCounts?> = combine(
        profiles.observeProfileCount(),
        medicines.observeMedicineCount(),
        profileMedicines.observeActiveCourseCount(),
    ) { profileCount, medicineCount, activeCount -> DashboardCounts(profileCount, medicineCount, activeCount) }
        .stateWhileSubscribed(this, null)
}

@Composable
fun HomeScreen(
    onOpenProfiles: () -> Unit,
    onAddProfile: () -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.care_chime_mark),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                )
                Column(Modifier.padding(start = 16.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.app_tagline), color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.titleLarge)

            val current = counts
            if (current != null) {
                SummaryCard(
                    title = "Family profiles",
                    value = current.profiles,
                    detail = "People whose records are kept on this device",
                    onClick = onOpenProfiles,
                )
                SummaryCard(
                    title = "Medicine library",
                    value = current.libraryMedicines,
                    detail = "Reference entries you have added",
                    onClick = onOpenLibrary,
                )
                SummaryCard(
                    title = "Active medicine records",
                    value = current.activeCourses,
                    detail = "Prescribed medicines marked active across all profiles",
                    onClick = onOpenProfiles,
                )
                if (current.profiles == 0) {
                    Button(onClick = onAddProfile, modifier = Modifier.fillMaxWidth()) {
                        Text("Add your first profile")
                    }
                } else {
                    OutlinedButton(onClick = onAddProfile, modifier = Modifier.fillMaxWidth()) {
                        Text("Add another profile")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Reminders are not available yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "This version stores records only. It does not send medicine or water reminders, " +
                            "so do not rely on it to remember doses.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: Int, detail: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClickLabel = "Open $title", onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}
