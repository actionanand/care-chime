package com.actionanand.carechime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Initial, intentionally non-clinical app shell. No dose/alarm behavior is implemented yet. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CareChimeRoot() }
    }
}

private val blue = Color(0xFF334B9B)
private val background = Color(0xFFF7F8FF)
private val tabs = listOf("Home", "Profiles", "Medicines", "History")

@Composable
private fun CareChimeRoot() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = lightColorScheme(primary = blue, background = background)) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Text(when (index) { 0 -> "⌂"; 1 -> "♧"; 2 -> "+"; else -> "◷" }) },
                            label = { Text(title) }
                        )
                    }
                }
            }) { insets ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(insets),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Care Chime", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Care, right on time.", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(20.dp))
                        Text(tabs[selectedTab], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(when (selectedTab) {
                                    0 -> "Your care dashboard"
                                    1 -> "Family health profiles"
                                    2 -> "Medicine library and schedules"
                                    else -> "Medical and dose history"
                                }, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Initial project setup is complete. This screen is a placeholder until " +
                                        "secure storage, medical records, and reminder features are implemented.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            "No medicines, doses, profiles, or alarms have been added. " +
                                "The app does not send medication reminders yet.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
