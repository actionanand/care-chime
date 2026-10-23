package com.actionanand.carechime.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Schema v1. Conventions (part of the future portable export contract):
 * - Primary keys are random UUID strings, never autoincrement integers.
 * - Calendar dates are ISO-8601 text (yyyy-MM-dd); instants are epoch milliseconds (UTC).
 * - Enums are stored by constant name.
 * Any change here requires a version bump, an explicit Migration and a committed schema JSON.
 */

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dateOfBirth: String?,
    val approximateAgeYears: Int?,
    val approximateAgeRecordedOn: String?,
    val relationship: String?,
    val bloodGroup: String?,
    val medicalNotes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "profile_hospitals",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
data class HospitalEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val hospitalName: String,
    val patientId: String,
    val placeName: String?,
    val locationDetails: String?,
    val mapsUrl: String?,
    val doctorName: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "allergies",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
data class AllergyEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val category: String,
    val allergen: String,
    val reaction: String?,
    val severity: String,
    val notedOn: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "medical_conditions",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
data class ConditionEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val status: String,
    val diagnosedOn: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "medical_history",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HospitalEntity::class,
            parentColumns = ["id"],
            childColumns = ["hospitalId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("profileId"), Index("hospitalId")],
)
data class HistoryEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val eventDate: String,
    val title: String,
    val category: String,
    val hospitalId: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brandName: String?,
    val genericNames: String?,
    val form: String,
    val strength: String?,
    val concentration: String?,
    val ageReferenceNotes: String?,
    val manufacturer: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "profile_medicines",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = HospitalEntity::class,
            parentColumns = ["id"],
            childColumns = ["hospitalId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("profileId"), Index("medicineId"), Index("hospitalId"), Index("status")],
)
data class ProfileMedicineEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val medicineId: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: String?,
    val foodTiming: String,
    val startDate: String,
    val endDate: String?,
    val prescribedBy: String?,
    val hospitalId: String?,
    val notes: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)
