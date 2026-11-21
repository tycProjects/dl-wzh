# Stage 65 — Agent UI toolkit & pantang menyerah

## Akar masalah yang ditemukan
- Agent tidak punya aksi untuk menekan Enter/Cari (hanya `search_in_app` yang menekan Enter di dalam alurnya sendiri), tidak bisa mengetuk ikon tanpa teks, dan hanya melihat daftar teks layar, bukan elemen yang bisa diketuk.
- `accessibility_service_config.xml` tidak mendeklarasikan `canPerformGestures`, sehingga setiap `dispatchGesture` (tap koordinat, swipe, fallback klik) tidak bekerja. Flag `flagReportViewIds` dan `flagRetrieveInteractiveWindows` juga tidak ada, jadi view id dan jendela keyboard tidak terbaca.
- `type` melewati kolom pencarian (InputPicker) dan ditolak guard bila pesan pengguna tidak memuat kata "ketik".
- Loop berhenti pada "tanpa aksi" / `done=true` meski model baru menulis "saya tidak bisa, silakan tekan sendiri".

## Perubahan
- Aksi baru: `press_enter`, `tap(x,y)`, `click_index(index)` (juga `click` dengan `index`).
  - `press_enter`: Accessibility IME_ENTER -> tombol aksi keyboard (jendela IME) -> Shizuku keyevent 66 -> 84.
  - `tap`: dispatchGesture -> Shizuku `input tap`. `click_index` memakai pusat elemen dari snapshot layar terbaru.
- Observasi layar kini diakhiri daftar "Elemen interaktif" bernomor (label, fokus, koordinat tengah), maks 60, tanpa kolom kata sandi.
- `type` mengisi kolom yang sedang fokus lebih dulu (kolom pencarian yang difokuskan agent), baru memakai pemilih kotak pesan.
- Guard: aksi baru memakai aturan yang sama seperti `click`; `type` diizinkan untuk tugas cari/kirim/tulis. `click_index`/`tap` tetap lewat ClickGuard memakai label elemen. `press_enter` di kolom non-pencarian diperlakukan seperti tombol "Kirim".
- Loop: `AgentLoopPolicy.shouldChallengeStop` menolak berhenti (maks 3 kali) saat done=false tanpa aksi, langkah terakhir gagal, atau balasan menyerahkan tugas ke pengguna. Layar dibaca ulang dan `AgentRecoveryPolicy.persistencePrompt` memberi tangga jalur alternatif. `decide` tidak berubah.
- Prompt sistem: alat baru, aturan UI TOOLKIT dan PERSISTENCE; skema tool native OpenAI/Gemini diperluas (x, y, index).

## WAJIB setelah memasang
Matikan lalu nyalakan lagi layanan Accessibility Android Island (atau pasang ulang aplikasinya). Android hanya membaca izin gesture dan flag baru saat layanan di-bind ulang.

## Verifikasi (apa yang benar-benar dijalankan)
- AgentLoopPolicy, AgentRecoveryPolicy, ScreenElements dan 4 test lama terkait: dikompilasi dengan kotlinc 2.0.21 dan dijalankan (28 lulus, 0 gagal).
- UserIntentActionGuardTest (dengan stub kecil): 5 lulus, 1 gagal. Kegagalan `assistantProseIsNeverAnAuthorizationSource` sudah ada di zip asli (OpenApp dianggap diizinkan karena nama app muncul di kalimat "Jelaskan fungsi YouTube"); tidak diubah.
- Seluruh sumber utama dikompilasi terhadap android.jar API 35: jumlah error tidak berubah dibanding zip asli (semuanya karena dependensi androidx/Compose/ML Kit tidak ada di sini), dan nol error di file yang diubah. Uji sensitivitas dengan error sengaja memastikan kompiler melaporkannya.
- Belum dijalankan: Gradle (`./gradlew testDebugUnitTest`, `assembleDebug`), test JUnit asli, dan uji di perangkat (TikTok/WhatsApp, Enter, tap). Jalankan sebelum rilis.
