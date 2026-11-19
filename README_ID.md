# LINKBYPASS PRO — Android Native

Aplikasi Android native berbasis Kotlin (bukan HTML/WebView wrapper sebagai aplikasi utama; WebView hanya digunakan sebagai browser internal).

## Fitur
- Membuka URL HTTP/HTTPS dan mengikuti redirect normal melalui browser internal.
- Tombol paste clipboard.
- Menangani unduhan yang dipicu halaman melalui WebView DownloadListener.
- Menggunakan Android DownloadManager untuk unduhan dan notifikasi sistem.
- Riwayat URL sederhana.
- Antarmuka gelap dengan aksen merah.

## Batasan penting
Aplikasi ini **tidak dapat menjamin melewati semua halaman iklan**. Situs dapat memakai JavaScript, CAPTCHA, login, token sementara, cookie, atau proteksi lain. LINKBYPASS PRO tidak memecahkan CAPTCHA, tidak menghapus paywall, tidak melewati login, dan tidak mengakses file tanpa izin. Untuk halaman yang hanya menampilkan iklan dan tidak menyediakan tautan file yang dapat diakses, pengguna perlu mengikuti alur resmi situs tersebut.

## Build APK
1. Ekstrak ZIP.
2. Buka folder `LINKBYPASS_PRO` melalui Android Studio.
3. Pastikan Android SDK Platform 35 dan JDK 17 tersedia.
4. Tunggu Gradle Sync selesai.
5. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. APK debug biasanya berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Identitas aplikasi
- Application ID: `com.carter.linkbypasspro`
- Minimum Android: Android 6.0 (API 23)
- Target SDK: 35
- Version: 1.0.0

## Catatan
Build pertama memerlukan koneksi internet untuk mengunduh Gradle dan Android Gradle Plugin. Proyek ini berisi source code, bukan APK yang sudah dikompilasi.

## Pembaruan scanner (v1.1 source)
Tombol **SCAN DOWNLOAD LINK** memeriksa tautan `<a href>` yang tampak pada DOM halaman dan mencari ekstensi file umum atau teks tombol unduh. Kandidat ditampilkan agar pengguna memilih link yang benar. Link file yang dikenali dapat dikirim ke Android DownloadManager; link perantara dibuka agar dapat dipindai lagi.

Scanner ini bukan bypass universal: tautan yang dibuat setelah login, CAPTCHA, JavaScript khusus, atau berada di iframe/halaman lain mungkin tidak terdeteksi. Filter iklan hanya menyaring sejumlah domain pelacak/iklan yang dikenal dan tidak menghapus seluruh iklan dari website.

## LINKBYPASS PRO v1.2
- Resolver HTTP: mengikuti redirect HTTP standar (301/302/303/307/308) menggunakan HEAD dan fallback GET.
- Menampilkan URL tujuan dan tipe konten sebelum mengunduh.
- Scanner DOM tetap tersedia untuk tautan file/tombol unduh yang sudah ada pada halaman.
- Resolver generik tidak berarti dukungan khusus/terjamin untuk semua layanan (termasuk Bicolink). Shortlink yang membutuhkan JavaScript khusus, CAPTCHA, login, token, atau interaksi situs tidak dapat diselesaikan oleh HTTP redirect resolver; buka halaman dan gunakan SCAN LINK untuk kandidat yang tersedia.
- Jangan gunakan untuk menghindari pembayaran, login, CAPTCHA, atau kontrol akses. Tidak ada klaim bahwa semua iklan bisa dihapus.

## LINKBYPASS PRO v1.3 — Provider Recognition
- Registry pengenal domain: SFL.GL (`sfl.gl`), Bicolink (`bicolink.net`), Khaddavi (`khaddavi.net`), Linkvertise, OUO, ShrinkMe, ShortURL, Bitly, TinyURL.
- Tombol **UJI LINK SFL.GL** mengisi dan membuka URL contoh `https://sfl.gl/woE9vko`.
- Provider recognition hanya memberi label/diagnostik. Semua provider tetap diproses melalui redirect HTTP standar dan pemindaian DOM generik; tidak ada jaminan semua shortlink dapat di-resolve.
- Situs yang memakai CAPTCHA, login, token dinamis, anti-bot, pembayaran, atau kontrol akses tidak diakali. Tidak ada adapter khusus yang menghapus halaman iklan atau mengekstrak tujuan tersembunyi dari layanan pihak ketiga.
- Domain Bicolink dapat memakai domain/alias berbeda; registry dapat diperluas sesuai domain resmi yang terverifikasi.

## LINKBYPASS PRO v1.4 — UI & scanner fixes
- Tombol dibuat sebagai TextView interaktif dengan tinggi/padding eksplisit agar teks tidak terpotong di berbagai versi Android.
- Keyboard disembunyikan saat aksi utama dijalankan; tombol Go pada keyboard juga membuka URL.
- Soft input memakai adjustResize.
- Pesan hasil scan kosong tidak lagi menyebut CAPTCHA sebagai dugaan otomatis; pesan menjelaskan bahwa belum ada URL unduhan yang terlihat.
- Scanner juga memeriksa atribut data-href/data-url, form action, dan URL sederhana pada onclick. Ini tetap pemindaian terbatas terhadap DOM yang dapat diakses, bukan bypass universal.
- WebView diberi tinggi adaptif yang lebih ringkas untuk mengurangi scroll berlebihan.
