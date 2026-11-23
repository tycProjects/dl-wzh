# LEN AI Floating Assistant V3

Native Android Kotlin source project.

## Perubahan V3
- Bubble logo floating kecil (48dp), dapat digeser.
- Panel floating lebih besar dan modern.
- Panel dapat diminimalkan dengan tombol garis pada header.
- Area hasil menggunakan ScrollView sehingga hasil OCR/Gemini panjang dapat digulir.
- Tombol NONAKTIFKAN FLOATING / SCREEN READING di aplikasi utama menghentikan service.
- Izin screen capture diminta dari aplikasi utama terlebih dahulu.
- Setelah izin disetujui, pengguna dapat berpindah ke Chrome/aplikasi target.
- Menekan BACA LAYAR di floating panel tidak membuka Activity LEN AI. Panel disembunyikan sementara, satu frame layar aplikasi di belakang ditangkap, lalu hasil ditampilkan kembali.
- OCR dilakukan lokal memakai ML Kit; teks kemudian dikirim ke Gemini untuk pembahasan.
- MediaProjection berjalan sebagai sesi yang disetujui pengguna. Android menampilkan notifikasi foreground service selama sesi aktif.

## Cara mencoba
1. Ekstrak ZIP dan buka folder project di Android Studio/IDE Android yang mendukung project Gradle.
2. Jalankan Gradle Sync.
3. Buat API key Gemini dari Google AI Studio dan simpan di aplikasi.
4. Izinkan tampil di atas aplikasi lain.
5. Tekan AKTIFKAN FLOATING LEN AI.
6. Tekan AKTIFKAN IZIN BACA LAYAR dan setujui dialog Android. Ini diperlukan karena Android mewajibkan persetujuan pengguna untuk screen capture.
7. Setelah kembali ke LEN AI, buka Chrome atau aplikasi yang ingin dibaca.
8. Ketuk bubble LEN AI, lalu tekan BACA LAYAR SEKARANG. Panel akan hilang sementara agar tidak menutupi konten target.
9. Untuk berhenti, buka LEN AI dan tekan NONAKTIFKAN FLOATING / SCREEN READING.

## Batasan dan privasi
- Android dapat memblokir screen capture pada aplikasi tertentu (misalnya konten yang memakai FLAG_SECURE); hasilnya dapat hitam/kosong.
- OCR paling baik untuk teks yang jelas dan tidak terlalu kecil. Diagram, tabel kompleks, tulisan tangan, atau teks yang tertutup mungkin tidak terbaca sempurna.
- Sesi screen capture hanya berjalan setelah persetujuan Android dan dapat dihentikan dari notifikasi/sistem.
- API key yang disimpan pada aplikasi klien tetap berisiko diekstrak dari APK. Untuk distribusi publik, gunakan backend proxy dan jangan membagikan API key.
- Gemini dapat keliru. Periksa kembali hasil penjelasan dan gunakan untuk belajar, bukan sebagai pengganti pemahaman sendiri.

## Build
Build APK melalui Android Studio: Build > Build APK(s). File APK debug biasanya berada di `app/build/outputs/apk/debug/app-debug.apk`.

Catatan: source ini sudah diperbaiki secara statis berdasarkan bug yang dilaporkan, tetapi belum dapat diklaim bebas bug sebelum Gradle build dan pengujian pada perangkat Android nyata.
