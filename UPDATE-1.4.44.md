# Android Island 1.4.44

## Build fix
- `staggerIn` menerima `Animatable<Float, AnimationVector1D>` (Animatable bukan `State`); memperbaiki 5 error type mismatch di ExpandedIsland.kt dan WhatsAppBanner.kt.

## Build fix (dari 1.4.43)
- `FluidGlass.kt`: hapus `import ...layout.matchParentSize` (anggota BoxScope, bukan fungsi top-level) yang membuat `compileReleaseKotlin` gagal.

## Latar buram + fluid glass (expand & banner WhatsApp)
- Foto album / foto profil sekarang jadi latar BURAM seperti referensi, di bawah lapisan kaca.
- Blur murah: bitmap dikecilkan bertahap ke ~36 px (beberapa KB) di thread Default, lalu digambar membesar dengan filter bilinear. Tanpa RenderEffect, tanpa offscreen render target; hasil di-cache untuk artwork yang sama.
- Fluid: foto drift/zoom pelan lewat GPU layer + 3 cahaya radial lembut. Brush dibuat sekali per ukuran, fase dibaca di fase draw (tanpa recomposition). Berhenti total di Mode hemat / Ultra.
- Warna teks/ikon adaptif: putih bila foto gelap, hitam bila terang (dihitung dari bitmap kecil tadi).
- Ganti foto = cross-fade 420 ms.

## Animasi detail kecil
- Expand: masuk bertahap (header → progres → tombol), pop pada art saat ganti lagu, judul & lirik cross-fade, progres + thumb digambar halus (tanpa recomposition), tombol memantul saat ditekan, pop pada like, ikon play/pause cross-fade + bump, dorongan kecil pada prev/next.
- Banner: masuk bertahap, avatar pop, badge jumlah pesan pop, tombol memantul, panel balasan buka/tutup animasi.

## Model animasi buka-tutup baru: Opsi 3 · Jelly
- Lebar membuka lebih dulu, tinggi menyusul, sedikit memantul (squash & stretch). Overshoot dibatasi 3,5% agar tidak terpotong window.
- Di Mode hemat: siluet sama tanpa pantulan.
- Pilih di Pengaturan → Gaya animasi.

## Dihapus
- Slider "Blur artwork" / `blurRadius` (tidak terpakai), `glassSweepPauseMs`.

## Catatan
Belum dijalankan di perangkat dari sisi saya; kompilasi diverifikasi oleh build Anda.
