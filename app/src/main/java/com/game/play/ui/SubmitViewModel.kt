package com.game.play.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.game.play.data.SubmitRepository
import com.game.play.data.install.Net
import com.game.play.data.verifier.ApkVerificationResult
import com.game.play.data.verifier.ApkVerifier
import com.game.play.data.verifier.CheckState
import com.game.play.data.verifier.VerificationCheck
import com.game.play.notify.Notifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SubmitForm(
    val name: String = "",
    val packageName: String = "",
    val category: String = "",
    val description: String = "",
    val apkName: String? = null,
    val result: ApkVerificationResult? = null,
    val iconUri: Uri? = null,
    val screenshots: List<Uri> = emptyList(),
    val verifying: Boolean = false,
    val submitting: Boolean = false,
    val progress: String? = null,
    val message: String? = null,
    val success: Boolean = false
)

private val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+\$")

/** Daftar pemeriksaan yang harus lulus sebelum pengajuan boleh dikirim. */
fun buildChecklist(f: SubmitForm): List<VerificationCheck> {
    val list = mutableListOf<VerificationCheck>()
    val name = f.name.trim()
    val pkg = f.packageName.trim()
    val result = f.result

    list += if (name.length >= 3) VerificationCheck("Nama aplikasi", CheckState.PASS, name)
    else VerificationCheck("Nama aplikasi", CheckState.FAIL, "Isi minimal 3 karakter.")

    list += if (PACKAGE_REGEX.matches(pkg)) VerificationCheck("Package", CheckState.PASS, pkg)
    else VerificationCheck("Package", CheckState.FAIL, "Format package tidak valid, contoh: com.contoh.aplikasi")

    list += if (f.description.trim().length >= 10) VerificationCheck("Deskripsi", CheckState.PASS, "Deskripsi terisi.")
    else VerificationCheck("Deskripsi", CheckState.FAIL, "Isi deskripsi minimal 10 karakter.")

    if (result == null) {
        list += VerificationCheck("File APK", CheckState.FAIL, "Belum memilih file .apk.ROP.")
    } else {
        list += result.checks
        val apkPkg = result.packageName
        if (apkPkg != null) {
            list += if (apkPkg == pkg) VerificationCheck("Package sesuai APK", CheckState.PASS, "Package form sama dengan package di APK.")
            else VerificationCheck("Package sesuai APK", CheckState.FAIL, "Form: $pkg, APK: $apkPkg")
        }
        val apkName = result.appName
        if (apkName != null && name.isNotEmpty()) {
            list += if (apkName.equals(name, ignoreCase = true)) VerificationCheck("Nama sesuai APK", CheckState.PASS, "Nama sama dengan label di APK.")
            else VerificationCheck("Nama sesuai APK", CheckState.WARNING, "Label di APK: $apkName")
        }
    }

    list += if (f.iconUri != null) VerificationCheck("Foto aplikasi", CheckState.PASS, "Foto/ikon dipilih.")
    else VerificationCheck("Foto aplikasi", CheckState.FAIL, "Pilih foto/ikon aplikasi.")

    list += if (f.screenshots.isNotEmpty()) VerificationCheck("Screenshot", CheckState.PASS, "${f.screenshots.size} screenshot dipilih.")
    else VerificationCheck("Screenshot", CheckState.WARNING, "Disarankan menambah screenshot (opsional).")
    return list
}

fun canSubmit(f: SubmitForm): Boolean =
    f.result != null && !f.verifying && !f.submitting && buildChecklist(f).none { it.state == CheckState.FAIL }

class SubmitViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application.applicationContext
    private val repo = SubmitRepository()
    private var apkFile: File? = null

    private val _form = MutableStateFlow(SubmitForm())
    val form: StateFlow<SubmitForm> = _form.asStateFlow()

    fun edit(block: SubmitForm.() -> SubmitForm) { _form.update { it.block() } }

    fun setIcon(uri: Uri?) { if (uri != null) _form.update { it.copy(iconUri = uri) } }

    fun setScreenshots(uris: List<Uri>) { _form.update { it.copy(screenshots = uris.take(4)) } }

    fun pickApk(uri: Uri) {
        viewModelScope.launch {
            _form.update { it.copy(verifying = true, result = null, apkName = null, message = null, success = false) }
            try {
                val (file, display) = withContext(Dispatchers.IO) { copyApk(uri) }
                apkFile?.delete()
                apkFile = file
                val result = withContext(Dispatchers.IO) { ApkVerifier(ctx).verify(file) }
                val apkPkg = result.packageName
                val apkLabel = result.appName
                _form.update { f ->
                    f.copy(
                        apkName = display,
                        result = result,
                        verifying = false,
                        packageName = if (f.packageName.isBlank() && apkPkg != null) apkPkg else f.packageName,
                        name = if (f.name.isBlank() && !apkLabel.isNullOrBlank()) apkLabel else f.name
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _form.update { it.copy(verifying = false, message = "Gagal membaca file: ${e.message ?: "kesalahan tidak diketahui"}") }
            }
        }
    }

    private fun copyApk(uri: Uri): Pair<File, String> {
        val display = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "selected-file"
        val safe = display.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(ctx.cacheDir, "submit_" + System.nanoTime() + "_" + safe)
        val input = ctx.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Tidak dapat membaca file.")
        input.use { source -> file.outputStream().use { sink -> source.copyTo(sink) } }
        return file to display
    }

    private fun copyImage(uri: Uri, base: String): Triple<File, String, String> {
        val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val file = File(ctx.cacheDir, "submit_" + System.nanoTime() + "_" + base + "." + ext)
        val input = ctx.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Foto tidak dapat dibaca.")
        input.use { source -> file.outputStream().use { sink -> source.copyTo(sink) } }
        return Triple(file, "$base.$ext", mime)
    }

    fun submit(developerName: String, notify: Boolean) {
        val f = _form.value
        val result = f.result
        val apk = apkFile
        val icon = f.iconUri
        if (!canSubmit(f) || result == null || apk == null || icon == null) return
        if (!Net.isOnline(ctx)) {
            _form.update { it.copy(message = "Tidak ada koneksi internet. Pengajuan butuh internet.") }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(submitting = true, message = null, success = false, progress = "Mengunggah APK…") }
            val temp = mutableListOf<File>()
            try {
                val payload = withContext(Dispatchers.IO) {
                    val apkUrl = repo.upload(apk, apk.name, "application/vnd.android.package-archive")
                    _form.update { it.copy(progress = "Mengunggah foto aplikasi…") }
                    val (iconFile, iconName, iconMime) = copyImage(icon, "icon")
                    temp += iconFile
                    val iconUrl = repo.upload(iconFile, iconName, iconMime)
                    val shotUrls = mutableListOf<String>()
                    f.screenshots.forEachIndexed { index, uri ->
                        _form.update { it.copy(progress = "Mengunggah screenshot ${index + 1}/${f.screenshots.size}…") }
                        val (file, fileName, mime) = copyImage(uri, "screenshot${index + 1}")
                        temp += file
                        shotUrls += repo.upload(file, fileName, mime)
                    }
                    SubmitRepository.Payload(
                        name = f.name.trim(),
                        packageName = f.packageName.trim(),
                        developer = developerName,
                        category = f.category.trim(),
                        description = f.description.trim(),
                        versionName = result.versionName ?: "",
                        versionCode = result.versionCode,
                        sha256 = result.sha256 ?: "",
                        certSha256 = result.certificateSha256 ?: "",
                        apkUrl = apkUrl,
                        iconUrl = iconUrl,
                        screenshotUrls = shotUrls,
                        sizeBytes = apk.length()
                    )
                }
                _form.update { it.copy(progress = "Mengirim pengajuan…") }
                withContext(Dispatchers.IO) { repo.submit(payload) }
                _form.update { it.copy(submitting = false, progress = null, success = true, message = "Pengajuan terkirim dan menunggu ditinjau.") }
                if (notify) Notifier.info(ctx, 3001, "Pengajuan terkirim", "${payload.name} menunggu ditinjau.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = when (e) {
                    is java.net.UnknownHostException, is java.net.SocketTimeoutException, is java.net.ConnectException ->
                        "Koneksi terputus. Coba lagi saat online."
                    else -> e.message ?: "Gagal mengirim pengajuan."
                }
                _form.update { it.copy(submitting = false, progress = null, message = msg) }
            } finally {
                temp.forEach { it.delete() }
            }
        }
    }

    override fun onCleared() {
        apkFile?.delete()
        super.onCleared()
    }
}
