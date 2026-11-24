package com.tyce.enhancer

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API keys are encrypted with an AES-256-GCM key that lives in the Android Keystore
 * (non-exportable). Only ciphertext is written to SharedPreferences. Keys are never logged,
 * and the prefs file is excluded from backup (see backup rules).
 */
object SecureKeyStore {
    private const val ALIAS = "alter_api_keys_v1"
    private const val PREFS = "secure_keys"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build()
        )
        return g.generateKey()
    }

    fun put(context: Context, provider: String, secret: String) {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val ct = c.doFinal(secret.toByteArray(Charsets.UTF_8))
        val blob = Base64.encodeToString(c.iv + ct, Base64.NO_WRAP)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(provider, blob).apply()
    }

    fun get(context: Context, provider: String): String? {
        val blob = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(provider, null) ?: return null
        return try {
            val raw = Base64.decode(blob, Base64.NO_WRAP)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, 12))
            String(c.doFinal(raw, 12, raw.size - 12), Charsets.UTF_8)
        } catch (_: Throwable) { null }
    }

    fun has(context: Context, provider: String) = get(context, provider) != null

    fun remove(context: Context, provider: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(provider).apply()
    }
}
