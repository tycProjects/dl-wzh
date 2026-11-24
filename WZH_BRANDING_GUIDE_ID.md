# Pengaturan nama & ikon aplikasi di WZH

WZH APK Builder hanya mengemas source yang dimasukkan. Pada source sebelumnya, label dan ikon di-hardcode langsung pada AndroidManifest.xml sehingga setting WZH tidak berhasil menimpa nilainya.

Setelah mengimpor ZIP V2.1:

1. Nama aplikasi: buka `app/src/main/res/values/strings.xml`, ubah nilai `app_name`.
2. Ikon aplikasi: ganti `app/src/main/res/drawable/ic_launcher_foreground.xml` dengan foreground vector baru dan, bila perlu, ubah `ic_launcher_background` dalam `app/src/main/res/values/colors.xml`.
3. Manifest sudah menunjuk ke resource `@string/app_name`, `@mipmap/ic_launcher`, dan `@mipmap/ic_launcher_round`. Jangan kembalikan referensi hardcode sebelumnya.
4. Jika WZH punya field App Name/App Icon dan otomatis membuat `mipmap/ic_launcher`, pastikan field itu mengubah resource Android yang sama. Setelah build, periksa APK metadata/launcher icon di hasilnya.

`applicationId` tetap `com.ryomd.nativeapp`; jangan menggantinya jika ingin APK meng-update instalasi lama. `versionCode` dinaikkan menjadi 4. Untuk update in-place, WZH wajib menandatangani APK memakai keystore yang sama dengan APK yang sudah terpasang. Jangan uninstall aplikasi lama sebelum memastikan signing.
