package com.game.play.data.verifier

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipFile

enum class CheckState { PASS, WARNING, FAIL }
data class VerificationCheck(val title: String, val state: CheckState, val detail: String)
data class ApkVerificationResult(val state: CheckState, val packageName: String? = null, val appName: String? = null, val versionName: String? = null, val versionCode: Long = 0, val sha256: String? = null, val certificateSha256: String? = null, val permissions: List<String> = emptyList(), val checks: List<VerificationCheck>)

class ApkVerifier(private val context: Context, private val maxBytes: Long = 200L * 1024L * 1024L) {
    fun verify(original: File, registeredCertificateSha256: String? = null): ApkVerificationResult {
        val checks = mutableListOf<VerificationCheck>()
        fun add(title: String, state: CheckState, detail: String) { checks += VerificationCheck(title, state, detail) }
        if (!original.name.endsWith(".apk.ROP", ignoreCase = true)) { add("Nama file", CheckState.FAIL, "Nama harus berakhiran .apk.ROP"); return ApkVerificationResult(CheckState.FAIL, checks=checks) }
        if (!original.isFile || original.length() <= 0 || original.length() > maxBytes) { add("Ukuran file", CheckState.FAIL, "File kosong, tidak tersedia, atau melebihi batas 200 MB"); return ApkVerificationResult(CheckState.FAIL, checks=checks) }
        add("Nama dan ukuran", CheckState.PASS, "Nama sesuai dan ukuran ${original.length()} byte")
        val header = FileInputStream(original).use { input -> ByteArray(4).also { if (input.read(it) != 4) return@use ByteArray(0) } }
        if (!header.contentEquals(byteArrayOf(0x50,0x4b,0x03,0x04))) { add("Header ZIP", CheckState.FAIL, "Header bukan ZIP APK biasa; format terenkripsi/wadah tidak didukung"); return ApkVerificationResult(CheckState.FAIL, checks=checks) }
        val hasManifest = runCatching { ZipFile(original).use { it.getEntry("AndroidManifest.xml") != null } }.getOrDefault(false)
        if (!hasManifest) { add("Struktur APK", CheckState.FAIL, "AndroidManifest.xml tidak ditemukan"); return ApkVerificationResult(CheckState.FAIL, checks=checks) }
        add("Struktur APK", CheckState.PASS, "Header ZIP dan AndroidManifest.xml ditemukan")
        val apk = File(context.cacheDir, "verify-${System.nanoTime()}.apk")
        try {
            original.copyTo(apk, overwrite=true)
            val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_PERMISSIONS else PackageManager.GET_SIGNATURES or PackageManager.GET_PERMISSIONS
            val pi = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            if (pi == null) { add("Metadata APK", CheckState.FAIL, "PackageManager tidak dapat membaca APK"); return ApkVerificationResult(CheckState.FAIL, checks=checks) }
            pi.applicationInfo?.let { it.sourceDir = apk.absolutePath; it.publicSourceDir = apk.absolutePath }
            val pkg = pi.packageName
            val versionCode = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
            val permissions = pi.requestedPermissions?.toList().orEmpty()
            val certBytes = if (Build.VERSION.SDK_INT >= 28) pi.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray() else pi.signatures?.firstOrNull()?.toByteArray()
            if (certBytes == null) { add("Tanda tangan", CheckState.FAIL, "Sertifikat penandatangan tidak ditemukan"); return ApkVerificationResult(CheckState.FAIL, pkg, versionName=pi.versionName, versionCode=versionCode, permissions=permissions, checks=checks) }
            val certHash = sha256(certBytes)
            add("Tanda tangan", CheckState.PASS, "Sertifikat tersedia; SHA-256 dihitung")
            val fileHash = FileInputStream(original).use { stream -> val digest=MessageDigest.getInstance("SHA-256"); val buffer=ByteArray(64*1024); while(true){val n=stream.read(buffer);if(n<0)break;digest.update(buffer,0,n)}; digest.digest().joinToString("") { "%02x".format(it) } }
            add("Hash file", CheckState.PASS, "SHA-256 dihitung")
            if (!registeredCertificateSha256.isNullOrBlank() && !certHash.equals(registeredCertificateSha256.replace(":", ""), true)) add("Sertifikat terdaftar", CheckState.FAIL, "Sertifikat berbeda dari data developer terdaftar") else if (registeredCertificateSha256.isNullOrBlank()) add("Sertifikat terdaftar", CheckState.WARNING, "Belum dapat dibandingkan: server belum menyediakan sertifikat terdaftar") else add("Sertifikat terdaftar", CheckState.PASS, "Sertifikat cocok")
            val risky = permissions.filter { p -> listOf("READ_CONTACTS", "READ_SMS", "SEND_SMS", "SYSTEM_ALERT_WINDOW", "BIND_ACCESSIBILITY_SERVICE", "RECORD_AUDIO", "READ_CALL_LOG", "REQUEST_INSTALL_PACKAGES").any { p.contains(it) } }
            if (risky.isEmpty()) add("Izin aplikasi", CheckState.PASS, "Tidak ditemukan izin dalam daftar peringatan lokal") else add("Izin aplikasi", CheckState.WARNING, "Perlu ditinjau: ${risky.joinToString()}")
            val state = when { checks.any { it.state == CheckState.FAIL } -> CheckState.FAIL; checks.any { it.state == CheckState.WARNING } -> CheckState.WARNING; else -> CheckState.PASS }
            return ApkVerificationResult(state, pkg, pi.applicationInfo?.loadLabel(context.packageManager)?.toString(), pi.versionName, versionCode, fileHash, certHash, permissions, checks)
        } finally { apk.delete() }
    }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
