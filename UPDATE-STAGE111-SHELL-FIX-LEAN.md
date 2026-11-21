# Stage 111 (basis stage110) — belum dikompilasi

1. BUG shell (dibuat di stage110, sekarang diperbaiki): `ShizukuShell.awaitExitCode` memakai `Process.waitFor(timeout)`; wrapper Process Shizuku
   tidak mendukungnya dengan andal, jadi tiap perintah gagal (`SHELL_RUN exit=none timedOut=false stdoutChars=0`). Dikembalikan ke polling `exitValue()`,
   kini dengan jeda bertahap 10 -> 250 ms (perintah cepat terdeteksi ~10 ms, perintah lama dicek maks 4x/detik).
2. Pencatat Diagnostik tidak lagi merekam sentuhan/koordinat (TOUCH_FORWARD, OUTSIDE_TOUCH), sampel morph per frame (MORPH_SAMPLE), COMPOSE_STATE,
   DRAW_LAYOUT, TOUCH_BOUNDS_*, STATE_OBSERVER, WINDOW_LAYOUT_UPDATE. Diagnostik default mati; semua log hanya lokal (tidak diunggah).
3. build.gradle.kts: build release ditandatangani debug key supaya `./gradlew assembleRelease` menghasilkan APK yang bisa dipasang.
   Release = minify + shrink resources; debug tidak. Ukur RAM dengan release.
4. Low-End lebih hemat (permintaan pengguna, mengubah keputusan stage92/93 "disamakan"): artwork 224 px (Normal 320), tick animasi dekoratif 50 ms (Normal 33 ms).
5. Terverifikasi dari log pengguna: `IDLE_TRIM reason=capsule renderer=true gc=false` muncul ~8 dtk setelah capsule diam (trim stage110 jalan).
6. Default model Gemini = `gemini-3.5-flash-lite` (permintaan pengguna): `GEMINI_DEFAULT_MODEL` (AiClient), preset Gemini di HomeScreen (defaultModel terisi),
   dan urutan `ModelPicker.GEMINI_CHAT_PRIORITY` (Lite dulu: 3.5-flash-lite, 3.1-flash-lite; baru Flash penuh terbaru). Pilihan eksplisit pengguna tetap menang.
   Pengguna lama yang sudah memilih model sendiri tidak diubah; yang `aiModel` kosong kini otomatis ke Lite bila tersedia di katalog live.
   thinkingLevel sudah LOW untuk gemini-3.x (tidak diubah). Tes baru: `prefersGemini35FlashLiteAsDefaultWhenAvailable`.
7. Varian debug disamakan dengan release (`isMinifyEnabled`, `isShrinkResources`, `isDebuggable=false`, ProGuard sama) supaya APK builder yang menjalankan
   `assembleDebug` tetap menghasilkan APK ringan. `defaultTasks("assembleRelease")` di build.gradle.kts root untuk `./gradlew` tanpa argumen.
   Konsekuensi: tidak bisa di-debug, stack trace diacak (mapping di `app/build/outputs/mapping`). Bila build gagal/crash karena R8, kirim errornya
   (kemungkinan perlu aturan keep di `proguard-rules.pro` untuk kode yang dipakai lewat reflection).
