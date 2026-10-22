MSI ASTRA WEBVIEW - PROYEK APK ANDROID

Isi:
- Aplikasi Android sederhana yang membuka https://msi-astra-webview.vercel.app/
- WebView diatur dengan viewport lebar dan mode desktop untuk mendekati susunan PC.
- JavaScript, DOM storage, dan pemutaran media tanpa gesture diaktifkan.

PENTING:
Ini adalah proyek sumber, bukan file APK yang sudah dikompilasi. APK harus dibuild di Android IDE atau komputer.

CARA BUILD DARI HP ANDROID:
1. Ekstrak ZIP ini ke folder.
2. Buka proyek folder MSI_Astra_Android_Project melalui AndroidIDE yang mendukung proyek Gradle Android.
3. Tunggu Gradle Sync selesai. Jika diminta, gunakan JDK 17 dan Android SDK 35.
4. Pilih Build > Assemble/Build APK (nama menu bisa berbeda menurut versi IDE).
5. Cari APK debug di app/build/outputs/apk/debug/app-debug.apk.
6. Instal APK tersebut. Jika Android memblokir, izinkan instalasi dari sumber aplikasi yang Anda gunakan.

CATATAN TAMPILAN:
Mode desktop dapat membuat elemen tampak lebih kecil di layar HP karena seluruh tata letak PC diperkecil agar muat. Perilaku akhirnya tergantung pada CSS situs, server, dan versi Android System WebView. Agar benar-benar identik, situs harus menyediakan tata letak desktop yang responsif atau perlu disalin/dikemas sebagai aset lokal dan disesuaikan.

Alamat situs yang dipakai:
https://msi-astra-webview.vercel.app/

Untuk mengganti alamat, ubah konstanta SITE di:
app/src/main/java/com/xelnoxz/msi_astra/MainActivity.java
