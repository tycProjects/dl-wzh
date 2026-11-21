# point.md — Catatan diskusi AndroidIsland (lengkap sampai stage110)

Simpan file ini dan unggah ke obrolan/akun baru bersama ZIP proyek terbaru untuk melanjutkan diskusi.

## 0. Petunjuk untuk AI yang membaca file ini (WAJIB)
1. Baca seluruh file ini dulu, lalu ringkas singkat ke pengguna: status terakhir dan pertanyaan yang masih terbuka. Setelah itu tanyakan mau lanjut dari bagian mana.
2. Aturan kerja dari pengguna:
   - Catat setiap poin diskusi di file ini.
   - Berinisiatif memperbaiki hal yang perlu diperbaiki tanpa menunggu disuruh (contoh: hapus slider yang sudah tidak berfungsi).
   - Bila perlu event, pakai event/callback, jangan polling di latar belakang (hemat RAM, baterai, CPU).
   - Jangan mematikan fungsi dasar: capsule saat musik diputar, dan banner notifikasi pengganti pop-up bawaan Android yang dimatikan Shizuku.
   - Target utama proyek: hemat RAM; prioritaskan temuan yang menurunkan RAM dan ukur dengan `dumpsys meminfo`.
   - Bahasa: Indonesia.
3. Jujur soal verifikasi: tulis apa yang benar-benar dijalankan. Jangan menyatakan "sudah dikompilasi/diuji" bila tidak. Lingkungan Claude tidak punya Android SDK/Gradle, jadi semua perubahan stage87 sampai stage111 belum dikompilasi dan belum diuji di HP.
4. Sebelum mengubah kode, baca dulu file terkait dan telusuri pemanggilnya. Perubahan sekecil dan seaman mungkin, karena tidak bisa dikompilasi di sesi ini.
5. **Di akhir sesi (atau saat pengguna bilang limit hampir habis), buat file laporan serupa**: perbarui `point.md` ini (tambah ringkasan sesi di "Riwayat perubahan", perbarui "Keputusan pengguna", "Pertanyaan terbuka", dan "Langkah berikutnya"), lalu berikan sebagai file yang bisa diunduh. Tambahkan juga `UPDATE-STAGE<N>-<NAMA>.md` di ZIP untuk rincian teknis tiap stage. Struktur wajib: Konteks proyek, Keputusan pengguna, Riwayat perubahan, Data perangkat, Pertanyaan terbuka, Langkah berikutnya.

## 1. Konteks proyek
- Aplikasi: Android Island (Dynamic Island overlay), Kotlin + Jetpack Compose, minSdk 26, compileSdk 35. Package `com.android.island`.
- **TARGET UTAMA: hemat RAM** (lalu baterai dan CPU), tanpa mematikan fungsi dasar. Setiap perubahan dinilai dulu dari dampaknya ke RAM.
- Berjalan sebagai AccessibilityService (overlay `TYPE_ACCESSIBILITY_OVERLAY`), NotificationListener (media + notifikasi), dengan Shizuku untuk perintah sistem.
- ZIP terbaru: `AndroidIsland_stage111_shell_fix_lean.zip` (basis stage110, sebelumnya stage109 `AndroidIsland_stage109_no_stack.zip`). Rincian: `UPDATE-STAGE111-SHELL-FIX-LEAN.md`, `UPDATE-STAGE110-EVENT-ONLY.md`, `UPDATE-STAGE109-NO-STACK-AGENT-STOP.md`, `UPDATE-STAGE104-MEDIA-WATCH-ALL-NOTIF-AGENT-STOP.md` (berisi juga bagian Stage 108).
- Kalau build gagal, pengguna mengirim error dan AI memperbaikinya.

## 2. Keputusan yang sudah dibuat pengguna
- Mode Normal dan Low-End hanya berbeda di efek liquid glass; tinggi window, cap panel AI 46%, tick animasi, artwork 320 px, cache lirik 8, dan lainnya disamakan (stage92, 93).
- Panel AI tanpa liquid glass, neon statis di semua mode (stage94).
- Restart paksa (force-stop, re-grant accessibility, `pm trim-caches 999G`, buka app lagi) tetap berjalan seperti sebelumnya (stage99). Pembersihan cache dari chat AI hanya lewat frasa eksplisit (stage98).
- Agent hanya membaca perintah saat ini, bukan riwayat chat sebelumnya; agent berhenti sendiri begitu tujuan tercapai.
- Banner dan indikator samping hanya untuk notifikasi yang bergetar. Tap membuka app pengirim, bukan WhatsApp. Semua app memakai logika banner WhatsApp lama (stage108).
- Sistem banner stack dihapus total; indikator samping polos, tanpa glow/animasi, tidak bisa ditekan (stage109).
- Jawaban AI tidak boleh terpotong (lanjut otomatis). Teks chat AI bisa dipilih per kata. Kode tidak terpakai dibuang dan memori di-trim saat idle (stage108).
- Tidak ada browser dengan UI sendiri di Island. Link (open_url, tap "Baca Sumber" setelah mencari berita) langsung dialihkan ke Chrome/Google di perangkat (stage108).
- Animasi bar capsule saat musik dipertahankan, loop memakai delay `DeviceProfile.tickMs` (bukan withFrameNanos) (stage95).
- Gunakan event, bukan polling, di latar belakang (stage109, 110). Pengecualian: `ShizukuShell.awaitExitCode` WAJIB polling `exitValue()` (jangan pakai `Process.waitFor(timeout)`, merusak semua perintah shell Shizuku; ada komentar di kode).
- Island tidak boleh mengumpulkan data analisis (sentuhan, koordinat, dsb.) kecuali yang dibutuhkan fungsi dasar. Diagnostik default mati, hanya log lokal, dan tidak merekam sentuhan (stage111).
- Default model Gemini = `gemini-3.5-flash-lite` (stage111). Pilihan eksplisit pengguna tetap menang; urutan otomatis: Lite dulu, lalu Flash penuh terbaru.
- Low-End sengaja lebih hemat daripada Normal (artwork 224 px, tick 50 ms), mengubah keputusan stage92/93 (stage111).

## 3. Riwayat perubahan penting
- stage87: glass tanpa blur dan tanpa vignette.
- stage89: notifikasi aplikasi lain ikut masuk; latar avatar (WhatsApp) atau ikon aplikasi.
- stage90 lalu stage95: animasi bar capsule; loop dikembalikan ke delay `DeviceProfile.tickMs` (withFrameNanos terlalu boros).
- stage91-93: composer AI setinggi kartu tanpa dim; penyamaan mode Normal dan Low-End.
- stage94: panel AI statis, divider wave dimatikan.
- stage96, 102, 103: callback audio untuk menyambung ulang listener (nama API dikoreksi: `onPlaybackConfigChanged`; tanpa membaca `isActive`/`playerState`).
- stage97: clock breath marker WhatsApp hanya aktif saat ada marker (kemudian dibuang di stage109).
- stage98-99: pembersihan cache hanya lewat frasa eksplisit; restart paksa dipertahankan.
- stage100: log diagnostik `ASSIST_SESSION_SHOW` untuk panel AI via tombol power.
- stage104:
  - Capsule: semua sesi media dipantau (`watchAll`), bukan hanya sesi terpilih. Penyebab: setelah idle, pemutar sering punya sesi tanpa state/metadata (rank 0), tidak pernah terpilih, dan tidak ada callback saat lagu mulai.
  - Callback audio memanggil `onAudioActivity`: sambung ulang listener bila putus, atau baca ulang sesi sekali (debounce 1,5 s, tunda 700 ms) bila island tidak menampilkan musik yang sedang diputar.
  - Banner: semua notifikasi importance >= DEFAULT lolos. Ongoing, ringkasan grup, dan notifikasi app sendiri dibuang. Judul boleh dari `EXTRA_TITLE_BIG`; teks boleh kosong atau `EXTRA_SUB_TEXT`.
  - Agent: riwayat hanya perintah saat ini (`AiSession.runObserveActLoop`); berhenti setelah `PlaySong` berhasil bila perintah tanpa langkah lanjutan ("lalu", "kemudian", "setelah", dst.), lewat `AgentLoopPolicy.isSingleGoal`. Ada dua unit test.
- stage105: slider "Blur artwork (Gaussian)" dihapus dari `HomeScreen.kt` (blur sudah dibuang di stage87, `frostStrength` tak dipakai). Field pengaturan dibiarkan agar data lama terbaca. Audit 36 slider lain: semua masih dibaca di kode render.
- stage106: pemangkasan RAM native/grafis. Temuan: overlay selalu terlihat bagi Android, jadi trim bawaan (UI hidden) tidak pernah datang dan cache renderer HWUI/Skia tidak pernah dikosongkan. `util/NativeTrim.kt` meminta trim sendiri 1,5 s setelah window mengecil ke capsule, setelah overlay disembunyikan, dan 3 s setelah layar mati (rate limit 20 s; satu GC maks tiap 5 menit). `ScreenOcr` melepas recognizer ML Kit setelah 10 s idle. Log `IDLE_TRIM` menunjukkan `renderer=true` bila berhasil. Besar penurunan belum terukur.
- stage107: Java heap (Dalvik 31 MB PSS, Alloc 20,7 MB, Free 20,7 MB). Heap menyimpan ukuran committed setelah GC, jadi separuh kosong tetap dihitung di PSS. `NativeTrim` menjalankan GC lalu `VMRuntime.trimHeap()` di thread latar saat idle (maks tiap 5 menit). `IslandDiagnostics`: ekor RAM 2000 -> 500 baris, log dibaca streaming. Catatan ukur: matikan Diagnostik saat mengukur RAM/CPU.
- stage108: Browser Island dibuang (`BrowserActivity` + intent-filter VIEW/BROWSABLE di manifest dihapus). `openUrl` memakai `ACTION_VIEW` biasa; "buka browser/internet/web" memakai `CATEGORY_APP_BROWSER`. Dugaan (belum terukur): WebView memuat Chromium ke proses Island dan menambah Native Heap.
- stage108 lanjutan:
  - Filter getar: `willVibrate()` memakai `Ranking.channel.shouldVibrate()` (filter lama memakai `NotificationManager.getNotificationChannel` yang hanya membaca channel app sendiri, jadi untuk app lain selalu lolos). Importance < DEFAULT, DND, ringer senyap menolak.
  - Tap ke app pengirim: `pkg` yang hard-code `com.whatsapp` diganti `packageName` app pengirim; `contentIntent` dulu, lalu `getLaunchIntentForPackage`.
  - Indikator: ikon lonceng gradasi ungu-cyan; aksen banner dan ikon latar kartu mengikuti app (`accentForPackage`, `appIconBitmap`, LRU 6).
  - Replay notifikasi < 120 s saat listener tersambung ulang untuk semua app. Pemisahan grup lewat titik dua hanya WhatsApp.
  - Berita: feed diambil paralel (batas 7 s), cache 90 s, timeout 4/5 s.
  - Jawaban terpotong: `AiClient` deteksi `finish_reason=length`/`MAX_TOKENS` lalu minta lanjutan otomatis (maks 3x, tidak untuk JSON agent).
  - Salin per kata: `SelectionContainer` di bubble chat (tidak saat streaming); tautan memakai `LinkAnnotation`; long-press bubble dihapus.
  - Pembersihan kode mati: `gaussianBoxRadii`, `blurAxis`, `buildBlurEffect`, `runLocalCommands`, `clearPending`, `cleanupAction`, `markWhatsAppSystemRemoving`, `exportLyricsFile`, `clearVerifiedAi`, `IslandDiagnostics.exportText`. Antrean notifikasi maks 20, kartu > 12 jam dibuang.
  - Test baru: `IslandStackGroupingTest`.
- stage109 (permintaan pengguna):
  - Indikator samping tanpa glow/animasi dan tidak bisa ditekan; stack dihapus total (kode, UI, anggaran jendela, slider). Tap capsule notifikasi membuka app pengirim.
  - `replyOpen` jadi StateFlow; jam breath dibuang. Default cutout iPhone 170x32 dp.
  - Animasi tutup: capsule opak penuh di bawah panel sebelum panel memudar (dugaan, belum diuji).
  - Artwork latar panel lagu dikecilkan ke 96 px (RGB_565, buram halus tanpa blur pass).
  - Panel AI tidak menutup sendiri: notifikasi tidak menggantikan kartu AI; event systemui hanya Recents; sentuhan agent tidak dihitung ketuk luar (`AiSession.agentBusy`).
  - Agent berhenti saat tujuan tercapai: STOP RULES di prompt, cek tujuan tiap langkah, challenge tidak lagi memaksa lanjut.
- **stage110 (sesi ini, audit polling/event):**
  - Hasil audit: hanya satu polling nyata yang tersisa di latar belakang (cek hati/repeat 2,5 dtk saat island expanded). Loop lain sudah event atau one-shot.
  - `WhatsAppNotificationListener`: `refreshTick` dihapus. Pengganti event: `onExtrasChanged` di semua sesi yang dipantau, callback metadata/playback yang ada, pantauan notifikasi media, dan baca ulang sekali saat island dibuka (+450 ms, +1300 ms).
  - `Controls`: `MediaControllerCompat.Callback.onRepeatModeChanged` didaftarkan saat sesi di-bind (dilepas saat sesi ganti/hilang); repeat dibaca sekali saat bind.
  - `ShizukuShell.awaitExitCode`: loop `Thread.sleep(50)` diganti `Process.waitFor(timeout)`.
  - `ScreenState.awaitVisible()` (layar nyala DAN tidak terkunci) dipakai loop dekoratif: bar capsule, shine lirik, drift gelombang. Timer posisi lagu tetap `awaitInteractive`.
  - `IslandAccessibilityService.onAccessibilityEvent`: membuka app mana pun yang punya kartu notifikasi membersihkan kartu app itu (sebelumnya hanya WhatsApp, sehingga banner Telegram/Gmail dll menggantung). Hanya scan daftar di memori.
  - `AiSession`: aturan berhenti saat tujuan tercapai kini juga untuk `PlayInApp` (sebelumnya hanya `PlaySong`).
  - **RAM (temuan penting):** penyusutan window ke capsule sengaja dimatikan (`windowShrinkAllowed = false`, bikin stutter), sehingga trim RAM idle stage106 yang menempel di jalur itu tidak pernah jalan di kondisi capsule. Kini `updateIdleTrim()` memicu `NativeTrim.trimWhenIdle("capsule")` sekali, 8 dtk setelah island settle di COMPACT (dibatalkan saat keluar COMPACT, tanpa loop). Besar penghematan belum terukur.
  - Riwayat chat AI dari memori sengaja tidak dilepas (hanya 30 pesan teks, hemat tak berarti).
  - Sengaja dipertahankan: bar capsule saat musik (30 Hz, turun di Battery Saver), timer posisi lagu 250 ms, debounce touch-bounds, burst cek lock 250/800/2000 ms setelah layar nyala/mati.
  - Tidak dikerjakan: parameter `breath` di `CompactIsland` (selalu null, kode mati kecil); melepas riwayat chat AI dari memori saat panel ditutup.
  - Verifikasi: tidak dikompilasi; hanya dibaca dan diedit.

- **stage111 (sesi ini):**
  - BUG shell dari stage110 diperbaiki: `waitFor(timeout)` gagal di wrapper Process Shizuku (log: `SHELL_RUN exit=none timedOut=false stdoutChars=0 stderrChars=54`, `/shell` dan `dumpsys meminfo` tidak jalan). Kembali ke polling `exitValue()` dengan jeda 10 -> 250 ms.
  - Pencatat Diagnostik tidak lagi merekam sentuhan/koordinat, sampel morph per frame, COMPOSE_STATE, DRAW_LAYOUT, TOUCH_BOUNDS_*, STATE_OBSERVER, WINDOW_LAYOUT_UPDATE.
  - `build.gradle.kts`: release ditandatangani debug key, jadi `./gradlew assembleRelease` bisa dipasang. Debug build tidak di-minify/optimasi dan debuggable, jadi RAM-nya lebih tinggi; ukur RAM pakai release.
  - Low-End: artwork 224 px, tick dekoratif 50 ms.
  - Varian debug disamakan dengan release (minify, shrink, tidak debuggable) + `defaultTasks("assembleRelease")`, karena APK builder pengguna ("newly") kemungkinan menjalankan assembleDebug. Bila crash/error R8: tambah aturan keep di `proguard-rules.pro`.
  - Default model Gemini diubah ke `gemini-3.5-flash-lite` (`GEMINI_DEFAULT_MODEL`, preset Gemini, `GEMINI_CHAT_PRIORITY` Lite dulu); tes `ModelPickerTest` ditambah.
  - Lingkungan Claude TIDAK bisa membangun APK (tanpa Android SDK, jaringan ke Gradle/Google diblokir); `assembleRelease` harus dijalankan pengguna.
  - Terverifikasi dari log pengguna: `IDLE_TRIM reason=capsule renderer=true` muncul ~8 dtk setelah capsule diam (trim capsule stage110 berjalan).
  - Penjelasan RSS/PSS tinggi (analisis, bukan terukur): lihat bagian 5 butir 0.

## 4. Data perangkat
### meminfo Low-End (stage103)
- Total PSS 151 MB; Native Heap 48 MB (Bitmap hanya 2,4 MB, sisanya cache Skia/HWUI dan alokasi native lain); Dalvik 31 MB (Alloc 20,7 MB); Graphics 36,7 MB (GL mtrack 34 MB, EGL 2,5 MB); Activities 3, ViewRootImpl 6.
- Belum diketahui apakah panel AI/Composer terbuka saat dump diambil. Dump lama (panel AI terbuka): buffer grafis sekitar 33 MB: `AiComposerActivity` 648x1440, `DynamicIsland` 648x1166, `AndroidIslandTouch` 614x1058.
### batterystats (51 detik, Power Save aktif)
- Proses Island (UID u0a645): sekitar 9,6 s CPU dalam 51 s, tetapi 11,9 s di antaranya app terbuka di depan, jadi bukan angka idle. Tidak cukup untuk menyimpulkan boros baterai idle.

## 5. Pertanyaan dan pengukuran yang masih terbuka
0. **Kenapa PSS 151 MB padahal fiturnya sedikit (analisis dari dump + kode):** fitur app sendiri kecil (Bitmap total 2,4 MB). Sisanya biaya dasar proses: (a) build debug tidak di-minify dan tanpa optimasi, jadi seluruh Compose/Material3/ikon-extended/ML Kit dimuat apa adanya (Dalvik 31 MB); (b) Graphics ~37 MB = buffer window overlay (draw window 648x691 px selalu penuh, jadi 648x763 saat panel AI, plus window sentuh dan composer 648x1440), tiap window ber-buffer ganda/triple; (c) Native Heap 48 MB = cache Skia/HWUI + runtime Compose + library native; (d) PSS menghitung porsi library bersama (libhwui, libart, font sistem). Window penuh tidak bisa dikecilkan karena dulu bikin stutter (`windowShrinkAllowed=false`). Langkah ukur yang jujur: pasang `assembleRelease`, tunggu capsule diam 10 dtk, `dumpsys meminfo com.android.island` (kini bisa lewat /shell).
0b. Uji stage111: `/shell` dan perintah Shizuku kembali berjalan (mis. `dumpsys meminfo com.android.island`); Low-End: bar capsule dan cover masih tampak baik.
0c. Uji trim capsule stage110: aktifkan Diagnostik sebentar, biarkan capsule diam ~10 dtk, cari log `IDLE_TRIM reason=capsule renderer=true`; matikan Diagnostik lalu ukur meminfo (bandingkan dengan Total PSS 151 MB, Native Heap 48 MB).
1. RAM Low-End masih di atas 100 MB. Butuh tiga dump pada kondisi sama, dengan Diagnostik MATI: (a) hanya capsule tampil, (b) panel AI terbuka, (c) Settings terbuka. Perintah: `dumpsys meminfo com.android.island -d` dan `dumpsys gfxinfo com.android.island`. Tunggu sekitar 5 detik setelah capsule agar trim idle berjalan. Tanpa ini, sumber 34 MB GL mtrack dan 47 MB native heap belum bisa dipastikan.
2. Baterai idle: `dumpsys batterystats --reset`, layar menyala 30 menit tanpa disentuh (sekali dengan musik, sekali tanpa), lalu `dumpsys batterystats com.android.island`.
3. Capsule/banner setelah idle lama: kalau masih gagal, kirim log `LISTENER_REBIND`, `MEDIA_SESSION_SELECTED`, `WA_ALERT`, `WA_FILTERED` (aktifkan Diagnostik dulu).
4. Panel AI lewat tombol power: cek Pengaturan > Aplikasi default > Asisten digital dan pemetaan tombol samping; log `ASSIST_SESSION_SHOW` menunjukkan event sampai ke app atau tidak.
5. CPU dan jaringan tinggi: `top -H -p <pid>` saat idle dan saat musik diputar.
6. Uji stage110 (hati/repeat tanpa polling): ubah like di app pemutar (Spotify/YouTube Music) saat island terbuka, lihat apakah hati di island ikut berubah; ubah repeat di app pemutar dan di island. Bila hati telat/tidak berubah, kirim kabar (kemungkinan perlu burst tambahan setelah aksi pengguna).
7. Uji stage110: buka Telegram/Gmail dari ikon biasa; kartu notifikasi app itu harus hilang.
8. Uji stage110: perintah "buka Spotify cari X lalu putar" (`PlayInApp`) harus berhenti sendiri begitu lagu diputar, dan bar capsule berhenti saat layar terkunci.
9. Uji stage108: tap "Baca Sumber" dan "buka browser" langsung ke Chrome (cek Pengaturan > Aplikasi default > Aplikasi browser = Chrome); (a) channel senyap tidak muncul, yang bergetar muncul; (b) tap banner membuka app pengirim; (c) pilih teks per kata di chat AI memunculkan toolbar salin di overlay (belum terbukti di window overlay); (d) jawaban panjang tersambung mulus; (e) waktu "berita hari ini" dibanding sebelumnya.
10. Uji stage109: indikator polos dan tidak bisa ditekan; tutup panel lagu mulus; panel AI bertahan saat notifikasi masuk; agent WhatsApp berhenti di dalam percakapan.
11. Melepas riwayat chat AI dari memori saat panel ditutup (hemat kecil, perlu menyentuh alur simpan dan muat riwayat).
12. Hapus parameter `breath` mati di `CompactIsland`/`BreathDot` (semua pemanggil di `IslandRoot` mengirim `null`).

## 6. Langkah berikutnya yang disarankan
1. Kompilasi stage111: `./gradlew assembleDebug` dan `./gradlew testDebugUnitTest`; kirim error bila ada. Risiko terbesar: perubahan stage109 di IslandRoot/ExpandedIsland setelah stack dibuang, `AiSession.agentBusy`, API `LinkAnnotation`/`withLink` (Compose BOM 2024.10.01), `Ranking.channel`, konstruktor `ThreadPoolExecutor` dengan lambda ThreadFactory; dan perubahan stage110 yang kecil: `MediaControllerCompat.Callback` di `Controls`, `Process.waitFor(timeout, unit)` di `ShizukuShell`, `ScreenState.awaitVisible()`.
2. Pasang, restart paksa, lalu jalankan uji butir 6 sampai 10 di bagian 5.
3. Kirim tiga dump meminfo/gfxinfo (capsule saja, panel AI, Settings) dengan Diagnostik dimatikan.
4. Lanjutkan inisiatif perbaikan: cari fitur/pengaturan lain yang sudah tidak berfungsi dan bersihkan; bersihkan `breath` mati; pertimbangkan melepas riwayat chat AI saat panel ditutup.
5. Di akhir sesi, perbarui file ini sesuai bagian 0 butir 5.
