# Stage 50 — Percepatan kelanjutan Looping Agent

## Perubahan
- Parser tugas pencarian aplikasi menerima penghubung bahasa Indonesia yang lebih alami: `lalu`, `kemudian`, `terus`, `dan`, `setelah itu`, serta variasi tanda baca.
- Parser hanya menganggap permintaan memilih hasil pertama jika kalimat memang menyebut hasil/video pertama atau yang muncul.
- Setelah `search_in_app` berhasil dan pesan asli pengguna secara eksplisit meminta hasil pertama, agen langsung menjalankan satu tindakan `tap_first_result` yang sudah diizinkan oleh perintah asli. Agen tidak membuang satu putaran API hanya untuk memutuskan langkah yang sudah jelas diminta pengguna.
- Setelah tindakan tersebut, layar tetap diamati dan hasilnya diverifikasi oleh putaran agen berikutnya. Jika gagal, kegagalan disampaikan ke agen untuk pemulihan; tidak diklaim berhasil.

## Tujuan pengalaman pengguna
- Mengurangi waktu tunggu dan jumlah permintaan API untuk pola tugas pencarian yang jelas.
- Menghindari ketukan pada label umum `Cari` atau chip pencarian terkait.
- Mempertahankan pola amati → bertindak → amati dan batas pengulangan.

## Verifikasi
- Uji kompilasi terisolasi dan uji asap `AgentTaskParser` berhasil untuk dua bentuk perintah Indonesia.
- Uji negatif memastikan pencarian biasa tanpa permintaan hasil pertama tidak memicu ketukan otomatis.
- Integritas ZIP diperiksa.
- Build Android penuh dan uji perangkat belum dapat dijalankan karena Gradle 8.9 tidak dapat diunduh (`UnknownHostException: services.gradle.org`).
