# Flipper (Android)

Aplikasi Android berisi tampilan Flipper (HTML di dalam WebView) dengan fitur
**Scan WiFi sekitar** di popup tombol Sub-GHz.

## Cara membuat aplikasinya
1. Ekstrak ZIP ini.
2. Buka **Android Studio** (versi terbaru) -> **Open** -> pilih folder `FlipperApp`.
3. Tunggu *Gradle sync* selesai (butuh internet pertama kali, Android Studio akan
   mengunduh Gradle 8.7 dan SDK yang kurang; ikuti tawaran "Install" kalau muncul).
4. Sambungkan HP (USB debugging) lalu tekan **Run**, atau buat file instalasi lewat
   **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
   Hasilnya: `app/build/outputs/apk/debug/app-debug.apk`.

## Cara memakai
Buka tab **Archive** -> tekan **Sub-GHz** -> tekan **Scan WiFi Sekitar**.
Saat pertama kali, Android akan meminta izin Lokasi / Perangkat sekitar.
WiFi dan Lokasi (GPS) di HP harus menyala.

## Fitur perangkat tersambung
Setelah scan WiFi, tekan salah satu jaringan untuk melihat detailnya. Kalau itu WiFi yang
sedang kamu pakai, muncul tombol **Lihat Perangkat Tersambung**: aplikasi memeriksa
alamat IP di jaringan itu (maks. 254 alamat) lalu menampilkan jumlah perangkat, IP, dan
nama (jika router menyimpannya). Android tidak mengizinkan melihat perangkat di WiFi yang
tidak sedang tersambung. Pakai hanya di jaringan milikmu atau yang kamu punya izinnya.



## Modul 1: Mesin Akses Memori (mem_engine) -- BARU, masih tahap pondasi
Ini bagian inti buat fitur "edit koin tanpa GameGuardian" yang lagi dibangun
bertahap. Sejauh ini baru **modul 1**: mesin baca/tulis memori proses lain.
Belum ada UI pencarian nilai koin (itu modul 2) -- untuk sekarang, tesnya
lewat cara ini:

### WAJIB
- **HP harus sudah di-root** (Magisk atau sejenisnya). Tanpa root, modul ini
  tidak akan pernah berfungsi -- ini batasan sistem Android, bukan kode ini.
- Buka project ini di **Android Studio dengan NDK terpasang** (Android
  Studio akan menawarkan instalasi NDK otomatis kalau belum ada, lewat
  SDK Manager > SDK Tools > NDK). Tanpa NDK, bagian `app/src/main/cpp/`
  tidak akan ter-compile dan modul ini tidak akan berfungsi.

### Cara menguji
1. **Run** aplikasi ke HP yang sudah di-root (bukan cuma Build APK -- Run
   langsung lebih gampang buat lihat Logcat).
2. Buka tab **Archive**, cari baris **Game Offline**.
3. **Tekan dan TAHAN** (±1 detik) baris itu, lepas. Ini BUKAN tekan biasa
   (tekan biasa tetap membuka popup daftar game seperti sebelumnya).
4. HP akan menampilkan dialog minta izin root (Magisk dll) -- setujui.
5. Muncul pesan kecil di bawah layar:
   - **✅ SUKSES...** -> mesin berhasil nempel ke proses aplikasi ini
     sendiri, membaca daftar region memori, dan membaca 16 byte pertama dari
     salah satu region. Modul 1 berfungsi, siap lanjut ke modul 2 (pencarian
     nilai koin).
   - **⚠️ ...** -> ada yang gagal; pesannya menjelaskan di langkah
     mana (binary tidak ketemu / root ditolak / attach gagal / dst).
6. Untuk detail teknis, sambungkan HP ke komputer dan buka **Logcat** di
   Android Studio, filter tag `MemEngine`.

### Kalau build CMake gagal
Build > Build Bundle(s)/APK(s) akan ikut menjalankan CMake untuk
`app/src/main/cpp/mem_engine.c`. Kalau Android Studio menolak karena NDK
belum terpasang, ikuti saja tawaran instalasinya (Tools > SDK Manager >
SDK Tools > centang "NDK (Side by side)" dan "CMake").

Catatan: `mem_engine.c` ini sudah diuji logikanya (attach ke proses lain,
baca nilai, tulis nilai baru, proses lain beneran melihat perubahannya)
di komputer biasa sebelum ditaruh di sini. Yang belum pernah dites adalah
jalannya di HP Android asli lewat `su` -- makanya ada self-test di atas,
supaya ketahuan dari awal kalau ada yang perlu disesuaikan di HP kamu.


## Modul 2-5: Scan Nilai, Tulis Nilai, UI Pemilih Proses, Bubble Overlay Beneran
Lanjutan dari modul 1. Sekarang fitur "Edit Koin" ini TIDAK bergantung pada
GameGuardian/VirtualXposed lagi -- semua dibangun sendiri di dalam aplikasi
ini.

### Alur pakainya
1. Tab **Archive** -> **Game Offline** -> pilih game dari daftar.
2. Game itu otomatis terbuka. Kalau izin **"Tampil di atas aplikasi lain"**
   belum aktif untuk Flipper, Android akan membuka halaman izin itu --
   aktifkan, lalu kembali (biasanya otomatis).
3. **Bubble oranye bulat** (simbol ₢) muncul mengambang di layar,
   di atas aplikasi apa pun -- termasuk pas lagi di dalam game. Bisa diseret
   ke posisi manapun.
4. Butuh beberapa detik untuk mesin nemu proses game-nya dan nempel (attach).
   Sambil nunggu main dulu aja, nanti bubble-nya tetap ada.
5. Ketuk bubble -> muncul panel kecil:
   - Masukkan **nilai koin yang sekarang kelihatan di game**, tekan
     **Cari (Scan Baru)**. Ini scan ke SELURUH memori game (bisa beberapa
     detik), ketemu banyak kandidat (wajar, nilai kecil seperti itu umum).
   - Pergi ke game, buat nilainya berubah (dapat koin / pakai koin), balik
     ke bubble.
   - Tekan tombol **Naik** (kalau kamu baru dapat koin), **Turun** (kalau
     baru dipakai), **Berubah**, atau **Tetap**, sesuai yang terjadi.
     Daftar kandidat makin menyempit.
   - Ulangi langkah di atas (ubah nilai di game, saring lagi) sampai tersisa
     sedikit (idealnya 1).
   - Isi nilai baru yang diinginkan di kotak atas, lalu tekan
     **Tulis Nilai ke SEMUA Hasil**. Atau ketuk salah satu baris hasil di
     daftar untuk pilih satu alamat spesifik.
6. **Hentikan Overlay** di panel (atau tombol di notifikasi) untuk mematikan
   bubble dan melepas koneksi ke game.

### Kenapa harus "scan lalu saring", bukan langsung cari?
Game tidak memberi tahu di mana persisnya koin disimpan di memori. Satu kali
scan nilai tertentu (misal 500) akan nemu RATUSAN tempat yang kebetulan juga
bernilai 500 (bisa HP, skor, apa saja). Setiap "saring" (Naik/Turun/dst)
membuang kandidat yang tidak ikut berubah sesuai, sampai yang tersisa hampir
pasti benar. Ini cara kerja asli semua tool sejenis (termasuk GameGuardian),
bukan jalan pintas yang kami buat beda sendiri.

### Teknis (kalau penasaran / mau audit kodenya)
- `app/src/main/cpp/mem_engine.c`: mesin C yang jalan sebagai root,
  simpan daftar alamat kandidat, baca ulang nilainya tiap kali disaring.
  **Sudah diuji di komputer** (bukan di HP): program ini terbukti bisa
  nemu alamat variabel yang NILAI AWALNYA diketahui tapi ALAMATNYA tidak
  diberi tahu, menyaringnya berdasarkan kenaikan nilai, menuliskan nilai
  baru, dan proses targetnya benar-benar melihat perubahan itu.
- `MemEngine.kt`: jembatan Kotlin ke mesin itu (scanNew/scanFilter/scanPage/
  writeValue/listProcesses).
- `OverlayService.kt`: bubble + panel, semuanya native (bukan WebView),
  pakai `SYSTEM_ALERT_WINDOW` -- izin resmi Android yang sama dipakai
  chat-head Messenger. Berjalan sebagai *foreground service* (karena itu
  ada notifikasi kecil yang tidak bisa disembunyikan, ini aturan Android,
  bukan pilihan kami).
- Batas aman yang sengaja dipasang: maksimal 200.000 kandidat per scan,
  region memori di atas 200MB dilewati -- supaya scan tetap cepat & tidak
  membebani HP.

### Kalau ada yang gagal
- **"Gagal mendapat akses root"** -> HP belum di-root, atau izin root untuk
  aplikasi ini ditolak di Magisk/manajer root.
- **"Proses belum terdeteksi"** -> pastikan game sudah benar-benar terbuka
  (bukan baru loading), tunggu beberapa detik, atau tutup-buka lagi gamenya
  lalu ketuk bubble sekali lagi.
- Panel tidak muncul sama sekali -> cek izin "Tampil di atas aplikasi lain"
  untuk Flipper di Pengaturan HP > Aplikasi > Flipper > Tampil di atas
  aplikasi lain.
- Untuk detail error, sambungkan ke Android Studio dan lihat **Logcat**,
  filter tag `MemEngine`.

### Pengingat penting
Pakai hanya untuk game **offline** milikmu sendiri. Game online hampir pasti
mendeteksi ini sebagai cheat (bisa kena banned permanen). Fitur ini murni
mengubah angka di memori HP kamu sendiri saat game berjalan -- tidak
menyentuh server, akun, atau perangkat orang lain.


## Jalan Tanpa Root (lewat VirtualXposed)

**Prioritas tetap root** -- kalau HP kamu sudah/akan di-root, pakai jalur di
atas, itu yang paling diuji dan paling stabil. Bagian ini untuk HP yang
BELUM di-root, lewat VirtualXposed.

### Cara kerjanya (biar nggak dipakai buta-buta)
Karena belum root, Flipper tidak bisa ptrace ke proses lain dari luar. Jadi
triknya dibalik: Flipper DIPASANG SEBAGAI "modul" di dalam VirtualXposed,
bersama game yang mau diedit. Begitu game itu dibuka di dalam VirtualXposed,
kode Flipper ikut "disuntik" ke proses game tersebut -- dari SITU, baca/tulis
memori "diri sendiri" tidak butuh izin spesial apa pun (semua proses boleh
baca/tulis `/proc/self/mem` miliknya sendiri). Kode yang tersuntik itu buka
server kecil di localhost; Flipper yang biasa (di luar VirtualXposed)
tinggal konek ke situ. Teknisnya ada di `app/src/main/java/com/flipper/app/xposed/`.

### Yang perlu disiapkan (sekali saja)
1. Install **VirtualXposed** (cari di web, bukan dari Play Store -- biasanya
   dari GitHub resminya atau APK mirror tepercaya; app ini sendiri tidak
   menyediakan link karena bisa berubah).
2. Buka VirtualXposed, lewat menu di dalamnya **clone/tambahkan dua
   aplikasi**: (a) game yang mau diedit, dan (b) **Flipper** (app ini
   sendiri, dipasang DUA KALI di HP: sekali biasa, sekali lagi di-clone ke
   dalam VirtualXposed).
3. Di VirtualXposed, buka menu **Xposed Module / Installed Modules**,
   cari **"Flipper"**, lalu **aktifkan**.
4. Restart VirtualXposed (beberapa versi minta ini supaya modul kebaca).
5. Buka **game** itu DARI DALAM VirtualXposed (bukan dari launcher HP
   biasa) -- biarkan game-nya terbuka/loading.
6. Buka **Flipper yang BIASA** (di luar VirtualXposed, ikon yang asli),
   tab Archive > Game Offline > pilih game yang sama > ikuti alur bubble
   seperti biasa. Flipper otomatis coba root dulu, gagal (karena belum
   root), lalu otomatis coba jalur VirtualXposed ini.

### Soal keamanan izin "Tampil di atas aplikasi lain"
Ini tetap dipakai supaya bubble-nya kelihatan di layar; tidak ada
hubungannya dengan root/VirtualXposed, izinkan seperti biasa saat diminta.

### KEJUJURAN PENTING soal bagian ini
Jalur root (modul 1-3) sudah saya uji ALGORITMA dan ALUR KERJANYA secara
menyeluruh di komputer (bukan cuma ditulis, beneran dijalankan dan
dibuktikan hasilnya benar). Jalur VirtualXposed ini:
- Algoritma scan-nya (`InProcessEngine.kt`) **sudah diuji identik** lewat
  program Java terpisah di komputer -- cari nilai tanpa tahu alamat, saring
  presisi, tulis, dan dibuktikan tidak menyentuh alamat lain yang kebetulan
  bernilai sama. Jadi OTAKNYA sudah teruji benar.
- Yang **BELUM pernah dites** karena sandbox pengembangan ini tidak punya
  Android asli maupun VirtualXposed: apakah hook Xposed-nya beneran ke-load
  oleh VirtualXposed, apakah socket localhost bisa dijangkau lintas proses
  virtual di versi VirtualXposed yang kamu pakai, dan apakah dependency
  Gradle `de.robv.android.xposed:api:82` ke-resolve lancar saat kamu build.
- Kemungkinan ada penyesuaian yang perlu dilakukan setelah percobaan
  pertama (versi VirtualXposed berbeda-beda perilakunya). Kalau gagal,
  kirim pesan error/Logcat-nya, saya bantu perbaiki dari situ.
- VirtualXposed sendiri adalah aplikasi pihak ketiga yang sudah lama tidak
  banyak di-update; kompatibilitasnya dengan Android versi terbaru (14+)
  tidak terjamin, terlepas dari kode Flipper ini benar atau tidak.


## Jalan Tanpa Root, Opsi Lain: App Cloner Biasa (BlackBox, Parallel Space, dkk)

Ini jalur TERPISAH dari VirtualXposed di atas, dan sebenarnya ini mekanisme
yang SAMA dipakai GameGuardian untuk mode "tanpa root"-nya -- jadi kalau
GameGuardian pernah jalan di BlackBox/Parallel Space kamu, jalur ini
punya peluang bagus.

### Cara kerjanya
Android mengizinkan satu proses "menempel" (ptrace, dasar dari semua fitur
baca/tulis memori di app ini) ke proses lain kalau keduanya punya UID Android
yang sama -- tidak harus root. App cloner seperti BlackBox/Parallel Space
menjalankan SEMUA aplikasi yang kamu clone ke dalamnya dengan UID yang SAMA
(karena dari sudut pandang Android, itu semua cuma "proses milik BlackBox").
Jadi kalau Flipper DAN game sama-sama di-clone ke cloner yang sama, Flipper
bisa menempel ke game itu tanpa root sama sekali.

### Cara pakainya
1. Install salah satu: **BlackBox**, atau **Parallel Space** (banyak
   tersedia, cari yang rating/review-nya bagus dan bukan dari situs
   mencurigakan -- ini aplikasi app-cloner biasa, bukan hal eksotis, jadi
   lebih mudah dicari versi resminya dibanding VirtualXposed).
2. Di dalam cloner itu, **clone DUA aplikasi**: game yang mau diedit, DAN
   **Flipper (app ini sendiri)**.
3. Buka **game** dari dalam cloner.
4. Buka **Flipper yang SUDAH DI-CLONE itu** (bukan Flipper yang biasa/asli
   di luar) -- icon-nya ada di dalam cloner tersebut.
5. Tab Archive > Game Offline > pilih game yang sama > ikuti alur bubble
   seperti biasa. Flipper otomatis coba root dulu (gagal), lalu otomatis
   coba mode sama-UID ini.

### Urutan Flipper mencoba (otomatis, tidak perlu dipilih manual)
1. **Root** (kalau HP sudah di-root) -- paling stabil, paling teruji.
2. **Mode sama-UID** (BlackBox/Parallel Space, bagian ini) -- dicoba kalau
   root tidak ada.
3. **VirtualXposed** (modul Xposed, bagian sebelumnya) -- dicoba paling
   akhir kalau dua di atas gagal.

### Kejujuran soal jalur ini
Mekanisme dasarnya (ptrace sesama UID tanpa root) adalah fitur Linux/Android
yang nyata dan dipakai GameGuardian sungguhan di dunia nyata -- bukan saya
karang. Tapi, seperti jalur VirtualXposed, saya tidak punya HP Android atau
BlackBox/Parallel Space sungguhan untuk mengetes langsung, jadi tidak bisa
memastikan 100% berfungsi di HP kamu -- khususnya karena beberapa versi
Android membatasi ptrace sesama-UID lebih ketat dari yang lain (pengaturan
bernama "ptrace_scope"). Kalau gagal di attach (PID ketemu tapi attach
ditolak), kemungkinan besar itu penyebabnya, dan jalur VirtualXposed di
atas jadi alternatif berikutnya.


## Infrared: Kirim Sinyal IR (remote universal, termasuk AC)

### Kenapa bukan Sub-GHz?
Sub-GHz (433/315MHz, buat ngontrol gerbang garasi, stasiun cuaca, dll) butuh
chip radio khusus yang CUMA ada di perangkat seperti Flipper Zero asli. HP
Android (apapun mereknya) **secara fisik tidak punya chip itu** -- ini bukan
batasan software yang bisa diprogram ulang, chipnya memang tidak terpasang.
Jadi permintaan "AC nyala pakai Sub-GHz" saya pindah ke **Infrared**, yang
memang untuk kebutuhan seperti ini dan beneran didukung sebagian HP.

### Syarat
HP harus punya **IR blaster fisik** (lampu kecil infra merah, biasanya di
atas HP dekat kamera depan/tombol power). Banyak Xiaomi/Redmi/POCO dan
sebagian Samsung/Huawei lama punya ini; kebanyakan HP flagship baru (seri
Pixel, iPhone jelas tidak, banyak Samsung seri S terbaru) **tidak punya**.
Buka tab Archive > Infrared untuk cek otomatis -- Flipper akan bilang jelas
kalau HP kamu tidak punya IR blaster.

### Dua mode pengiriman
- **Kode NEC (hex)**: untuk remote sederhana satu-tombol (banyak kipas, TV,
  sebagian AC portable satu-fungsi). Masukkan kode hex seperti `00FFA25D`.
- **Pola Mentah**: masukkan frekuensi + daftar durasi mikrodetik persis.
  Ini cara **paling akurat** untuk AC split/rumahan modern, yang kodenya
  jauh lebih rumit dari NEC (satu sinyal membawa info suhu+mode+kecepatan
  kipas sekaligus, beda-beda tiap merk/model).

### KEJUJURAN PENTING
Saya **tidak menyertakan kode IR spesifik merk AC tertentu** (Daikin,
Panasonic, Gree, dst) di dalam aplikasi ini. Alasannya: kode-kode itu
panjang, rumit, dan beda-beda per model -- saya tidak punya cara menguji
keakuratannya di sini (tidak ada AC atau penerima IR di lingkungan
pengembangan ini), dan memberikan kode yang SALAH lebih buruk daripada
tidak memberikan sama sekali (bisa gagal diam-diam, atau mengubah
pengaturan AC jadi tidak sesuai niat).

Cara mendapatkan kode yang presisi untuk AC kamu:
1. Cari di database IR publik (misalnya proyek open-source IRremoteESP8266
   punya banyak protokol AC terdokumentasi, atau situs/App seperti
   "IRDB") dengan kata kunci merk+model AC kamu.
2. Kalau ketemu, kode itu biasanya dalam format frekuensi+daftar durasi --
   tinggal tempel ke mode **Pola Mentah** di atas.
3. Remote AC fisik kamu juga bisa "direkam" pakai HP lain yang punya
   penerima IR + aplikasi perekam IR, lalu hasil rekamannya ditempel di
   sini -- tapi ini butuh hardware penerima IR terpisah, HP pengirim
   (Flipper) tidak bisa merekam, cuma mengirim.

Kalau kamu kasih tahu saya merk & model AC/remote-nya, saya bisa bantu
cari tahu protokol yang kemungkinan besar cocok dan jelaskan cara pakainya
di mode Pola Mentah -- tapi saya tetap tidak bisa menjamin 100% akurat
tanpa alat untuk mengetesnya langsung.


## Infrared v2: Kategori Device + Protokol Sony + Kode Tersimpan

Sejak sekarang popup Infrared mendukung:
- **Kategori device**: TV, AC, Set-top Box, Kipas, Proyektor, Soundbar, Lampu,
  Lainnya -- hanya label, tidak mengubah cara kirim, tapi bikin daftar kode
  tersimpan kamu rapi.
- **Protokol Sony SIRC**, selain NEC yang sudah ada -- dipakai banyak TV,
  soundbar, dan proyektor Sony (dan sebagian merk lain yang ikut protokol
  ini). Masukkan angka Command (0-127) dan Device (0-31) -- kode-kode ini
  umum didokumentasikan publik untuk fungsi standar (power, volume, ganti
  channel/input) per merk.
- **Kode Tersimpan**: simpan kode yang sudah terbukti cocok dengan nama
  bebas (misal "AC Kamar - Power", "TV Ruang Tamu - Volume Up"), supaya
  lain kali tinggal tap sekali, tidak perlu ketik ulang. Disimpan di
  `localStorage` milik aplikasi ini sendiri di HP kamu (bukan dikirim ke
  mana pun).

Barang-barang yang BISA dikontrol lewat IR (sama seperti AC, infrastruktur
yang sama): TV, set-top box/receiver TV kabel, proyektor, kipas angin
modern, soundbar/speaker aktif, AC portable/split, lampu LED strip dengan
remote kotak kecil (bukan Bluetooth), sebagian air purifier/humidifier,
kamera DSLR/mirrorless lama. Semua pakai sinyal infrared, bukan Sub-GHz,
jadi beneran bisa lewat IR blaster HP -- tinggal cari kode yang sesuai
mereknya (lihat bagian "KEJUJURAN PENTING" di atas soal dari mana
mencarinya).


## NFC: Baca Kartu/Tag (UID, Teknologi, Uji Kunci Default, Data NDEF)

Beda dari Sub-GHz/RFID 125/iButton, fitur ini **beneran berfungsi** di HP
mana pun yang punya chip NFC (hampir semua HP menengah ke atas, termasuk
kemungkinan besar HP kamu).

### Cara pakai
Tab Archive > **NFC** > tempelkan kartu/tag ke bagian belakang HP (dekat
kamera belakang, posisi persisnya beda tiap HP). Hasilnya langsung
tampil:
- **UID** -- nomor identitas unik kartu
- **Teknologi** -- jenis chip NFC-nya (MifareClassic, MifareUltralight,
  IsoDep/smart card, NfcV, dst)
- **Status keamanan**, dengan tiga warna:
  - **Merah (lemah)**: kartu MIFARE Classic yang sektor awalnya masih
    bisa dibuka pakai **kunci default pabrik** (`FFFFFFFFFFFF`) -- ini
    BUKAN brute-force, cuma mengecek SATU kunci yang memang didokumentasikan
    publik secara luas sebagai kelemahan klasik MIFARE Classic. Banyak kartu
    akses lama di dunia nyata masih begini karena tidak pernah diganti dari
    bawaan pabrik.
  - **Hijau (oke)**: kunci default ditolak (kemungkinan sudah pakai kunci
    custom), atau tag jenis yang memang didesain terbuka untuk data publik
    (NFC Tag Type 2/Ultralight), atau smart card yang lebih aman (IsoDep,
    kemungkinan DESFire/sejenis e-KTP).
  - **Abu-abu**: jenis tag yang tidak sempat diklasifikasi.
- **Data NDEF**, kalau kartu/tag itu menyimpan teks atau link yang bisa
  dibaca langsung.
- **Riwayat scan** di sesi itu -- tempel beberapa kartu berturut-turut,
  semua hasilnya tersimpan di daftar bawah, tinggal tap buat lihat lagi.

### Kenapa ini aman dipakai buat belajar keamanan
- Cuma membaca kartu yang **kamu tempelkan sendiri** ke HP -- tidak bisa
  dipakai diam-diam ke kartu/dompet orang dari jarak jauh (jarak baca NFC
  cuma beberapa cm, dan harus benar-benar ditempelkan).
- Uji kunci default itu persis demo yang dipakai tim security fisik
  sungguhan untuk menunjukkan kenapa kartu akses lama berisiko: KALAU
  kartunya kebuka dengan kunci yang memang publik, itu artinya siapa pun
  dengan alat serupa bisa baca juga -- bukan Flipper yang menciptakan
  celahnya, cuma menunjukkan celah yang sudah ada di desain kartunya.
- Tidak ada fitur tulis/clone di sini -- murni baca & klasifikasi.

### Catatan teknis
`MainActivity.kt` memakai `NfcAdapter` + foreground dispatch bawaan
Android (API resmi, bukan trik), jadi hanya aktif memproses tag saat popup
NFC ini sedang terbuka di layar (supaya tidak diam-diam membaca kartu di
dompet kamu kapan saja aplikasi lain dibuka).


## Pemindai IoT Tanpa Password (gantiin slot Infrared)

Infrared dihapus total (HP tidak punya hardware-nya, dikonfirmasi lewat
`hasIrEmitter()` dan spesifikasi resmi). Slot itu sekarang diisi fitur yang
beneran jalan di HP APAPUN karena cuma butuh WiFi: **Pemindai IoT Tanpa
Password**.

### Konsepnya
Banyak perangkat pintar (Chromecast, smart plug/colokan pintar Tasmota,
printer jaringan) itu DEFAULT-nya memang tidak punya password sama sekali
untuk dikontrol -- siapa pun yang nyambung ke WiFi yang sama bisa langsung
mengendalikannya. Ini bukan bug yang "dibobol" -- ini desain resmi dari
pabrikannya (demi kemudahan pakai), dan ini salah satu pelajaran paling
terkenal di cybersecurity (botnet Mirai yang terkenal menyerang jutaan
perangkat IoT justru lewat celah seperti ini).

### Yang dideteksi & diuji
- **Chromecast** (port 8008): dicek lewat API resmi DIAL (dipakai app
  legal buat "cast", tanpa otentikasi). Kalau ketemu, ada tombol demo
  "Hentikan Cast" yang beneran mengirim perintah stop resmi.
- **Smart Plug Tasmota** (port 80): firmware open-source populer buat
  colokan pintar, API kontrolnya default tanpa password. Ada tombol demo
  nyala/matikan.
- **Printer jaringan** (port 9100/631): **HANYA dideteksi**, aplikasi ini
  **tidak pernah mengirim apa pun** ke printer -- supaya tidak membuang
  kertas orang lain tanpa sengaja.

### Kenapa ini aman buat edukasi keamanan
- Cuma mengandalkan port yang memang terbuka dan API yang memang publik
  -- bukan brute-force, bukan bypass otentikasi apa pun.
- Cuma jalan di WiFi yang sedang kamu/tim pakai -- tidak bisa menjangkau
  device di luar jaringan itu.
- Hasil scan langsung ngasih pelajaran konkret: kalau ketemu sesuatu, itu
  tandanya perangkat itu perlu diberi password/diamankan.

### Teknis
Dibangun di atas infrastruktur scan jaringan yang sama dengan fitur
"Lihat Perangkat Tersambung" (iterasi /24 subnet), ditambah pengecekan
port spesifik per jenis perangkat. Semua lewat `Socket` biasa di Kotlin,
tidak ada library tambahan, tidak butuh root.


## BLE Scanner: Sinyal Bluetooth di Sekitar (gantiin slot RFID 125)

RFID 125 diganti jadi **BLE Scanner**, karena Bluetooth itu hardware beneran
ada di hampir semua HP (beda dari RFID 125kHz yang butuh antena LF khusus
yang nggak ada di HP manapun).

### Cara kerja
Mendengarkan (pasif, cuma dengerin, bukan nyambung) semua siaran Bluetooth
Low Energy yang lagi dipancarkan di sekitar HP -- headset wireless, fitness
band, smart lock, speaker, dan **tracker** (AirTag, Tile, dan sejenisnya).

### Kenapa ini fitur keamanan beneran
Ini persis mekanisme yang dipakai fitur "Item Tracker Alert"/"Deteksi
Tracker yang Tidak Dikenal" yang sekarang jadi fitur bawaan iOS & Android --
buat bantu orang sadar kalau ada AirTag/tracker asing yang diem-diem
diselipkan ke tas/mobil mereka buat diikutin (stalking). Aplikasi ini
mengenali kemungkinan tracker lewat pola siaran Bluetooth yang
terdokumentasi publik (identifier resmi Apple untuk protokol Find My, yang
terdaftar di spesifikasi Bluetooth SIG), ditambah deteksi nama perangkat
yang menyebut "AirTag"/"Tile".

### Yang ditampilkan per perangkat
- Nama (kalau disiarkan) dan alamat MAC
- Kekuatan sinyal (RSSI) + perkiraan jarak kasar
- Badge merah kalau terdeteksi kemungkinan tracker, dengan catatan
  penjelasan

### Batasan jujur
- Klasifikasi "kemungkinan tracker" itu **perkiraan berdasarkan pola yang
  terdokumentasi publik**, bukan deteksi 100% pasti -- banyak tracker modern
  mengacak alamat MAC-nya secara berkala, dan tidak semua merk tracker
  punya pola yang sama terdokumentasi.
- Perkiraan jarak dari RSSI itu KASAR BANGET (sinyal Bluetooth gampang
  terpengaruh tembok, badan orang, dll) -- jangan dianggap presisi meteran.
- Ini scan PASIF, cuma dengerin siaran publik yang memang disiarkan ke
  udara terbuka -- tidak menyambung, tidak meretas, tidak mengubah apa pun
  di perangkat manapun.


## Audit Keamanan HP (baris baru di Archive, sebelum "Deleted")

Ini daftar yang PERTAMA dicek kalau HP jatuh ke tangan orang lain (dicolong,
ketinggalan, dipinjam sebentar) -- dibikin jadi fitur self-check biar kamu
yang beresin duluan sebelum itu kejadian.

### Yang diperiksa
1. **Kunci layar** -- PIN/pola/password/biometrik aktif apa nggak.
2. **USB Debugging (ADB)** -- kalau aktif & HP dicolok ke komputer tak
   dipercaya, data bisa diakses tanpa kunci layar.
3. **Opsi Developer** -- buka akses ke pengaturan lanjutan.
4. **Sumber Tak Dikenal** -- izin install APK dari luar Play Store, jalur
   paling umum masuknya malware.
5. **Patch Keamanan Android** -- kapan terakhir update, ditandai kalau
   sudah lebih dari 6 bulan.
6. **Aplikasi dengan akses Accessibility** -- izin paling kuat di Android
   (bisa baca layar & simulasi ketukan), paling sering disalahgunakan
   spyware/stalkerware. Kalau ada yang tidak kamu kenal di daftar ini,
   itu tanda bahaya nyata.
7. **Aplikasi dengan hak Device Admin** -- bisa kunci/hapus HP dari jarak
   jauh, biasanya dipakai MDM kantor resmi -- pastikan yang aktif memang
   yang kamu pasang sendiri.

### Kenapa ini relevan ke permintaan "fitur kayak hacker"
Poin 6 dan 7 itu justru kebalikan dari permintaan "app Pixel" yang saya
tolak sebelumnya (app nyusup pakai Accessibility buat kontrol jarak jauh) --
fitur ini yang mendeteksi kalau ada SESUATU seperti itu sudah terpasang di
HP kamu sendiri. Ini mengubah "cara kerja hacker" jadi alat deteksi,
bukan alat serang -- persis posisi yang app ini pegang dari awal.

Semua lewat API baca-saja resmi Android, tidak ada yang diubah oleh
pemeriksaan ini.

## Fitur Game Offline (Edit Koin) -- catatan lama, lihat bagian di atas
Tekan **Game Offline** di tab Archive untuk melihat daftar game yang terpasang di HP
(dideteksi dari kategori "Game" di sistem Android). Pilih satu game untuk membukanya;
sebuah bubble oranye lalu muncul melayang di layar dan bisa diseret ke mana saja.

Menekan bubble itu membuka **GameGuardian** atau **VirtualXposed** kalau salah satunya
sudah terpasang di HP -- aplikasi ini TIDAK membaca atau mengubah memori game sendiri,
hanya membuka alat yang sudah ada. Kalau belum terpasang, muncul pesan dengan tautan
unduhan resminya.

Catatan penting:
- Alat semacam ini pada dasarnya butuh **akses root** (atau, untuk sebagian game lama,
  ruang virtual seperti VirtualXposed) supaya benar-benar bisa mengubah angka di dalam
  game. Tanpa itu, GameGuardian/VirtualXposed akan tetap terbuka tapi mungkin tidak bisa
  mengedit apa-apa -- itu batasan dari aplikasi pihak ketiga tersebut, bukan dari aplikasi
  Flipper ini.
- Pakai hanya pada game **offline milikmu sendiri**. Banyak game, terutama yang online,
  mendeteksi dan memblokir alat seperti ini (anti-cheat / bisa kena banned).
- Daftar game diambil dari kategori aplikasi bawaan Android, jadi game yang tidak ditandai
  "Game" oleh developernya bisa saja tidak muncul di daftar.

## Mengubah tampilan
Seluruh tampilan ada di `app/src/main/assets/index.html`. Ubah file itu lalu Run lagi.

## Struktur
- `app/src/main/assets/index.html` : tampilan
- `app/src/main/java/com/flipper/app/MainActivity.kt` : WebView + scan WiFi
- `app/src/main/AndroidManifest.xml` : izin aplikasi
- `app/build.gradle.kts`, `build.gradle`, `settings.gradle` : konfigurasi Gradle
- `gradle/wrapper/gradle-wrapper.properties` : versi Gradle (8.7)
- `gradlew`, `gradlew.bat` : pengganti wrapper (mengunduh Gradle 8.7 otomatis bila belum ada)

Catatan: Android membatasi scan WiFi (sekitar 4 kali per 2 menit untuk aplikasi di
latar depan). Kalau terkena batas, aplikasi menampilkan hasil scan terakhir.
