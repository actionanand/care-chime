package com.actionanand.carechime.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/*
 * Never use OnConflictStrategy.REPLACE for updates: SQLite implements REPLACE as delete + insert,
 * which would fire ON DELETE CASCADE and silently erase child medical records.
 */

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY name COLLATE NOCASE, createdAt")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observe(id: String): Flow<ProfileEntity?>

    @Query("SELECT COUNT(*) FROM profiles")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun get(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ProfileEntity)

    @Update
    suspend fun update(entity: ProfileEntity): Int

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT COUNT(*) FROM profile_hospitals WHERE profileId = :profileId")
    suspend fun countHospitals(profileId: String): Int

    @Query("SELECT COUNT(*) FROM allergies WHERE profileId = :profileId")
    suspend fun countAllergies(profileId: String): Int

    @Query("SELECT COUNT(*) FROM medical_conditions WHERE profileId = :profileId")
    suspend fun countConditions(profileId: String): Int

    @Query("SELECT COUNT(*) FROM medical_history WHERE profileId = :profileId")
    suspend fun countHistory(profileId: String): Int

    @Query("SELECT COUNT(*) FROM profile_medicines WHERE profileId = :profileId")
    suspend fun countMedicines(profileId: String): Int
}

@Dao
interface HospitalDao {
    @Query("SELECT * FROM profile_hospitals WHERE profileId = :profileId ORDER BY hospitalName COLLATE NOCASE")
    fun observeForProfile(profileId: String): Flow<List<HospitalEntity>>

    @Query("SELECT * FROM profile_hospitals WHERE profileId = :profileId AND id = :id")
    suspend fun get(profileId: String, id: String): HospitalEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: HospitalEntity)

    @Update
    suspend fun update(entity: HospitalEntity): Int

    @Query("DELETE FROM profile_hospitals WHERE profileId = :profileId AND id = :id")
    suspend fun delete(profileId: String, id: String): Int
}

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM allergies WHERE profileId = :profileId ORDER BY category, allergen COLLATE NOCASE")
    fun observeAllergies(profileId: String): Flow<List<AllergyEntity>>

    @Query("SELECT * FROM allergies WHERE profileId = :profileId AND id = :id")
    suspend fun getAllergy(profileId: String, id: String): AllergyEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllergy(entity: AllergyEntity)

    @Update
    suspend fun updateAllergy(entity: AllergyEntity): Int

    @Query("DELETE FROM allergies WHERE profileId = :profileId AND id = :id")
    suspend fun deleteAllergy(profileId: String, id: String): Int

    @Query("SELECT * FROM medical_conditions WHERE profileId = :profileId ORDER BY name COLLATE NOCASE")
    fun observeConditions(profileId: String): Flow<List<ConditionEntity>>

    @Query("SELECT * FROM medical_conditions WHERE profileId = :profileId AND id = :id")
    suspend fun getCondition(profileId: String, id: String): ConditionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCondition(entity: ConditionEntity)

    @Update
    suspend fun updateCondition(entity: ConditionEntity): Int

    @Query("DELETE FROM medical_conditions WHERE profileId = :profileId AND id = :id")
    suspend fun deleteCondition(profileId: String, id: String): Int

    @Query("SELECT * FROM medical_history WHERE profileId = :profileId ORDER BY eventDate DESC, createdAt DESC")
    fun observeHistory(profileId: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM medical_history WHERE profileId = :profileId AND id = :id")
    suspend fun getHistory(profileId: String, id: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHistory(entity: HistoryEntity)

    @Update
    suspend fun updateHistory(entity: HistoryEntity): Int

    @Query("DELETE FROM medical_history WHERE profileId = :profileId AND id = :id")
    suspend fun deleteHistory(profileId: String, id: String): Int
}

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MedicineEntity>>

    @Query("SELECT COUNT(*) FROM medicines")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun get(id: String): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MedicineEntity)

    @Update
    suspend fun update(entity: MedicineEntity): Int

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT COUNT(*) FROM profile_medicines WHERE medicineId = :medicineId")
    suspend fun usageCount(medicineId: String): Int
}

@Dao
interface ProfileMedicineDao {
    @Query("SELECT * FROM profile_medicines WHERE profileId = :profileId ORDER BY status, startDate DESC")
    fun observeForProfile(profileId: String): Flow<List<ProfileMedicineEntity>>

    @Query("SELECT COUNT(*) FROM profile_medicines WHERE status = 'ACTIVE'")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM profile_medicines WHERE profileId = :profileId AND id = :id")
    suspend fun get(profileId: String, id: String): ProfileMedicineEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ProfileMedicineEntity)

    @Update
    suspend fun update(entity: ProfileMedicineEntity): Int

    @Query("DELETE FROM profile_medicines WHERE profileId = :profileId AND id = :id")
    suspend fun delete(profileId: String, id: String): Int
}
