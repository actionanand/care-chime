package com.actionanand.carechime.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.actionanand.carechime.app.data.db.CareChimeDatabase
import com.actionanand.carechime.app.data.db.CareChimeMigrations
import com.actionanand.carechime.app.data.repository.RoomHospitalRepository
import com.actionanand.carechime.app.data.repository.RoomMedicalRecordRepository
import com.actionanand.carechime.app.data.repository.RoomMedicineCatalogRepository
import com.actionanand.carechime.app.data.repository.RoomProfileMedicineRepository
import com.actionanand.carechime.app.data.repository.RoomProfileRepository
import com.actionanand.carechime.app.domain.model.AllergyCategory
import com.actionanand.carechime.app.domain.model.AllergyDraft
import com.actionanand.carechime.app.domain.model.AllergySeverity
import com.actionanand.carechime.app.domain.model.BloodGroup
import com.actionanand.carechime.app.domain.model.ConditionStatus
import com.actionanand.carechime.app.domain.model.CourseStatus
import com.actionanand.carechime.app.domain.model.FoodTiming
import com.actionanand.carechime.app.domain.model.HistoryCategory
import com.actionanand.carechime.app.domain.model.HospitalDraft
import com.actionanand.carechime.app.domain.model.MedicalConditionDraft
import com.actionanand.carechime.app.domain.model.MedicalHistoryDraft
import com.actionanand.carechime.app.domain.model.MedicineDraft
import com.actionanand.carechime.app.domain.model.MedicineForm
import com.actionanand.carechime.app.domain.model.MedicineRoute
import com.actionanand.carechime.app.domain.model.PrescribedMedicine
import com.actionanand.carechime.app.domain.model.ProfileDraft
import com.actionanand.carechime.app.domain.model.ProfileMedicineDraft
import com.actionanand.carechime.app.domain.model.RecordedHospital
import com.actionanand.carechime.app.domain.repository.MedicineInUseException
import com.actionanand.carechime.app.domain.repository.OwnershipException
import com.actionanand.carechime.app.domain.repository.RecordNotFoundException
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = now
}

private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return
        throw AssertionError("Expected ${T::class.java.simpleName} but got $e")
    }
    fail("Expected ${T::class.java.simpleName} to be thrown")
}

/** Committed Room schema JSON; Gradle runs unit tests from the app module directory. */
private fun schemaFile(version: Int): File =
    File("schemas/${CareChimeDatabase::class.java.name}/$version.json")

/** Recreates the exact tables, indices and Room metadata of a committed schema version. */
private fun createFromSchema(db: SQLiteDatabase, version: Int) {
    val schema = JSONObject(schemaFile(version).readText()).getJSONObject("database")
    assertEquals(version, schema.getInt("version"))
    val entities = schema.getJSONArray("entities")
    for (i in 0 until entities.length()) {
        val entity = entities.getJSONObject(i)
        val table = entity.getString("tableName")
        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
        val indices = entity.optJSONArray("indices") ?: continue
        for (j in 0 until indices.length()) {
            db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
        }
    }
    val setup = schema.getJSONArray("setupQueries")
    for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
}

/* All fixtures are synthetic; never use real patient data in tests. */
private fun profileDraft(name: String) = ProfileDraft(
    name = name,
    dateOfBirth = LocalDate.of(1990, 5, 17),
    approximateAgeYears = null,
    approximateAgeRecordedOn = null,
    relationship = "Self",
    bloodGroup = BloodGroup.B_POSITIVE,
    medicalNotes = null,
)

private fun hospitalDraft(name: String = "Test General Hospital") =
    HospitalDraft(name, "TEST-0001", "Test City", null, "https://maps.app.goo.gl/test", "Dr Example", null)

private val medicineDraft = MedicineDraft(
    name = "Examplemol", brandName = "Example Brand", genericNames = "Example salt", form = MedicineForm.SYRUP,
    strength = null, concentration = "120 mg/5 ml", ageReferenceNotes = null, manufacturer = null, notes = null,
)

private fun courseDraft(medicineId: String, hospitalId: String? = null) = ProfileMedicineDraft(
    medicineId = medicineId, doseAmount = "5", doseUnit = "ml", route = MedicineRoute.ORAL,
    foodTiming = FoodTiming.AFTER_FOOD, startDate = LocalDate.of(2026, 10, 1), endDate = null,
    prescribedBy = null, hospitalId = hospitalId, notes = null, status = CourseStatus.ACTIVE,
)

@RunWith(RobolectricTestRunner::class)
class RoomRepositoryTest {
    private lateinit var db: CareChimeDatabase
    private val clock = MutableClock(Instant.parse("2026-10-23T08:00:00Z"))
    private lateinit var profiles: RoomProfileRepository
    private lateinit var hospitals: RoomHospitalRepository
    private lateinit var medical: RoomMedicalRecordRepository
    private lateinit var medicines: RoomMedicineCatalogRepository
    private lateinit var courses: RoomProfileMedicineRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CareChimeDatabase::class.java).allowMainThreadQueries().build()
        profiles = RoomProfileRepository(db, clock)
        hospitals = RoomHospitalRepository(db, clock)
        medical = RoomMedicalRecordRepository(db, clock)
        medicines = RoomMedicineCatalogRepository(db, clock)
        courses = RoomProfileMedicineRepository(db, clock)
    }

    @After fun tearDown() {
        db.close()
    }

    @Test fun profileCrudUsesUuidAndKeepsCreatedAt() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        UUID.fromString(id)
        val created = profiles.getProfile(id)!!
        assertEquals("Test Person", created.name)
        assertEquals(LocalDate.of(1990, 5, 17), created.dateOfBirth)
        assertEquals(BloodGroup.B_POSITIVE, created.bloodGroup)

        clock.now = Instant.parse("2026-10-24T09:00:00Z")
        profiles.updateProfile(id, profileDraft("Renamed Person").copy(bloodGroup = null))
        val updated = profiles.getProfile(id)!!
        assertEquals("Renamed Person", updated.name)
        assertNull(updated.bloodGroup)
        assertEquals(created.createdAt, updated.createdAt)
        assertEquals(clock.now, updated.updatedAt)
        assertEquals(1, profiles.observeProfiles().first().size)

        assertThrows<RecordNotFoundException> { profiles.updateProfile("missing", profileDraft("X")) }
    }

    @Test fun hospitalsAreScopedToTheirProfile() = runBlocking {
        val a = profiles.createProfile(profileDraft("Person A"))
        val b = profiles.createProfile(profileDraft("Person B"))
        val hospitalId = hospitals.createHospital(a, hospitalDraft())
        hospitals.createHospital(a, hospitalDraft("Second Hospital"))

        assertEquals(2, hospitals.observeHospitals(a).first().size)
        assertTrue(hospitals.observeHospitals(b).first().isEmpty())
        assertNotNull(hospitals.getHospital(a, hospitalId))
        assertNull(hospitals.getHospital(b, hospitalId))
        assertThrows<RecordNotFoundException> { hospitals.updateHospital(b, hospitalId, hospitalDraft("X")) }
        assertThrows<RecordNotFoundException> { hospitals.deleteHospital(b, hospitalId) }
        assertThrows<RecordNotFoundException> { hospitals.createHospital("missing", hospitalDraft()) }
        assertEquals(a, hospitals.getHospital(a, hospitalId)!!.profileId)
    }

    @Test fun linksToAnotherProfilesHospitalAreRejected() = runBlocking {
        val a = profiles.createProfile(profileDraft("Person A"))
        val b = profiles.createProfile(profileDraft("Person B"))
        val hospitalOfA = hospitals.createHospital(a, hospitalDraft())
        val medicineId = medicines.createMedicine(medicineDraft)
        val history = MedicalHistoryDraft(LocalDate.of(2026, 1, 2), "Visit", HistoryCategory.CONSULTATION, hospitalOfA, null)

        assertThrows<OwnershipException> { medical.createHistoryEntry(b, history) }
        assertThrows<OwnershipException> { courses.createProfileMedicine(b, courseDraft(medicineId, hospitalOfA)) }
        assertNotNull(medical.createHistoryEntry(a, history))
        assertNotNull(courses.createProfileMedicine(a, courseDraft(medicineId, hospitalOfA)))
    }

    @Test fun updatingProfileNeverDeletesChildRecords() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        hospitals.createHospital(id, hospitalDraft())
        medical.createAllergy(id, AllergyDraft(AllergyCategory.MEDICATION, "Test allergen", null, AllergySeverity.MILD, null, null))
        profiles.updateProfile(id, profileDraft("Test Person Updated"))
        assertEquals(1, hospitals.observeHospitals(id).first().size)
        assertEquals(1, medical.observeAllergies(id).first().size)
    }

    @Test fun deletingProfileCascadesOnlyItsOwnRecords() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        val other = profiles.createProfile(profileDraft("Other Person"))
        val hospitalId = hospitals.createHospital(id, hospitalDraft())
        hospitals.createHospital(other, hospitalDraft())
        medical.createAllergy(id, AllergyDraft(AllergyCategory.FOOD, "Test food", null, AllergySeverity.UNKNOWN, null, null))
        medical.createCondition(id, MedicalConditionDraft("Test condition", ConditionStatus.ACTIVE, null, null))
        medical.createHistoryEntry(id, MedicalHistoryDraft(LocalDate.of(2026, 2, 1), "Test", HistoryCategory.TEST, hospitalId, null))
        val medicineId = medicines.createMedicine(medicineDraft)
        courses.createProfileMedicine(id, courseDraft(medicineId))

        val summary = profiles.deletionSummary(id)
        assertEquals(1, summary.hospitals)
        assertEquals(1, summary.allergies)
        assertEquals(1, summary.conditions)
        assertEquals(1, summary.historyEntries)
        assertEquals(1, summary.medicines)
        assertEquals(5, summary.total)

        profiles.deleteProfile(id)
        assertNull(profiles.getProfile(id))
        assertTrue(hospitals.observeHospitals(id).first().isEmpty())
        assertTrue(medical.observeAllergies(id).first().isEmpty())
        assertTrue(medical.observeConditions(id).first().isEmpty())
        assertTrue(medical.observeHistory(id).first().isEmpty())
        assertTrue(courses.observeProfileMedicines(id).first().isEmpty())
        assertEquals(1, hospitals.observeHospitals(other).first().size)
        assertNotNull("Catalog medicines are independent of profiles", medicines.getMedicine(medicineId))
    }

    @Test fun deletingHospitalKeepsHistoryAndUnlinksIt() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        val hospitalId = hospitals.createHospital(id, hospitalDraft())
        val entryId = medical.createHistoryEntry(
            id,
            MedicalHistoryDraft(LocalDate.of(2026, 3, 4), "Test visit", HistoryCategory.CONSULTATION, hospitalId, null),
        )
        hospitals.deleteHospital(id, hospitalId)
        val entry = medical.getHistoryEntry(id, entryId)!!
        assertNull(entry.hospitalId)
        assertEquals("Test visit", entry.title)
        assertEquals(RecordedHospital("Test General Hospital", "Test City"), entry.recordedHospital)
    }

    @Test fun editingLibraryMedicineNeverRewritesRecordedPrescriptions() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        val medicineId = medicines.createMedicine(medicineDraft.copy(manufacturer = "Test Labs"))
        val courseId = courses.createProfileMedicine(id, courseDraft(medicineId))
        val asPrescribed = PrescribedMedicine(
            name = "Examplemol", brandName = "Example Brand", genericNames = "Example salt", form = MedicineForm.SYRUP,
            strength = null, concentration = "120 mg/5 ml", manufacturer = "Test Labs",
        )
        assertEquals(asPrescribed, courses.getProfileMedicine(id, courseId)!!.prescribedMedicine)

        clock.now = Instant.parse("2026-11-01T08:00:00Z")
        medicines.updateMedicine(
            medicineId,
            medicineDraft.copy(
                name = "Examplemol Forte", form = MedicineForm.TABLET, strength = "650 mg",
                concentration = "250 mg/5 ml", manufacturer = "Other Labs",
            ),
        )
        val before = courses.getProfileMedicine(id, courseId)!!
        assertEquals(asPrescribed, before.prescribedMedicine)
        assertEquals(Instant.parse("2026-10-23T08:00:00Z"), before.updatedAt)
        val item = courses.observeProfileMedicines(id).first().single()
        assertEquals(asPrescribed, item.course.prescribedMedicine)
        assertEquals("250 mg/5 ml", item.medicine?.concentration)

        // Editing the course itself (same medicine) keeps the prescription-time details.
        courses.updateProfileMedicine(id, courseId, courseDraft(medicineId).copy(doseAmount = "2.5"))
        val edited = courses.getProfileMedicine(id, courseId)!!
        assertEquals("2.5", edited.doseAmount)
        assertEquals(asPrescribed, edited.prescribedMedicine)

        // Explicitly choosing a different medicine captures that medicine's current details.
        val otherId = medicines.createMedicine(medicineDraft.copy(name = "Othermol", concentration = "5 mg/ml"))
        courses.updateProfileMedicine(id, courseId, courseDraft(otherId))
        val switched = courses.getProfileMedicine(id, courseId)!!.prescribedMedicine!!
        assertEquals("Othermol", switched.name)
        assertEquals("5 mg/ml", switched.concentration)
    }

    @Test fun editingOrDeletingHospitalKeepsRecordedHospitalOnHistoryAndPrescriptions() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        val hospitalId = hospitals.createHospital(id, hospitalDraft())
        val medicineId = medicines.createMedicine(medicineDraft)
        val historyDraft = MedicalHistoryDraft(LocalDate.of(2026, 3, 4), "Test visit", HistoryCategory.CONSULTATION, hospitalId, null)
        val entryId = medical.createHistoryEntry(id, historyDraft)
        val courseId = courses.createProfileMedicine(id, courseDraft(medicineId, hospitalId))
        val recorded = RecordedHospital("Test General Hospital", "Test City")

        hospitals.updateHospital(id, hospitalId, hospitalDraft("Renamed Hospital").copy(placeName = "Elsewhere"))
        assertEquals(recorded, medical.getHistoryEntry(id, entryId)!!.recordedHospital)
        assertEquals(recorded, courses.getProfileMedicine(id, courseId)!!.recordedHospital)

        hospitals.deleteHospital(id, hospitalId)
        val course = courses.getProfileMedicine(id, courseId)!!
        assertNull(course.hospitalId)
        assertEquals(recorded, course.recordedHospital)

        // Re-saving after the hospital was deleted keeps what was recorded.
        medical.updateHistoryEntry(id, entryId, historyDraft.copy(hospitalId = null, notes = "Follow-up"))
        courses.updateProfileMedicine(id, courseId, courseDraft(medicineId).copy(notes = "Updated"))
        assertEquals(recorded, medical.getHistoryEntry(id, entryId)!!.recordedHospital)
        assertEquals(recorded, courses.getProfileMedicine(id, courseId)!!.recordedHospital)
    }

    @Test fun explicitlyUnlinkingHospitalClearsRecordedHospital() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        val hospitalId = hospitals.createHospital(id, hospitalDraft())
        val draft = MedicalHistoryDraft(LocalDate.of(2026, 3, 4), "Test visit", HistoryCategory.CONSULTATION, hospitalId, null)
        val entryId = medical.createHistoryEntry(id, draft)
        medical.updateHistoryEntry(id, entryId, draft.copy(hospitalId = null))
        assertNull(medical.getHistoryEntry(id, entryId)!!.recordedHospital)
    }

    @Test fun medicineInUseCannotBeDeleted() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        val medicineId = medicines.createMedicine(medicineDraft)
        val courseId = courses.createProfileMedicine(id, courseDraft(medicineId))
        assertEquals(1, medicines.usageCount(medicineId))
        assertThrows<MedicineInUseException> { medicines.deleteMedicine(medicineId) }

        courses.deleteProfileMedicine(id, courseId)
        medicines.deleteMedicine(medicineId)
        assertNull(medicines.getMedicine(medicineId))
    }

    @Test fun profileMedicinesJoinCatalogAndCountActive() = runBlocking {
        val id = profiles.createProfile(profileDraft("Test Person"))
        val medicineId = medicines.createMedicine(medicineDraft)
        val courseId = courses.createProfileMedicine(id, courseDraft(medicineId))
        courses.createProfileMedicine(id, courseDraft(medicineId).copy(status = CourseStatus.PAUSED))

        val items = courses.observeProfileMedicines(id).first()
        assertEquals(2, items.size)
        assertTrue(items.all { it.medicine?.name == "Examplemol" })
        assertEquals(1, courses.observeActiveCourseCount().first())

        val stored = courses.getProfileMedicine(id, courseId)!!
        assertEquals("5", stored.doseAmount)
        assertEquals(FoodTiming.AFTER_FOOD, stored.foodTiming)
        assertThrows<RecordNotFoundException> { courses.createProfileMedicine(id, courseDraft("missing-medicine")) }
    }
}

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {
    @Test fun everySchemaVersionHasAnExplicitMigrationPath() {
        for (from in 1 until CareChimeDatabase.VERSION) {
            assertTrue(
                "Missing migration $from -> ${from + 1}",
                CareChimeMigrations.ALL.any { it.startVersion == from && it.endVersion == from + 1 },
            )
        }
    }

    @Test fun everySchemaVersionHasACommittedSchemaFile() {
        for (version in 1..CareChimeDatabase.VERSION) {
            assertTrue("Missing committed Room schema $version.json", schemaFile(version).isFile)
        }
    }

    /** Builds a real v1 database from the committed 1.json baseline, then opens it with the current code. */
    @Test fun migration1To2KeepsRecordsAndBackfillsSnapshots() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-1-2-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { legacy ->
            createFromSchema(legacy, version = 1)
            legacy.execSQL("INSERT INTO profiles (id, name, createdAt, updatedAt) VALUES ('p1', 'Legacy Person', 1, 1)")
            legacy.execSQL(
                "INSERT INTO profile_hospitals (id, profileId, hospitalName, patientId, placeName, createdAt, updatedAt) " +
                    "VALUES ('h1', 'p1', 'Test General Hospital', 'TEST-0001', 'Test City', 1, 1)",
            )
            legacy.execSQL(
                "INSERT INTO medicines (id, name, brandName, form, concentration, manufacturer, createdAt, updatedAt) " +
                    "VALUES ('m1', 'Examplemol', 'Example Brand', 'SYRUP', '120 mg/5 ml', 'Test Labs', 1, 1)",
            )
            legacy.execSQL(
                "INSERT INTO profile_medicines (id, profileId, medicineId, doseAmount, doseUnit, foodTiming, startDate, " +
                    "hospitalId, status, createdAt, updatedAt) " +
                    "VALUES ('c1', 'p1', 'm1', '5', 'ml', 'AFTER_FOOD', '2026-10-01', 'h1', 'ACTIVE', 1, 1)",
            )
            legacy.execSQL(
                "INSERT INTO medical_history (id, profileId, eventDate, title, category, hospitalId, createdAt, updatedAt) " +
                    "VALUES ('e1', 'p1', '2026-03-04', 'Linked visit', 'CONSULTATION', 'h1', 1, 1), " +
                    "('e2', 'p1', '2026-03-05', 'Unlinked visit', 'OTHER', NULL, 1, 1)",
            )
            legacy.version = 1
        }

        val db = Room.databaseBuilder(context, CareChimeDatabase::class.java, name)
            .addMigrations(*CareChimeMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC)
            assertEquals("Legacy Person", RoomProfileRepository(db, clock).getProfile("p1")?.name)
            val courses = RoomProfileMedicineRepository(db, clock)
            val course = courses.getProfileMedicine("p1", "c1")!!
            val expected = PrescribedMedicine("Examplemol", "Example Brand", null, MedicineForm.SYRUP, null, "120 mg/5 ml", "Test Labs")
            assertEquals("5", course.doseAmount)
            assertEquals(expected, course.prescribedMedicine)
            assertEquals(RecordedHospital("Test General Hospital", "Test City"), course.recordedHospital)
            val medical = RoomMedicalRecordRepository(db, clock)
            assertEquals(RecordedHospital("Test General Hospital", "Test City"), medical.getHistoryEntry("p1", "e1")!!.recordedHospital)
            assertNull(medical.getHistoryEntry("p1", "e2")!!.recordedHospital)

            RoomMedicineCatalogRepository(db, clock).updateMedicine("m1", medicineDraft.copy(concentration = "250 mg/5 ml"))
            assertEquals(expected, courses.getProfileMedicine("p1", "c1")!!.prescribedMedicine)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun recordsSurviveClosingAndReopeningTheDatabaseFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(CareChimeDatabase.FILE_NAME)
        val clock = Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC)

        val first = CareChimeDatabase.build(context)
        val id = RoomProfileRepository(first, clock).createProfile(profileDraft("Persisted Person"))
        first.close()

        val reopened = CareChimeDatabase.build(context)
        try {
            assertEquals("Persisted Person", RoomProfileRepository(reopened, clock).getProfile(id)?.name)
        } finally {
            reopened.close()
            context.deleteDatabase(CareChimeDatabase.FILE_NAME)
        }
    }
}
