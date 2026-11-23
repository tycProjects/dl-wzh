package com.lenqin.lenai
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
object SecurePrefs {
 fun get(c:Context)=EncryptedSharedPreferences.create(c,"len_ai_prefs",
 MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
 EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
 EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
}
