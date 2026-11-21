# Android Island 1.4.71

## Layout kartu media mengikuti referensi Liquid Glass
- Baris atas: cover (rounded, bingkai putih tipis) di kiri, judul di kanan atas, lalu tombol favorit, sebelumnya, putar/jeda (cakram putih), dan berikutnya di bawah judul.
- Baris bawah: waktu, bar progres, waktu dalam satu baris. Lirik tersinkron (atau nama artis bila lirik dimatikan) berada di antara keduanya.
- Radius sudut kaca kini sama dengan radius klip panel ("Lekukan sudut"), jadi rim kaca mengikuti sudut yang sebenarnya.
- Kaca mengisi penuh tinggi panel. Tinggi minimum kartu media dihitung otomatis (`mediaCardMinHeightDp`), jadi tidak ada lagi konten yang terpotong. Pengaturan "Tinggi minimum" hanya bisa menambah tinggi, dan sisa ruang dibagi rata di dalam kartu.
- Efek kaca (ringan, tetap statis dan di-cache): kolam cahaya di sudut kiri atas dan rim dalam (bevel) yang tipis, sehingga kaca terlihat tebal. Tidak ada blur baru.
- Gelombang dua lapis di tepi bawah kartu saat musik diputar (seperti widget gelap pada referensi). Amplitudo naik halus saat play dan tenang saat pause. Mati di mode hemat daya, berhenti saat layar mati.

## Animasi: dari detail kecil sampai transisi besar
Besar
- Buka/tutup: cahaya (bloom) berwarna aksen cover tumbuh dari posisi kapsul, memuncak di tengah transisi, dan hilang di kedua ujung. Kaca dan isi muncul dengan kurva smoothstep, bukan lagi rampa linear yang curam.
- Isi kartu masuk berurutan (cascade), satu per satu: cover, judul, empat tombol (masing-masing), lirik, bar progres. Dimulai 70 ms setelah kaca mulai membuka supaya kaca terlihat membuka dulu.
- Geser untuk menutup: isi kartu ikut bergeser dan memudar di dalam kaca, kartu sedikit memudar dan mengecil mengikuti jari.

Sedang
- Ganti judul: judul baru naik masuk, judul lama terangkat keluar.
- Ganti lirik: baris baru naik masuk sambil sedikit membesar, baris lama terangkat.
- Ganti cover: cross-fade lembut (sebelumnya langsung berganti).
- Ikon putar/jeda: berganti dengan skala + fade, ditambah cincin yang memancar keluar setiap kali status berubah.
- Ikon favorit: berganti dengan pop. Saat menjadi merah, ada cincin dan enam percikan kecil.

Kecil
- Semua tombol (favorit, sebelumnya, berikutnya) punya halo bundar yang mekar saat ditekan dan memudar pelan setelah dilepas.
- Bar progres: ketukan seek membuat thumb berdenyut. Bagian terisi punya cahaya lembut saat musik diputar atau saat di-scrub.
- Kapsul: menunduk sedikit di bawah jari lalu memantul kembali. Ikon di dalam kapsul mengikuti geseran (rubber band, maks 6 dp) saat ganti lagu, lalu memantul kembali. Kapsul itu sendiri tidak bergerak.
- Indikator musik di kapsul: gerak berbasis waktu (halus, bukan lagi lompatan 10 kali per detik). Saat jeda, batang mengecil perlahan menjadi tiga pil kecil, tidak lagi cross-fade ke titik.
- Banner WhatsApp ikut mendapat cascade dan halo tombol baru karena memakai helper yang sama.

## Berkas
- Baru: `LiquidWave.kt` (gelombang tepi bawah dan bloom transisi).
- Diubah: `MicroMotion.kt` (staggerIn lebih halus dengan skala, `pressHalo`, parameter `haloColor` pada `PressIconButton`), `LiquidGlass.kt`, `ExpandedIsland.kt` (MediaExpanded dan ProgressSection ditulis ulang), `CompactIsland.kt`, `IslandRoot.kt`.
- Tidak ada pengaturan baru, jadi tidak ada yang perlu dihubungkan di layar Pengaturan.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error. Setelah itu coba di HP: buka/tutup island, ganti lagu, tekan tombol, geser untuk menutup, dan seek.
