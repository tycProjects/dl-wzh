# UPDATE 1.17.5 - STAGE 78 - NOTCH KLASIK RAPI, LIRIK LEBIH LONGGAR

## Notch klasik
- Siluet (tepi atas rata, kuping cekung, sisi lurus, dua sudut bawah bulat) sudah sesuai notch iPhone X/XS/11, tidak diubah.
- Bug: padding isi kapsul hanya 8 dp, padahal kuping + sudut bawah memakan sisi kiri/kanan, jadi avatar 20 dp terpotong. Padding kini 8 dp + lebar kuping (`iPhoneNotchEar`, satu rumus dipakai bentuk dan layout).
- Bug: notch saat idle tidak memakai `horizontalOffset` seperti kapsul aktif, jadi bergeser saat aktif. Kini sama.

## Lirik
- Gerbang keras cakupan 40%/45% dihapus: lirik berstempel waktu yang hanya sebagian lagu tetap tampil. Di validate hanya jadi pengurang skor kecil (-5).
- Skor minimum 65 -> 40 (satu konstanta `MIN_SCORE`, dipakai LRC, TTML, QQ, lyricsfile).
- Selisih durasi maksimum 12 s -> 20 s.
- LRCLIB: nama artis berbeda ejaan diterima bila judul sama dan selisih durasi <= 4 s.
- Varian judul ketiga tanpa kurung/siku di akhir ("Lagu (Acoustic)" -> "Lagu").
- Lirik polos tanpa stempel tetap tidak ditampilkan.
