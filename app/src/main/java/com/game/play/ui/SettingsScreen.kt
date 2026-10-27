package com.game.play.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.game.play.BuildConfig
import com.game.play.notify.Notifier

@Composable
fun SettingsScreen(vm: StoreViewModel, onBack: () -> Unit, onSubmit: () -> Unit = {}) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
        if (!granted) {
            Toast.makeText(context, "Izin notifikasi ditolak. Aktifkan lewat pengaturan sistem.", Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali") }
        Text("Pengaturan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        SettingSwitch(
            title = "Notifikasi",
            subtitle = "Progres unduhan, aplikasi baru, dan pembaruan.",
            checked = settings.notifications && Notifier.hasPermission(context)
        ) { on ->
            if (on && Build.VERSION.SDK_INT >= 33 && !Notifier.hasPermission(context)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                vm.setNotifications(on)
            }
        }
        if (settings.notifications && Notifier.hasPermission(context)) {
            TextButton(onClick = { vm.testNotification() }) { Text("Kirim notifikasi uji") }
        }

        SettingSwitch(
            title = "Mode gelap",
            subtitle = "Ganti tampilan terang/gelap.",
            checked = settings.darkMode ?: systemDark
        ) { vm.setDark(it) }

        SettingSwitch(
            title = "Unduh hanya lewat Wi-Fi",
            subtitle = "Unduhan diblokir saat memakai data seluler.",
            checked = settings.wifiOnly
        ) { vm.setWifiOnly(it) }

        TextButton(onClick = onSubmit, modifier = Modifier.padding(top = 12.dp)) { Text("Buat Aplikasi") }

        Text("Keamanan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 22.dp))
        Text("Pemeriksaan integritas bukan jaminan bebas malware.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tentang · PorStore ${BuildConfig.VERSION_NAME}", modifier = Modifier.padding(top = 22.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
