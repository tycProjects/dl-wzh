# Android Island 1.4.73

## Liquid glass statis seperti referensi player
- Latar album/foto profil kini kaca cair STATIS: art buram (64 px, ~16 KB) dibelokkan gelombang cair tetap (mesh 24x12, drawBitmapMesh) + lensa cembung di tengah-kanan.
- Tidak ada lagi drift, blob animasi, atau jam 30 Hz di latar. Satu-satunya gerak: cross-fade saat art berganti.
- Cover, teks, dan tombol tetap di layer atas sehingga tidak ikut terdistorsi (pemisahan layer/kedalaman).
- Tepi kaca: tambah inner glow putih tipis di dalam rim (LiquidGlass.kt), melengkapi rim, glint, dan edge lens yang sudah ada.
- Parameter baru opsional `waveStrength` (default 1f; 0 = buram datar) di FluidGlassBackdrop. Pemanggil lama tidak perlu diubah.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error. Jika distorsi terasa terlalu kuat/lemah, ubah `waveStrength` atau konstanta 0.035 / 0.070 / 0.08 di buildWaveMesh.

## Revisi: tepi lensa kaca ditipiskan
- Band refraksi (LiquidLens) dari 14-30 dp menjadi 8-13 dp, pergeseran lebih kecil: tepi tidak lagi tampak meleleh pada Lensa tepi 100%.
- Garis lensa dan inner glow (LiquidGlass) dipersempit dan diredupkan: tidak lagi membentuk bingkai pucat tebal.

## Revisi 1.4.75: kecerahan & rim mendekati widget referensi
- Lapisan hitam di atas art dikurangi (kartu media 0.80 -> 0.90, banner WhatsApp 0.85 -> 0.90) dan ditambah frost veil putih tipis, supaya kaca terasa tebal-berkabut, bukan gelap.
- Gelombang refraksi sedikit lebih kuat (waveStrength default 1.15).
- Rim putih dan inner glow dipertegas lagi (di antara 1.4.73 yang terlalu tebal dan 1.4.74 yang terlalu tipis).
