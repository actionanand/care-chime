package com.actionanand.carechime.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.actionanand.carechime.app.ui.NEW_RECORD_ID
import com.actionanand.carechime.app.ui.home.HomeScreen
import com.actionanand.carechime.app.ui.hospitals.HospitalEditScreen
import com.actionanand.carechime.app.ui.hospitals.HospitalsScreen
import com.actionanand.carechime.app.ui.medical.AllergyEditScreen
import com.actionanand.carechime.app.ui.medical.ConditionEditScreen
import com.actionanand.carechime.app.ui.medical.HistoryEditScreen
import com.actionanand.carechime.app.ui.medical.MedicalRecordKind
import com.actionanand.carechime.app.ui.medical.MedicalRecordsScreen
import com.actionanand.carechime.app.ui.medicines.CREATED_MEDICINE_ID_KEY
import com.actionanand.carechime.app.ui.medicines.MedicineEditScreen
import com.actionanand.carechime.app.ui.medicines.MedicineLibraryScreen
import com.actionanand.carechime.app.ui.medicines.ProfileMedicineEditScreen
import com.actionanand.carechime.app.ui.medicines.ProfileMedicinesScreen
import com.actionanand.carechime.app.ui.profiles.ProfileDetailScreen
import com.actionanand.carechime.app.ui.profiles.ProfileEditScreen
import com.actionanand.carechime.app.ui.profiles.ProfilesScreen
import com.actionanand.carechime.app.ui.settings.SettingsScreen

object Routes {
    const val ARG_PROFILE_ID = "profileId"
    const val ARG_RECORD_ID = "recordId"

    const val HOME = "home"
    const val PROFILES = "profiles"
    const val LIBRARY = "medicine-library"
    const val SETTINGS = "settings"

    const val PROFILE_DETAIL = "profile/{$ARG_PROFILE_ID}"
    const val PROFILE_EDIT = "profile-edit/{$ARG_PROFILE_ID}"
    const val HOSPITALS = "hospitals/{$ARG_PROFILE_ID}"
    const val HOSPITAL_EDIT = "hospital-edit/{$ARG_PROFILE_ID}/{$ARG_RECORD_ID}"
    const val MEDICAL_RECORDS = "medical-records/{$ARG_PROFILE_ID}"
    const val ALLERGY_EDIT = "allergy-edit/{$ARG_PROFILE_ID}/{$ARG_RECORD_ID}"
    const val CONDITION_EDIT = "condition-edit/{$ARG_PROFILE_ID}/{$ARG_RECORD_ID}"
    const val HISTORY_EDIT = "history-edit/{$ARG_PROFILE_ID}/{$ARG_RECORD_ID}"
    const val MEDICINE_EDIT = "medicine-edit/{$ARG_RECORD_ID}"
    const val PROFILE_MEDICINES = "profile-medicines/{$ARG_PROFILE_ID}"
    const val PROFILE_MEDICINE_EDIT = "profile-medicine-edit/{$ARG_PROFILE_ID}/{$ARG_RECORD_ID}"

    private fun seg(id: String?) = Uri.encode(id ?: NEW_RECORD_ID)

    fun profileDetail(profileId: String) = "profile/${seg(profileId)}"
    fun profileEdit(profileId: String?) = "profile-edit/${seg(profileId)}"
    fun hospitals(profileId: String) = "hospitals/${seg(profileId)}"
    fun hospitalEdit(profileId: String, hospitalId: String?) = "hospital-edit/${seg(profileId)}/${seg(hospitalId)}"
    fun medicalRecords(profileId: String) = "medical-records/${seg(profileId)}"
    fun medicalRecordEdit(profileId: String, kind: MedicalRecordKind, recordId: String?): String {
        val prefix = when (kind) {
            MedicalRecordKind.ALLERGY -> "allergy-edit"
            MedicalRecordKind.CONDITION -> "condition-edit"
            MedicalRecordKind.HISTORY -> "history-edit"
        }
        return "$prefix/${seg(profileId)}/${seg(recordId)}"
    }
    fun medicineEdit(medicineId: String?) = "medicine-edit/${seg(medicineId)}"
    fun profileMedicines(profileId: String) = "profile-medicines/${seg(profileId)}"
    fun profileMedicineEdit(profileId: String, courseId: String?) =
        "profile-medicine-edit/${seg(profileId)}/${seg(courseId)}"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.PROFILES, "Profiles", Icons.Filled.Person),
    TopLevelDestination(Routes.LIBRARY, "Medicines", Icons.AutoMirrored.Filled.List),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

/** Pops only if [entry] is still on top, so a save completion plus a back tap cannot pop twice. */
private fun NavHostController.popIfCurrent(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareChimeApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigateTopLevel(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenProfiles = { navController.navigateTopLevel(Routes.PROFILES) },
                    onAddProfile = { navController.navigate(Routes.profileEdit(null)) },
                    onOpenLibrary = { navController.navigateTopLevel(Routes.LIBRARY) },
                )
            }
            composable(Routes.PROFILES) {
                ProfilesScreen(
                    onAddProfile = { navController.navigate(Routes.profileEdit(null)) },
                    onOpenProfile = { navController.navigate(Routes.profileDetail(it)) },
                )
            }
            composable(Routes.LIBRARY) {
                MedicineLibraryScreen(
                    onAddMedicine = { navController.navigate(Routes.medicineEdit(null)) },
                    onEditMedicine = { navController.navigate(Routes.medicineEdit(it)) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(Routes.PROFILE_DETAIL) { entry ->
                ProfileDetailScreen(
                    onBack = { navController.popIfCurrent(entry) },
                    onEdit = { navController.navigate(Routes.profileEdit(it)) },
                    onOpenHospitals = { navController.navigate(Routes.hospitals(it)) },
                    onOpenMedicalRecords = { navController.navigate(Routes.medicalRecords(it)) },
                    onOpenMedicines = { navController.navigate(Routes.profileMedicines(it)) },
                )
            }
            composable(Routes.PROFILE_EDIT) { entry ->
                ProfileEditScreen(
                    onBack = { navController.popIfCurrent(entry) },
                    onSaved = { createdId ->
                        navController.popIfCurrent(entry)
                        if (createdId != null) navController.navigate(Routes.profileDetail(createdId))
                    },
                )
            }
            composable(Routes.HOSPITALS) { entry ->
                HospitalsScreen(
                    onBack = { navController.popIfCurrent(entry) },
                    onAddHospital = { profileId -> navController.navigate(Routes.hospitalEdit(profileId, null)) },
                    onEditHospital = { profileId, hospitalId ->
                        navController.navigate(Routes.hospitalEdit(profileId, hospitalId))
                    },
                )
            }
            composable(Routes.HOSPITAL_EDIT) { entry -> HospitalEditScreen(onBack = { navController.popIfCurrent(entry) }) }
            composable(Routes.MEDICAL_RECORDS) { entry ->
                MedicalRecordsScreen(
                    onBack = { navController.popIfCurrent(entry) },
                    onOpenRecord = { profileId, kind, recordId ->
                        navController.navigate(Routes.medicalRecordEdit(profileId, kind, recordId))
                    },
                )
            }
            composable(Routes.ALLERGY_EDIT) { entry -> AllergyEditScreen(onBack = { navController.popIfCurrent(entry) }) }
            composable(Routes.CONDITION_EDIT) { entry -> ConditionEditScreen(onBack = { navController.popIfCurrent(entry) }) }
            composable(Routes.HISTORY_EDIT) { entry -> HistoryEditScreen(onBack = { navController.popIfCurrent(entry) }) }
            composable(Routes.MEDICINE_EDIT) { entry ->
                MedicineEditScreen(
                    onBack = { navController.popIfCurrent(entry) },
                    onSaved = { createdId ->
                        if (createdId != null) {
                            navController.previousBackStackEntry?.savedStateHandle?.set(CREATED_MEDICINE_ID_KEY, createdId)
                        }
                        navController.popIfCurrent(entry)
                    },
                )
            }
            composable(Routes.PROFILE_MEDICINES) { entry ->
                ProfileMedicinesScreen(
                    onBack = { navController.popIfCurrent(entry) },
                    onAddCourse = { profileId -> navController.navigate(Routes.profileMedicineEdit(profileId, null)) },
                    onEditCourse = { profileId, courseId ->
                        navController.navigate(Routes.profileMedicineEdit(profileId, courseId))
                    },
                )
            }
            composable(Routes.PROFILE_MEDICINE_EDIT) { entry ->
                ProfileMedicineEditScreen(
                    onBack = { navController.popIfCurrent(entry) },
                    onCreateMedicine = { navController.navigate(Routes.medicineEdit(null)) },
                )
            }
        }
    }
}
