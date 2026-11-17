package com.droidstore.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.droidstore.app.data.Api
import com.droidstore.app.data.AppForm
import com.droidstore.app.data.AppItem
import com.droidstore.app.data.CATEGORIES
import com.droidstore.app.data.STATUS_LABELS
import com.droidstore.app.data.Studio
import com.droidstore.app.ui.components.ErrorText
import com.droidstore.app.ui.components.SecurityNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Точка входа в DroidStore Studio: создание студии или управление приложениями. */
@Composable
fun StudioScreen(onOpenEditor: (studioId: Int, appId: Int?) -> Unit) {
    val scope = rememberCoroutineScope()
    var studios by remember { mutableStateOf<List<Studio>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }

    fun reload() {
        scope.launch {
            try {
                val list = Api.myStudios()
                studios = list
                apps = list.firstOrNull()?.let { Api.studioApps(it.id) }.orEmpty()
            } catch (e: Exception) { error = e.message }
        }
    }
    LaunchedEffect(Unit) { reload() }

    val list = studios
    when {
        list == null -> CircularProgressIndicator(Modifier.padding(24.dp))
        list.isEmpty() -> CreateStudioForm(onCreated = { reload() })
        else -> {
            val studio = list.first()
            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column {
                        Text(studio.name, style = MaterialTheme.typography.headlineSmall)
                        Text(studio.contactEmail, style = MaterialTheme.typography.bodySmall)
                        Text("Приложения студии", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                item {
                    Button(onClick = { onOpenEditor(studio.id, null) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Опубликовать новое приложение")
                    }
                }
                item { ErrorText(error) }
                items(apps.size) { i ->
                    val app = apps[i]
                    Card(
                        colors = CardDefaults.cardColors(),
                        modifier = Modifier.fillMaxWidth().clickable {
                            // Редактировать можно только черновики, отклонённые и с запрошенными изменениями
                            if (app.status in listOf("draft", "changes_requested", "rejected")) onOpenEditor(studio.id, app.id)
                        },
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(app.title, style = MaterialTheme.typography.titleMedium)
                            Text("${STATUS_LABELS[app.status] ?: app.status} · v${app.versionName ?: "—"} · ⬇ ${app.downloads}", style = MaterialTheme.typography.bodySmall)
                            if (app.status == "changes_requested" || app.status == "rejected") {
                                Text("Открыть для редактирования", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            if (app.securityStatus != null) SecurityNotice(app.securityStatus, app.securityScore, app.securityLimitations)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateStudioForm(onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Создание студии", style = MaterialTheme.typography.headlineSmall)
        Text("Студия нужна, чтобы публиковать приложения. Галочки Verified выдаёт администрация после проверки.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(name, { name = it }, label = { Text("Название студии") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Контактный email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(website, { website = it }, label = { Text("Сайт (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ErrorText(error)
        Button(
            enabled = !busy && name.isNotBlank() && email.isNotBlank(),
            onClick = {
                scope.launch {
                    busy = true
                    try {
                        Api.createStudio(name.trim(), description.trim(), email.trim(), website.trim())
                        onCreated()
                    } catch (e: Exception) {
                        error = "Не удалось создать студию: ${e.message}"
                    } finally { busy = false }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "Создаём..." else "Создать студию") }
    }
}

/** Редактор приложения: метаданные, APK, иконка, скриншоты и отправка на модерацию. */
@Composable
fun AppEditorScreen(studioId: Int, appId: Int?, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var shortDescription by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first().first) }
    var website by remember { mutableStateOf("") }
    var supportEmail by remember { mutableStateOf("") }
    var privacyUrl by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("1.0") }
    var versionCode by remember { mutableStateOf("1") }
    var releaseNotes by remember { mutableStateOf("") }
    var apkUri by remember { mutableStateOf<Uri?>(null) }
    var iconUri by remember { mutableStateOf<Uri?>(null) }
    var shotUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var status by remember { mutableStateOf("Черновик") }
    var progress by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentId by remember { mutableStateOf(appId) }
    var expanded by remember { mutableStateOf(false) }

    // Загрузка существующего приложения для редактирования
    LaunchedEffect(appId) {
        if (appId != null) try {
            val a = Api.appDetail(appId)
            title = a.title; packageName = a.packageName; shortDescription = a.shortDescription
            description = a.description; category = a.category; supportEmail = a.supportEmail
            privacyUrl = a.privacyPolicyUrl; website = ""
        } catch (e: Exception) { error = e.message }
    }

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { apkUri = it }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { iconUri = it }
    val shotPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { shotUris = it.take(10) }

    fun readBytes(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IllegalStateException("Не удалось прочитать файл")

    fun mimeOf(uri: Uri): String = context.contentResolver.getType(uri) ?: "image/png"

    fun publish() {
        scope.launch {
            error = null
            try {
                val form = AppForm(packageName.trim(), title.trim(), shortDescription.trim(), description.trim(), category, website.trim(), supportEmail.trim(), privacyUrl.trim())
                if (apkUri == null && currentId == null) throw IllegalStateException("Выберите APK-файл")
                progress = "Сохраняем карточку приложения..."
                val id = currentId ?: Api.createApp(studioId, form).id.also { currentId = it }
                if (currentId != null && appId != null) Api.updateApp(id, form, null, emptyList())

                apkUri?.let { uri ->
                    progress = "Загружаем APK..."
                    val bytes = withContext(Dispatchers.IO) { readBytes(uri) }
                    Api.uploadApk(id, bytes, versionCode.toIntOrNull() ?: 1, versionName.trim(), releaseNotes.trim())
                }
                var iconId: Int? = null
                iconUri?.let { uri ->
                    progress = "Загружаем иконку..."
                    val bytes = withContext(Dispatchers.IO) { readBytes(uri) }
                    iconId = Api.uploadImage("icon", bytes, mimeOf(uri))
                }
                val shotIds = mutableListOf<Int>()
                shotUris.forEachIndexed { i, uri ->
                    progress = "Загружаем скриншот ${i + 1} из ${shotUris.size}..."
                    val bytes = withContext(Dispatchers.IO) { readBytes(uri) }
                    shotIds += Api.uploadImage("screenshot", bytes, mimeOf(uri))
                }
                progress = "Сохраняем..."
                val current = Api.appDetail(id)
                Api.updateApp(id, form, iconId ?: current.iconId, if (shotIds.isNotEmpty()) shotIds else current.screenshotIds)

                progress = "Отправляем на модерацию и проверку безопасности..."
                val submitted = Api.submit(id)
                status = STATUS_LABELS[submitted.status] ?: submitted.status
                progress = null
                onDone()
            } catch (e: Exception) {
                progress = null
                error = e.message?.let { "Ошибка: $it" } ?: "Неизвестная ошибка"
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (appId == null) "Новое приложение" else "Редактирование", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(packageName, { packageName = it }, label = { Text("Package name, например com.studio.app") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(shortDescription, { shortDescription = it }, label = { Text("Краткое описание") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Полное описание") }, modifier = Modifier.fillMaxWidth())

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = CATEGORIES.first { it.first == category }.second,
                onValueChange = {},
                readOnly = true,
                label = { Text("Категория") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CATEGORIES.forEach { (key, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { category = key; expanded = false })
                }
            }
        }

        OutlinedTextField(website, { website = it }, label = { Text("Сайт") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(supportEmail, { supportEmail = it }, label = { Text("Email поддержки") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(privacyUrl, { privacyUrl = it }, label = { Text("Ссылка на политику конфиденциальности") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(versionName, { versionName = it }, label = { Text("Версия") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(versionCode, { versionCode = it }, label = { Text("Номер версии") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(releaseNotes, { releaseNotes = it }, label = { Text("Что нового") }, modifier = Modifier.fillMaxWidth())

        OutlinedButton(onClick = { apkPicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (apkUri == null) "Выбрать APK" else "APK выбран ✓")
        }
        OutlinedButton(onClick = { iconPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (iconUri == null) "Выбрать иконку" else "Иконка выбрана ✓")
        }
        OutlinedButton(onClick = { shotPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (shotUris.isEmpty()) "Выбрать скриншоты (до 10)" else "Скриншотов: ${shotUris.size}")
        }

        progress?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        ErrorText(error)
        Button(
            enabled = progress == null && title.isNotBlank() && packageName.isNotBlank(),
            onClick = { publish() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Отправить на модерацию") }
        Text("Статус: $status", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}
