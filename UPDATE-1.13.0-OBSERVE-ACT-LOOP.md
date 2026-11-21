# Update 1.13.0 — Loop Observasi–Aksi untuk Asisten AI

Desain: docs/superpowers/specs/2026-11-17-ai-observe-act-loop-design.md
Rencana: docs/superpowers/plans/2026-11-17-ai-observe-act-loop.md

## Yang berubah
- Asisten membaca layar setelah setiap langkah (maks 3 aksi per langkah), lalu memutuskan langkah berikutnya.
- Berhenti saat `done`, tanpa aksi, batas 8 langkah, batas 90 detik, atau aksi yang sama gagal dua kali.
- Tombol berisiko (hapus, bayar, beli, uninstall, reset, kirim) hanya diketuk jika pesan pengguna menyebut kata itu.
- Kolom kata sandi yang sedang fokus tidak pernah diisi asisten.
- Setiap langkah tercatat di chat dan di IslandDiagnostics (`AI_LOOP_STEP`).
- Jika berhenti karena batas, jawaban menyebut jumlah langkah yang sudah selesai.

## Status verifikasi
Test unit ditulis lebih dulu (AgentLoopPolicyTest, ClickGuardTest, ScreenSummaryTest, AiActionProtocolTest). Sesi ini tidak punya Kotlin compiler, Gradle, Android SDK, maupun jaringan, sehingga test belum dijalankan. Jalankan `./gradlew testDebugUnitTest` dan `./gradlew assembleDebug` sebelum rilis.

## Keputusan (rulings)
- Ruling: prompt diberi field `done` agar loop tahu kapan selesai — desain tidak menyebut mekanisme ini secara eksplisit — biaya jika salah: model yang tidak mengisi `done` membuat loop berhenti lewat "tanpa aksi" atau berjalan sampai batas.
- Ruling: "Membaca layar..." ditampilkan lewat status, bukan pesan chat per langkah — menghindari chat penuh — biaya: langkah tidak terlihat satu per satu di chat selain log hasil.
- Ruling: `runActionRounds` digantikan seluruhnya — tidak ada jalur lama yang tersisa — biaya: perilaku pencarian web lama ikut berubah ke loop baru.
- Ruling: Stop dicek di awal setiap langkah. Jeda di dalam aksi (scroll, tunggu, pencarian aplikasi) tidak dibatalkan — biaya: Stop bisa tertunda beberapa detik.
- Ruling: kegagalan dikenali dari pola teks hasil (mis. "Tidak ", "Gagal") — biaya: hasil yang berbeda bunyi bisa tidak terdeteksi sebagai gagal, sehingga loop berulang sampai batas langkah.
- Ruling: layar kosong tidak menghentikan loop sendiri — biaya: model bisa mengulang sampai batas langkah jika layar tidak terbaca.

## Deferred minors
- Pengujian integrasi loop di perangkat (AgentLoopInstrumentedTest) belum ditulis; hanya logika murninya yang punya test.
