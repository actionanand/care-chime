# Android release artifacts

GitHub Actions writes the latest Android APK/AAB here after a successful build on `main-android`.
Unsigned artifacts are **not** installable/updatable like a signed release APK and cannot be published as a normal signed Play release.
The workflow replaces older APK/AAB files in this folder to keep repository growth bounded.
Downloadable artifacts are also available in the GitHub Actions run for 30 days.

**Never** put keystores, secret values, or private patient data in this directory.
