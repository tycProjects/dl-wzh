# Static validation — MediaStore v2

- Android project source basis diekstrak lengkap; semua assets web dipertahankan.
- applicationId tetap `com.ryomd.nativeapp`; versionCode dinaikkan ke 3.
- Manifest dan network security config tidak diubah.
- MainActivity hanya mengubah konstruktor bridge; port 3002, file picker, DownloadListener, WebView settings, dan lifecycle dipertahankan.
- Bridge menambahkan capability query serta save methods yang dibatasi ke endpoint download lokal.
- MediaSaver memeriksa MIME, jumlah byte, dan row MediaStore sebelum menampilkan keberhasilan.
- Validasi arsip ZIP dan parse XML: pass.
- Java lexical delimiter scan: pass; bukan pengganti build Android Gradle.
- Build APK penuh dan pengujian perangkat: belum dilakukan karena Android SDK tidak tersedia.
