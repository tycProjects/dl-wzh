SAKURA PESAN - Android Project
==============================
Aplikasi chat berbasis ID 4 digit. Tampilan web (gelap + gradien pink)
dibungkus WebView dalam proyek Android Gradle.

BUKA & JALANKAN
1. Android Studio (Koala atau lebih baru, JDK 17) > Open > pilih folder ini.
2. Tunggu Gradle sync, lalu Run di perangkat/emulator (minSdk 26).
   APK: Build > Build APK(s).
   Wrapper belum menyertakan gradle-wrapper.jar; Android Studio akan memakai
   Gradle 8.7. Dari terminal: "gradle wrapper" sekali, lalu ./gradlew assembleDebug.

CHAT ANTAR PERANGKAT
Tanpa server, pesan hanya tersimpan di perangkat sendiri (mode lokal).
1. console.firebase.google.com > buat proyek > Realtime Database > Create.
2. Rules: { "rules": { ".read": true, ".write": true } } lalu Publish.
3. Isi PESAN_DB_URL di gradle.properties dengan URL database
   (https://NAMA-default-rtdb.firebaseio.com), atau tempel di layar masuk
   pada bagian "SERVER (OPSIONAL)".

STRUKTUR
app/src/main/assets/        index.html, style.css, app.js  (UI & logika chat)
app/src/main/java/...       MainActivity.java (WebView + tombol Back)
app/src/main/res/           tema, warna, ikon

CATATAN KEAMANAN
ID 4 digit bukan kata sandi dan aturan database di atas terbuka. Siapa pun
yang tahu URL-nya bisa memakai ID mana pun dan membaca pesannya. Jangan kirim
hal sensitif. Pesan berjalan lewat internet, bukan SMS operator.
