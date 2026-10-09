package com.cybervshack.game

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/** Penyimpanan lokal terenkripsi AES-256-GCM, kunci ada di Android Keystore (tidak bisa diekstrak). */
class SecureStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("sec", Context.MODE_PRIVATE)
    private val alias = "cvh_master"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return g.generateKey()
    }
    fun put(k: String, v: String) {
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key())
        prefs.edit().putString(k, Base64.encodeToString(c.iv + c.doFinal(v.toByteArray()), Base64.NO_WRAP)).apply()
    }
    fun get(k: String): String? {
        val s = prefs.getString(k, null) ?: return null
        return try {
            val b = Base64.decode(s, Base64.NO_WRAP)
            val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b, 0, 12))
            String(c.doFinal(b, 12, b.size - 12))
        } catch (e: Exception) { null }
    }
    fun csv(k: String): Set<String> = get(k)?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    fun toggleCsv(k: String, v: String) { val s = csv(k); put(k, (if (v in s) s - v else s + v).joinToString(",")) }
}

object Auth {
    private fun hash(p: String, salt: ByteArray): String {
        val s = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(p.toCharArray(), salt, 120_000, 256))
        return Base64.encodeToString(s.encoded, Base64.NO_WRAP)
    }
    /** Mode offline: akun lokal. Mode online: ganti dengan login ke backend (lihat backend/README.md). */
    fun loginOrRegister(st: SecureStore, u: String, p: String): Boolean {
        val cur = st.get("user")
        if (cur == null) {
            if (u.length < 3 || p.length < 6) return false
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            st.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP)); st.put("hash", hash(p, salt)); st.put("user", u)
            return true
        }
        val salt = Base64.decode(st.get("salt")!!, Base64.NO_WRAP)
        return u == cur && hash(p, salt) == st.get("hash")
    }
}

object Bio {
    fun prompt(a: FragmentActivity, ok: () -> Unit) {
        val info = BiometricPrompt.PromptInfo.Builder().setTitle("Verifikasi sidik jari").setNegativeButtonText("Batal").build()
        BiometricPrompt(a, ContextCompat.getMainExecutor(a), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) { ok() }
        }).authenticate(info)
    }
}

object Integrity {
    /**
     * Minta token Play Integrity dengan nonce yang dibuat oleh backend.
     * Jangan menerima nonce buatan client sebagai bukti kepercayaan.
     * Callback mengembalikan pasangan token + nonce agar keduanya dikirim ke backend.
     */
    fun token(ctx: Context, serverNonce: String, cb: (token: String?, nonce: String) -> Unit) {
        if (serverNonce.length !in 16..500) {
            cb(null, serverNonce)
            return
        }
        IntegrityManagerFactory.create(ctx)
            .requestIntegrityToken(IntegrityTokenRequest.builder().setNonce(serverNonce).build())
            .addOnSuccessListener { cb(it.token(), serverNonce) }
            .addOnFailureListener { cb(null, serverNonce) }
    }
}

/** Validasi URL sebelum request: hanya HTTPS dan host API yang dikonfigurasi. */
object ApiUrl {
    fun isAllowed(raw: String): Boolean = try {
        val u = okhttp3.HttpUrl.get(raw)
        u.isHttps && u.host.equals(BuildConfig.API_HOST, ignoreCase = true)
    } catch (_: Exception) { false }
}

object Net {
    /**
     * HTTPS + certificate pinning + batas waktu request.
     * Isi API_HOST dan kedua pin dengan nilai asli sebelum build produksi.
     * Pin placeholder akan menyebabkan koneksi gagal (fail closed), bukan melewati pinning.
     */
    fun client(token: () -> String?): OkHttpClient {
        val pin = CertificatePinner.Builder()
            .add(BuildConfig.API_HOST, BuildConfig.PIN_1)
            .add(BuildConfig.API_HOST, BuildConfig.PIN_2)
            .build()
        return OkHttpClient.Builder()
            .certificatePinner(pin)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                require(request.url.isHttps && request.url.host.equals(BuildConfig.API_HOST, ignoreCase = true)) {
                    "URL API ditolak: gunakan host HTTPS yang dikonfigurasi"
                }
                val builder = request.newBuilder().header("Accept", "application/json")
                token()?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            .build()
    }
}
