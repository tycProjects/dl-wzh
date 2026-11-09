package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClipboardRepository
import com.example.data.KeyboardSettingsManager
import com.example.feedback.HapticSoundManager
import com.example.model.*
import com.example.ui.keyboard.SVKeyboardView
import com.example.ui.keyboard.ThemePaletteResolver

enum class StudioTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    STYLES("Theme & Glass", Icons.Filled.Palette),
    WALLPAPERS("Wallpapers", Icons.Filled.Wallpaper),
    FONTS("Fonts & Icons", Icons.Filled.FontDownload),
    FEEDBACK("Haptics & Sound", Icons.Filled.Vibration),
    TYPING("Smart Typing", Icons.Filled.Spellcheck),
    CLIPBOARD("Clipboard", Icons.Filled.ContentPaste)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardStudioScreen(
    settingsManager: KeyboardSettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsManager.settingsFlow.collectAsState()
    val clipboardRepo = remember { ClipboardRepository.getInstance(context) }
    val clips by clipboardRepo.clips.collectAsState()
    val hapticSoundManager = remember { HapticSoundManager.getInstance(context) }

    var selectedTab by remember { mutableStateOf(StudioTab.STYLES) }
    var testText by remember { mutableStateOf("Welcome to SV Keyboard! Type here to test liquid glass & haptics ✨") }
    var newClipText by remember { mutableStateOf("") }
    var showAddClipDialog by remember { mutableStateOf(false) }

    // Check system IME status
    var isImeEnabled by remember { mutableStateOf(false) }
    var isImeActive by remember { mutableStateOf(false) }

    val checkImeStatus = {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null) {
            val enabledList = imm.enabledInputMethodList
            val myPackage = context.packageName
            isImeEnabled = enabledList.any { it.packageName == myPackage }
            // Note: checking default IME on Android API 34+ is restricted, but we can verify enabled status accurately
            val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            isImeActive = currentIme?.contains(myPackage) == true
        }
    }

    LaunchedEffect(Unit) {
        checkImeStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFFD500F9))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SV",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SV KEYBOARD",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Customization Studio & Live Test Lab",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                            checkImeStatus()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = "Switch Keyboard",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch IME", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Activation Banner if not yet active or enabled
            ActivationCard(
                isImeEnabled = isImeEnabled,
                isImeActive = isImeActive,
                onEnableClick = {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                onSelectClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showInputMethodPicker()
                    checkImeStatus()
                }
            )

            // Live Testing Notepad Area
            NotepadArea(
                text = testText,
                onTextChange = { testText = it },
                onClear = { testText = "" },
                onCopy = {
                    clipboardRepo.copyToSystemClipboard(testText)
                }
            )

            // Customization Tab Navigation
            StudioTabRow(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it }
            )

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    StudioTab.STYLES -> StyleSettingsContent(
                        settings = settings,
                        onUpdate = { settingsManager.updateSettings(it) }
                    )
                    StudioTab.WALLPAPERS -> WallpaperSettingsContent(
                        settings = settings,
                        onUpdate = { settingsManager.updateSettings(it) }
                    )
                    StudioTab.FONTS -> FontAndIconSettingsContent(
                        settings = settings,
                        onUpdate = { settingsManager.updateSettings(it) }
                    )
                    StudioTab.FEEDBACK -> FeedbackSettingsContent(
                        settings = settings,
                        onUpdate = { settingsManager.updateSettings(it) },
                        onTestHaptic = {
                            hapticSoundManager.triggerFeedback(settings, isHeavy = true)
                        }
                    )
                    StudioTab.TYPING -> SmartTypingSettingsContent(
                        settings = settings,
                        onUpdate = { settingsManager.updateSettings(it) }
                    )
                    StudioTab.CLIPBOARD -> ClipboardSettingsContent(
                        clips = clips,
                        onAddClip = { showAddClipDialog = true },
                        onTogglePin = { clipboardRepo.togglePin(it) },
                        onDeleteClip = { clipboardRepo.deleteClip(it) },
                        onClearUnpinned = { clipboardRepo.clearUnpinned() }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Embedded Live Interactive Keyboard View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color.Black)
            ) {
                SVKeyboardView(
                    settings = settings,
                    onCommitText = { char ->
                        testText += char
                    },
                    onDeleteSurroundingText = { beforeLength, _ ->
                        if (testText.isNotEmpty()) {
                            testText = testText.dropLast(beforeLength.coerceAtMost(testText.length))
                        }
                    },
                    onEnterAction = {
                        testText += "\n"
                    },
                    onCursorMove = { /* Cursor preview in test box */ },
                    onSelectAllText = { /* Select all preview */ },
                    onCopyText = { clipboardRepo.copyToSystemClipboard(testText) },
                    onCutText = {
                        clipboardRepo.copyToSystemClipboard(testText)
                        testText = ""
                    },
                    onPasteText = {
                        val latest = clips.firstOrNull()?.text ?: ""
                        testText += latest
                    },
                    onSwitchIme = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                    onOpenSettingsInApp = {
                        selectedTab = StudioTab.STYLES
                    }
                )
            }
        }
    }

    if (showAddClipDialog) {
        AlertDialog(
            onDismissRequest = { showAddClipDialog = false },
            title = { Text("Add Clipboard Snippet") },
            text = {
                OutlinedTextField(
                    value = newClipText,
                    onValueChange = { newClipText = it },
                    label = { Text("Snippet or Template") },
                    placeholder = { Text("e.g. My email or address...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newClipText.isNotBlank()) {
                            clipboardRepo.addClip(newClipText.trim(), isPinned = true)
                            newClipText = ""
                            showAddClipDialog = false
                        }
                    }
                ) {
                    Text("Save & Pin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClipDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActivationCard(
    isImeEnabled: Boolean,
    isImeActive: Boolean,
    onEnableClick: () -> Unit,
    onSelectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isImeActive) {
                Color(0xFF00E676).copy(alpha = 0.12f)
            } else if (isImeEnabled) {
                Color(0xFF00E5FF).copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isImeActive) Icons.Filled.CheckCircle else Icons.Filled.Info,
                    contentDescription = null,
                    tint = if (isImeActive) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isImeActive) "SV Keyboard is Active System Keyboard"
                               else if (isImeEnabled) "SV Keyboard is Enabled! Tap to Select"
                               else "Enable SV Keyboard for Android",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isImeActive) "Ready to use in WhatsApp, Chrome, Notes & all apps"
                               else if (isImeEnabled) "Step 2: Switch default keyboard to SV Keyboard"
                               else "Step 1: Turn on in Android Keyboard Settings",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isImeEnabled) {
                Button(
                    onClick = onEnableClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("1. Enable", fontSize = 11.sp)
                }
            } else if (!isImeActive) {
                Button(
                    onClick = onSelectClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("2. Select", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun NotepadArea(
    text: String,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Live Interactive Typing Test Pad",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Copy", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Clear", fontSize = 11.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp, max = 74.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Touch keyboard keys below to start typing...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioTabRow(
    selectedTab: StudioTab,
    onTabSelect: (StudioTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        StudioTab.values().forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelect(tab) },
                text = { Text(tab.label, fontSize = 12.sp, fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
private fun StyleSettingsContent(
    settings: KeyboardSettings,
    onUpdate: ((KeyboardSettings) -> KeyboardSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Key Border Style Section
        item {
            Text(
                text = "Key Border & Glass Style",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeyBorderStyle.values().forEach { style ->
                    val isSelected = style == settings.keyBorderStyle
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onUpdate { it.copy(keyBorderStyle = style) }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFD500F9)))) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (style) {
                                    KeyBorderStyle.BORDERLESS -> Icons.Outlined.CropFree
                                    KeyBorderStyle.BORDERED -> Icons.Outlined.BorderAll
                                    KeyBorderStyle.LIQUID_GLASS -> Icons.Filled.BlurOn
                                },
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = style.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Liquid Glass Opacity Slider
        if (settings.keyBorderStyle == KeyBorderStyle.LIQUID_GLASS) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Liquid Glass Translucency", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${(settings.glassOpacity * 100).toInt()}%", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.glassOpacity,
                            onValueChange = { opacity ->
                                onUpdate { it.copy(glassOpacity = opacity) }
                            },
                            valueRange = 0.15f..0.70f
                        )
                        Text(
                            "Balances the frosted gloss specular shine with background wallpaper visibility",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Color Theme Presets
        item {
            Text(
                text = "Theme Customization (Dark Mode & OLED Included)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyboardThemePreset.values().forEach { preset ->
                    val isSelected = preset == settings.themePreset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                onUpdate { it.copy(themePreset = preset) }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onUpdate { it.copy(themePreset = preset) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preset.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperSettingsContent(
    settings: KeyboardSettings,
    onUpdate: ((KeyboardSettings) -> KeyboardSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Keyboard Wallpapers & Backdrops",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pair aesthetic wallpapers with Liquid Glass for an exquisite frosted depth",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(KeyboardWallpaper.values()) { wallpaper ->
            val isSelected = wallpaper == settings.wallpaper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onUpdate { it.copy(wallpaper = wallpaper) } }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onUpdate { it.copy(wallpaper = wallpaper) } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wallpaper.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (wallpaper.isDrawable) "High definition visual asset" else "Smooth dynamic procedural backdrop",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FontAndIconSettingsContent(
    settings: KeyboardSettings,
    onUpdate: ((KeyboardSettings) -> KeyboardSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Font Selection
        item {
            Text(
                text = "Keycap Font Style",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(KeyboardFontFamily.values()) { font ->
            val isSelected = font == settings.fontFamily
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onUpdate { it.copy(fontFamily = font) } }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onUpdate { it.copy(fontFamily = font) } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = font.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Sample: ${font.sample}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Icon Style Selection
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Utility Icon Style",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(KeyboardIconStyle.values()) { iconStyle ->
            val isSelected = iconStyle == settings.iconStyle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onUpdate { it.copy(iconStyle = iconStyle) } }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onUpdate { it.copy(iconStyle = iconStyle) } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = iconStyle.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FeedbackSettingsContent(
    settings: KeyboardSettings,
    onUpdate: ((KeyboardSettings) -> KeyboardSettings) -> Unit,
    onTestHaptic: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic Feedback (Vibration)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Tactile physical vibration on each key tap", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.hapticEnabled,
                            onCheckedChange = { enabled ->
                                onUpdate { it.copy(hapticEnabled = enabled) }
                            }
                        )
                    }

                    if (settings.hapticEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Vibration Strength", fontSize = 12.sp)
                            Text("${settings.hapticStrength} ms", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.hapticStrength.toFloat(),
                            onValueChange = { strength ->
                                onUpdate { it.copy(hapticStrength = strength.toInt()) }
                            },
                            valueRange = 10f..70f,
                            steps = 5
                        )
                        Button(
                            onClick = onTestHaptic,
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Haptic", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Keypress Click Sound", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Audible mechanical tick sound effect on key tap", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.soundEnabled,
                            onCheckedChange = { enabled ->
                                onUpdate { it.copy(soundEnabled = enabled) }
                            }
                        )
                    }

                    if (settings.soundEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Click Volume", fontSize = 12.sp)
                            Text("${(settings.soundVolume * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.soundVolume,
                            onValueChange = { vol ->
                                onUpdate { it.copy(soundVolume = vol) }
                            },
                            valueRange = 0.1f..1.0f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartTypingSettingsContent(
    settings: KeyboardSettings,
    onUpdate: ((KeyboardSettings) -> KeyboardSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SettingSwitchRow(
                title = "Auto-Correction",
                subtitle = "Automatically fixes typos & expands shortcuts when Space is pressed",
                checked = settings.autoCorrectEnabled,
                onCheckedChange = { onUpdate { s -> s.copy(autoCorrectEnabled = it) } }
            )
        }

        item {
            SettingSwitchRow(
                title = "Word Suggestions & Predictions",
                subtitle = "Show top candidates and emoji hints above the keyboard",
                checked = settings.suggestionsEnabled,
                onCheckedChange = { onUpdate { s -> s.copy(suggestionsEnabled = it) } }
            )
        }

        item {
            SettingSwitchRow(
                title = "Auto-Capitalization",
                subtitle = "Capitalizes first letter of new sentences automatically",
                checked = settings.autoCapitalization,
                onCheckedChange = { onUpdate { s -> s.copy(autoCapitalization = it) } }
            )
        }

        item {
            SettingSwitchRow(
                title = "Double-Space for Period",
                subtitle = "Tapping spacebar twice quickly inserts a period and space",
                checked = settings.doubleSpacePeriod,
                onCheckedChange = { onUpdate { s -> s.copy(doubleSpacePeriod = it) } }
            )
        }

        item {
            SettingSwitchRow(
                title = "Dedicated Number Row on Top",
                subtitle = "Show permanent 1-0 number keys above the QWERTY row",
                checked = settings.numberRowEnabled,
                onCheckedChange = { onUpdate { s -> s.copy(numberRowEnabled = it) } }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Keyboard Key Height", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyHeight.values().forEach { height ->
                            val isSelected = height == settings.keyHeight
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdate { it.copy(keyHeight = height) } },
                                label = { Text(height.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ClipboardSettingsContent(
    clips: List<ClipboardItem>,
    onAddClip: () -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onClearUnpinned: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clipboard Snippets & Templates",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                TextButton(onClick = onClearUnpinned) {
                    Text("Clear Recents", fontSize = 12.sp)
                }
                Button(
                    onClick = onAddClip,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No clips saved yet. Tap '+ Add' to save a custom snippet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(clips, key = { it.id }) { clip ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (clip.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = clip.text,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onTogglePin(clip.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = if (clip.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "Pin",
                                    tint = if (clip.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteClip(clip.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
