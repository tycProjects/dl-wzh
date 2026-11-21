# Android Island AI Expansion — Phase 2

## Implemented in this revision

### Explicit local AI memory
- Added `AiMemoryStore`, a small local-only memory store encrypted with AES-256-GCM.
- Encryption keys are generated and retained by Android Keystore; plaintext memories are not written to SharedPreferences.
- Memory is disabled by default and has an explicit setting in the AI settings page.
- When enabled, only user-authored explicit commands are stored (`ingat ...` / `simpan ke memori ...`).
- Supported management commands: `apa yang kamu ingat`, `lihat memori`, `hapus memori nomor 2`, `ubah memori nomor 2 menjadi ...`, and `hapus semua memori`.
- Memory backup is excluded because Android Keystore keys are device-bound and are not restored with app backups.
- Memory is capped at 50 entries and 500 characters per entry. Relevant entries are added as context, clearly labeled as user-approved data rather than instructions.
- Disabling memory stops retrieval and storage; it does not erase existing entries. Use `hapus semua memori` while enabled to erase them.

### Text-to-Speech
- Added lazy, process-scoped Android TextToSpeech output.
- Assistant messages now expose Speak and Stop controls.
- Speech is capped at 6,000 characters per request and does not keep a foreground service running.
- Uses the device locale and falls back to US English if that voice is unavailable.

## Files added
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/java/com/android/island/ai/AiMemoryStore.kt`
- `app/src/main/java/com/android/island/ai/voice/SpeechOutput.kt`

## Files changed
- `app/src/main/java/com/android/island/ai/AiSession.kt`
- `app/src/main/java/com/android/island/data/SettingsRepository.kt`
- `app/src/main/java/com/android/island/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/android/island/island/AiPanel.kt`

## Compatibility and security notes
- No additional Gradle dependency or Android permission was added. The backup rules exclude only the encrypted AI-memory preference file.
- Keystore AES/GCM is available on the project's API 26 minimum.
- Memory is opt-in and local. If Android backup restores the encrypted preference without its Keystore key, the unreadable memory is treated as unavailable; it is not sent to the model.
- TTS uses the device's installed speech engine; actual language availability depends on the device and installed voice data.

## Validation status
- Source-level integration and archive integrity checks performed.
- Gradle compile and unit tests are not certified in this environment because Gradle 8.9 is not installed/cached and the Gradle distribution cannot be fetched without network access.
- Device-level TTS voice availability, Keystore behavior on OEM devices, and runtime UI behavior still require device testing.


## Catatan 1.14.9
- Lihat UPDATE-1.14.9-SHELL-SEARCH-ENTER.md untuk perbaikan shell Shizuku, diagnosa meminfo, dan pencarian dalam aplikasi.
