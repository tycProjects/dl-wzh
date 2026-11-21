# Stage 104
- Capsule: semua sesi media dipantau (bukan hanya yang terpilih). Callback audio ikut membaca ulang sesi bila island tidak menampilkan musik yang diputar. Event-driven, tanpa polling.
- Banner: semua notifikasi non-silent (importance >= DEFAULT) masuk, bukan hanya message-like/HIGH. Ongoing, ringkasan grup, dan notifikasi app sendiri tetap dibuang.
- Agent: hanya membaca perintah saat ini (tanpa riwayat chat lama); berhenti setelah PlaySong berhasil bila perintah tanpa langkah lanjutan.
- Belum dikompilasi dan belum diuji di perangkat.

# Stage 105
- Dibuang: slider "Blur artwork (Gaussian)". Blur sudah dihapus di stage87 dan parameter frostStrength tidak dipakai lagi di FluidGlass, jadi slider tidak berefek. Field pengaturan dibiarkan agar data lama tetap terbaca.

# Stage 106
- `util/NativeTrim.kt`: saat island diam (capsule saja, tersembunyi, layar mati) minta trim cache renderer (`HardwareRenderer.trimMemory(UI_HIDDEN)` lewat HiddenApiBypass) dan sesekali satu GC (maks tiap 5 menit). Overlay selalu terlihat bagi Android, jadi trim bawaan tidak pernah datang. Event-driven, rate limit 20 s.
- `ScreenOcr`: recognizer ML Kit dilepas setelah 10 s idle (sebelumnya 60 s).

# Stage 107
- Java heap: `NativeTrim` kini menjalankan GC lalu `VMRuntime.trimHeap()` di thread latar (maks tiap 5 menit, saat island diam), agar halaman heap yang kosong kembali ke OS.
- `IslandDiagnostics`: ekor RAM 2000 -> 500 baris, dan file log dibaca streaming (tidak lagi dimuat seluruhnya). Hanya berlaku saat Diagnostik aktif.

# Stage 108
- Dibuang: Browser Island (`BrowserActivity`, WebView in-app) beserta intent-filter https/http BROWSABLE di manifest. Link (`open_url`, "buka browser", tap "Baca Sumber") kini langsung ke Chrome/browser default perangkat.
- Notifikasi: hanya yang benar-benar bergetar yang masuk (banner, stack, indikator samping). Filter memakai `Ranking.channel.shouldVibrate()` dari listener (sebelumnya `NotificationManager.getNotificationChannel` hanya bisa membaca channel app sendiri, jadi filter lama selalu lolos untuk app lain). Importance < DEFAULT, filter interupsi (DND), dan mode ringer senyap ikut menolak. Tanpa channel: flag `DEFAULT_VIBRATE` / `vibrate`.
- Tap banner/kartu/stack membuka app pengirim (`IslandActivity.WhatsApp.packageName`), lewat `contentIntent`, fallback `getLaunchIntentForPackage`. Sebelumnya selalu jatuh ke WhatsApp. Membuka app hanya menghapus kartu app itu.
- Stack per app (`IslandUiState.stackItems()`): kartu dan hitungan `messageCount` hanya dari app yang sama dengan banner aktif; dedupe pesan kini memperhitungkan app. Menekan indikator membuka stack app dengan notifikasi terbaru.
- Indikator samping: ikon lonceng dengan gradasi ungu-cyan (bukan hijau WhatsApp). Aksen banner dan ikon latar kartu mengikuti app pengirim (`accentForPackage`, `appIconBitmap` dengan LRU 6).
- Semua app memakai logika banner WhatsApp lama: replay notifikasi < 120 s saat listener tersambung ulang kini berlaku untuk semua app; pemisahan "grup: pengirim" lewat tanda titik dua hanya untuk WhatsApp; avatar dari large icon, fallback ikon app.
- Berita: feed RSS diambil paralel (4 worker, batas 7 s total, timeout 4 s/5 s), hasil parse di-cache 90 s (bacaan ganda topik + ringkasan tidak lagi dua kali ke jaringan). Deskripsi berita dipotong di batas kata dengan "…".
- Jawaban AI tidak lagi terpotong: bila provider berhenti karena batas keluaran (`finish_reason=length` / Gemini `MAX_TOKENS`), `AiClient.chatCompletions` otomatis meminta lanjutan (maks 3 kali, bukan untuk JSON agent).
- Chat AI: teks pesan bisa dipilih per kata (SelectionContainer, tidak saat streaming). Tautan "Baca Sumber" memakai `LinkAnnotation`. Long-press seluruh bubble dihapus karena bentrok dengan seleksi; ikon salin tetap menyalin seluruh pesan.
- Pembersihan: dibuang `gaussianBoxRadii`, `blurAxis`, `buildBlurEffect`, `runLocalCommands`, `clearPending`, `cleanupAction`, `markWhatsAppSystemRemoving`, `exportLyricsFile`, `clearVerifiedAi`, `IslandDiagnostics.exportText`. Antrean notifikasi dibatasi 20 dan kartu > 12 jam dibuang. `NativeTrim` idle kini juga mengosongkan cache berita dan ikon app.
- Test baru: `IslandStackGroupingTest`.
- Belum dikompilasi dan belum diuji di perangkat.
