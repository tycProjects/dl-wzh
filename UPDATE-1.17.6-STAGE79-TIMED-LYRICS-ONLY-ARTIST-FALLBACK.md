# UPDATE 1.17.6 - STAGE 79 - LIRIK HANYA BERTIMESTAMP, FALLBACK NAMA ARTIS

- Hanya lirik yang punya timestamp asli yang ditampilkan. Lirik polos tanpa timestamp tidak pernah tampil.
- `LyricsRepository.importLyricsFile`: file Lyricsfile yang hanya berisi `plain` (tanpa `lines` bertimestamp) kini ditolak; fungsi `spread()` yang membuat timestamp perkiraan dihapus.
- `ExpandedIsland`: bila tidak ada lirik bertimestamp yang bisa ditampilkan (lagu tanpa lirik, intro sebelum timestamp pertama, atau baris kosong), baris lirik menampilkan nama artis, bukan "♪ judul". Tidak ada teks lirik yang dipaksakan.
- Tes baru: `plainOnlyLyricsfileIsRejectedInsteadOfSpreadOverTheSong`.
