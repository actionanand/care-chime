package com.actionanand.carechime.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        ProfileEntity::class,
        HospitalEntity::class,
        AllergyEntity::class,
        ConditionEntity::class,
        HistoryEntity::class,
        MedicineEntity::class,
        ProfileMedicineEntity::class,
    ],
    version = CareChimeDatabase.VERSION,
    exportSchema = true,
)
abstract class CareChimeDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun hospitalDao(): HospitalDao
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun medicineDao(): MedicineDao
    abstract fun profileMedicineDao(): ProfileMedicineDao

    companion object {
        const val VERSION = 1
        const val FILE_NAME = "care-chime.db"

        /** Destructive fallback is deliberately never enabled: medical history must survive upgrades. */
        fun build(context: Context): CareChimeDatabase =
            Room.databaseBuilder(context.applicationContext, CareChimeDatabase::class.java, FILE_NAME)
                .addMigrations(*CareChimeMigrations.ALL)
                .build()
    }
}

/**
 * Explicit, append-only migration list. Every schema version bump must add a Migration from the
 * previous version here, plus a migration test against the committed app/schemas JSON.
 */
object CareChimeMigrations {
    val ALL: Array<Migration> = arrayOf()
}
