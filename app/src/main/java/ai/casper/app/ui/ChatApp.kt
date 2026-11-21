package ai.casper.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import ai.casper.app.ChatViewModel
import ai.casper.app.data.ModelInfo
import ai.casper.app.data.StoredMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatApp(vm: ChatViewModel) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var dropdownOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var freeOnly by remember { mutableStateOf(false) }
    val modelName = vm.models.firstOrNull { it.id == vm.model }?.name ?: vm.model
    val filtered = vm.models.filter {
        (!freeOnly || it.isFree) && "${it.id} ${it.name}".contains(query.trim(), ignoreCase = true)
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Text("Диалоги", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(
                    label = { Text("Новый диалог") },
                    selected = false,
                    onClick = {
                        vm.newChat()
                        scope.launch { drawer.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Настройки") },
                    selected = false,
                    onClick = {
                        showSettings = true
                        scope.launch { drawer.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider()
                LazyColumn {
                    items(vm.chats, key = { it.id }) { c ->
                        NavigationDrawerItem(
                            label = { Text(c.title, maxLines = 1) },
                            selected = c.id == vm.activeId,
                            onClick = {
                                vm.open(c)
                                scope.launch { drawer.close() }
                            },
                            badge = {
                                IconButton(onClick = { vm.delete(c) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Casper.Card,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Casper.Card),
                    title = {
                        SpringDropdown(
                            label = modelName,
                            expanded = dropdownOpen,
                            onExpandedChange = { dropdownOpen = it }
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("Поиск модели") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = freeOnly, onCheckedChange = { freeOnly = it })
                                Text("Только бесплатные", style = MaterialTheme.typography.bodySmall)
                            }
                            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                                items(filtered, key = { it.id }) { m ->
                                    ModelRow(m) {
                                        vm.setModel(m.id)
                                        dropdownOpen = false
                                        query = ""
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    Composer(
                        vm = vm,
                        onNewChat = { vm.newChat() },
                        onPickModel = { dropdownOpen = true }
                    )
                    RubberDock(
                        tabs = listOf(
                            DockTab("История", Icons.Default.History) { scope.launch { drawer.open() } },
                            DockTab("Новый чат", Icons.Default.Edit) { vm.newChat() },
                            DockTab("Модели", Icons.Default.Tune) { dropdownOpen = true },
                        ),
                        selected = if (dropdownOpen) 2 else 1
                    )
                }
            }
        ) { padding ->
            MessageList(vm, Modifier.padding(padding))
        }
    }

    if (showSettings) {
        SettingsSheet(vm, onDismiss = { showSettings = false })
    }
}

@Composable
fun ModelRow(m: ModelInfo, onPick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(m.name, maxLines = 1)
        Text(
            "${m.provider} · ${priceText(m)}",
            style = MaterialTheme.typography.bodySmall,
            color = Casper.Muted
        )
    }
}

@Composable
fun MessageList(vm: ChatViewModel, modifier: Modifier) {
    if (vm.messages.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Чем помочь?", style = MaterialTheme.typography.titleLarge)
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(vm.messages.size, vm.messages.lastOrNull()?.content?.length) {
        listState.animateScrollToItem(vm.messages.lastIndex)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(vm.messages) { m -> Bubble(m) }
    }
}

@Composable
fun Bubble(m: StoredMessage) {
    val isUser = m.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else Casper.Panel,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                m.images.forEach { url ->
                    val bmp = remember(url) { decodeImage(url) }
                    if (bmp != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 6.dp).size(160.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                Text(m.content.ifEmpty { "…" })
            }
        }
    }
}

@Composable
fun Composer(vm: ChatViewModel, onNewChat: () -> Unit, onPickModel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            when {
                bytes == null -> vm.reportError("Не удалось открыть изображение")
                bytes.size > 5_000_000 -> vm.reportError("Изображение больше 5 МБ")
                else -> {
                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    pending = pending + "data:$mime;base64,$b64"
                }
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            when {
                bytes == null -> vm.reportError("Не удалось открыть файл")
                bytes.size > 1_000_000 -> vm.reportError("Файл больше 1 МБ")
                bytes.take(1000).any { it == 0.toByte() } -> vm.reportError("Поддерживаются только текстовые файлы")
                else -> {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
                    val body = String(bytes, Charsets.UTF_8)
                    text = (if (text.isBlank()) "" else text + "\n\n") + "Файл $name:\n```\n$body\n```"
                }
            }
        }
    }
    val actions = listOf(
        GooAction("Файл", Icons.Default.AttachFile) { picker.launch("*/*") },
        GooAction("Фото", Icons.Default.Image) { imagePicker.launch("image/*") },
        GooAction("Новый чат", Icons.Default.Add) { onNewChat() },
        GooAction("Модель", Icons.Default.Tune) { onPickModel() },
    )

    Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding()) {
        vm.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (pending.isNotEmpty()) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Изображений: ${pending.size}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { pending = emptyList() }) { Text("Убрать") }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            GooMenu(actions = actions, enabled = !vm.busy)
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Напишите запрос") },
                maxLines = 5,
                shape = RoundedCornerShape(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            if (vm.busy) {
                Button(onClick = { vm.stop() }) {
                    Icon(Icons.Default.Stop, contentDescription = "Стоп")
                }
            } else {
                Button(
                    onClick = {
                        vm.send(text, pending)
                        text = ""
                        pending = emptyList()
                    },
                    enabled = text.isNotBlank() || pending.isNotEmpty()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Отправить")
                }
            }
        }
    }
}

fun priceText(m: ModelInfo): String = when {
    m.isFree -> "бесплатно"
    m.pricePrompt != null -> "\$%.2f / 1M токенов".format(m.pricePrompt * 1_000_000)
    else -> "цена неизвестна"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(vm: ChatViewModel, onDismiss: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Настройки", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text("Модель по умолчанию: ${vm.defaultModel}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Применяется к новым диалогам. Выбирается через меню модели.",
                style = MaterialTheme.typography.bodySmall,
                color = Casper.Muted
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.setDefaultModel(vm.model) }, modifier = Modifier.fillMaxWidth()) {
                Text("Сделать текущую моделью по умолчанию")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Удалить все диалоги на телефоне")
            }
            if (confirm) {
                AlertDialog(
                    onDismissRequest = { confirm = false },
                    title = { Text("Удалить все диалоги?") },
                    text = { Text("Диалоги на этом телефоне будут удалены без возможности восстановления.") },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.clearAll()
                            confirm = false
                            onDismiss()
                        }) { Text("Удалить") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirm = false }) { Text("Отмена") }
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
