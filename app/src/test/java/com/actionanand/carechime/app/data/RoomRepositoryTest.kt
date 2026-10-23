package com.actionanand.carechime.app.data

import android.content.Context
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
import com.actionanand.carechime.app.domain.model.ProfileDraft
import com.actionanand.carechime.app.domain.model.ProfileMedicineDraft
import com.actionanand.carechime.app.domain.repository.MedicineInUseException
import com.actionanand.carechime.app.domain.repository.OwnershipException
import com.actionanand.carechime.app.domain.repository.RecordNotFoundException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
