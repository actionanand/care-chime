# Medical data model

## Implemented: Room schema v2

`CareChimeDatabase` (`app/src/main/java/.../data/db`), file `care-chime.db` in app-private storage, version **2**. Exported schemas: `app/schemas/com.actionanand.carechime.app.data.db.CareChimeDatabase/<version>.json`; every version from 1 to the current one must be committed (`scripts/check-room-schemas.py`, enforced in CI).

| Table | Key columns | Relationships |
| --- | --- | --- |
| `profiles` | `id`, `name`, `dateOfBirth?`, `approximateAgeYears?`, `approximateAgeRecordedOn?`, `relationship?`, `bloodGroup?`, `medicalNotes?` | root |
| `profile_hospitals` | `profileId`, `hospitalName`, `patientId`, `placeName?`, `locationDetails?`, `mapsUrl?`, `doctorName?`, `notes?` | `profileId` → profiles **CASCADE** |
| `allergies` | `profileId`, `category` (MEDICATION/FOOD/ENVIRONMENTAL/OTHER), `allergen`, `reaction?`, `severity`, `notedOn?` | → profiles **CASCADE** |
| `medical_conditions` | `profileId`, `name`, `status`, `diagnosedOn?` | → profiles **CASCADE** |
| `medical_history` | `profileId`, `eventDate`, `title`, `category`, `hospitalId?`, v2: `hospitalName?`, `hospitalPlaceName?` | → profiles **CASCADE**; `hospitalId` → profile_hospitals **SET NULL** |
| `medicines` | `name`, `brandName?`, `genericNames?`, `form`, `strength?`, `concentration?`, `ageReferenceNotes?`, `manufacturer?` | independent catalog |
| `profile_medicines` | `profileId`, `medicineId`, `doseAmount`, `doseUnit`, `route?`, `foodTiming`, `startDate`, `endDate?`, `prescribedBy?`, `hospitalId?`, `status`, v2: `medicineName?`, `medicineBrandName?`, `medicineGenericNames?`, `medicineForm?`, `medicineStrength?`, `medicineConcentration?`, `medicineManufacturer?`, `hospitalName?`, `hospitalPlaceName?` | → profiles **CASCADE**; `medicineId` → medicines **RESTRICT**; `hospitalId` → profile_hospitals **SET NULL** |

Every table also has `createdAt` and `updatedAt`. Foreign-key columns are indexed.

### Historical snapshots (v2)

The medicine library and hospital records are editable reference data, so a record that points at them must not silently change when they are edited.

- When a profile medicine is created, the library entry's name, brand, generic names, form, strength, concentration and manufacturer are copied into the `medicine*` columns. Editing the library entry later never changes them. Editing the profile medicine keeps them while `medicineId` is unchanged; explicitly choosing a different medicine captures that medicine's current details.
- When history or a profile medicine is linked to a hospital, its `hospitalName` and `placeName` are copied. They survive hospital edits and hospital deletion (`hospitalId` becomes null via SET NULL, the recorded name stays and the UI marks it "hospital record removed"). Explicitly unlinking the hospital in the editor clears them.
- The UI shows the recorded details; the medicine list notes when the library entry has changed since.

### Migrations

- `MIGRATION_1_2` is additive only: `ALTER TABLE … ADD COLUMN` (nullable TEXT) on `profile_medicines` and `medical_history`, then backfills the snapshot columns from the current `medicines` / `profile_hospitals` rows. No table is dropped or recreated.
- `DatabaseMigrationTest` builds a real v1 database from the committed `1.json` (tables, indices, Room metadata), migrates it with the production migration list and checks records and backfilled snapshots; Room validates the migrated schema against the v2 entities.

### Conventions (also the future portable export contract)

- Primary keys are random **UUID strings** generated on device; never autoincrement integers.
- Calendar dates are ISO-8601 text `yyyy-MM-dd`; instants are epoch milliseconds (UTC).
- Enums are stored by **constant name**; never rename a constant. Unknown values read back safely (an unknown course status reads as `PAUSED`, so it can never start reminders).
- `doseAmount` is stored as normalised decimal text exactly as prescribed (e.g. `0.5`), never as a float, with ≤3 decimals.
- Age is **never stored**. It is derived from `dateOfBirth`, or from `approximateAgeYears` plus the years elapsed since `approximateAgeRecordedOn`. A profile has either a DOB or an approximate age, not both. Display: under 1 month in days, under 1 year in months, 1–4 years as years and months (e.g. "2 years 3 months"), 5+ in years. Approximate ages show whole years only, prefixed "About", and never invent months.

### Integrity rules

- Child records are always read, updated and deleted by `(profileId, recordId)`; a record cannot be reached through another profile. Linking history or a medicine record to a hospital of a different profile is rejected (`OwnershipException`).
- Updates use `@Update`, never `OnConflictStrategy.REPLACE`; SQLite REPLACE is delete + insert and would fire `ON DELETE CASCADE`.
- Deleting a profile is a confirmed user action that shows the number of hospitals, allergies, conditions, history entries and medicine records that will be removed with it. Catalog medicines are not deleted.
- A catalog medicine used by any profile medicine record cannot be deleted (`MedicineInUseException` + FK `RESTRICT`).
- Deleting a hospital keeps history entries and medicine records, removing only the link; the hospital name/place recorded with them is kept.
- Editing a library medicine or hospital never rewrites existing profile medicine or history records (v2 snapshots).
- Allergies (substances causing reactions, including medication allergies) are a separate table from `profile_medicines` (medicines prescribed, including those that treat allergies).
- No destructive migrations. Each version bump requires a `Migration` in `CareChimeMigrations.ALL`, a committed schema JSON and a migration test; unit tests fail if a step or schema file is missing.

## Planned entities (not implemented yet)

**This remains a design proposal; it is not Room code yet.**

- `Visit(id, profileId, hospitalId?, doctor?, attendedAt, notes?)`
- `Schedule(id, medicationOrderId, type, ruleJsonVersioned, zoneId, scheduleRevision, status)` — `profile_medicines` is the `MedicationOrder`; rule shapes are defined in `domain/schedule`.
- `DoseOccurrence(id, scheduleId, plannedAt, status, sourceRevision)`
- `DoseEvent(id, occurrenceId?, orderId, administeredAt?, recordedAt, type, actorProfileId?, notes?, correctionOf?)`
- `WaterEvent(id, profileId, amountMl, consumedAt, recordedAt)`
- `Vaccination(id, profileId, vaccineName, doseNumber?, dueAt?, administeredAt?, hospitalId?, lotNumber?, status)`
- `Attachment(id, ownerRecordId, mimeType, length, contentHash, encryptedContentReference)`
- `ChangeLog(id, entityType, entityId, operation, revision, happenedAt, deviceId)`

## Original proposal notes

Use UUID/ULID stable identifiers (not autoincrement IDs across exports), creation/update timestamps, record provenance and schema versions. **This is a proposed design, not Room migration code yet.**

## Entities

- `Profile(id, name, dateOfBirth?, approximateAge?, relationship?, bloodGroup?, createdAt, updatedAt)`
- `Hospital(id, name, placeText?, googleMapsUrl?, notes?)`
- `ProfileHospital(id, profileId, hospitalId, patientId?, notes?)` (unlimited)
- `Allergy(id, profileId, allergenType, allergenName, reaction?, severity?, notes?)`
- `Condition(id, profileId, diagnosisName, diagnosedAt?, state, notes?)`
- `Visit(id, profileId, hospitalId?, doctor?, attendedAt, notes?)`
- `MedicineCatalog(id, brandName, genericNames[], form, strength?, concentration?, manufacturer?, notes?, guidanceSource?)`
- `MedicationOrder(id, profileId, medicineId, doseAmount, doseUnit, prescribedBy?, route?, instructions?, startsAt?, endsAt?, status)`
- `Schedule(id, medicationOrderId, type, ruleJsonVersioned, zoneId, scheduleRevision, status)`
- `DoseOccurrence(id, scheduleId, plannedAt, status, sourceRevision)`
- `DoseEvent(id, occurrenceId?, orderId, administeredAt?, recordedAt, type, actorProfileId?, notes?, correctionOf?)`
- `WaterEvent(id, profileId, amountMl, consumedAt, recordedAt)`
- `Vaccination(id, profileId, vaccineName, doseNumber?, dueAt?, administeredAt?, hospitalId?, lotNumber?, status)`
- `Attachment(id, ownerRecordId, mimeType, length, contentHash, encryptedContentReference)`
- `ChangeLog(id, entityType, entityId, operation, revision, happenedAt, deviceId)`

Prescribed orders are profile-specific. The general medicine catalog is **reference-only**; never turn optional age guidance into a dosing calculator. Record syringe/drop concentration and dose units explicitly, never guess conversion factors. Allergies and allergy medications are distinct.

For vaccines, show under-five prompts when a date of birth is available; retain life-long historical records and allow older-age schedules. With no DOB, allow manual vaccine dates.

## Rules

Every dose has an immutable event trail with planned vs actual time. Medicine stock is optional and separate from administered-dose records. Deleting a profile should be a deliberate, reviewed user action; don't accidentally cascade deletion of external backup files. Export the complete history and all attachments with data-preserving migrations.
