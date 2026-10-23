# Future medical data model (schema proposal)

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
