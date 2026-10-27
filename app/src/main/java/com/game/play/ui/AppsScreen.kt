package com.game.play.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.game.play.domain.model.AppInfo
import com.game.play.domain.model.DlState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(vm: StoreViewModel, onDetail: (String) -> Unit, onSettings: () -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val installed by vm.installed.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Semua") }
    val categories = remember(apps) { listOf("Semua") + apps.map { it.category }.filter { it.isNotBlank() }.distinct() }
    val shown = remember(apps, query, category) {
        apps.filter { app ->
            (category == "Semua" || app.category == category) &&
                (app.name.contains(query, ignoreCase = true) || app.developer.contains(query, ignoreCase = true))
        }
    }
    val featured = remember(apps) { apps.maxByOrNull { it.rating } }

    PullToRefreshBox(isRefreshing = loading, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Jelajahi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.NotificationsNone, "Notifikasi & pengaturan", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (offline && apps.isNotEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Sedang offline — menampilkan data tersimpan.",
                                modifier = Modifier.padding(start = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Cari aplikasi") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            if (categories.size > 1) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { name ->
                            FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) }, shape = RoundedCornerShape(22.dp))
                        }
                    }
                }
            }
            if (featured != null && query.isBlank() && category == "Semua") {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primary).padding(22.dp)
                    ) {
                        Text("PILIHAN HARI INI", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(featured.name, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (featured.developer.isNotBlank()) Text(featured.developer, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onDetail(featured.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2F6BFF))
                        ) { Text("Lihat") }
                    }
                }
            }
            item {
                Text("Semua aplikasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            if (apps.isEmpty()) {
                item {
                    if (loading) {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    } else {
                        MessageCard(error ?: "Belum ada aplikasi yang tersedia.")
                    }
                }
            } else if (shown.isEmpty()) {
                item { MessageCard("Tidak ada aplikasi yang cocok.") }
            }
            items(shown) { app ->
                AppRow(
                    app = app,
                    installedInfo = app.packageName?.let { installed[it] },
                    dl = downloads[app.id] ?: DlState(),
                    onClick = { onDetail(app.id) },
                    onInstall = { vm.install(app) },
                    onOpen = { vm.open(app) }
                )
            }
        }
    }
}

@Composable
private fun MessageCard(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = cardBorder(), modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    installedInfo: com.game.play.domain.model.InstalledInfo?,
    dl: DlState,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    onOpen: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = cardBorder(),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.iconUrl, app.name, 48.dp)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(app.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    val parts = listOfNotNull(
                        if (app.rating > 0) "★ " + String.format(Locale.US, "%.1f", app.rating) else null,
                        app.sizeLabel.ifBlank { null },
                        app.versionName.ifBlank { null }
                    )
                    if (parts.isNotEmpty()) {
                        Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                InstallActions(app, installedInfo, dl, onInstall, onOpen, compact = true)
            }
            if (dl.busy) {
                LinearProgressIndicator(progress = { dl.progress }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }
            dl.message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
