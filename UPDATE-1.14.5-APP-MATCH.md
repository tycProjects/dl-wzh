# Update 1.14.5 — Pencocokan Nama Aplikasi

- Pencocokan nama aplikasi sekarang lewat AppMatcher: nama persis (abaikan huruf dan spasi), nama di dalam label, label di dalam nama, lalu alias Indonesia (catatan → Samsung Notes/Keep, kamera → Camera, galeri → Gallery, dan lainnya).
- Jika open_app gagal, asisten diminta memanggil list_apps lalu mencoba lagi dengan nama yang tepat.
- Asisten tidak boleh mengklaim tugas selesai kecuali hasil langkah mengonfirmasinya.

Test: AppMatcherTest. Belum dijalankan (tidak ada toolchain di sandbox).
