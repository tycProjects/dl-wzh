package com.game.play.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.game.play.domain.model.DlState
import java.util.Locale

@Composable
fun DetailScreen(vm: StoreViewModel, id: String, onBack: () -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val details by vm.details.collectAsStateWithLifecycle()
    val installed by vm.installed.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    LaunchedEffect(id) { vm.loadDetail(id) }

    val app = details[id] ?: apps.firstOrNull { it.id == id }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali") }
        Spacer(Modifier.height(8.dp))

        if (app == null) {
            Text(
                if (loading) "Memuat…" else "Aplikasi tidak ditemukan. Data belum tersimpan untuk dilihat offline.",
                color = muted
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.iconUrl, app.name, 84.dp)
            Column(Modifier.padding(start = 16.dp)) {
                Text(app.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onSurface)
                val sub = listOf(app.developer, app.category).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, color = muted)
            }
        }

        val meta = listOfNotNull(
            if (app.rating > 0) "★ " + String.format(Locale.US, "%.1f", app.rating) +
                (if (app.ratingCount > 0) " (${app.ratingCount})" else "") else null,
            app.sizeLabel.ifBlank { null },
            app.versionName.ifBlank { null }?.let { "v$it" }
        )
        if (meta.isNotEmpty()) {
            Text(meta.joinToString(" · "), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
        }

        val dl = downloads[app.id] ?: DlState()
        Spacer(Modifier.height(10.dp))
        InstallActions(
            app = app,
            installed = app.packageName?.let { installed[it] },
            dl = dl,
            onInstall = { vm.install(app) },
            onOpen = { vm.open(app) }
        )
        if (dl.busy) {
            LinearProgressIndicator(progress = { dl.progress }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }
        dl.message?.let { Text(it, color = muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }

        if (app.screenshots.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(app.screenshots) { url ->
                    AsyncImage(
                        model = url, contentDescription = "Screenshot ${app.name}", contentScale = ContentScale.Crop,
                        modifier = Modifier.height(260.dp).width(150.dp).clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("Tentang aplikasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onSurface)
        Text(
            app.description.ifBlank { "Belum ada deskripsi." },
            modifier = Modifier.padding(top = 8.dp), color = muted
        )

        Spacer(Modifier.height(22.dp))
        Text("Beri rating", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onSurface)
        var rating by remember { mutableIntStateOf(5) }
        var review by remember { androidx.compose.runtime.mutableStateOf("") }
        Row {
            (1..5).forEach { n ->
                IconButton(onClick = { rating = n }) {
                    Icon(
                        Icons.Filled.Star, contentDescription = "$n bintang",
                        tint = if (n <= rating) Amber else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        OutlinedTextField(value = review, onValueChange = { if (it.length <= 1000) review = it }, label = { Text("Tulis ulasan (opsional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.rate(app.id, rating, review) }) { Text("Kirim rating $rating/5") }
        Spacer(Modifier.height(24.dp))
    }
}
