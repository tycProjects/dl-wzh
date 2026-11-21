# UPDATE 1.17.2 — STAGE 74 — ENAM BUG: NOTCH ASLI, SENTUHAN BEBAS, ROUTING TITIK, RAM, LIRIK, BANNER

Tanggal: 20 Nov 2026

Semua laporan dari sesi 16:54 (log internal + dumpsys meminfo + ekspor chat) diperbaiki di satu stage.

## 1. Notch iPhone — bentuk asli seperti foto, dan tetap bisa disentuh

- Siluet `IPhoneNotchCapsuleShape` digambar ulang mengikuti foto referensi (iPhone asli):
  tepi atas rata penuh, sisi nyaris lurus turun dengan lekuk halus ke dalam (bukan meruncing
  segitiga seperti versi stage 72), dua sudut bawah membulat lebar, dan tepi bawah lurus
  sekitar 80 persen lebar atas.
- Profil kapsul "iPhone" kini 200x30 dp (proporsi notch asli sekitar 7:1, lebar dan pendek),
  sebelumnya 130x32. Semua sisi (gambar, jendela, batas sentuh) membaca ukuran dari satu
  fungsi yang sama, jadi tidak ada sisi yang meleset.
- Notch saat island menganggur (HIDDEN) kini punya aksi ketuk: menyummon kartu asisten AI.
  Sebelumnya kotak notch tidak punya handler sama sekali, ketukannya hilang begitu saja.
  Kapsul compact tetap punya tap/dobel tap/tekan lama/swipe seperti sebelumnya.

## 2. Panel AI yang tak terlihat tidak boleh memblokir sentuhan sistem

- Akar masalahnya: selama AI sedang menjawab (aiTyping), ketukan di luar island sengaja
  diabaikan supaya panel tidak menutup. Efek sampingnya: panel besar yang sudah tidak
  dipakai tetap terbuka menutupi layar dan menelan sentuhan (log memperlihatkan banjir
  OUTSIDE_TOUCH aiTyping=true saat pengguna marah-marah mengetuk).
- Sekarang hanya dua kondisi yang menahan penutupan: editor balasan WhatsApp terbuka
  (replyOpen) dan jendela eksternal AI (kolom ketik komposer / pemilih file) sedang tampil.
  AI yang sekadar sedang menjawab tidak lagi menahan: ketukan di luar menutup panel,
  sentuhan sistem langsung bebas. Percakapan tetap aman di sesi, jawaban muncul di
  riwayat saat panel dibuka lagi.

## 3. Chat dan agen dipisah tegas — titik adalah satu-satunya penanda agen

- Tanpa titik, SEMUA pesan adalah chat biasa. Heuristik lama yang menerka "buka tiktok"
  sebagai perintah agen dihapus; pengguna tidak akan melihat panel menjalankan aksi HP
  secara tiba-tiba dari obrolan biasa.
- ". buka tiktok", ".buka tiktok", ".. cari preset di tiktok lalu enter" semuanya masuk agen.
- Fallback aneh dihapus dari prompt chat. Aturan lama menyuruh model menjawab satu kalimat
  "perintah perangkat dijalankan oleh mode agen..." — itulah loop pembatalan yang membuat
  pengguna marah di ekspor chat. Aturan baru: pertanyaan biasa SELALU dijawab isinya;
  hanya bila pengguna jelas minta aksi HP, jawab ringkas sekali dan contohkan awalan titik
  (". buka tiktok"), tanpa mengulang-ulang penolakan.
- Tanda bintang dihapus dari jawaban: prompt (chat dan agen) melarang ** dan *, dan lapisan
  pembersih baru menghapus **tebal**, miring, serta bullet bintang dari jawaban sebelum
  ditampilkan — hanya di luar blok kode, jadi file unduhan tidak pernah berubah isinya.
  Kasus seperti "2 * 3", "3*4=12", "5*10**2" dibiarkan utuh.
- Ketekunan agen dari stage 72 (arah ulang ke tahap berikutnya, laporan kemajuan, strategi
  ikon pencarian per aplikasi, search_in_app deterministik) tetap dipakai apa adanya.

## 4. RAM — jendela gambar tidak lagi memenuhi layar saat panel AI terbuka

- Data: TOTAL PSS 146 MB, GL mtrack 52 MB + EGL 10 MB. Selama panel AI terbuka, jendela
  gambar sebenarnya 648x1440 px (layar penuh) — konfirmasi di log WINDOW_LAYOUT_UPDATE.
  Penyebabnya: cap panel 0,78 layar ditambah cadangan penuh crest jelly (sampai 22 persen
  dari tinggi panel di elastisitas HIGH) di atasnya, sehingga total melewati tinggi layar.
- Perbaikan dua arah di WindowTuning (satu sumber untuk jendela gambar, host IslandRoot,
  dan jendela sentuh): cap panel 0,78 menjadi 0,74 layar, dan saat panel AI terbuka
  cadangan crest jelly dibatasi 16 dp. IslandRoot sudah menjepit amplitudo jelly ke ruang
  yang benar-benar ada, jadi pantulan tetap terlihat dan tidak ada yang terpotong datar.
- Hasil: jendela saat panel terbuka turun dari 1440 px ke sekitar 1150-1200 px (hemat
  sekitar seperempat permukaan grafis terbesar), tanpa menyentuh fitur premium.
- Java heap tetap sehat (alloc 14,9 MB / free 14,9 MB) dan bitmap kecil (303 KB) —
  tumpukan bitmap tier dari stage 69/71 terbukti bekerja; OCR ML Kit tetap dilepas setelah
  60 detik diam. Idle tetap jendela pendek 547 px.

## 5. Penyedia lirik diperkaya + hanya timestamp valid dari awal sampai akhir lagu

- Penyedia baru tanpa API key: NetEase Cloud Music (music.163.com/api/search/pc untuk
  pencarian, /api/song/lyric untuk LRC tersinkron). Pencocokan identitas ketat (judul,
  artis, selisih durasi maksimal 12 detik) sebelum dipakai.
- Terverifikasi lewat dokumentasi resmi: LRCLIB (lrclib.net) gratis, tanpa kunci, akses
  anonim cukup untuk penggunaan normal — kunci registrasi hanya untuk limit lebih tinggi
  dan memang tidak wajib, jadi tidak ada kunci yang perlu disematkan. BetterLyrics: cache
  hit memang publik tanpa kunci; cache miss butuh kunci yang tidak dirilis publik, jadi
  tetap sebagai penyedia best-effort seperti sebelumnya.
- Pintu kualitas baru "cakupan penuh": bila durasi lagu diketahui, lirik yang mulai setelah
  40 persen lagu, atau berhenti sebelum 45 persen lagu, DITOLAK (penalti skor di validator
  + gerbang keras di hasil LRC semua penyedia). Lirik parsial/salah tag tidak akan muncul
  lagi di layar; lirik yang lolos tetap memenuhi syarat timestamp monoton dan skor minimal.

## 6. Banner pesan dan indikator tidak boleh mati saat lagu diputar

- Akar masalahnya di filter stage 72: pesan WhatsApp dari kanal senyap (chat yang di-bisukan
  atau build yang tidak menyertakan CATEGORY_MESSAGE) dibuang — banner tidak muncul dan
  indikator hijau tidak muncul, karena keduanya bergantung pada pesan yang lolos.
- Sekarang semua notifikasi berkategori pesan/e-mail/sosial ATAU membawa payload
  MessagingStyle (cara WhatsApp mengirim notifikasi chat) SELALU ditampilkan, seberapa pun
  tingkat kepentingan kanalnya. Yang tetap disaring: notifikasi panggilan tidak pernah
  disaring (tetap), dan chatter non-pesan di bawah HEADS_UP (sinkronisasi, layanan, promo)
  tetap dibuang — kini dengan log WA_FILTERED supaya mudah didiagnosis.
- Alur musik diputar: banner pesan muncul di bawah kapsul, setelah waktu tampil habis
  kapsul kembali ke lagu dengan indikator hijau bertahan — logika bus existing untuk jalur
  ini sudah benar dan kini benar-benar mendapat pesan lagi.

## Verifikasi

- Audit import seluruh file Kotlin dijalankan ulang (audit_imports.py): 31 temuan, semua
  false-positive yang sudah diverifikasi sejak stage 68-73 (argumen bernama alpha/clip,
  isi string prompt). Tidak ada missing import baru pada 11 file yang diubah.
- Pemeriksaan keseimbangan kurung pada 11 file yang diubah: OK.
- Semantik regex pembersih bintang diuji dengan 10 kasus (tebal, miring, bullet, perkalian,
  eksponen, blok kode tetap utuh).
