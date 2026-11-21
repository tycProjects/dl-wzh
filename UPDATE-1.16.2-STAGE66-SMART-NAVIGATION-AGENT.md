# Stage 66 — Agen navigasi cerdas

## Masalah dari ekspor chat
1. "Cari di google ..." dan "Buka whatsapp lalu buka kontak ..." tanpa awalan ". " dianggap chat biasa, sehingga model menulis skrip Python "simulasi" dan HP tidak disentuh.
2. Guard niat menolak `type "Ummi Faris"` pada perintah "buka kontak", lalu seluruh tugas berhenti.
3. Agen tidak kembali ke halaman utama aplikasi sebelum mencari ikon/kolom cari.

## Perubahan
- AgentIntentClassifier + AgentCommandPrefix: perintah HP yang jelas masuk mode agen otomatis. ". " tetap memaksa agen; "/chat " memaksa chat biasa. Pertanyaan, penjelasan, permintaan kode/tulisan tetap chat.
- Prompt chat biasa melarang skrip "simulasi" untuk tugas HP.
- AiCommandRouter: "cari di google X" / "carikan X di youtube" -> SearchInApp satu langkah, dijalankan langsung (jalur cepat); Google membuka halaman hasil sungguhan.
- SearchControlMatcher: pengenalan ikon/kolom cari lebih luas (Cari, Search, Telusuri, Pencarian, hint, view id), menolak chip "Pencarian ·", suara, lensa, riwayat.
- SearchNavigationPolicy + DeviceActions.reachSearch/appHome + aksi baru `app_home`: pakai pencarian bila terlihat; kalau tidak, tab Beranda, lalu Back (maks 4), baru buka ulang aplikasi.
- ScreenStateAnalyzer: setiap observasi diberi "Petunjuk navigasi" (halaman utama atau halaman dalam).
- Aksesibilitas: tap gesture cadangan bila ACTION_CLICK gagal, pencarian di bilah bawah, klik label awalan unik, kolom chat tidak pernah dianggap kolom cari.
- UserIntentActionGuard: teks yang diketik boleh bila berasal dari kata pengguna; alias Google/Chrome; penolakan guard kini diulang ke model (maks 2x) alih-alih menghentikan tugas. Perbaikan test lama (OpenApp pada "Jelaskan fungsi YouTube").
- Prompt agen: NAVIGATION PLAYBOOK dan field `thought`.

## Verifikasi
Logika murni dikompilasi dengan kotlinc 2.0.21 dan 68 unit test lulus di luar Gradle. Kode yang menyentuh Android (service aksesibilitas, DeviceActions, AiSession, ScreenReader) belum dikompilasi dan belum diuji di perangkat: jalankan `./gradlew testDebugUnitTest assembleDebug`.

## Revisi: Enter nyata, sentuhan manusia, dan kejujuran
Dari ekspor chat kedua: "Enter ditekan lewat Accessibility IME_ENTER" dilaporkan berhasil padahal TikTok mengabaikannya dan daftar saran tetap tampil.
- DeviceActions.submitEnter: urutan rute Shizuku dulu (keyevent ENTER nyata, ketukan jari pada tombol aksi keyboard, keyevent SEARCH), baru Accessibility. Sebuah rute dianggap berhasil hanya bila layar berubah (SubmitVerifier). Kalau tidak, laporannya "belum terbukti" dan rute berikutnya dicoba.
- type di kolom cari, press_enter, dan pencarian otomatis memakai jalur yang sama dan tidak lagi mengaku "Enter ditekan" tanpa bukti.
- HumanInput: tap dengan titik bergeser kecil dan waktu tahan 55-120 ms, scroll berupa swipe miring berkecepatan manusia. tapXY/click_index dan scroll memakai Shizuku lebih dulu (sentuhan nyata), Accessibility sebagai cadangan.
- "belum terbukti" dan "layar tidak berubah" kini dihitung sebagai kegagalan oleh loop agen; prompt agen punya aturan KEJUJURAN.
