package com.cybervshack.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlin.math.min

class MainActivity : FragmentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) // blok screenshot
        val store = SecureStore(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF00FF9C))) {
                Surface(Modifier.fillMaxSize()) { App(this, store) }
            }
        }
    }
}

data class Rival(val name: String, val sec: Int)
val rivals = listOf(Rival("Zer0Cool", 1), Rival("NullByte", 2), Rival("ShadowRoot", 3), Rival("IronWall", 4))
val groups = listOf("Blue Team ID", "Red Team ID", "CTF Pemula", "Bug Hunters")

@Composable
fun App(act: FragmentActivity, st: SecureStore) {
    var user by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("home") }
    val ai = remember { AiHelper(st) }
    if (user == null) { LoginScreen(act, st) { user = it }; return }
    Scaffold(bottomBar = {
        NavigationBar {
            listOf("home" to "Beranda", "match" to "Match", "code" to "Kode", "learn" to "Akademi", "group" to "Grup", "profile" to "Profil").forEach { (k, l) ->
                NavigationBarItem(selected = tab == k, onClick = { tab = k }, icon = {}, label = { Text(l) })
            }
        }
    }) { p ->
        Box(Modifier.padding(p).padding(12.dp).verticalScroll(rememberScrollState())) {
            when (tab) {
                "home" -> Home(user!!, st, ai) { tab = it }
                "learn" -> AcademyScreen(st)
                "match" -> MatchScreen(st, ai)
                "code" -> CodeScreen(st)
                "group" -> GroupScreen(st)
                else -> ProfileScreen(act, user!!, st)
            }
        }
    }
}

@Composable
fun LoginScreen(act: FragmentActivity, st: SecureStore, onOk: (String) -> Unit) {
    var u by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }; var msg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("CYBER vs HACKER", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(if (st.get("user") == null) "Buat akun baru (user ≥3, sandi ≥6)" else "Masuk")
        OutlinedTextField(u, { u = it }, label = { Text("Username") }, singleLine = true)
        OutlinedTextField(p, { p = it }, label = { Text("Sandi") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Button(onClick = { if (Auth.loginOrRegister(st, u, p)) onOk(u) else msg = "Data salah / tidak valid" }) { Text("Masuk / Daftar") }
        if (st.get("bio") == "1" && st.get("user") != null)
            OutlinedButton(onClick = { Bio.prompt(act) { onOk(st.get("user")!!) } }) { Text("Masuk dengan sidik jari") }
        Text(msg, color = Color.Red)
    }
}

@Composable
fun Home(user: String, st: SecureStore, ai: AiHelper, navigate: (String) -> Unit) {
    // Backend belum dikonfigurasi untuk memberikan nonce sekali pakai.
    // Jangan membuat nonce di client lalu menganggapnya sebagai verifikasi keamanan.
    val integ = "backend belum dikonfigurasi"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Halo, $user", style = MaterialTheme.typography.titleLarge)
        Text("Menang ${st.get("wins") ?: "0"} • Kalah ${st.get("losses") ?: "0"}")
        Text("Jatah AI hari ini: ${ai.remaining()}/3 (reset 05.00)")
        Text("Play Integrity: $integ")
        Text("Buka tab Match untuk bertanding. Spin menentukan kamu Hacker atau Cyber.")
        Divider()
        Text("MISI HARIAN", style = MaterialTheme.typography.titleMedium)
        val wins = st.get("wins")?.toIntOrNull() ?: 0
        val lessons = st.get("lessons_done")?.toIntOrNull() ?: 0
        MissionRow("Selesaikan 1 pertandingan", (st.get("matches_done")?.toIntOrNull() ?: 0) >= 1)
        MissionRow("Pelajari 1 modul akademi", lessons >= 1)
        MissionRow("Menangkan 3 ronde", wins >= 3)
        Text("Progres: ${listOf((st.get("matches_done")?.toIntOrNull() ?: 0) >= 1, lessons >= 1, wins >= 3).count { it }}/3")
        Button(onClick = { navigate("learn") }, modifier = Modifier.fillMaxWidth()) { Text("Buka Akademi Keamanan") }
        Text("PENCAPAIAN", style = MaterialTheme.typography.titleMedium)
        AchievementRow("Pemula", "Buat akun dan masuk", st.get("user") != null)
        AchievementRow("Analis", "Selesaikan pelajaran pertama", lessons >= 1)
        AchievementRow("Pemenang", "Menangkan 3 ronde", wins >= 3)
    }
}

@Composable
fun LoadoutPicker(role: Roll, sel: List<Module>, onChange: (List<Module>) -> Unit) {
    val used = sel.sumOf { it.cost }
    Text("Budget $used/${Catalog.BUDGET} • Power ${Engine.power(sel)}")
    Catalog.pool(role).forEach { mo ->
        val on = mo in sel
        Row(Modifier.fillMaxWidth().clickable {
            if (on) onChange(sel - mo) else if (used + mo.cost <= Catalog.BUDGET) onChange(sel + mo)
        }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(on, null); Text("${mo.name} (biaya ${mo.cost}, power ${mo.power})")
        }
    }
}

@Composable
fun CodeScreen(st: SecureStore) {
    var role by remember { mutableStateOf(Roll.CYBER) }
    var sel by remember { mutableStateOf(Catalog.load(st, Roll.CYBER)) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Pembuatan kode ${if (role == Roll.CYBER) "Cyber (pertahanan)" else "Hacker (serangan)"}", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Roll.values().forEach { r -> FilterChip(role == r, { role = r; sel = Catalog.load(st, r) }, { Text(r.name) }) }
        }
        LoadoutPicker(role, sel) { sel = it }
        Button(onClick = { st.put("preset_$role", sel.joinToString(",") { it.name }) }) { Text("Simpan preset") }
    }
}

@Composable
fun ChatBox(chat: MutableList<String>) {
    var t by remember { mutableStateOf("") }
    Text("Chat tim", style = MaterialTheme.typography.titleSmall)
    chat.takeLast(5).forEach { Text(it) }
    Row { OutlinedTextField(t, { t = it }, singleLine = true, modifier = Modifier.weight(1f))
        Button(onClick = { if (t.isNotBlank()) { chat += "Kamu: $t"; chat += "Bot${(1..3).random()}: siap, ikut strategimu!"; t = "" } }) { Text("Kirim") } }
}

@Composable
fun MatchScreen(st: SecureStore, ai: AiHelper) {
    var m by remember { mutableStateOf<Match?>(null) }
    var phase by remember { mutableStateOf("mode") }
    var roll by remember { mutableStateOf(Roll.HACK) }
    var sel by remember { mutableStateOf(listOf<Module>()) }
    var hint by remember { mutableStateOf("") }
    var elapsed by remember { mutableStateOf(0.0) }
    var breach by remember { mutableStateOf(0.0) }
    var won by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(1) }
    val chat = remember { mutableStateListOf<String>() }
    val speed = (st.get("speed") ?: "1").toInt()

    LaunchedEffect(phase) {
        if (phase == "run") {
            val end = min(breach, Engine.LIMIT.toDouble())
            while (elapsed < end) { delay(1000); elapsed += speed }
            won = (roll == Roll.HACK) == Engine.hackerWins(breach)
            m!!.record(won)
            if (m!!.finished()) st.put("matches_done", ((st.get("matches_done")?.toIntOrNull() ?: 0) + 1).toString())
            val k = if (won) "wins" else "losses"; st.put(k, ((st.get(k)?.toIntOrNull() ?: 0) + 1).toString())
            phase = "result"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (phase) {
            "mode" -> {
                Text("Pilih mode", style = MaterialTheme.typography.titleMedium)
                listOf(1, 2, 4).forEach { n -> Button(onClick = { size = n; m = Match(n); chat.clear(); phase = "spin" }) { Text("${n}v$n") } }
            }
            "spin" -> {
                Text("Skor ${m!!.userScore}-${m!!.oppScore} • Ronde ${m!!.round + 1}" + if (m!!.tie()) " (PENENTU)" else "")
                Button(onClick = { roll = spin(); sel = Catalog.load(st, roll); hint = ""; phase = "build" }) { Text("SPIN") }
            }
            "build" -> {
                Text("Roll kamu: $roll", style = MaterialTheme.typography.titleMedium)
                LoadoutPicker(roll, sel) { sel = it }
                OutlinedButton(onClick = { hint = ai.use(roll, sel) ?: "Jatah AI habis. Reset jam 05.00" }) { Text("Minta bantuan AI (${ai.remaining()}/3)") }
                if (hint.isNotEmpty()) Text(hint)
                if (size > 1) ChatBox(chat)
                Button(onClick = {
                    val mine = (listOf(Engine.power(sel)) + (1 until size).map { Engine.power(Catalog.randomLoadout(roll)) }).average()
                    val opp = (1..size).map { Engine.power(Catalog.randomLoadout(Catalog.other(roll))) }.average()
                    val atk = if (roll == Roll.HACK) mine else opp; val def = if (roll == Roll.HACK) opp else mine
                    breach = Engine.breachSeconds(atk.toInt(), def.toInt()); elapsed = 0.0; phase = "run"
                }) { Text("MULAI") }
            }
            "run" -> {
                val left = (Engine.LIMIT - elapsed).coerceAtLeast(0.0).toInt()
                Text("Waktu: %d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.headlineMedium)
                Text("Penetrasi hacker")
                LinearProgressIndicator(progress = { min(1f, (elapsed / breach).toFloat()) }, modifier = Modifier.fillMaxWidth())
                if (size > 1) ChatBox(chat)
            }
            else -> {
                Text(if (won) "Kamu menang ronde ini!" else "Kamu kalah ronde ini.", style = MaterialTheme.typography.titleLarge)
                Text("Skor ${m!!.userScore}-${m!!.oppScore}")
                if (m!!.finished()) {
                    Text(if (m!!.userScore > m!!.oppScore) "🏆 MATCH MENANG" else "MATCH KALAH")
                    Button(onClick = { phase = "mode" }) { Text("Menu") }
                } else Button(onClick = { phase = "spin" }) { Text(if (m!!.tie()) "Seri! Ronde penentu" else "Ronde berikutnya") }
            }
        }
    }
}

@Composable
fun GroupScreen(st: SecureStore) {
    var tick by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Grup", style = MaterialTheme.typography.titleLarge)
        groups.forEach { g ->
            val j = g in st.csv("groups").also { tick }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(g); Button(onClick = { st.toggleCsv("groups", g); tick++ }) { Text(if (j) "Keluar" else "Gabung") }
            }
        }
        Text("Chat grup realtime butuh backend (lihat README).")
    }
}

@Composable
fun ProfileScreen(act: FragmentActivity, user: String, st: SecureStore) {
    var sub by remember { mutableStateOf("main") }
    var tick by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (sub) {
            "main" -> {
                Text("Profil: $user", style = MaterialTheme.typography.titleLarge)
                Text("Followers: ${(user.length * 7) % 40 + 3} • Mengikuti: ${st.csv("follow").size}")
                Button(onClick = { sub = "sec" }) { Text("Keamanan akun") }
                Button(onClick = { sub = "hack" }) { Text("Hack akun orang") }
                Button(onClick = { sub = "follow" }) { Text("Followers / ikuti") }
                Button(onClick = { sub = "set" }) { Text("Pengaturan") }
            }
            "sec" -> {
                Text("Keamanan akun (level ${secLevel(st)}/4)", style = MaterialTheme.typography.titleMedium)
                listOf("sec_pin" to "PIN tambahan", "bio" to "Sidik jari", "sec_2fa" to "Kode 2FA", "sec_lock" to "Kunci setelah 3x gagal").forEach { (k, l) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(st.get(k) == "1" && tick >= 0, { on ->
                            if (k == "bio" && on) Bio.prompt(act) { st.put(k, "1"); tick++ } else { st.put(k, if (on) "1" else "0"); tick++ }
                        }); Text(" $l")
                    }
                }
                Text("Makin tinggi level, makin sulit akunmu di-hack pemain lain.")
                OutlinedButton(onClick = { sub = "main" }) { Text("Kembali") }
            }
            "hack" -> {
                var log by remember { mutableStateOf("") }
                Text("Hack akun (simulasi, akun bot)", style = MaterialTheme.typography.titleMedium)
                rivals.forEach { r ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("${r.name} (sec ${r.sec})")
                        Button(onClick = {
                            val chance = 0.8 - 0.18 * r.sec
                            log = if (Math.random() < chance) "BERHASIL hack ${r.name}:\n" + listOf("Login 02:14", "Menang vs NullByte", "Join grup Red Team ID", "Ubah preset Cyber").joinToString("\n") { "• $it" }
                            else "GAGAL, keamanan ${r.name} menahan serangan."
                        }) { Text("Hack") }
                    }
                }
                Text(log)
                OutlinedButton(onClick = { sub = "main" }) { Text("Kembali") }
            }
            "follow" -> {
                Text("Ikuti pemain", style = MaterialTheme.typography.titleMedium)
                rivals.forEach { r ->
                    val f = r.name in st.csv("follow").also { tick }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(r.name); Button(onClick = { st.toggleCsv("follow", r.name); tick++ }) { Text(if (f) "Berhenti" else "Ikuti") }
                    }
                }
                OutlinedButton(onClick = { sub = "main" }) { Text("Kembali") }
            }
            else -> {
                Text("Pengaturan", style = MaterialTheme.typography.titleMedium)
                Text("Kecepatan simulasi: ${st.get("speed") ?: "1"}x (6x = ronde 6 menit jadi 1 menit)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1", "6").forEach { s -> Button(onClick = { st.put("speed", s); tick++ }) { Text("${s}x") } }
                }
                OutlinedButton(onClick = { sub = "main" }) { Text("Kembali") }
            }
        }
    }
}

fun secLevel(st: SecureStore) = listOf("sec_pin", "bio", "sec_2fa", "sec_lock").count { st.get(it) == "1" }


@Composable
fun MissionRow(label: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = done, onCheckedChange = null)
        Text(label, modifier = Modifier.weight(1f))
        Text(if (done) "SELESAI" else "BELUM", color = if (done) Color(0xFF00D98B) else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AchievementRow(name: String, description: String, unlocked: Boolean) {
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = if (unlocked) Color(0xFF123A31) else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(name, style = MaterialTheme.typography.titleSmall); Text(description, style = MaterialTheme.typography.bodySmall) }
            Text(if (unlocked) "TERBUKA" else "TERKUNCI", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private data class Lesson(val title: String, val body: String, val question: String, val options: List<String>, val correct: Int, val explain: String)
private val lessons = listOf(
    Lesson("Kata Sandi Kuat", "Gunakan kata sandi unik dan panjang. Jangan memakai sandi yang sama di banyak layanan. Pengelola kata sandi dapat membantu membuat dan menyimpan sandi berbeda.", "Praktik mana yang paling aman?", listOf("Satu sandi untuk semua akun", "Sandi unik dan panjang untuk tiap akun", "Bagikan sandi ke teman"), 1, "Sandi unik membatasi dampak jika satu layanan mengalami kebocoran."),
    Lesson("Phishing", "Phishing mencoba menipu pengguna agar memberikan data atau membuka tautan berbahaya. Periksa alamat pengirim dan domain situs, serta jangan terburu-buru karena pesan mendesak.", "Apa langkah terbaik saat menerima tautan mencurigakan?", listOf("Klik agar tahu isinya", "Teruskan ke semua teman", "Verifikasi melalui kanal resmi tanpa membuka tautan"), 2, "Gunakan aplikasi atau situs resmi yang dibuka sendiri, bukan tautan dari pesan."),
    Lesson("Autentikasi Dua Faktor", "Autentikasi dua faktor menambah lapisan keamanan selain kata sandi. Aplikasi autentikator atau kunci keamanan umumnya lebih tahan terhadap phishing daripada kode SMS.", "Apa manfaat utama 2FA?", listOf("Menambah lapisan verifikasi", "Membuat akun tidak perlu sandi", "Mempercepat semua unduhan"), 0, "Penyerang membutuhkan bukti tambahan selain kata sandi."),
    Lesson("Pembaruan Sistem", "Pembaruan memperbaiki bug dan celah keamanan. Aktifkan pembaruan otomatis bila memungkinkan dan unduh aplikasi dari sumber tepercaya.", "Mengapa pembaruan penting?", listOf("Hanya mengubah tampilan", "Menutup kerentanan yang diketahui", "Menonaktifkan antivirus"), 1, "Patch keamanan mengurangi risiko dari kerentanan yang telah ditemukan."),
    Lesson("Wi-Fi Publik", "Jaringan publik bisa saja tidak tepercaya. Pastikan situs menggunakan HTTPS, hindari transaksi sensitif di jaringan yang meragukan, dan matikan koneksi otomatis ke hotspot yang tidak dikenal.", "Apa kebiasaan yang lebih aman?", listOf("Mengabaikan peringatan sertifikat", "Memakai hotspot bernama mirip tanpa memeriksa", "Memastikan domain dan HTTPS sebelum memasukkan data"), 2, "HTTPS membantu melindungi koneksi, tetapi tetap periksa domain dan peringatan browser."),
    Lesson("Cadangan Data", "Cadangan berkala membantu memulihkan data akibat kerusakan perangkat atau ransomware. Simpan salinan terpisah dan uji proses pemulihan.", "Strategi cadangan yang baik adalah…", listOf("Satu salinan di perangkat yang sama", "Beberapa salinan termasuk satu yang terpisah/offline", "Tidak pernah menguji pemulihan"), 1, "Salinan terpisah mengurangi risiko semua cadangan ikut terdampak."),
    Lesson("Privasi Aplikasi", "Berikan izin seperlunya. Aplikasi senter tidak membutuhkan akses kontak atau lokasi untuk fungsi utamanya. Tinjau izin secara berkala.", "Apa prinsip izin aplikasi?", listOf("Izinkan semuanya", "Berikan izin minimum yang diperlukan", "Matikan kunci layar"), 1, "Prinsip hak akses minimum mengurangi paparan data."),
    Lesson("Respons Insiden", "Jika akun diduga diambil alih, gunakan perangkat tepercaya untuk mengganti sandi, cabut sesi yang tidak dikenal, aktifkan 2FA, dan hubungi dukungan resmi.", "Langkah awal yang masuk akal adalah…", listOf("Mengabaikannya", "Mengamankan akun lewat kanal resmi dan mencabut sesi asing", "Mengirim kode OTP ke orang yang mengaku petugas"), 1, "Jangan pernah membagikan OTP; petugas resmi tidak membutuhkannya."),
    Lesson("Malware dan Aplikasi Berbahaya", "Malware adalah perangkat lunak yang dirancang untuk merusak, memata-matai, atau mengakses sistem tanpa izin. Hindari APK dari sumber tak dikenal, periksa pengembang, dan hapus aplikasi yang mencurigakan melalui pengaturan perangkat.", "Apa tindakan pencegahan yang tepat?", listOf("Memasang semua APK yang dikirim orang", "Mengunduh dari sumber resmi dan memeriksa izin", "Mematikan seluruh pembaruan"), 1, "Sumber tepercaya dan pemeriksaan izin membantu mengurangi risiko."),
    Lesson("Rekayasa Sosial", "Rekayasa sosial memanfaatkan kepercayaan, rasa takut, atau rasa penasaran agar korban membocorkan informasi. Penipu bisa berpura-pura menjadi teman, teknisi, atau petugas layanan.", "Seseorang mengaku petugas dan meminta OTP. Apa yang dilakukan?", listOf("Berikan agar akun aman", "Tolak dan verifikasi melalui kanal resmi", "Kirim juga kata sandi"), 1, "OTP dan kode pemulihan tidak boleh dibagikan kepada siapa pun."),
    Lesson("Keamanan Email", "Email sering menjadi pintu pemulihan akun lain. Amankan email utama dengan kata sandi unik, MFA, serta alamat dan nomor pemulihan yang masih dikuasai.", "Mengapa email utama perlu dilindungi ekstra?", listOf("Email tidak pernah diretas", "Email dapat digunakan untuk memulihkan akun lain", "Agar inbox lebih penuh"), 1, "Mengambil alih email dapat membantu penyerang mereset akun lain."),
    Lesson("Enkripsi", "Enkripsi mengubah data menjadi bentuk yang tidak mudah dibaca tanpa kunci yang tepat. Gunakan kunci layar, penyimpanan terenkripsi, dan koneksi HTTPS; enkripsi bukan alasan untuk mengabaikan pemeriksaan situs.", "Apa tujuan utama enkripsi?", listOf("Melindungi kerahasiaan data", "Membuat sandi tidak diperlukan", "Menghapus semua malware otomatis"), 0, "Enkripsi membantu menjaga data agar tidak mudah dibaca pihak tanpa izin."),
    Lesson("Izin Aplikasi Android", "Android menyediakan kontrol untuk kamera, mikrofon, lokasi, kontak, dan berkas. Berikan izin saat fungsi benar-benar membutuhkannya, lalu tinjau kembali secara berkala.", "Aplikasi kalkulator meminta akses mikrofon terus-menerus. Apa pilihan terbaik?", listOf("Izinkan tanpa memeriksa", "Tinjau kebutuhan izin dan cabut jika tidak relevan", "Berikan semua izin lain juga"), 1, "Izin minimum mengurangi risiko penyalahgunaan data."),
    Lesson("Keamanan Browser", "Perhatikan peringatan keamanan, domain, unduhan, dan ekstensi browser. HTTPS membantu melindungi koneksi tetapi tidak membuktikan bahwa sebuah situs jujur.", "Apakah ikon HTTPS menjamin situs pasti aman?", listOf("Ya, selalu", "Tidak; domain dan tujuan situs tetap harus diperiksa", "Ya, jika tampilannya bagus"), 1, "Situs penipuan juga dapat menggunakan HTTPS."),
    Lesson("Kebocoran Data", "Jika layanan mengumumkan kebocoran, ganti kata sandi yang terdampak dan setiap sandi lain yang pernah dipakai ulang. Aktifkan MFA dan waspadai pesan penipuan susulan.", "Jika sandi yang bocor dipakai di beberapa situs, kamu harus…", listOf("Membiarkannya", "Mengganti sandi tersebut di semua akun terkait dengan sandi unik", "Mengirim sandi ke teman"), 1, "Pemakaian ulang sandi membuat satu kebocoran berdampak ke banyak akun."),
    Lesson("Backup 3-2-1", "Prinsip backup 3-2-1 menyarankan tiga salinan data, pada dua jenis media, dengan satu salinan terpisah dari lokasi utama. Pastikan cadangan tidak selalu terhubung ke perangkat.", "Apa manfaat salinan cadangan terpisah?", listOf("Mengurangi risiko semua salinan ikut terkena insiden", "Membuat file otomatis publik", "Menghilangkan kebutuhan menguji pemulihan"), 0, "Salinan terpisah dapat membantu saat perangkat utama atau akun cloud terdampak."),
    Lesson("Pelaporan Kerentanan", "Jika menemukan celah, jangan mengambil data lebih dari yang diperlukan untuk membuktikan dampak. Ikuti kebijakan pengujian, simpan bukti minimal, dan laporkan secara privat kepada pemilik sistem.", "Bagaimana melaporkan celah secara bertanggung jawab?", listOf("Publikasikan data pengguna", "Ikuti kebijakan dan hubungi pemilik melalui kanal resmi", "Uji terus pada sistem tanpa izin"), 1, "Pengujian harus memiliki izin dan laporan tidak boleh mengekspos data pribadi."),
    Lesson("Log dan Aktivitas Login", "Riwayat login dan peringatan keamanan dapat menunjukkan perangkat atau lokasi yang tidak dikenal. Keluar dari sesi asing, ganti sandi lewat aplikasi resmi, dan aktifkan MFA.", "Kamu melihat login yang tidak dikenal. Apa langkah aman?", listOf("Abaikan saja", "Cabut sesi asing dan amankan akun lewat kanal resmi", "Klik tautan dari pesan acak"), 1, "Gunakan halaman keamanan resmi, bukan tautan yang dikirim pengirim tak dikenal."),
    Lesson("Kunci API dan Rahasia", "Token API, kunci privat, dan kata sandi tidak boleh dimasukkan ke repositori publik atau tangkapan layar. Gunakan pengelola rahasia, batasi hak akses, dan cabut kunci yang terlanjur bocor.", "Jika token rahasia masuk ke repositori publik, kamu perlu…", listOf("Menganggapnya aman setelah dihapus dari tampilan", "Mencabut atau merotasi token dan memeriksa aktivitas", "Membagikan token ke tim lain"), 1, "Riwayat repositori mungkin tetap menyimpan rahasia yang sudah dihapus."),
    Lesson("Dasar Jaringan", "Alamat IP membantu mengidentifikasi antarmuka jaringan, DNS menerjemahkan nama domain ke alamat, dan router meneruskan lalu lintas. Informasi jaringan harus dipakai untuk pemecahan masalah yang berizin.", "Apa fungsi utama DNS?", listOf("Menerjemahkan nama domain ke alamat yang dibutuhkan", "Menyimpan semua kata sandi", "Menghapus virus dari ponsel"), 0, "DNS membantu perangkat menemukan alamat layanan berdasarkan nama domain."),
    Lesson("CTF dan Lab Legal", "Capture The Flag (CTF) adalah latihan keamanan dalam lingkungan yang sengaja disediakan. Mulailah dari lab pemula, baca aturan, dan jangan memindahkan teknik latihan ke sistem nyata tanpa izin.", "Di mana latihan eksploitasi sebaiknya dilakukan?", listOf("Situs acak milik orang lain", "Lab atau CTF yang memberi izin jelas", "Jaringan sekolah tanpa izin"), 1, "Lingkungan lab memberi ruang belajar dengan batasan dan izin yang jelas."),
    Lesson("Ransomware", "Ransomware dapat mengunci atau mengenkripsi data untuk memeras korban. Pencegahan meliputi pembaruan, kewaspadaan lampiran, hak akses minimum, serta cadangan offline yang diuji.", "Persiapan apa yang membantu pemulihan?", listOf("Cadangan terpisah yang diuji", "Satu-satunya salinan tetap di laptop", "Menonaktifkan pembaruan"), 0, "Cadangan terpisah dapat membantu pemulihan tanpa bergantung pada perangkat yang terdampak."),
    Lesson("Keamanan Bluetooth dan Hotspot", "Matikan Bluetooth dan hotspot saat tidak digunakan, jangan menerima permintaan pairing tak dikenal, dan hapus perangkat lama yang tidak lagi dipakai dari daftar pasangan.", "Permintaan pairing muncul dari perangkat asing. Apa yang aman?", listOf("Terima otomatis", "Tolak dan pastikan perangkat yang benar sebelum memasangkan", "Bagikan PIN akun"), 1, "Pairing hanya dilakukan dengan perangkat yang dikenal dan memang diperlukan."),
)

@Composable
fun AcademyScreen(st: SecureStore) {
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var answered by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val lesson = lessons[index]
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("AKADEMI KEAMANAN", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Modul ${index + 1} dari ${lessons.size} • Dasar hingga menengah")
        LinearProgressIndicator(progress = { (index + 1f) / lessons.size }, modifier = Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(lesson.title, style = MaterialTheme.typography.titleLarge)
            Text(lesson.body)
            Divider()
            Text(lesson.question, style = MaterialTheme.typography.titleMedium)
            lesson.options.forEachIndexed { i, option ->
                Row(Modifier.fillMaxWidth().clickable(enabled = !answered) { selected = i }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == i, onClick = { if (!answered) selected = i }, enabled = !answered)
                    Text(option)
                }
            }
            if (answered) Text(message, color = if (selected == lesson.correct) Color(0xFF00D98B) else Color(0xFFFF7979))
            Button(onClick = {
                if (!answered && selected >= 0) {
                    answered = true
                    val right = selected == lesson.correct
                    message = (if (right) "Benar. " else "Belum tepat. ") + lesson.explain
                    if (right) st.put("quiz_score", ((st.get("quiz_score")?.toIntOrNull() ?: 0) + 1).toString())
                    st.put("lessons_done", ((st.get("lessons_done")?.toIntOrNull() ?: 0) + 1).toString())
                } else if (answered) { index = (index + 1) % lessons.size; selected = -1; answered = false; message = "" }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (answered) "Pelajaran berikutnya" else "Periksa jawaban") }
        } }
        Text("Skor jawaban benar: ${st.get("quiz_score") ?: "0"}")
        Text("Konten ini adalah materi edukasi defensif. Latihan berlangsung lokal dan tidak menyerang sistem nyata.", style = MaterialTheme.typography.bodySmall)
    }
}
