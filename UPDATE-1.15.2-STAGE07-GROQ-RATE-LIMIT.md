# Android Island — Tahap 7: Groq HTTP 429 dan jalur cadangan

## Perubahan
- Waktu tunggu yang diberikan provider kini dihormati hingga 60 detik.
- Retry otomatis diizinkan untuk pembatasan sementara dengan waktu tunggu sampai 30 detik.
- Kuota harian dan waktu tunggu lebih dari 30 detik tidak dipaksa di-retry berulang.
- Kunci yang menerima HTTP 429 masuk masa cooldown sesuai petunjuk provider; jika tidak ada petunjuk, cooldown default 5 detik.
- Jika seluruh kunci sedang cooldown, key ring tidak mengirim permintaan ulang ke kunci yang sama sebelum waktunya.
- Profil provider cadangan yang juga mengalami 401/403/429 ikut masuk cooldown sehingga tidak dipanggil berulang pada setiap putaran.
- Pesan status 429 disederhanakan untuk pengguna.
- Pengaturan provider cadangan menjelaskan bahwa kunci/API cadangan harus disediakan pengguna; aplikasi tidak membuat kredensial sendiri.

## Batasan
- Provider cadangan lintas layanan hanya dapat dipakai jika pengguna telah mengisinya dengan provider, URL, model, dan API key yang benar.
- Tidak ada API key cadangan yang dibuat otomatis.
- Pengujian koneksi nyata ke akun Groq/provider dan pengujian pada perangkat belum dilakukan.
- Build Gradle dan unit test proyek penuh belum dapat dijalankan karena wrapper tidak dapat mengakses `services.gradle.org` (DNS/UnknownHostException).

## Verifikasi yang dilakukan
- Smoke test Kotlin untuk parsing waktu tunggu 18.668 detik, batas harian, batas 45.36 detik, cooldown tanpa petunjuk, dan rotasi kunci: LULUS.
