package com.android.island.ui

import android.content.Context
import java.security.MessageDigest

/**
 * Tiny UI-only state that must survive a Shizuku hard restart (force-stop): the settings scroll
 * position and the last verified AI connection. Plain SharedPreferences, so a read is instant on
 * the first frame and no DataStore round-trip is needed.
 */
object UiPrefs {
    private const val FILE = "island_ui_state"
    private const val KEY_SCROLL = "home_scroll"
    private const val KEY_AI_FP = "ai_verified_fp"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun homeScroll(context: Context): Int = prefs(context).getInt(KEY_SCROLL, 0).coerceAtLeast(0)

    fun saveHomeScroll(context: Context, px: Int) {
        prefs(context).edit().putInt(KEY_SCROLL, px.coerceAtLeast(0)).apply()
    }

    /** One-way fingerprint of the AI setup, so the API key itself is never written here. */
    fun aiFingerprint(provider: String, baseUrl: String, model: String, key: String): String {
        val raw = "$provider|$baseUrl|$model|$key"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    fun saveVerifiedAi(context: Context, fingerprint: String) {
        prefs(context).edit().putString(KEY_AI_FP, fingerprint).commit()
    }

    fun isAiVerified(context: Context, fingerprint: String): Boolean =
        prefs(context).getString(KEY_AI_FP, null) == fingerprint
}
