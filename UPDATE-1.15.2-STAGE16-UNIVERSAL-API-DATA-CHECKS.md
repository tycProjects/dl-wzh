# Tahap 16 — Pemeriksaan umum data API lintas katalog

## Tujuan
Memperluas pemeriksaan respons API agar setiap respons JSON yang dipanggil melalui PublicApis mendapat pemeriksaan umum, bukan hanya tiga profil penyedia dan delapan kategori yang sudah ada.

## Perubahan kode
- `ApiDomainValidator` kini menjalankan pemeriksaan umum pada respons JSON dari seluruh penyedia/kategori: objek/array utama kosong, kontainer hasil (`data`, `results`, `items`, dan sejenisnya) kosong/null, flag `success`/`ok` bernilai false, status error, proporsi field null yang tinggi, serta timestamp yang tidak terbaca atau berada jauh di masa depan.
- Pemeriksaan JSON tetap berjalan jika server tidak mengirim header Content-Type, selama isi respons memang terlihat sebagai JSON.
- Keluaran validasi kini secara eksplisit menyebut tingkat pemeriksaan dan melarang AI menyebut data sebagai "terverifikasi benar" hanya karena lolos pemeriksaan dasar.
- Instruksi AI diperbarui agar membedakan katalog kandidat, validasi struktur, pemeriksaan kewajaran, validasi kontrak resmi, dan kebenaran faktual.
- Ditambahkan empat pengujian unit untuk kontainer kosong/status error, timestamp, ketiadaan timestamp, dan JSON tanpa Content-Type.

## Audit katalog statis
Audit lokal atas `app/src/main/assets/public_apis.txt`:
- 1.064 baris entri.
- 1.064 entri lolos pemeriksaan format dasar HTTPS dan kolom wajib.
- 46 kategori.
- 957 host unik.
- 7 host muncul pada lebih dari satu entri.

Audit ini hanya memeriksa format katalog dan pengelompokan host. Ini TIDAK membuktikan endpoint hidup, URL adalah endpoint (bukan dokumentasi), API gratis, autentikasi tidak diperlukan, respons sesuai dokumentasi, atau data akurat.

## Batasan yang belum terselesaikan
- Belum ada skema OpenAPI/JSON Schema resmi yang diambil dan divalidasi untuk seluruh penyedia. Hanya Open-Meteo, Frankfurter, dan REST Countries yang mempunyai profil parsial.
- Belum ada pengujian live untuk seluruh endpoint; sebagian endpoint membutuhkan API key, OAuth, header khusus, POST, atau parameter yang tidak didukung runner saat ini.
- Validasi umum tidak dapat membuktikan bahwa nilai cuaca, harga, kurs, skor olahraga, data medis, dan sebagainya benar terhadap sumber otoritatif.
- Timestamp tidak selalu tersedia atau relevan untuk data statis. Ketiadaannya berarti freshness tidak diketahui, bukan otomatis datanya salah.
- Pemeriksaan kewajaran dapat menghasilkan false positive pada API yang skemanya berbeda.

## Verifikasi tahap ini
- Audit statis katalog dijalankan ulang dan menghasilkan 1.064 entri / 46 kategori / 957 host unik / 0 baris format invalid.
- Ditambahkan 4 test baru; total test `ApiDomainValidatorTest` menjadi 17.
- Gradle `testDebugUnitTest --no-daemon --offline` dicoba, tetapi wrapper tetap mencoba mengunduh Gradle 8.9 dan gagal karena DNS `services.gradle.org` tidak tersedia. Jadi test unit dan build Android belum dapat dinyatakan lulus.
- Tidak ada endpoint API live yang dihubungi pada tahap ini.
