# Cyber vs Hacker — Panduan Keamanan Offline

Materi ini adalah referensi ringkas yang dapat dibaca tanpa koneksi internet.

## 1. Dasar perlindungan akun
- Gunakan kata sandi unik dan panjang untuk setiap akun.
- Aktifkan autentikasi multifaktor; utamakan aplikasi autentikator atau kunci keamanan bila tersedia.
- Jangan membagikan kata sandi, kode OTP, kode pemulihan, atau kunci privat.
- Simpan kode pemulihan di tempat aman yang terpisah dari perangkat utama.

## 2. Mengenali phishing
- Periksa domain dengan teliti; nama tampilan pengirim dapat dipalsukan.
- Waspadai pesan yang mendesak, mengancam, atau menjanjikan hadiah yang tidak masuk akal.
- Jangan masuk melalui tautan pesan yang tidak diminta. Buka aplikasi atau situs resmi secara mandiri.
- Jangan mengabaikan peringatan sertifikat atau browser.

## 3. Perangkat yang aman
- Pasang pembaruan sistem dan aplikasi dari sumber resmi.
- Aktifkan kunci layar dan enkripsi perangkat.
- Tinjau izin aplikasi secara berkala dan cabut izin yang tidak diperlukan.
- Pasang aplikasi hanya dari pengembang dan sumber yang tepercaya.

## 4. Jaringan dan privasi
- Anggap Wi-Fi publik sebagai jaringan yang tidak tepercaya.
- Pastikan nama domain dan HTTPS benar sebelum memasukkan informasi.
- Matikan koneksi otomatis ke hotspot yang tidak dikenal.
- Bagikan lokasi dan informasi pribadi hanya saat memang diperlukan.

## 5. Cadangan dan pemulihan
- Buat beberapa salinan cadangan, termasuk satu salinan yang terpisah atau offline.
- Uji pemulihan cadangan secara berkala.
- Jika menduga akun diambil alih, gunakan perangkat tepercaya untuk mengganti kata sandi, cabut sesi asing, aktifkan MFA, dan hubungi dukungan resmi.
- Jangan membayar atau menghubungi pelaku berdasarkan pesan yang tidak terverifikasi.

## 6. Latihan keamanan yang etis
- Lakukan pengujian hanya pada perangkat, akun, dan laboratorium yang Anda miliki atau yang secara eksplisit mengizinkan pengujian.
- Tentukan ruang lingkup, waktu, dan cara pelaporan sebelum pengujian.
- Jangan mengakses data orang lain, mengganggu layanan, atau menyebarkan data yang ditemukan.
- Catat temuan secara bertanggung jawab dan prioritaskan mitigasi.

## 7. Checklist mingguan
- [ ] Periksa pembaruan sistem dan aplikasi.
- [ ] Tinjau aktivitas login dan sesi akun.
- [ ] Tinjau izin aplikasi dan akses lokasi.
- [ ] Pastikan cadangan terbaru tersedia dan dapat dipulihkan.
- [ ] Periksa email pemulihan dan nomor pemulihan akun.

Catatan: game ini menyajikan simulasi lokal dan edukasi defensif. Materi tidak menggantikan panduan resmi vendor atau audit keamanan profesional.

## 8. Dasar jaringan
- DNS membantu menerjemahkan nama domain menjadi alamat yang dibutuhkan perangkat.
- Router meneruskan lalu lintas ant jaringan; firewall menerapkan aturan lalu lintas.
- Gunakan perintah diagnostik jaringan hanya pada perangkat atau jaringan yang diizinkan.

## 9. Malware dan ransomware
- Hindari APK, lampiran, dan file bajakan dari sumber tak dikenal.
- Perbarui sistem, batasi hak administrator, dan tinjau aplikasi yang tidak dikenal.
- Simpan cadangan terpisah/offline dan uji pemulihannya.

## 10. Belajar ethical hacking
- Mulai dari konsep jaringan, Linux dasar, HTTP, autentikasi, dan pemrograman.
- Gunakan CTF dan laboratorium yang secara eksplisit mengizinkan pengujian.
- Tetapkan ruang lingkup; jangan memindai, mengeksploitasi, atau mengakses data sistem nyata tanpa izin.
- Saat menemukan kerentanan, dokumentasikan dampak secara minimal dan laporkan secara privat kepada pemilik sistem.

## 11. Respons kebocoran kredensial
1. Dari perangkat tepercaya, ganti sandi yang terdampak dan sandi lain yang dipakai ulang.
2. Cabut sesi dan token yang tidak dikenal; rotasi token API yang bocor.
3. Aktifkan MFA dan periksa alamat pemulihan.
4. Periksa aktivitas akun dan hubungi dukungan resmi jika perlu.
5. Jangan pernah membagikan OTP atau kode pemulihan kepada orang yang menghubungi secara mendadak.

## 12. Glosarium singkat
- **MFA/2FA:** verifikasi menggunakan lebih dari satu faktor.
- **Phishing:** upaya menipu korban untuk memperoleh informasi atau akses.
- **Patch:** pembaruan yang memperbaiki bug atau celah.
- **Enkripsi:** mengubah data agar tidak mudah dibaca tanpa kunci.
- **DNS:** sistem yang membantu menemukan alamat layanan dari nama domain.
- **CTF:** latihan keamanan dalam lingkungan yang disediakan dan memiliki aturan.
- **Least privilege:** memberikan akses minimum yang dibutuhkan.
