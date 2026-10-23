# Encrypted portable backup — design only

**No backup/encryption implementation exists in this starter.** This document is the future interoperability contract to finalize before shipping.

The export payload will reuse the storage conventions already fixed by Room schema v1 (UUID string IDs, ISO `yyyy-MM-dd` dates, epoch-millisecond instants, enum constant names, decimal-text dose amounts); see [DATA_MODEL.md](DATA_MODEL.md).

## Requirements

- User-supplied password, password confirmation and no server dependency.
- Encrypted payload includes profiles, hospitals, medicines, dose events, reminders, vaccines, water logs, attachments and settings needed to rebuild alarms.
- Portable across Android versions and future Angular web client; do not export a raw, device-bound Android Keystore object.
- Manifest includes format version, cipher suite and non-sensitive metadata only. NEVER expose profile names, hospitals, exact dates, medicine names or attachments in plaintext.
- Password-based derivation using a reviewed memory-hard KDF (e.g. Argon2id) and a random salt; authenticated encryption (e.g. AES-256-GCM) with unique nonce per encryption. Test a single format in both Kotlin and TypeScript.
- Restore verifies integrity, checks record/schema compatibility, stages data in a temporary DB, and atomically swaps only after verification and user confirmation.
- Snapshot export must include a consistent database view. Prevent partial restorations, duplicate dose events, stale alarms and wrong-profile joins.
- Prefer full replacement restore initially; merging requires separate conflict semantics and an audit trail.
- File extension, MIME type, header and versioned schema will be selected and documented after final implementation.

## Operational safety

Use Android's Storage Access Framework for import/export; avoid broad file permissions. A user's encrypted backup password cannot be reset from server if no recovery system exists. Warn clearly and enable test-restore. Opt-out of OS unencrypted backups via manifest and data extraction rules (already included).

## Web and synchronization distinction

Opening and exporting a file in Angular does **not** constitute real-time sync. Live sync later needs identity, authorization, conflict handling and a way for Android to acknowledge a newly applied alarm schedule.
