package com.game.play.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.game.play.data.verifier.CheckState

@Composable
fun SubmitScreen(store: StoreViewModel, onBack: () -> Unit) {
    val vm: SubmitViewModel = viewModel()
    val form by vm.form.collectAsStateWithLifecycle()
    val settings by store.settings.collectAsStateWithLifecycle()
    val checks = buildChecklist(form)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onBackground

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.pickApk(uri)
    }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> vm.setIcon(uri) }
    val shotPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uris ->
        if (uris.isNotEmpty()) vm.setScreenshots(uris)
    }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali") }
        Text("Buat Aplikasi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ink)
        Text(
            "Lengkapi data, pilih file .apk.ROP dan foto aplikasi. Semua pemeriksaan wajib lulus sebelum dikirim.",
            modifier = Modifier.padding(top = 8.dp), color = muted
        )

        SectionTitle("1. Data aplikasi")
        OutlinedTextField(
            value = form.name, onValueChange = { v -> vm.edit { copy(name = v) } },
            label = { Text("Nama aplikasi") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = form.packageName, onValueChange = { v -> vm.edit { copy(packageName = v.trim()) } },
            label = { Text("Package (contoh: com.contoh.aplikasi)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = form.category, onValueChange = { v -> vm.edit { copy(category = v) } },
            label = { Text("Kategori (mis. Game, Produktif)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = form.description, onValueChange = { v -> vm.edit { copy(description = v) } },
            label = { Text("Deskripsi") }, minLines = 3, modifier = Modifier.fillMaxWidth()
        )

        SectionTitle("2. File aplikasi")
        Text("Nama file harus berakhiran .apk.ROP", color = muted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { apkPicker.launch(arrayOf("*/*")) },
            enabled = !form.verifying && !form.submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.AttachFile, null)
            Spacer(Modifier.width(8.dp))
            Text("Pilih file .apk.ROP")
        }
        if (form.verifying) {
            Text("Menyalin dan memeriksa file…", color = muted, modifier = Modifier.padding(top = 10.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        form.apkName?.let { Text(it, fontWeight = FontWeight.SemiBold, color = ink, modifier = Modifier.padding(top = 10.dp)) }
        form.result?.let { r ->
            val info = listOfNotNull(
                r.packageName?.let { "Package: $it" },
                r.versionName?.let { "Versi: $it (${r.versionCode})" },
                r.sha256?.let { "SHA-256: ${it.take(16)}…" }
            )
            info.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = muted) }
        }

        SectionTitle("3. Foto aplikasi")
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = form.iconUri
            if (icon != null) {
                AsyncImage(
                    model = icon, contentDescription = "Foto aplikasi", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            OutlinedButton(onClick = { iconPicker.launch(imageOnly) }, enabled = !form.submitting) {
                Icon(Icons.Outlined.AddPhotoAlternate, null)
                Spacer(Modifier.width(8.dp))
                Text(if (icon == null) "Pilih foto/ikon" else "Ganti foto")
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { shotPicker.launch(imageOnly) }, enabled = !form.submitting) {
            Icon(Icons.Outlined.AddPhotoAlternate, null)
            Spacer(Modifier.width(8.dp))
            Text("Pilih screenshot (maks 4, opsional)")
        }
        if (form.screenshots.isNotEmpty()) {
            LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(form.screenshots) { uri ->
                    AsyncImage(
                        model = uri, contentDescription = "Screenshot", contentScale = ContentScale.Crop,
                        modifier = Modifier.height(120.dp).width(70.dp).clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }

        SectionTitle("4. Verifikasi")
        checks.forEach { check ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    when (check.state) {
                        CheckState.PASS -> Icons.Filled.CheckCircle
                        CheckState.WARNING -> Icons.Filled.Warning
                        CheckState.FAIL -> Icons.Filled.Cancel
                    },
                    contentDescription = null,
                    tint = when (check.state) {
                        CheckState.PASS -> Green
                        CheckState.WARNING -> Amber
                        CheckState.FAIL -> Red
                    }
                )
                Column(Modifier.padding(start = 10.dp)) {
                    Text(check.title, fontWeight = FontWeight.Bold, color = ink)
                    Text(check.detail, color = muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { vm.submit(settings.displayName, settings.notifications) },
            enabled = canSubmit(form),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp)
        ) { Text(if (form.submitting) "Mengirim…" else "Kirim untuk ditinjau") }

        if (form.submitting) {
            form.progress?.let { Text(it, color = muted, modifier = Modifier.padding(top = 10.dp)) }
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        form.message?.let {
            Text(
                it,
                color = if (form.success) Green else Red,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)
    )
}
