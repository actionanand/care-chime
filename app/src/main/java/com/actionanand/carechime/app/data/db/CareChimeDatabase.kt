package com.actionanand.carechime.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        const val VERSION = 2
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
    /**
     * Additive only: nullable snapshot columns on profile_medicines and medical_history, backfilled
     * from the library medicine and linked hospital as they exist at upgrade time.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "medicineName", "medicineBrandName", "medicineGenericNames", "medicineForm",
                "medicineStrength", "medicineConcentration", "medicineManufacturer",
                "hospitalName", "hospitalPlaceName",
            ).forEach { db.execSQL("ALTER TABLE `profile_medicines` ADD COLUMN `$it` TEXT") }
            listOf("hospitalName", "hospitalPlaceName").forEach {
                db.execSQL("ALTER TABLE `medical_history` ADD COLUMN `$it` TEXT")
            }
            db.execSQL(
                """
                UPDATE `profile_medicines` SET
                    `medicineName` = (SELECT `name` FROM `medicines` m WHERE m.`id` = `profile_medicines`.`medicineId`),
                    `medicineBrandName` = (SELECT `brandName` FROM `medicines` m WHERE m.`id` = `profile_medicines`.`medicineId`),
                    `medicineGenericNames` = (SELECT `genericNames` FROM `medicines` m WHERE m.`id` = `profile_medicines`.`medicineId`),
                    `medicineForm` = (SELECT `form` FROM `medicines` m WHERE m.`id` = `profile_medicines`.`medicineId`),
                    `medicineStrength` = (SELECT `strength` FROM `medicines` m WHERE m.`id` = `profile_medicines`.`medicineId`),
                    `medicineConcentration` = (SELECT `concentration` FROM `medicines` m WHERE m.`id` = `profile_medicines`.`medicineId`),
                    `medicineManufacturer` = (SELECT `manufacturer` FROM `medicines` m WHERE m.`id` = `profile_medicines`.`medicineId`)
                """.trimIndent(),
            )
            listOf("profile_medicines", "medical_history").forEach { table ->
                db.execSQL(
                    """
                    UPDATE `$table` SET
                        `hospitalName` = (SELECT `hospitalName` FROM `profile_hospitals` h WHERE h.`id` = `$table`.`hospitalId`),
                        `hospitalPlaceName` = (SELECT `placeName` FROM `profile_hospitals` h WHERE h.`id` = `$table`.`hospitalId`)
                    WHERE `hospitalId` IS NOT NULL
                    """.trimIndent(),
                )
            }
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
