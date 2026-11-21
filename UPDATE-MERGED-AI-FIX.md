# Gabungan AI fix (dari androidisland-fixed-files + 1.11.6)

File AI dari fixes-only diterapkan di atas proyek 1.11.6:
AiClient, HomeScreen, UiKit, AiPanel, AiComposerActivity.

## Model (diverifikasi web, Nov 2026)
- Gemini: default `gemini-3.8-flash` (Flash stabil terbaru). `gemini-3.0-flash` dihapus karena tidak ada di changelog Gemini API.
- OpenAI: `gpt-6.1-terra` (rilis API 29 Okt 2026). `gpt-4o-mini` dihapus.
- Groq: `openai/gpt-oss-120b`. `llama-3.3-70b-versatile` pensiun 16 Agu 2026.
- DeepSeek: `deepseek-v4-flash` (alias legacy `deepseek-chat` sudah dihentikan 24 Jul 2026).
- xAI: `grok-4.7`.
- Belum diverifikasi: Together, Fireworks, Cerebras, Mistral. Daftar model dari endpoint tetap menjadi sumber utama.

## Perintah lokal
- Input dibersihkan dari tanda baca di akhir dan kata sapaan (dong, tolong, ya, sih, pls...) sebelum dicocokkan.
- Perintah shell tidak diubah.
- Permintaan halus ("... dong") tetap menyalakan setting.

## Belum diverifikasi
- Tidak ada Android SDK di lingkungan ini, jadi build dan unit test belum dijalankan.

## Perintah suara (baru)
- `ai/voice/SpeechInput.kt`: pembungkus SpeechRecognizer bawaan Android (bahasa id-ID, hasil sementara ditampilkan saat bicara).
- Komposer AI: tombol "Bicara". Hasil akhir langsung dikirim seperti tombol Kirim, dan lewat router lokal dulu.
- Panel AI: chip "Bicara" membuka komposer dan langsung mendengarkan.
- Izin RECORD_AUDIO diminta saat pertama kali dipakai. Tambahan `<queries>` untuk RecognitionService.
- Label chip: "Tulis perintah" menjadi "Ketik perintah".
- Belum diuji di perangkat. Kualitas pengenalan bergantung pada layanan suara Google di perangkat.

## Pemahaman perintah (baru)
- `ai/IntentResolver.kt`: jika router kata kunci tidak mengenali perintah pendek (bukan pertanyaan), model diminta mengubahnya menjadi aksi JSON.
- Model hanya bisa mengembalikan aksi yang sudah ada (buka aplikasi, setting, media, volume, cari). Shell tidak pernah ditawarkan ke model.
- Gagal atau bukan perintah -> pesan diteruskan ke chat seperti biasa.
- Trade-off: perintah pendek menambah satu panggilan model sebelum jawaban. Pertanyaan dan pesan panjang tidak terpengaruh.

## Perbaikan build
- AiSession.kt: regex pembersih markdown memakai escape `\*` yang tidak valid di string Kotlin. Diganti `\\*`. Bug ini sudah ada di 1.11.6 asli, bukan dari perubahan baru.

## Chip panel AI (layout)
- Chip tidak lagi bisa digeser. Dua baris tetap: Ketik, Bicara, Layar | Lampiran, Hapus chat.
- Saat AI sedang bekerja, hanya tombol Hentikan yang tampil.
- Label diperpendek agar muat di layar.

## Diketahui, belum diperbaiki
- Panel menutup sendiri saat chip ditekan. Penyebab yang paling mungkin: `DisposableEffect` di AiPanel mengatur `aiTyping` ke false saat composable dibuang, sementara pindah ke komposer atau pemilih file membuat overlay kehilangan fokus. Belum bisa dipastikan tanpa log. IslandDiagnostics mencatat OUTSIDE_TOUCH.

## Penutupan panel saat chip ditekan (diperbaiki)
- Penyebab: saat komposer atau pemilih file dibuka, panel dibuang dan langsung menghapus status "sedang mengetik". Akibatnya sentuhan di luar dianggap menutup panel.
- Perbaikan: IslandBus punya penghitung aktivitas AI eksternal (beginExternalAi / endExternalAi). Panel tidak menghapus status selama penghitung > 0. Penghitung dikurangi di onDestroy komposer dan pemilih file, serta saat pembukaan gagal.

## Verifikasi model
- gpt-6.1-terra: dikonfirmasi di dokumentasi resmi OpenAI (tersedia di Chat Completions dan Responses).
- grok-4.7: dikonfirmasi di dokumentasi resmi xAI (tersedia di Chat Completions dan Responses).

## Versi
- versionName 1.11.7, versionCode 126.

## Belum diverifikasi
- Build Gradle belum dijalankan ulang setelah perubahan ini.
- Together, Fireworks, Cerebras, dan Mistral belum dicek.

## Screenshot untuk analisis layar
- Overlay (panel AI dan island) disembunyikan dulu (alpha 0, 180 ms), lalu screenshot diambil, lalu overlay dikembalikan. Pemulihan dijalankan di finally.
- Teks konteks tetap diambil dari jendela aplikasi, bukan jendela AI.
- Belum diuji di perangkat: apakah 180 ms cukup agar frame tanpa overlay sudah tergambar.

## Perbaikan pengalaman pengguna
- Indikator "Memahami perintah…" muncul saat perintah pendek sedang dicek model. Status baru: UNDERSTANDING.
- Tombol Layar selalu tampil. Kalau fitur analisis layar belum aktif, tombol menjelaskan cara mengaktifkannya di chat, bukan diam saja.
- Setup di Pengaturan AI sekarang berupa checklist: penyedia, API key, model, dan Accessibility. Checklist hilang sendiri kalau semua sudah siap.

## Perbaikan terbaru
- UI chip: satu grid 3 kolom dengan lebar dan tinggi sama. Hentikan selebar penuh saat AI bekerja.
- Hentikan: membatalkan juga pemahaman perintah (intentJob), mengosongkan teks stream, dan mengembalikan status ke IDLE. Sebelumnya tidak bekerja sama sekali saat status UNDERSTANDING.
- Pemahaman perintah: batas waktu 3 detik. Pesan hanya dikirim ke model jika mengandung kata perangkat (nyalain, wifi, lagu, dll.) dan maksimal 8 kata. Sapaan dan pertanyaan langsung ke chat tanpa menunggu.
- Tutup panel (X): mereset flag mengetik, penghitung aktivitas eksternal, dan tinggi panel AI. Tujuannya agar kapsul musik dan banner kembali muncul. Belum terverifikasi di perangkat.

## Perbaikan suara (Bicara)
- Sebelumnya gagal diam-diam. Sekarang setiap jenis gagal punya pesan: suara tidak terdengar, butuh internet, izin mikrofon, atau pengenal sedang sibuk.
- Kalau pengenal dalam aplikasi tidak bisa jalan, dibuka pengenal suara sistem sebagai cadangan.
- Tombol "Mendengarkan…" bisa ditekan lagi untuk berhenti.
- Belum diuji di perangkat.

## Arsitektur AI-first (baru)
- Setiap pesan dikirim ke AI yang sudah diatur. Jawaban berupa JSON: reply untuk pengguna dan actions untuk perangkat.
- Aksi dijalankan berurutan oleh app. Setelah buka aplikasi, ada jeda 1,5 detik sebelum perintah media.
- Aksi divalidasi: hanya jenis yang sudah ada, tanpa shell.
- Router lokal hanya dipakai kalau AI belum diatur, atau kalau AI tidak bisa dihubungi (error jaringan).
- Respons tidak di-stream, karena harus lengkap sebelum bisa diparse.

## Daftar model Gemini
- Hanya model chat generasi 3.5 ke atas dan 4+. Model 2.x, 3.0-3.4, TTS, image, live, embedding dikeluarkan.

## Akses perangkat yang lebih luas
- open_url: buka link http/https (misal hasil pencarian Google).
- click: tekan teks yang terlihat di layar aplikasi aktif (lewat Accessibility).
- scroll: gulir ke atas atau bawah.
- back: tombol kembali.
- Pertanyaan berita/fakta: model memanggil search, lalu hasilnya dirangkum menjadi jawaban, bukan daftar tautan mentah.
- Panel AI disembunyikan selama komposer terbuka, supaya tidak bertumpuk dengan keyboard.
- Belum diuji di perangkat. Ringkasan hanya sebagus cuplikan hasil pencarian. Untuk berita, membuka halaman lengkap belum dilakukan.

## Putar lagu tertentu
- Aksi baru play_song memakai intent MEDIA_PLAY_FROM_SEARCH, yang didukung aplikasi musik seperti Spotify dan YouTube Music.
- Kalau tidak ada aplikasi yang menangani, lagu dicari di YouTube Music lewat browser.
- Manifest: ditambah entri queries untuk intent tersebut (Android 11+).
- Belum diuji di perangkat. Apakah lagu langsung diputar atau hanya dicari tergantung aplikasi musiknya.

## Perluasan perintah dan akses perangkat
- Tombol sistem: home, recents, notifikasi, quick settings, kunci layar, screenshot, menu power.
- Timer dan alarm (langsung dibuat, tanpa membuka layar).
- Telepon dan SMS: hanya membuka dialer atau aplikasi pesan dengan data terisi. Tombol panggil atau kirim tetap ditekan pengguna.
- Navigasi ke alamat atau tempat (Google Maps, lalu peta umum sebagai cadangan).
- Kamera foto atau video.
- Volume media dalam persen.
- Salin ke clipboard, bagikan teks.
- Ketik teks ke kolom yang sedang aktif (mengganti isi kolom).
- Buka halaman pengaturan tertentu.
- Dari sebelumnya: buka aplikasi, link, klik teks, scroll, kembali, cari, putar lagu, media, setting, volume.
- Belum dikompilasi atau diuji di perangkat.

## Akses perangkat: daftar aplikasi dan status media
- list_apps: membaca daftar aplikasi yang bisa dibuka (QUERY_ALL_PACKAGES, untuk pemakaian sideload). Hasilnya dikembalikan ke model untuk dijawab.
- media_status: membaca judul dan artis yang sedang diputar. Juga dikembalikan ke model.
- Prompt: model diberi tahu bahwa aksesnya sudah lengkap lewat aksi, dan dilarang meminta izin atau hak sistem tambahan.
- Tidak ada akses shell tanpa batas untuk AI.

## Loop aksi multi-putaran (baru)
- Hasil aksi yang berupa data (search, read_screen, current_app, list_apps, media_status, search_in_app) dikirim kembali ke model.
- Model boleh menjawab atau mengambil langkah lanjutan. Maksimal 4 putaran (MAX_ROUNDS).
- Aksi biasa (buka aplikasi, klik, timer, dan lainnya) tetap dijalankan langsung.

## Berita: isi halaman dibaca
- WebSearch.deepSearch: ambil 3 hasil teratas dari DuckDuckGo Lite, lalu baca paragraf halaman (PageReader) sebagai bahan jawaban.
- Prompt: tanggal hari ini (17 November 2026) dimasukkan, dan model diminta meringkas peristiwa, bukan daftar situs.

## Musik dan aplikasi lain
- search_in_app: buka aplikasi, tekan kolom cari (Cari/Search/Telusuri), ketik kata kunci, tekan enter, lalu kembalikan teks layar. Model memilih judul lalu menekannya dengan click.
- read_screen: teks layar aplikasi di depan.
- current_app: nama aplikasi yang sedang aktif (bukan daftar proses).

## Catatan
- Kolom teks yang dicari juga bisa dipilih otomatis (findEditable) jika fokus tidak ada.
- Belum dikompilasi dan belum diuji di perangkat. Pembacaan halaman bergantung pada situs; situs yang memakai JavaScript tidak akan terbaca.

## Pencarian di aplikasi musik (diperbaiki)
- Untuk Spotify dan YouTube/YouTube Music, pencarian dibuka langsung lewat tautan pencarian resmi aplikasi. Tidak perlu mengetuk kolom cari.
- Untuk aplikasi lain, search_in_app tetap mengetuk tombol Cari. Kalau teks Cari terlihat tapi tidak bisa diklik, aplikasi sekarang mengetuk posisinya di layar (gesture).
- Prompt: model tidak boleh menyuruh kamu mengetuk sendiri kalau aksi bisa melakukannya.
- Belum diuji di perangkat.

## Optimasi memori (audit singkat)
- ShizukuShell.exec dan ShizukuRestart: proses shell sekarang dikuras dan di-waitFor di thread latar (ShizukuShell.drain). Sebelumnya pipa dan proses zombie tertahan setiap perintah.
- Screenshot untuk AI: salinan disimpan RGB_565 (separuh memori ARGB_8888). Mode low-end memakai sisi terpanjang 720 px (sebelumnya 1024 px).
- Sudah baik sebelumnya, tidak diubah: artwork media dikecilkan dan di-cache, lirik dibatasi di mode low-end, diagnostik memakai buffer tetap 600 entri, service accessibility membatalkan scope dan overlay saat onDestroy.
- Belum dibuktikan: kebocoran sebenarnya. Perlu profiler (Android Studio Memory Profiler atau LeakCanary) dengan heap dump.
- Belum dikerjakan: efek kaca (blur, shader) di mode low-end. Perlu data profiler dulu agar tidak asal memangkas tampilan.
- Belum dikompilasi.
