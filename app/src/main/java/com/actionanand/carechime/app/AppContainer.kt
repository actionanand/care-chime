package com.actionanand.carechime.app

import android.content.Context
import com.actionanand.carechime.app.data.db.CareChimeDatabase
import com.actionanand.carechime.app.data.repository.RoomHospitalRepository
import com.actionanand.carechime.app.data.repository.RoomMedicalRecordRepository
import com.actionanand.carechime.app.data.repository.RoomMedicineCatalogRepository
import com.actionanand.carechime.app.data.repository.RoomProfileMedicineRepository
import com.actionanand.carechime.app.data.repository.RoomProfileRepository
import com.actionanand.carechime.app.domain.repository.HospitalRepository
import com.actionanand.carechime.app.domain.repository.MedicalRecordRepository
import com.actionanand.carechime.app.domain.repository.MedicineCatalogRepository
import com.actionanand.carechime.app.domain.repository.ProfileMedicineRepository
import com.actionanand.carechime.app.domain.repository.ProfileRepository
import java.time.Clock

/** Manual dependency container; the database opens lazily on first repository use. */
class AppContainer(context: Context, private val fixedClock: Clock? = null) {
    /** Re-reads the system zone on each access so "today" follows device time-zone changes. */
    val clock: Clock get() = fixedClock ?: Clock.systemDefaultZone()

    private val appContext = context.applicationContext
    private val database: CareChimeDatabase by lazy { CareChimeDatabase.build(appContext) }

    val profiles: ProfileRepository by lazy { RoomProfileRepository(database, clock) }
    val hospitals: HospitalRepository by lazy { RoomHospitalRepository(database, clock) }
    val medicalRecords: MedicalRecordRepository by lazy { RoomMedicalRecordRepository(database, clock) }
    val medicines: MedicineCatalogRepository by lazy { RoomMedicineCatalogRepository(database, clock) }
    val profileMedicines: ProfileMedicineRepository by lazy { RoomProfileMedicineRepository(database, clock) }
}
