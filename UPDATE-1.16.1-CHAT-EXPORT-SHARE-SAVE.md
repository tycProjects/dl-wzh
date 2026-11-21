# Update 1.16.1 — Chat export, Android Sharesheet, and hidden save action

- Renamed the AI panel's `Simpan` chip to `Ekspor` without adding a new persistent UI control.
- A normal tap now creates a portable JSON file in the app cache and launches Android's native Sharesheet (`ACTION_SEND` + `EXTRA_STREAM`) through the existing FileProvider. Read permission and ClipData are attached so compatible share targets can open the file.
- A long-press on the same `Ekspor` chip preserves direct saving to `Downloads/AI Island`; a toast explains the long-press shortcut.
- The JSON export contains every message currently retained in the active chat history, role, message ID, full text, error flag, attachment names, downloadable file payloads when available, generated image bytes when the cached image is available and <=12 MiB, plus an in-progress assistant response when present.
- Export metadata explicitly records the current 30-message history cap. Older messages already discarded by the app's history limit cannot be reconstructed by this export.
- Validation: source inspection completed. Gradle compilation was attempted but could not start because the Gradle 8.9 distribution was not cached and this environment could not resolve `services.gradle.org`. Physical-device Sharesheet and save-path validation remain required.
