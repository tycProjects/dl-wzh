# Update 1.14.1 — Semua Model Gemini Bisa Dipakai

- Daftar model Gemini memuat semua model chat dan teks dari versi mana pun (sebelumnya hanya 3.5 ke atas).
  Model media, audio, embedding, dan agent tetap disaring.
- Model yang dipilih pengguna benar-benar dipakai. Routing diam-diam ke model default dihapus.
- Kunci API dikirim di header `x-goog-api-key`, bukan di URL (untuk chat dan daftar model).
- Model Gemini 3 mengirim `generationConfig.thinkingConfig.thinkingLevel = "LOW"`, sesuai dokumentasi REST.
- Test: GeminiModelFilterTest.

## Status verifikasi
Test ditulis lebih dulu. Sandbox tidak punya Kotlin compiler atau Gradle, jadi belum dijalankan. Uji dengan
`./gradlew testDebugUnitTest`, lalu pilih beberapa model (mis. gemini-2.5-flash, gemini-3.1-flash-lite) dan kirim chat.

## Catatan
- Model yang sudah dihentikan Google (mis. gemini-2.5-pro setelah 16 Oktober 2026) tidak muncul di daftar `/models`, sehingga tidak bisa dipilih.
- Thinking level diatur tetap ke LOW. Pengaturan per pengguna belum ada.
