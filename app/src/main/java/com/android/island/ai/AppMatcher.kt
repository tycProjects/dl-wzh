package com.android.island.ai

import java.util.Locale

/**
 * Finds the installed app a user means by name. Pure logic, unit tested.
 * Order: exact name, then the name inside a label, then the label inside the name, then an alias.
 * Aliases cover Indonesian names for common apps, because labels are often in English.
 */
object AppMatcher {

    private val ALIASES: Map<String, List<String>> = mapOf(
        "catatan" to listOf("notes", "keep", "memo"),
        "notes" to listOf("catatan", "notes"),
        "kamera" to listOf("camera"),
        "galeri" to listOf("gallery", "photos", "foto"),
        "foto" to listOf("gallery", "photos", "galeri"),
        "pengaturan" to listOf("settings"),
        "kalender" to listOf("calendar"),
        "kontak" to listOf("contacts"),
        "pesan" to listOf("messages", "sms"),
        "jam" to listOf("clock", "alarm"),
        "kalkulator" to listOf("calculator"),
        "peta" to listOf("maps"),
        "telepon" to listOf("phone", "dialer"),
        "file" to listOf("files", "my files"),
        "berkas" to listOf("files", "my files")
    )

    private fun normalize(s: String): String = s.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    /** Index in [labels] of the app the user means, or null when no label fits. */
    fun match(query: String, labels: List<String>): Int? {
        val q = normalize(query)
        if (q.isEmpty()) return null
        val norm = labels.map { normalize(it) }
        norm.indexOf(q).takeIf { it >= 0 }?.let { return it }
        norm.indexOfFirst { q.length >= 3 && it.contains(q) }.takeIf { it >= 0 }?.let { return it }
        norm.indexOfFirst { it.length >= 3 && q.contains(it) }.takeIf { it >= 0 }?.let { return it }
        for (alias in ALIASES[query.trim().lowercase(Locale.ROOT)].orEmpty()) {
            val a = normalize(alias)
            norm.indexOfFirst { it.contains(a) }.takeIf { it >= 0 }?.let { return it }
        }
        return null
    }
}
