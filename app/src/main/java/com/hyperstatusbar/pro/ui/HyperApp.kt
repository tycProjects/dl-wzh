package com.hyperstatusbar.pro.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.hyperstatusbar.pro.data.BarSettings
import com.hyperstatusbar.pro.logging.AppLogger
import com.hyperstatusbar.pro.root.RootManager
import com.hyperstatusbar.pro.systemui.SystemUiController
import com.hyperstatusbar.pro.utils.DeviceInfo
import kotlinx.coroutines.launch

private val Black = Color(0xFF000000)
private val SurfaceColor = Color(0xFF111116)
private val Blue = Color(0xFF0A84FF)
private val Muted = Color(0xFF92929A)

@Composable
fun HyperApp(rootManager: RootManager, logger: AppLogger) {
    var rootOk by remember { mutableStateOf<Boolean?>(null) }
    var rootMessage by remember { mutableStateOf("Проверка root-доступа…") }
    var settings by remember { mutableStateOf(BarSettings()) }
    var page by remember { mutableStateOf("Главная") }
    val scope = rememberCoroutineScope()
    val device = remember { DeviceInfo.detect() }

    LaunchedEffect(Unit) {
        val state = rootManager.checkRoot()
        rootOk = state.available
        rootMessage = state.output.ifBlank { if (state.available) "Root доступен" else "Root не обнаружен" }
        logger.info("Startup", "Root=${state.available}; device=${device.manufacturer} ${device.model}; supported=${device.supported}")
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Black, surface = SurfaceColor, primary = Blue,
            secondary = Color(0xFFBF5AF2), tertiary = Color(0xFF30D158)
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Black) {
            MainContent(
                    page = page, onPage = { page = it },
                    settings = settings,
                    onSettings = {
                        settings = it
                        logger.info("Settings", "Изменены параметры интерфейса")
                    },
                    onApply = {
                        scope.launch {
                            logger.info("Apply", "Начата публикация конфигурации")
                            val result = SystemUiController(rootManager).publish(settings)
                            rootMessage = result.fold(
                                { logger.info("Apply", "Конфигурация опубликована"); "Конфигурация передана модулю" },
                                { logger.error("Apply", "Не удалось передать конфигурацию", it); "Ошибка: ${it.message}" }
                            )
                        }
                    },
                    rootMessage = rootMessage,
                    rootAvailable = rootOk == true,
                    onCheckRoot = {
                        scope.launch {
                            rootMessage = "Запрашиваем root-доступ…"
                            val state = rootManager.checkRoot()
                            rootOk = state.available
                            rootMessage = state.output.ifBlank { if (state.available) "Root доступен" else "Root не предоставлен" }
                        }
                    },
                    onRestartSystemUi = {
                        scope.launch {
                            if (rootOk != true) {
                                val state = rootManager.checkRoot()
                                rootOk = state.available
                                rootMessage = state.output
                                if (!state.available) return@launch
                            }
                            val result = rootManager.restartSystemUi()
                            rootMessage = result.fold({ "Команда перезапуска отправлена" }, { "Ошибка перезапуска: ${it.message}" })
                        }
                    },
                    deviceModel = device.model,
                    supported = device.supported,
                    logger = logger
                )
        }
    }
}

@Composable
private fun MainContent(
    page: String,
    onPage: (String) -> Unit,
    settings: BarSettings,
    onSettings: (BarSettings) -> Unit,
    onApply: () -> Unit,
    rootMessage: String,
    rootAvailable: Boolean,
    onCheckRoot: () -> Unit,
    onRestartSystemUi: () -> Unit,
    deviceModel: String,
    supported: Boolean,
    logger: AppLogger
) {
    val pages = listOf("Главная", "Отступы", "Размер", "Стиль", "Цвета", "Профили", "Экспорт", "Логи", "О приложении")
    var showRestartConfirm by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Black,
        bottomBar = {
            NavigationBar(containerColor = SurfaceColor) {
                listOf("Главная", "Отступы", "Размер", "Логи").forEach { name ->
                    val icon = when (name) {
                        "Главная" -> Icons.Rounded.Home
                        "Отступы" -> Icons.Rounded.OpenWith
                        "Размер" -> Icons.Rounded.AspectRatio
                        else -> Icons.Rounded.Terminal
                    }
                    NavigationBarItem(
                        selected = page == name,
                        onClick = { onPage(name) },
                        icon = { Icon(icon, null) },
                        label = { Text(name) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("HYPERSTATUSBAR", color = Blue, fontSize = 12.sp, letterSpacing = 2.sp)
                Text(page, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("OLED • Xiaomi / Redmi / POCO", color = Muted)
            }
            when (page) {
                "Главная" -> {
                    item { PreviewCard(settings) }
                    item { InfoCard("Root: ${if (rootAvailable) "доступен" else "непредоставлен"}\n$rootMessage\nУстройство: $deviceModel\nСемейство Xiaomi: ${if (supported) "обнаружено" else "неподтверждено"}") }
                    item {
                        OutlinedButton(onClick = onCheckRoot, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Security, null); Spacer(Modifier.width(8.dp)); Text(if (rootAvailable) "Проверить Root ещё раз" else "Запросить / проверить Root")
                        }
                    }
                    item {
                        Button(onClick = { showRestartConfirm = true }, modifier = Modifier.fillMaxWidth(), enabled = rootAvailable) {
                            Icon(Icons.Rounded.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Перезагрузить System UI")
                        }
                        if (!rootAvailable) Text("Для перезапуска System UI нужен выданный root-доступ", color = Muted, fontSize = 11.sp)
                    }
                    item { Button(onClick = onApply, modifier = Modifier.fillMaxWidth(), enabled = rootAvailable) { Text("Применить конфигурацию") } }
                    item { NavigationLinks(pages.drop(1), onPage) }
                }
                "Отступы" -> {
                    item { SettingSlider("Левые иконки", settings.leftOffsetPx.toFloat(), -50f, 50f, "px") { onSettings(settings.copy(leftOffsetPx = it.toInt())) } }
                    item { SettingSlider("Правые иконки", settings.rightOffsetPx.toFloat(), -50f, 50f, "px") { onSettings(settings.copy(rightOffsetPx = it.toInt())) } }
                    item { SettingSlider("Расстояние между иконками", settings.iconSpacingDp.toFloat(), 0f, 24f, "dp") { onSettings(settings.copy(iconSpacingDp = it.toInt())) } }
                    item { SettingSlider("Высота статус-бара", settings.barHeightDp.toFloat(), 20f, 48f, "dp") { onSettings(settings.copy(barHeightDp = it.toInt())) } }
                    item { SettingSlider("Прозрачность", settings.opacity, 0f, 1f, "коэф.") { onSettings(settings.copy(opacity = it)) } }
                }
                "Размер" -> {
                    item { SettingSlider("Все иконки", settings.iconScale, .7f, 1.5f, "×") { onSettings(settings.copy(iconScale = it)) } }
                    item { SettingSlider("Часы", settings.clockScale, .7f, 1.5f, "×") { onSettings(settings.copy(clockScale = it)) } }
                    item { SettingSlider("Батарея", settings.batteryScale, .7f, 1.5f, "×") { onSettings(settings.copy(batteryScale = it)) } }
                    item { SettingSlider("Сеть", settings.networkScale, .7f, 1.5f, "×") { onSettings(settings.copy(networkScale = it)) } }
                }
                "Стиль" -> item {
                    ChoiceCard("Стиль иконок", listOf("iOS", "Material", "Line", "Filled"), settings.iconStyle) {
                        onSettings(settings.copy(iconStyle = it))
                    }
                }
                "Цвета" -> item {
                    ChoiceCard("Цветовая палитра", listOf("Adaptive", "Ocean", "Sunset", "Forest", "Cyber", "Mono", "Neon", "Graphite", "Violet", "Rose"), settings.colorTheme) {
                        onSettings(settings.copy(colorTheme = it))
                    }
                }
                "Профили" -> {
                    item {
                        ChoiceCard("Профиль", listOf("Game", "Video", "Daily", "Custom"), settings.activeProfile) {
                            onSettings(settings.copy(activeProfile = it))
                        }
                    }
                    item { InfoCard("Автопереключение по активному приложению требует отдельного разрешения Usage Access и backend-логики.") }
                }
                "Экспорт" -> item {
                    InfoCard("Экспорт/импорт JSON — следующий этап. Пока конфигурация сохраняется в Magisk config bridge после нажатия «Применить».")
                }
                "Логи" -> item { LogsPanel(logger) }
                else -> item {
                    InfoCard("HyperStatusBar Pro • 0.3.0\nМодель: $deviceModel\nRoot: ${if (rootAvailable) "доступен" else "непредоставлен"}\nСовместимость SystemUI требует проверки на конкретной сборке.")
                }
            }
            item {
                if (page !in listOf("Логи", "О приложении")) {
                    Button(onClick = onApply, modifier = Modifier.fillMaxWidth(), enabled = rootAvailable) {
                        Text("Сохранить и передать модулю")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        if (showRestartConfirm) {
            AlertDialog(
                onDismissRequest = { showRestartConfirm = false },
                title = { Text("Перезапустить System UI?") },
                text = { Text("Панель статуса и шторка уведомлений могут на несколько секунд исчезнуть. Сохраните важные действия перед продолжением.") },
                confirmButton = { TextButton(onClick = { showRestartConfirm = false; onRestartSystemUi() }) { Text("Перезапустить") } },
                dismissButton = { TextButton(onClick = { showRestartConfirm = false }) { Text("Отмена") } }
            )
        }
    }
}

@Composable
private fun NavigationLinks(pages: List<String>, onPage: (String) -> Unit) {
    Column {
        pages.forEach { name ->
            TextButton(onClick = { onPage(name) }) { Text("Открыть: $name") }
        }
    }
}

@Composable
private fun LogsPanel(logger: AppLogger) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var snapshot by remember { mutableStateOf(logger.readAll()) }
    var notice by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { snapshot = logger.readAll() }

    Column(
        Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Диагностический журнал", color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(
            "Содержит события приложения и результаты его root-команд. Полный системный logcat и личные данные не собираются.",
            color = Muted,
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    snapshot = logger.readAll()
                    clipboard.setText(AnnotatedString(snapshot))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    notice = "Логи скопированы. Теперь вставьте их в чат."
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.ContentCopy, null)
                Spacer(Modifier.width(6.dp))
                Text("Скопировать")
            }
            OutlinedButton(
                onClick = {
                    snapshot = logger.readAll()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "HyperStatusBar Pro logs")
                        putExtra(Intent.EXTRA_TEXT, snapshot)
                    }
                    context.startActivity(Intent.createChooser(send, "Отправить логи"))
                    notice = "Открыто меню отправки."
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Share, null)
                Spacer(Modifier.width(6.dp))
                Text("Отправить")
            }
        }
        OutlinedButton(
            onClick = {
                logger.clear()
                snapshot = logger.readAll()
                notice = "Журнал очищен."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.DeleteOutline, null)
            Spacer(Modifier.width(6.dp))
            Text("Очистить логи")
        }
        if (notice.isNotBlank()) Text(notice, color = Color(0xFF30D158), fontSize = 12.sp)
        Text("Последние записи", color = Muted, fontSize = 12.sp)
        Text(
            snapshot.takeLast(5000),
            color = Color(0xFFB8B8C0),
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun PreviewCard(settings: BarSettings) {
    Column(Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(24.dp)).padding(18.dp)) {
        Text("LIVE PREVIEW", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("9:41", color = Color.White, fontSize = (15 * settings.clockScale).sp)
            Row(horizontalArrangement = Arrangement.spacedBy(settings.iconSpacingDp.dp)) {
                Text("▮▮▮", color = Color.White, fontSize = (12 * settings.networkScale).sp)
                Text("Wi-Fi", color = Color.White, fontSize = (12 * settings.iconScale).sp)
                Text("▰ 87%", color = Color.White, fontSize = (12 * settings.batteryScale).sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Симуляция внутри приложения — не системный статус-бар", color = Muted, fontSize = 11.sp)
        Text("Стиль: ${settings.iconStyle} • Тема: ${settings.colorTheme}", color = Color.White, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingSlider(title: String, value: Float, min: Float, max: Float, unit: String, onValue: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(18.dp)).padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium)
                Text("Нажмите, чтобы настроить", color = Muted, fontSize = 11.sp)
            }
            Text("${"%.2f".format(value)} $unit", color = Blue)
            IconButton(onClick = { expanded = true }) { Icon(Icons.Rounded.Tune, null, tint = Blue) }
        }
        Slider(
            value = value.coerceIn(min, max),
            onValueChange = {
                onValue(it)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            valueRange = min..max
        )
    }
    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF17171D),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Muted) }
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp)) {
                    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text("${"%.2f".format(value)} $unit", color = Blue, fontSize = 18.sp)
                    Slider(
                        value = value.coerceIn(min, max),
                        onValueChange = {
                            onValue(it)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        valueRange = min..max
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${"%.1f".format(min)} $unit", color = Muted, fontSize = 11.sp)
                        Text("${"%.1f".format(max)} $unit", color = Muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth()) { Text("Готово") }
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        options.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        label = { Text(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().background(SurfaceColor, RoundedCornerShape(18.dp)).padding(16.dp),
        color = Muted
    )
}
