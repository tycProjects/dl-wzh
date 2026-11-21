# Stage 109 (basis stage108) — belum dikompilasi, belum diuji di HP

1. Indikator samping: cakram ungu-cyan polos + lonceng. Tanpa glow, tanpa animasi, tidak bisa ditekan. Jam "breath" (delay loop) dibuang.
2. Stack dihapus: IslandBus.expandWhatsAppStack/setWhatsAppStackHeight, whatsAppStackHeightDp, UI tumpukan di ExpandedIsland, anggaran jendela stack di IslandRoot dan service, GlassTuning.stack*, slider stack di HomeScreen. Field settings stackGap/stackInset dibiarkan. Tap capsule notifikasi membuka app pengirim; swipe up menyimpan banner; IslandBus.expand() mengabaikan WhatsApp.
3. Event, bukan polling: IslandBus.replyOpen kini StateFlow (replyOpenState); timer banner menunggu event, bukan delay(100). Sisa timer yang sengaja dipertahankan: posisi lagu/lirik saat diputar, bar capsule saat musik, dan cek hati 2,5 dtk hanya saat island expanded.
4. Default cutout iPhone: 170 x 32 dp (CutoutProfile.IPHONE/AUTO/CUSTOM, iphoneNotchWidth default 170).
5. Animasi tutup: capsule kini sudah opak penuh di bawah panel sebelum panel memudar (alpha capsule saat tutup: penuh di progress 0,08). capsuleFading ambang 0,35. Dugaan penyebab "ketinggalan lalu lompat".
6. Backdrop artwork dibatasi 96 px sisi terpanjang (RGB_565), diskalakan bilinear = buram halus tanpa blur pass. Cover depan tidak terpengaruh.
7. Panel AI tidak menutup sendiri: notifikasi tidak menggantikan kartu AI (hanya antre); event systemui hanya menutup bila kelas jendela Recents; ACTION_OUTSIDE diabaikan saat agent berjalan (AiSession.agentBusy).
8. Agent berhenti: STOP RULES di prompt sistem; tiap langkah menyertakan tujuan asli + cek keadaan akhir; AgentLoopPolicy.shouldChallengeStop tidak lagi mendorong lanjut bila model berhenti dengan kalimat biasa. Tes baru di AgentPersistenceTest.
