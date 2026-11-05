# Nexus Launcher

Aplikasi Android native (Kotlin) untuk membantu menjalankan game di HP low-end.

## Fitur

- **Daftar Game** – Menampilkan game yang terinstall di perangkat
- **Profil & Statistik** – Side drawer menampilkan waktu bermain & recent games
- **In-Game Overlay** – Tombol slide di samping layar saat game dibuka
  - FPS counter (simulasi)
  - Latency & suhu
  - Tools: Clear memory, Network optimasi, Screenshot, Screen record, Aim assist, dll.

## Cara Build

1. Buka folder `NexusLauncher` di **Android Studio** (Giraffe / Hedgehog / lebih baru)
2. Tunggu Gradle sync selesai
3. Connect HP atau pakai Emulator
4. Klik **Run** (tombol hijau)

### Permission yang dibutuhkan

- **Display over other apps** (SYSTEM_ALERT_WINDOW) → untuk overlay menu
- Usage Access (opsional, untuk real playtime)

## Struktur Project

```
app/src/main/
├── java/com/nexus/launcher/
│   ├── MainActivity.kt
│   ├── adapter/GameIconAdapter.kt
│   ├── model/GameApp.kt
│   ├── service/OverlayService.kt
│   └── utils/GameUtils.kt
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── drawer_profile.xml
    │   ├── overlay_menu.xml
    │   └── ...
    └── drawable/   (icons & backgrounds)
```

## Catatan

- Deteksi game menggunakan heuristic (nama package + keyword). Bisa ditambah manual.
- FPS / suhu / latency di overlay masih **simulasi**. Untuk real data butuh native module atau root.
- Overlay muncul setelah kamu tekan **Buka** pada sebuah game (dan sudah kasih izin overlay).

Dibuat sesuai UI referensi yang diberikan.
