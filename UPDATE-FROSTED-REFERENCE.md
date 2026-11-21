# Update - Liquid glass frosted seperti referensi UI kit (expanded + banner)

Berlaku untuk expanded card, WhatsApp banner, dan call banner (ketiganya memakai liquidGlass + edgeSheenOverlay).

## Body kaca (LiquidGlass.kt)
- Substrat dibuat matte dan berkabut (grey beku), bukan kaca bening.
- Intensitas diambil dari whiteOpacity dan transparency.
- Cahaya terkumpul di tepi bawah, dan tepi diberi falloff gelap halus.

## Tepi (LiquidLens.kt)
- Rim luar lebih tebal (1.6 dp) dan lebih terang, merata di seluruh tepi.
- Bevel dalam dibuat lebar (5 dp) agar tepi terbaca sebagai kaca yang tebal.
- Glow dalam dan kilau bawah diperkuat.

## Default (Models.kt, SettingsRepository.kt)
- whiteOpacity 0.08 -> 0.14
- frostStrength (blur artwork) 0.20 -> 0.55
- Pengguna yang sudah menyimpan nilai sendiri tidak terpengaruh; reset ke default di Kustomisasi untuk melihat tampilan baru.

## Catatan
- Belum dikompilasi dan belum diuji di perangkat (tidak ada Android SDK di lingkungan ini).
- Nilai visual perlu ditala di perangkat.

## Revisi lanjutan
- Dome dihapus permanen: render dome, slider dan pilihan jumlah dome di Kustomisasi, serta lensa dome di shader.
- Garis putih di tepi dihapus: rim stroke, glow putih, garis atas, dan kilau bawah. Tepi sekarang hanya bayangan lembut (terang di atas, gelap di bawah) agar terlihat timbul.
- Migrasi V2 (glass_look_v2) menaikkan blur dan kabut body ke nilai baru sekali saja. Slider lain tidak berubah.

## Liquid glass (shader baru)
- Shader lensa dibangun ulang: kaca cembung yang membelokkan gambar di tepi (refraksi kuat di rim, nol di tengah), dispersi warna tipis di tepi, dan pencahayaan bevel 3D dengan sumber cahaya dari kiri-atas.
- Refraksi tepi default dinyalakan (0.7). Migrasi V3 mengatur ini sekali untuk pengguna yang sudah ada.
- Perlu Android 13 ke atas dan DeviceProfile.lensEnabled. Di perangkat lain, kartu tetap tampil tanpa lensa.

## Ringkasan tahap 1-5 (ULGRE)
1. UnifiedGlassEngine: glassBody, glassEdge, dan glassControl jadi satu jalur untuk semua permukaan glass.
2. Kontrol expanded (prev, play, next) memakai glassControl. Capsule tetap hitam solid.
3. Pengaturan tersambung ke renderer: highlight tepi, pantulan ambient, aberasi kromatik, kilau per profil (expanded dan banner), dan tombol reset default glass.
4. Audit: capsule tetap diam saat banner muncul; banner tidak di-clip oleh root. Tidak ada perubahan kode.
5. Pembersihan: sisa dome dihapus dari settings, model, GlassTuning, IslandRoot, dan pipeline lensa; kode rim dan shader dome yang tidak dipakai dihapus.

Belum diverifikasi: build Gradle, perilaku animasi, dan performa di perangkat.
