# Ryomd Tube V2.4 — perbaikan build WZH + penyempurnaan fitur (basis: 047 V23 Quality/Feed)

`versionCode 9` · `1.1.8-ryomd-tube-quality-fix` · `applicationId com.ryomd.nativeapp`

## Build
1. **Core library desugaring ditambahkan** (`coreLibraryDesugaringEnabled true` + `desugar_jdk_libs_nio:2.1.5`).
   NewPipe Extractor mewajibkannya untuk minSdk < 33; tanpa ini build gagal/crash di Android lama.
2. **Gradle Wrapper resmi** (`gradle-wrapper.jar`, `gradlew`, `gradlew.bat`, Gradle 8.13) menggantikan skrip bootstrap kustom
   sehingga runner Gradle apa pun mengenalinya.
3. `settings.gradle` (Groovy) tetap berada di akar ZIP; ZIP dikemas dengan entri direktori lengkap dan izin file normal.
4. Panduan pemecahan masalah error "No Gradle project" ditambahkan di `WZH_BUILD_README_ID.md`.

## Fitur
5. **Beranda 100 video**: tahap 1 menampilkan 30 video dengan cepat, tahap 2 menambah sampai 100 di latar (tanpa menggeser tampilan).
6. **Tarik ke bawah untuk menyegarkan**: kini dipasang sekali (sebelumnya menumpuk listener tiap membuka Ryomd Tube dan bisa aktif di
   tool lain), ada indikator "Tarik/Lepas untuk menyegarkan", ambang 84px, dan tidak aktif saat sedang di halaman video/channel.
7. **Pilihan kualitas sebelum memutar** (Otomatis/360p/480p/720p/1080p/1440p/4K) di halaman tonton dan tab Tautan, tersimpan;
   menu kualitas di dalam pemutar tetap ada. **Otomatis kini maks 1080p** (sebelumnya bisa memilih 4K).
8. Pemeriksaan jembatan native menerima `playYouTubeQuality` saja.
9. Waktu tunggu katalog native untuk pencarian besar dinaikkan ke 120 dtk.
