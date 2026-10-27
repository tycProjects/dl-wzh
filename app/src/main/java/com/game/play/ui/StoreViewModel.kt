package com.game.play.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.game.play.data.AppSettings
import com.game.play.data.SettingsStore
import com.game.play.data.StoreRepository
import com.game.play.data.install.ApkInstaller
import com.game.play.data.install.Net
import com.game.play.domain.model.AppInfo
import com.game.play.domain.model.DlState
import com.game.play.domain.model.InstalledInfo
import com.game.play.notify.Notifier
import com.game.play.notify.UpdateScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application.applicationContext
    private val store = SettingsStore(ctx)
    private val repo = StoreRepository(ctx)
    private val installer = ApkInstaller(ctx)

    val settings: StateFlow<AppSettings> =
        store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _installed = MutableStateFlow<Map<String, InstalledInfo>>(emptyMap())
    val installed: StateFlow<Map<String, InstalledInfo>> = _installed.asStateFlow()

    private val _downloads = MutableStateFlow<Map<String, DlState>>(emptyMap())
    val downloads: StateFlow<Map<String, DlState>> = _downloads.asStateFlow()

    private val _details = MutableStateFlow<Map<String, AppInfo>>(emptyMap())
    val details: StateFlow<Map<String, AppInfo>> = _details.asStateFlow()

    init {
        viewModelScope.launch { store.ensureUser() }
        refresh()
    }

    private fun toast(message: String) = Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()

    private fun setDl(id: String, state: DlState) = _downloads.update { it + (id to state) }

    // ---------- data ----------

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val result = repo.loadApps()
            _apps.value = result.data
            _offline.value = result.offline
            _error.value = if (result.offline && result.data.isEmpty()) {
                result.error ?: "Tidak dapat memuat data."
            } else null
            _loading.value = false
            refreshInstalled()
        }
    }

    fun loadDetail(id: String) {
        viewModelScope.launch {
            val base = _apps.value.firstOrNull { it.id == id }
            val detail = repo.loadDetail(id, base)
            if (detail != null) {
                _details.update { it + (id to detail) }
                refreshInstalled()
            }
        }
    }

    /** Panggil saat app kembali aktif (mis. setelah selesai install) supaya tombol Buka ikut berubah. */
    fun refreshInstalled() {
        viewModelScope.launch {
            val packages = (_apps.value.mapNotNull { it.packageName } + _details.value.values.mapNotNull { it.packageName }).toSet()
            _installed.value = withContext(Dispatchers.IO) { installer.installedVersions(packages) }
        }
    }

    // ---------- install / buka ----------

    fun install(app: AppInfo) {
        if (_downloads.value[app.id]?.busy == true) return
        viewModelScope.launch {
            val s = settings.value
            if (s.wifiOnly && !Net.isWifi(ctx)) {
                setDl(app.id, DlState(message = "Pengaturan \"Unduh hanya lewat Wi-Fi\" aktif. Sambungkan ke Wi-Fi atau matikan di Pengaturan."))
                return@launch
            }
            if (!Net.isOnline(ctx)) {
                setDl(app.id, DlState(message = "Tidak ada koneksi internet."))
                return@launch
            }
            setDl(app.id, DlState(busy = true))
            try {
                val url = repo.resolveDownloadUrl(app)
                if (url == null) {
                    setDl(app.id, DlState(message = "APK untuk aplikasi ini belum tersedia."))
                    return@launch
                }
                val file = installer.download(app, url, s.notifications) { p -> setDl(app.id, DlState(busy = true, progress = p)) }
                startInstall(app, file, s.notifications)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setDl(app.id, DlState(message = e.message ?: "Gagal mengunduh."))
            }
        }
    }

    private fun startInstall(app: AppInfo, file: File, notify: Boolean) {
        val nid = Notifier.idFor(app.id)
        val pending = android.app.PendingIntent.getActivity(
            ctx, nid, installer.installIntent(file),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (notify) Notifier.downloadDone(ctx, nid, app.name, "Unduhan selesai. Ketuk untuk memasang.", pending)
        try {
            if (installer.needsInstallPermission()) {
                ctx.startActivity(installer.permissionIntent())
                setDl(app.id, DlState(message = "Izinkan PorStore memasang aplikasi, lalu tekan Install lagi."))
            } else {
                ctx.startActivity(installer.installIntent(file))
                setDl(app.id, DlState(message = "Unduhan selesai. Lanjutkan pemasangan di layar sistem."))
            }
        } catch (e: ActivityNotFoundException) {
            setDl(app.id, DlState(message = "Tidak ada pemasang aplikasi di perangkat ini."))
        }
    }

    fun open(app: AppInfo) {
        val pkg = app.packageName
        val ok = if (pkg == null) {
            false
        } else {
            try { installer.launch(pkg) } catch (e: Exception) { false }
        }
        if (!ok) toast("Tidak bisa membuka ${app.name}.")
    }

    fun rate(id: String, rating: Int, review: String = "") {
        viewModelScope.launch {
            val error = try {
                repo.postRating(id, rating, review)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Tidak bisa kirim rating"
            }
            toast(error ?: "Terima kasih! Rating $rating/5 terkirim.")
        }
    }

    // ---------- pengaturan & profil ----------

    fun setDark(on: Boolean) { viewModelScope.launch { store.setDark(on) } }

    fun setNotifications(on: Boolean) {
        viewModelScope.launch {
            store.setNotifications(on)
            if (on) Notifier.ensureChannels(ctx)
            UpdateScheduler.apply(ctx, on)
        }
    }

    fun setWifiOnly(on: Boolean) { viewModelScope.launch { store.setWifiOnly(on) } }

    fun testNotification() {
        Notifier.info(ctx, 1001, "PorStore", "Notifikasi berfungsi dengan baik.")
    }

    fun setName(name: String) {
        viewModelScope.launch { store.setUserName(name.trim().take(24)) }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) {
                    val target = File(ctx.filesDir, "profile_" + System.currentTimeMillis() + ".jpg")
                    val input = ctx.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Foto tidak bisa dibaca.")
                    input.use { source -> target.outputStream().use { sink -> source.copyTo(sink) } }
                    ctx.filesDir.listFiles()?.forEach { f ->
                        if (f.name.startsWith("profile_") && f != target) f.delete()
                    }
                    target.absolutePath
                }
                store.setPhotoPath(path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast(e.message ?: "Gagal mengganti foto.")
            }
        }
    }

    fun clearPhoto() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ctx.filesDir.listFiles()?.forEach { f -> if (f.name.startsWith("profile_")) f.delete() }
            }
            store.setPhotoPath("")
        }
    }
}
