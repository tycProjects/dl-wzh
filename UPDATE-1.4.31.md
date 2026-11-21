# Android Island 1.4.31

## Perbaikan
- System-dismiss WhatsApp kini memberi exit fade singkat sebelum model notifikasi dibuang; cleanup fallback tetap membersihkan item antrean yang sedang tidak tampil.
- Compose overlay view diberi background transparan eksplisit untuk mencegah kilatan bidang putih saat ukuran window berubah.
- Gesture compact memakai callback terbaru melalui `rememberUpdatedState` dan pointer-input keys yang mencakup konfigurasi terkait.
- Opsi tap, double-tap, dan long-press sekarang benar-benar mengikuti pengaturan, termasuk membuka aplikasi pemutar/WhatsApp untuk aksi `OPEN_APP`.
- Bentuk iPhone notch tidak lagi mematikan gesture horizontal secara diam-diam.
- Glass Sweep mendapat kontrol durasi lintasan (0,5–2,6 detik) dan jeda tengah (0–1,2 detik); lintasan dibagi dua segmen linear untuk berhenti di tengah lalu lanjut.

## Validasi
- ZIP integrity dan pemeriksaan statis sumber dilakukan.
- Kotlin compile belum dapat diverifikasi di lingkungan ini karena Gradle Wrapper memerlukan `services.gradle.org`, yang tidak dapat di-resolve (`UnknownHostException`).
- Pengujian perangkat nyata tetap diperlukan untuk memastikan artefak visual hilang pada perangkat pengguna.
