package com.droidstore.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/** Токен хранится в зашифрованных SharedPreferences. Ключи и секреты в APK не хранятся. */
object Session {
    private const val FILE = "droidstore_session"
    private const val KEY_TOKEN = "token"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        // API security-crypto 1.0.0: ключ создаётся через MasterKeys, затем передаётся alias.
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        Api.token = prefs?.getString(KEY_TOKEN, null)
    }

    fun save(token: String) {
        prefs?.edit()?.putString(KEY_TOKEN, token)?.apply()
        Api.token = token
    }

    fun clear() {
        prefs?.edit()?.remove(KEY_TOKEN)?.apply()
        Api.token = null
    }

    val isLoggedIn: Boolean get() = Api.token != null
}
