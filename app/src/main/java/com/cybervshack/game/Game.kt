package com.cybervshack.game

import java.util.Calendar
import kotlin.random.Random

enum class Roll { HACK, CYBER }
data class Module(val name: String, val cost: Int, val power: Int)

object Catalog {
    const val BUDGET = 100
    val cyber = listOf(Module("Firewall", 20, 25), Module("Enkripsi AES", 30, 40), Module("2FA", 15, 18),
        Module("IDS", 25, 30), Module("Honeypot", 20, 22), Module("Auto-Patch", 10, 10))
    val hack = listOf(Module("Phishing", 15, 18), Module("SQL Injection", 25, 30), Module("Zero-Day", 40, 55),
        Module("Brute Force", 10, 10), Module("Malware", 30, 38), Module("Social Eng.", 20, 22))
    fun pool(r: Roll) = if (r == Roll.HACK) hack else cyber
    fun other(r: Roll) = if (r == Roll.HACK) Roll.CYBER else Roll.HACK
    fun randomLoadout(r: Roll): List<Module> {
        var left = BUDGET; val out = mutableListOf<Module>()
        for (m in pool(r).shuffled()) if (m.cost <= left) { out += m; left -= m.cost }
        return out
    }
    fun load(st: SecureStore, r: Roll): List<Module> {
        val names = st.csv("preset_$r"); return pool(r).filter { it.name in names }
    }
}

fun spin() = if (Random.nextBoolean()) Roll.HACK else Roll.CYBER

object Engine {
    const val LIMIT = 360 // 6 menit
    fun power(l: List<Module>) = l.sumOf { it.power }
    /** Detik sampai pertahanan tembus. >= LIMIT berarti Cyber bertahan. */
    fun breachSeconds(atk: Int, def: Int): Double =
        if (atk <= 0) 1e9 else LIMIT * (def * 1.15) / atk * (0.85 + Random.nextDouble() * 0.3)
    fun hackerWins(breach: Double) = breach < LIMIT
}

/** 2 ronde; kalau seri lanjut ronde penentu sampai ada pemenang. */
class Match(val teamSize: Int) {
    var userScore = 0; var oppScore = 0; var round = 0
    fun record(userWon: Boolean) { round++; if (userWon) userScore++ else oppScore++ }
    fun finished() = round >= 2 && userScore != oppScore
    fun tie() = round >= 2 && userScore == oppScore
}

/** Bantuan AI: 3x per hari, reset jam 05.00. */
class AiHelper(private val st: SecureStore) {
    private fun dayKey(): String { val c = Calendar.getInstance(); c.add(Calendar.HOUR_OF_DAY, -5); return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}" }
    fun remaining(): Int = if (st.get("ai_day") != dayKey()) 3 else 3 - (st.get("ai_used")?.toIntOrNull() ?: 0)
    fun use(role: Roll, sel: List<Module>): String? {
        val r = remaining(); if (r <= 0) return null
        st.put("ai_day", dayKey()); st.put("ai_used", (3 - r + 1).toString())
        val left = Catalog.BUDGET - sel.sumOf { it.cost }
        val best = Catalog.pool(role).filter { it !in sel && it.cost <= left }.maxByOrNull { it.power.toDouble() / it.cost }
        return if (best != null) "AI: ambil ${best.name} (power/biaya terbaik, sisa budget $left)" else "AI: budget sudah optimal, tinggal mulai!"
    }
}
