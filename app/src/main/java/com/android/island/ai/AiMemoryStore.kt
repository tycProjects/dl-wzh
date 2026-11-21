package com.android.island.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small opt-in local memory. Values are AES-256-GCM encrypted with a non-exportable Android Keystore key. */
object AiMemoryStore {
    private const val PREFS = "ai_memory_v1"
    private const val BLOB = "encrypted_items"
    private const val ALIAS = "android_island_ai_memory_v1"
    private const val MAX_ITEMS = 50
    private const val MAX_ITEM_CHARS = 500

    @Synchronized fun add(context: Context, value: String): Boolean {
        val clean = value.trim().take(MAX_ITEM_CHARS)
        if (clean.isBlank()) return false
        val items = read(context).toMutableList()
        if (items.any { it.equals(clean, ignoreCase = true) }) return true
        items.add(clean)
        write(context, items.takeLast(MAX_ITEMS))
        return true
    }

    @Synchronized fun list(context: Context): List<String> = read(context)

    @Synchronized fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(BLOB).apply()
    }

    @Synchronized fun remove(context: Context, index: Int): Boolean {
        val items = read(context).toMutableList()
        if (index !in items.indices) return false
        items.removeAt(index)
        write(context, items)
        return true
    }

    @Synchronized fun update(context: Context, index: Int, value: String): Boolean {
        val clean = value.trim().take(MAX_ITEM_CHARS)
        if (clean.isBlank()) return false
        val items = read(context).toMutableList()
        if (index !in items.indices) return false
        items[index] = clean
        write(context, items.distinctBy { it.lowercase() }.takeLast(MAX_ITEMS))
        return true
    }

    private fun read(context: Context): List<String> = runCatching {
        val encoded = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(BLOB, null)
            ?: return emptyList()
        val packed = Base64.decode(encoded, Base64.NO_WRAP)
        require(packed.size > 12)
        val iv = packed.copyOfRange(0, 12)
        val cipherText = packed.copyOfRange(12, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val json = JSONArray(String(cipher.doFinal(cipherText), Charsets.UTF_8))
        (0 until json.length()).mapNotNull { json.optString(it).takeIf(String::isNotBlank) }.takeLast(MAX_ITEMS)
    }.getOrDefault(emptyList())

    private fun write(context: Context, values: List<String>) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(JSONArray(values).toString().toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(BLOB, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    /** True when the memory key can be created or loaded from the Keystore. */
    fun keystoreOk(): Boolean = runCatching { key(); true }.getOrDefault(false)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }
}
