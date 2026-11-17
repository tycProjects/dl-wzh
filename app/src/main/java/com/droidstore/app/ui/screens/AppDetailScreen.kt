package com.droidstore.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.droidstore.app.data.Api
import com.droidstore.app.data.AppItem
import com.droidstore.app.data.Review
import com.droidstore.app.ui.components.AppIcon
import com.droidstore.app.ui.components.BadgeChip
import com.droidstore.app.ui.components.ErrorText
import com.droidstore.app.ui.components.SecurityNotice
import com.droidstore.app.ui.components.StarPicker
import com.droidstore.app.ui.components.Stars
import kotlinx.coroutines.launch

private val REPORT_REASONS = listOf(
    "fake_app" to "Поддельное приложение",
    "brand_impersonation" to "Выдаёт себя за другой бренд",
    "misleading_description" to "Обманчивое описание",
    "suspicious_payments" to "Подозрительные платежи",
    "suspicious_behavior" to "Подозрительное поведение",
    "malicious_file" to "Вредоносный файл",
    "fake_developer" to "Поддельный разработчик",
    "copyright" to "Нарушение авторских прав",
    "other" to "Другое",
)

@Composable
fun AppDetailScreen(appId: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var app by remember { mutableStateOf<AppItem?>(null) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var myStars by remember { mutableStateOf(0) }
    var myComment by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var showReport by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            try {
                app = Api.appDetail(appId)
                val (summary, list) = Api.ratings(appId)
                reviews = list
                app = app?.let { it.copy(ratingAverage = summary.average, ratingCount = summary.count) }
            } catch (e: Exception) { error = e.message }
        }
    }
    LaunchedEffect(appId) { reload() }

    val a = app
    if (a == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (error == null) CircularProgressIndicator(Modifier.padding(24.dp)) else ErrorText(error)
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBack) { Text("Назад") }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(Api.imageUrl(a.iconId), 88)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.headlineSmall)
                Text(a.studioName ?: "", style = MaterialTheme.typography.bodyMedium)
                Stars(a.ratingAverage, a.ratingCount)
                Text("⬇ ${a.downloads} скачиваний · v${a.versionName ?: "—"}", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (a.badges.isNotEmpty() || a.studioName != null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(a.badges) { BadgeChip(it) }
            }
        }

        Button(
            onClick = {
                // Скачивание через системный DownloadManager; установку подтверждает пользователь.
                val request = DownloadManager.Request(Uri.parse(Api.downloadUrl(a.id)))
                    .setTitle(a.title)
                    .setDescription("Загрузка APK")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(context, android.os.Environment.DIRECTORY_DOWNLOADS, "${a.packageName}.apk")
                (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                message = "Загрузка началась. Когда файл будет готов, откройте его для установки."
                reload()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = a.status == "published",
        ) { Text("Скачать APK") }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        AnimatedVisibility(visible = a.screenshotIds.isNotEmpty(), enter = fadeIn()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(a.screenshotIds) { id ->
                    AsyncImage(
                        model = Api.imageUrl(id),
                        contentDescription = null,
                        modifier = Modifier.height(320.dp),
                    )
                }
            }
        }

        Text(a.shortDescription, style = MaterialTheme.typography.titleMedium)
        Text(a.description, style = MaterialTheme.typography.bodyMedium)

        SecurityNotice(a.securityStatus, a.securityScore, a.securityLimitations)

        Card(colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Поддержка: ${a.supportEmail}", style = MaterialTheme.typography.bodySmall)
                Text("Политика конфиденциальности: ${a.privacyPolicyUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Text("Оценки", style = MaterialTheme.typography.titleLarge)
        Text("Ваша оценка", style = MaterialTheme.typography.titleSmall)
        StarPicker(myStars) { myStars = it }
        OutlinedTextField(
            value = myComment,
            onValueChange = { myComment = it },
            label = { Text("Комментарий (необязательно)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = myStars in 1..5,
            onClick = {
                scope.launch {
                    try { Api.rate(a.id, myStars, myComment); myComment = ""; error = null; reload() }
                    catch (e: Exception) { error = "Не удалось сохранить оценку: ${e.message}" }
                }
            },
        ) { Text("Отправить оценку") }
        ErrorText(error)

        reviews.forEach { r ->
            Card(colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(12.dp)) {
                    Text("★".repeat(r.stars) + "☆".repeat(5 - r.stars), color = MaterialTheme.colorScheme.primary)
                    Text(r.userName, style = MaterialTheme.typography.labelMedium)
                    if (r.comment.isNotBlank()) Text(r.comment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        TextButton(onClick = { showReport = true }) { Text("Сообщить о мошенничестве или подозрительном приложении") }
    }

    if (showReport) {
        ReportDialog(
            onDismiss = { showReport = false },
            onSend = { reason, text ->
                scope.launch {
                    try { Api.report(a.id, reason, text); message = "Жалоба отправлена. Спасибо!" }
                    catch (e: Exception) { error = "Жалоба не отправлена: ${e.message}" }
                    showReport = false
                }
            },
        )
    }
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var reason by remember { mutableStateOf(REPORT_REASONS.first().first) }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пожаловаться") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                REPORT_REASONS.forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(selected = reason == key, onClick = { reason = key })
                        Text(label)
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Опишите проблему") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(enabled = text.isNotBlank(), onClick = { onSend(reason, text) }) { Text("Отправить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
