package com.android.island.ai

/**
 * Deterministic recovery guidance shared by every model provider. It never executes commands;
 * it only constrains the next plan. Device actions remain validated by AiActionProtocol,
 * ClickGuard, ConfirmPolicy and ShellPolicy before execution.
 */
object AgentRecoveryPolicy {
    /** How many times the intent guard may veto an action before the loop stops instead of re-planning. */
    const val MAX_GUARD_REJECTIONS = 2

    /**
     * Sent when the intent guard vetoed the proposed action. It tells the model what was refused and why it is
     * not a dead end, so the task continues with an action that directly serves the user's own words.
     */
    fun rejectedActionPrompt(rejected: String, userGoal: String, screenBlock: String, attempt: Int): String = buildString {
        append("AKSI DITOLAK oleh pemeriksa niat: ").append(rejected.take(200)).append(". ")
        append("Aksi itu tidak terhubung langsung dengan perintah asli pengguna: \"").append(userGoal.take(240)).append("\". ")
        append("Jangan mengulanginya. Pilih SATU aksi lain yang langsung melayani perintah itu: navigasi (app_home, back, click_index), ")
        append("ketuk ikon cari, atau type dengan teks yang memang disebut pengguna. ")
        append("Jika tujuan sudah terbukti tercapai di layar, set done true dengan actions kosong. ")
        append("Penolakan ke-").append(attempt).append(" dari ").append(MAX_GUARD_REJECTIONS).append(".\n\n")
        append(if (screenBlock.isBlank()) "Layar belum terbaca; panggil read_screen dulu." else "Layar sekarang:\n$screenBlock")
        append("\n\nKeluarkan tepat satu objek JSON sesuai protokol dengan satu aksi berikutnya.")
    }

    /** A failed action must not be executed again verbatim during the same user request. */
    fun shouldBlockReplay(actionKey: String, failedActionKeys: Set<String>): Boolean =
        actionKey.isNotBlank() && actionKey in failedActionKeys

    /** A successfully completed action must not be executed again in the same user task. */
    fun shouldBlockCompletedReplay(actionKey: String, completedActionKeys: Set<String>): Boolean =
        actionKey.isNotBlank() && actionKey in completedActionKeys

    /**
     * How many times a completed-action replay may be pushed back with escalating stage guidance
     * before the turn is ended with a progress report. STAGE 72: was 2 (stop) — now 3 guided
     * retries first, because a replay usually means the model is stuck on ONE affordance while
     * the remaining stages are still reachable. The user asked for "diarahkan melanjutkan ke
     * tahap berikutnya, bukan dibatalkan".
     */
    const val MAX_COMPLETED_REPLAY_RETRIES = 3

    /**
     * STAGE 72: escalating guidance for a completed-action replay. Instead of ending the turn,
     * the model receives the stage ladder of the original request, what is already done, and a
     * hard prohibition on touching the same control again. It must pick the NEXT stage's action.
     */
    fun completedReplayPrompt(
        completedAction: String,
        attempt: Int,
        userGoal: String,
        screenBlock: String
    ): String = buildString {
        append("Kamu baru saja mengusulkan ulang aksi yang SUDAH BERHASIL: ")
        append(completedAction.take(200)).append(". TUGAS BELUM SELESAI, jadi jangan berhenti dan jangan mengulanginya. ")
        append("Bergeraklah ke TAHAP BERIKUTNYA dari perintah asli: \"").append(userGoal.take(240)).append("\". ")
        append("Tahapan standar perintah perangkat: (1) buka aplikasi, (2) buka UI pencarian di aplikasi ")
        append("(kolom cari terlihat ATAU ikon kaca pembesar diketuk sekali), (3) ketik teks yang diminta, ")
        append("(4) tekan Enter/kirim, (5) bila diminta, buka hasil pertama. ")
        append("Cocokkan dengan riwayat: tahap yang selesai TIDAK boleh diulang; kerjakan tahap berikutnya yang belum ada buktinya di layar. ")
        append("Aturan anti-macet: jangan ketuk label yang sama dua kali; kalau layar tidak berubah setelah aksi terakhir, ")
        append("itu tanda target salah — pilih kandidat BERBEDA (kolom cari dengan hint, ikon kaca pembesar kanan atas, ")
        append("tab Cari/Telusuri di bar bawah), atau tekan back lalu coba rute lain. ")
        append("Percobaan arahan ke-").append(attempt).append(" dari ").append(MAX_COMPLETED_REPLAY_RETRIES)
        append(".\n\n")
        append(if (screenBlock.isBlank()) "Layar belum terbaca; panggil read_screen dulu." else "Layar sekarang:\n$screenBlock")
        append("\n\nKeluarkan tepat satu objek JSON sesuai protokol dengan SATU aksi untuk tahap berikutnya.")
    }

    /**
     * STAGE 72: the reply when all completed-replay guidance is exhausted. It is a progress
     * report, not a bare cancellation: what finished, where it stopped, and what the next step
     * would be — so sending "lanjut" resumes from the right stage.
     */
    fun completedReplayReport(userGoal: String, stepLines: List<String>): String = buildString {
        append("Berhenti karena aksi yang sudah berhasil terus diusulkan ulang. ")
        append("Yang sudah dikerjakan: ")
        append(if (stepLines.isEmpty()) "belum ada langkah yang terkonfirmasi" else stepLines.joinToString("; ").take(400))
        append(". Perintahmu: \"").append(userGoal.take(200)).append("\". ")
        append("Sisa tugas belum selesai — kirim \"lanjut\" dan saya coba rute berbeda dari tahap berikutnya.")
    }

    /**
     * Sent when the model stops without proof that the goal is reached. It carries the fresh screen
     * and an explicit ladder of routes, so "I cannot press Enter" turns into another attempt.
     */
    fun persistencePrompt(challengeNo: Int, screenBlock: String, shizukuAvailable: Boolean): String = buildString {
        append("JANGAN BERHENTI: tujuan pengguna belum terbukti selesai, dan kamu belum mencoba semua jalur. ")
        append("Menyerahkan pekerjaan ke pengguna (mis. \"silakan tekan sendiri\") BUKAN hasil yang diterima selama masih ada jalur lain. ")
        append("Pilih SATU jalur yang belum dicoba, urut dari yang paling kecil risikonya: ")
        append("(0) bila ikon atau kolom cari tidak terlihat, kamu kemungkinan di halaman dalam aplikasi: app_home untuk kembali ke halaman utama, lalu cari ikon cari; ")
        append("(1) press_enter untuk tombol Enter/Cari di keyboard; ")
        append("(2) click_index pada elemen bernomor di daftar \"Elemen interaktif\" (ikon kaca pembesar, tombol kirim, tombol Cari); ")
        append("(3) tap dengan x,y dari daftar yang sama jika elemen tidak bisa diketuk lewat click_index; ")
        append("(4) click_index pada kolom teks agar fokus, lalu type, lalu press_enter; ")
        append("(5) scroll atau scroll_to jika targetnya belum terlihat, lalu baca ulang; ")
        append("(6) back lalu ulangi dari layar sebelumnya, atau open_url/search di browser untuk tujuan yang sama. ")
        if (shizukuAvailable) append("Shizuku siap: press_enter otomatis memakai keyevent bila Accessibility gagal. ")
        append("Jangan mengulang aksi yang sudah gagal persis sama. Jangan mengarang hasil. ")
        append("Hanya set done true bila layar sekarang membuktikan tujuan tercapai; jika benar-benar buntu, ")
        append("sebutkan jalur apa saja yang sudah dicoba dan hambatan nyatanya. ")
        append("Percobaan dorongan ke-").append(challengeNo).append(" dari ").append(AgentLoopPolicy.MAX_CHALLENGES).append(".\n\n")
        append(if (screenBlock.isBlank()) "Layar belum terbaca; panggil read_screen dulu." else "Layar sekarang:\n$screenBlock")
        append("\n\nKeluarkan tepat satu objek JSON sesuai protokol dengan satu aksi berikutnya.")
    }

    fun alternativePrompt(
        failedAction: String,
        failureCount: Int,
        shizukuAvailable: Boolean
    ): String = buildString {
        append("PEMULIHAN WAJIB: aksi terakhir gagal atau menghasilkan bukti kegagalan. ")
        append("Jangan mengeluarkan ulang aksi yang sama: ").append(failedAction.take(240)).append(". ")
        append("Jangan berhenti hanya karena satu percobaan gagal. Teruskan tujuan pengguna dengan mencoba hingga empat jalur yang berbeda selama masih ada alternatif yang aman dan didukung. ")
        append("Pilih satu alternatif yang benar-benar berbeda dan paling kecil risikonya. ")
        append("Alat UI yang tersedia: click_index, tap(x,y), type, press_enter, scroll, back, wait. ")
        append("Urutan prioritas: (1) baca ulang layar/status untuk mengetahui kondisi nyata, ")
        append("(2) gunakan aksi Accessibility/UI yang sesuai dengan elemen yang terlihat, ")
        append("(3) gunakan intent Android atau aksi perangkat bawaan yang tersedia, ")
        append("(4) gunakan Shizuku hanya untuk operasi Android yang memang didukung dan relevan. ")
        if (shizukuAvailable) {
            append("Shizuku terdeteksi siap; shell tetap harus berupa perintah spesifik dan aman. ")
        } else {
            append("Shizuku tidak tersedia; jangan membuat seolah-olah perintah shell dapat berjalan. ")
        }
        append("Jangan mengarang hasil, jangan mengulang tindakan yang sudah sukses, ")
        append("jangan mengakali izin/konfirmasi, dan jangan memakai perintah destruktif. ")
        append("Jika alternatif memerlukan izin, minta konfirmasi. Jangan mengulang kegagalan sebagai alasan untuk mengakhiri tugas. Jika tidak ada alternatif yang aman dan didukung, ")
        append("jelaskan hambatan sebenarnya dan apa yang perlu diaktifkan pengguna. ")
        append("Keluarkan tepat satu objek JSON sesuai protokol. Percobaan alternatif ke-")
        append((failureCount + 1).coerceAtMost(4)).append(".")
    }
}
