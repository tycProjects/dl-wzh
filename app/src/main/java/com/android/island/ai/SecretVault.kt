package com.android.island.ai

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts small secrets such as API keys with an AES-256-GCM key kept in the Android Keystore.
 * Output is "iv:ciphertext" in Base64. encrypt() returns null when the Keystore cannot be used,
 * so a secret is never written in plain text as a fallback.
 */
object SecretVault {
    private const val ALIAS = "android_island_ai_secrets_v1"
    private const val SEP = ":"

    fun encrypt(plain: String): String? {
        if (plain.isEmpty()) return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val data = Base64.encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            "$iv$SEP$data"
        }.getOrNull()
    }

    /** Empty when the blob is missing, was made on another device, or cannot be decrypted. */
    fun decrypt(blob: String): String {
        if (blob.isEmpty()) return ""
        return runCatching {
            val (iv, data) = blob.split(SEP, limit = 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrDefault("")
    }

    /** True when a value survives an encrypt and decrypt round trip on this device. */
    fun selfTest(): Boolean {
        val probe = "island-probe"
        val enc = encrypt(probe) ?: return false
        return decrypt(enc) == probe
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
