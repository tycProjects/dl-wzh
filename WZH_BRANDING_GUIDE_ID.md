# Nama aplikasi Android Ryomd

- Nama yang tampil di launcher Android: **Ryomd panel**.
- Nama fitur/pemutar YouTube di dalam aplikasi: **Ryomd Tube**.
- Resource nama aplikasi: `app/src/main/res/values/strings.xml` → `app_name`.
- Manifest menggunakan `android:label="@string/app_name"` agar label yang terpasang menjadi **Ryomd panel**.
- `applicationId` tetap `com.ryomd.nativeapp` agar pembaruan dapat dipasang di atas aplikasi lama.
- `versionCode` sekarang 8; `versionName` `1.1.7-ryomd-tube-quality`.
- Untuk pembaruan in-place, WZH harus menggunakan signing key/release keystore yang sama dengan APK terpasang.

Ikon aplikasi tetap memakai resource `@mipmap/ic_launcher` dan `@mipmap/ic_launcher_round`. Jika WZH mengunggah ikon, pastikan hasil APK memakai ikon tersebut.
