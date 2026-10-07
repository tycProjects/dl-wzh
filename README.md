# Bubble AI

Chat AI mengambang di atas semua aplikasi. Java + Gradle.

## Fitur
- Dashboard 3 tab: **Beranda** (status, toggle bubble, statistik, tes koneksi, percakapan terakhir), **Akun** (nama, provider, model, API key tersamar), **Pengaturan**.
- Bubble **tidak bisa digeser** — hanya ketuk untuk buka/tutup chat. Posisi kiri/kanan diatur di Pengaturan.
- Bubble memakai **Accessibility Service** (`TYPE_ACCESSIBILITY_OVERLAY`), bukan izin "Tampil di atas aplikasi lain". Layanan ini tidak membaca isi layar.
- Keyboard muncul otomatis saat chat dibuka.
- Mendukung 3 format API (otomatis dari URL): Anthropic `/v1/messages`, OpenAI `/v1/responses`, OpenAI `/chat/completions`.
- Riwayat chat tersimpan lokal, ada instruksi AI (system prompt) opsional.

## Cara pakai
1. Pasang APK → buka app → tab **Pengaturan**: isi API key, pilih endpoint (chip), isi model → Simpan.
2. Tab **Beranda** → nyalakan Floating Bubble → aktifkan layanan "Bubble AI" di Aksesibilitas.
3. Android 13+ (APK dari luar Play Store): jika opsi abu-abu, buka Info Aplikasi → ⋮ → "Izinkan setelan terbatas", lalu ulangi.
4. Ketuk bubble untuk chat; ketuk lagi / ✕ / Back untuk menutup.

## Zip untuk builder
Zip isi project (`app/`, `build.gradle`, `settings.gradle`).
