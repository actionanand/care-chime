# Care Chime Angular web companion

Android remains the platform for native medication alarms. A browser app cannot be relied upon to deliver precisely timed alarms when closed or suspended. Web can provide profile editing, medicine catalog, reports, vaccination schedule management and encrypted import/export.

## Phase A: no server

Build Angular PWA separately under `web/` after the Android schema is stable. Use client-side decryption and IndexedDB only as a temporary working copy; exported encrypted file remains the portable backup. Protect against browser storage eviction. Show provenance and device snapshot date.

## Phase B: optional synchronization

Only after user demand, introduce backend with explicit opt-in, appropriate security and healthcare-data review. Use event IDs and revisions; do not implement last-write-wins for medical dose events. Display status: web edit saved vs received vs alarm applied on Android. Offline devices cannot be assumed to receive changes instantly.

## Contract

Android keeps scheduling decisions in framework-free Kotlin (`domain/schedule`, rule format version 1) so the same rules and test cases can be reproduced in TypeScript.


`shared/schemas/` will eventually hold language-neutral, versioned exports and validation fixtures. Never share an Android raw SQLite file as the sole web contract.
