# Privacy, security and safety guidelines

- **Offline-first**: do not add internet permission or analytics without explicit, documented functionality and opt-in decisions.
- **Health data**: use app-private storage, encryption at rest where feasible, strict logging/redaction and user-controlled export.
- **Backups**: authenticated password encryption; no unencrypted cloud or auto backup. `allowBackup=false` and extraction exclusions are set in the starter manifest.
- **No clinical inference**: app tracks *entered instructions*. Do not calculate pediatric dose solely from age/weight or modify prescribed schedule to catch up missed medication.
- **Reminders**: never claim guaranteed delivery with phone off, app force-stopped or permissions disabled. Track alarm reliability state.
- **Android signing**: keystore, Base64 keystore, passwords and PEM files must remain untracked. Keep offline copies of production signing key and password.
- **App ID**: `com.actionanand.carechime.app` is immutable once published.
- **Attachments**: validate size, content type and file handling; ensure health documents aren't shared publicly without user intent.
- **Play Store**: perform current health-app declarations, privacy policy and permission eligibility checks prior to publication.
