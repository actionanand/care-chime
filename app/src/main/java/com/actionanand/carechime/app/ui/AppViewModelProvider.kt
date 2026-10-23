package com.actionanand.carechime.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.actionanand.carechime.app.AppContainer
import com.actionanand.carechime.app.CareChimeApplication
import com.actionanand.carechime.app.ui.home.HomeViewModel
import com.actionanand.carechime.app.ui.hospitals.HospitalEditViewModel
import com.actionanand.carechime.app.ui.hospitals.HospitalsViewModel
import com.actionanand.carechime.app.ui.medical.AllergyEditViewModel
import com.actionanand.carechime.app.ui.medical.ConditionEditViewModel
import com.actionanand.carechime.app.ui.medical.HistoryEditViewModel
import com.actionanand.carechime.app.ui.medical.MedicalRecordsViewModel
import com.actionanand.carechime.app.ui.medicines.MedicineEditViewModel
import com.actionanand.carechime.app.ui.medicines.MedicineLibraryViewModel
import com.actionanand.carechime.app.ui.medicines.ProfileMedicineEditViewModel
import com.actionanand.carechime.app.ui.medicines.ProfileMedicinesViewModel
import com.actionanand.carechime.app.ui.profiles.ProfileDetailViewModel
import com.actionanand.carechime.app.ui.profiles.ProfileEditViewModel
import com.actionanand.carechime.app.ui.profiles.ProfilesViewModel

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CareChimeApplication).container

/** Single factory for all screens; route arguments arrive through the SavedStateHandle. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val c = container()
            HomeViewModel(c.profiles, c.medicines, c.profileMedicines)
        }
        initializer { ProfilesViewModel(container().profiles, container().clock) }
        initializer {
            val c = container()
            ProfileDetailViewModel(createSavedStateHandle(), c.profiles, c.hospitals, c.medicalRecords, c.profileMedicines, c.clock)
        }
        initializer { ProfileEditViewModel(createSavedStateHandle(), container().profiles, container().clock) }
        initializer {
            val c = container()
            HospitalsViewModel(createSavedStateHandle(), c.profiles, c.hospitals)
        }
        initializer { HospitalEditViewModel(createSavedStateHandle(), container().hospitals) }
        initializer {
            val c = container()
            MedicalRecordsViewModel(createSavedStateHandle(), c.profiles, c.medicalRecords, c.hospitals)
        }
        initializer { AllergyEditViewModel(createSavedStateHandle(), container().medicalRecords, container().clock) }
        initializer { ConditionEditViewModel(createSavedStateHandle(), container().medicalRecords, container().clock) }
        initializer {
            val c = container()
            HistoryEditViewModel(createSavedStateHandle(), c.medicalRecords, c.hospitals, c.clock)
        }
        initializer { MedicineLibraryViewModel(container().medicines) }
        initializer { MedicineEditViewModel(createSavedStateHandle(), container().medicines) }
        initializer {
            val c = container()
            ProfileMedicinesViewModel(createSavedStateHandle(), c.profiles, c.profileMedicines, c.hospitals)
        }
        initializer {
            val c = container()
            ProfileMedicineEditViewModel(createSavedStateHandle(), c.profileMedicines, c.medicines, c.hospitals, c.clock)
        }
    }
}
