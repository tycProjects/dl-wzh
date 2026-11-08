# Android Island 1.4.53

## Liquid glass (tetap statis = nol biaya per frame)
- Permukaan kaca kini berlapis: tint badan, band specular di atas, bayangan dalam di bawah, rim gradien (terang kiri-atas & kanan-bawah, redup di sisi), dan glow lensa tepi.
- Slider "Edge lens" sebelumnya tidak berfungsi; sekarang mengatur kekuatan glow lensa di tepi.
- Foto album/avatar buram diberi lonjakan saturasi 1.3x (GPU color matrix) agar warnanya hidup seperti referensi.
- Foto pertama yang selesai dibuat kini fade-in, bukan muncul tiba-tiba.

## Animasi
- Capsule -> expand: capsule memudar + mengecil halus di 40% awal; konten panel fade/scale/turun-halus setelah kaca terbuka.
- Expand -> capsule: konten memudar lebih dulu, capsule muncul kembali di 40% akhir. Penutupan memakai kurva tween cepat tanpa pantulan (tidak lagi undershoot yang memutus animasi di tengah).
- Capsule "bernapas" saat lagu/chat baru masuk; badge jumlah pesan pop; indikator play/pause cross-fade di slot tetap; waveform lebih halus (bar membulat, ~15 Hz).
- Lirik berganti dengan fade + geser vertikal. Artwork mengecil saat pause, membal kembali saat play. Tombol play/pause kini cakram putih (seperti referensi).

- Banner WhatsApp yang habis waktu (menciut ke indikator samping) kini memudar + naik halus ~220 ms, bukan hilang dalam satu frame. Banner yang di-dismiss atau dibuka tidak diputar ulang agar tidak muncul "hantu" di tengah.

## Fitur baru
- Geser (scrub) progres lagu: track menebal, thumb membesar, waktu mengikuti jari, seek saat dilepas. Tap-to-seek tetap ada.
- Geser ke atas pada kartu expanded untuk menutup.
- Getaran halus (setting baru "Getaran halus", default aktif).

## Perbaikan logika & efisiensi
- Kartu expanded tidak lagi stuck transparan setelah swipe-dismiss lalu lagu baru.
- Animasi "pop" play/pause tidak lagi terpicu saat panel baru dibuka.
- Tick posisi 4 Hz kini hanya me-recompose bar progres, bukan seluruh kartu.
- Drag vertikal di capsule direset saat dibatalkan.

Catatan: kode belum dikompilasi di lingkungan ini (tanpa Gradle/jaringan). Jalankan build debug dulu dan kabari bila ada error kompilasi.
