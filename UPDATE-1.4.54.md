# Android Island 1.4.54

## Perbaikan kotak putih samar saat banner di-dismiss
Penyebab (ada tiga, semuanya sudah diperbaiki):
- Setelah swipe-up, salinan banner "hantu" diputar ulang dari posisi awal (alpha 1 -> 0). Sekarang banner yang sudah beranimasi sendiri tidak diputar ulang (`BannerHold.skipExit`).
- Urutan `clip` -> `graphicsLayer` terbalik: isi kartu dipotong oleh batas ASLI kartu saat bergeser, lalu offset di-reset ke 0 sehingga kartu penuh sempat muncul kembali 1-2 frame. Kini transform dulu, clip kemudian, dan offset tidak di-reset sampai kartu benar-benar hilang.
- `clickable` memakai ripple bawaan yang menggambar persegi pucat di atas kartu begitu swipe dimulai. Ripple dimatikan (`indication = null`).
- Saat memudar, kartu kini di-flatten (`CompositingStrategy.Offscreen`) agar kaca, foto, dan teks tidak saling tembus dan terlihat seperti kotak pucat.
- Salinan ghost untuk timeout kini non-interaktif, tanpa pop avatar, tanpa drift.

## Animasi banner -> capsule (swipe-up & timeout) lebih halus
- Swipe-up kini mengikuti jari (sebelumnya hanya menunggu jari dilepas), turun ke bawah ditahan karet, dan fling cepat ikut dihitung.
- Setelah dilepas banner "tersedot" ke capsule: naik, mengecil, lalu memudar (460 ms, kurva lembut, bukan 300 ms yang terpotong). Jalur timeout memakai gerak dan kurva yang sama persis.
- Jika tidak ada yang mengambil alih state, banner dikembalikan dengan pegas, bukan dibiarkan tak terlihat.
- Banner kini fade/scale-in saat muncul; geser horizontal memudar mengikuti jarak.

## Jelly lebih kenyal
- Penyebab kaku: overshoot pegas dipotong rata di 1.035. Sekarang overshoot nyata dirender sebagai squash & stretch yang menjaga volume (tinggi -> sempit, pendek -> lebar), dengan ruang 18dp di bawah panel agar tidak terpotong jendela.
- Damping jelly diturunkan (HIGH 0.56 -> 0.50, dst) dan stiffness sedikit dinaikkan agar tetap lincah.
- Tombol: press-scale lebih jelly (squash vertikal lebih kuat, rebound lebih longgar). Entrance staggered punya sedikit overshoot. Pegas swipe-dismiss kartu expanded dilonggarkan.

## Expand -> capsule lebih lambat dan mulus (Flow & Jelly)
- Penutupan memakai pegas hampir-kritis (0.92) menggantikan tween 250 ms: kontinu terhadap kecepatan saat diketuk ulang, deselerasi lebih panjang, undershoot < 0.1%. Opsi 1 tidak berubah.

## Liquid glass
- Rim sudut menyala (sweep gradient: terang kiri-atas & kanan-bawah) di atas rim linear lama.
- Garis glint tipis di bawah tepi atas (bevel specular) seperti pada widget referensi.
- Saturasi foto buram 1.3x -> 1.55x.
- Semua statis (digambar sekali per ukuran), nol biaya per frame.

Catatan: kode belum dikompilasi di lingkungan ini (tanpa Gradle/jaringan). Jalankan build debug dan kabari bila ada error kompilasi.
