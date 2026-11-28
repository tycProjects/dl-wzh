package com.filemax.manager

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Optional password memory: AES-256-GCM with a non-exportable Android Keystore key. Entries are keyed by archive file name hash. */
object SecureStore {
    private const val ALIAS = "filemax_pw_key"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        return g.generateKey()
    }
    private fun prefs(c: Context) = c.getSharedPreferences("pwstore", Context.MODE_PRIVATE)
    private fun id(name: String) = MessageDigest.getInstance("SHA-256").digest(name.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)

    fun save(c: Context, name: String, pw: CharArray) = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = String(pw).toByteArray(Charsets.UTF_8)
        prefs(c).edit().putString(id(name), b64(cipher.iv) + ":" + b64(cipher.doFinal(bytes))).apply()
        bytes.fill(0)
    }

    fun load(c: Context, name: String): CharArray? = runCatching {
        val (iv, ct) = (prefs(c).getString(id(name), null) ?: return null).split(':').map { Base64.decode(it, Base64.NO_WRAP) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        String(cipher.doFinal(ct), Charsets.UTF_8).toCharArray()
    }.getOrNull()

    fun forget(c: Context, name: String) { prefs(c).edit().remove(id(name)).apply() }
    fun clear(c: Context) { prefs(c).edit().clear().apply() }
}
