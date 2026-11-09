package com.example.ui.keyboard

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.autocorrect.AutoCorrectEngine
import com.example.data.ClipboardRepository
import com.example.feedback.HapticSoundManager
import com.example.model.*

@Composable
fun SVKeyboardView(
    settings: KeyboardSettings,
    onCommitText: (String) -> Unit,
    onDeleteSurroundingText: (beforeLength: Int, afterLength: Int) -> Unit,
    onEnterAction: () -> Unit,
    onCursorMove: (delta: Int) -> Unit,
    onSelectAllText: () -> Unit,
    onCopyText: () -> Unit,
    onCutText: () -> Unit,
    onPasteText: () -> Unit,
    onSwitchIme: () -> Unit,
    onOpenSettingsInApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticSoundManager = remember { HapticSoundManager.getInstance(context) }
    val clipboardRepo = remember { ClipboardRepository.getInstance(context) }
    val clips by clipboardRepo.clips.collectAsState()

    var keyboardMode by remember { mutableStateOf(KeyboardMode.QWERTY) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var currentWordBuffer by remember { mutableStateOf("") }

    val palette = remember(settings.themePreset, settings.keyBorderStyle, settings.glassOpacity) {
        ThemePaletteResolver.resolvePalette(settings.themePreset, settings.keyBorderStyle, settings.glassOpacity)
    }
    val fontFamily = ThemePaletteResolver.resolveFontFamily(settings.fontFamily)

    val suggestions = remember(currentWordBuffer, settings.autoCorrectEnabled, settings.suggestionsEnabled) {
        if (settings.suggestionsEnabled) {
            AutoCorrectEngine.getSuggestions(currentWordBuffer, settings.autoCorrectEnabled)
        } else {
            emptyList()
        }
    }

    // Handle any key action
    val handleAction: (KeyAction) -> Unit = { action ->
        hapticSoundManager.triggerFeedback(settings, view)
        when (action) {
            is KeyAction.Character -> {
                val toCommit = when {
                    shiftState != ShiftState.OFF && action.char.length == 1 && action.char[0].isLetter() ->
                        action.char.uppercase()
                    else -> action.char
                }
                onCommitText(toCommit)
                if (shiftState == ShiftState.ON) {
                    shiftState = ShiftState.OFF
                }
                if (toCommit.length == 1 && toCommit[0].isLetter()) {
                    currentWordBuffer += toCommit
                } else {
                    currentWordBuffer = ""
                }
            }

            is KeyAction.Backspace -> {
                onDeleteSurroundingText(1, 0)
                if (currentWordBuffer.isNotEmpty()) {
                    currentWordBuffer = currentWordBuffer.dropLast(1)
                }
            }

            is KeyAction.Shift -> {
                shiftState = when (shiftState) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
            }

            is KeyAction.Space -> {
                if (settings.autoCorrectEnabled && currentWordBuffer.isNotEmpty()) {
                    val corrected = AutoCorrectEngine.checkAutoCorrection(currentWordBuffer)
                    if (corrected != currentWordBuffer) {
                        onDeleteSurroundingText(currentWordBuffer.length, 0)
                        onCommitText(corrected)
                    }
                }
                onCommitText(" ")
                currentWordBuffer = ""
                if (settings.autoCapitalization && shiftState == ShiftState.OFF) {
                    // Could auto-capitalize next sentence if after period
                }
            }

            is KeyAction.Enter -> {
                currentWordBuffer = ""
                onEnterAction()
            }

            is KeyAction.SwitchMode -> {
                keyboardMode = action.mode
            }

            is KeyAction.OpenEmoji -> {
                keyboardMode = KeyboardMode.EMOJI
            }

            is KeyAction.OpenClipboard -> {
                clipboardRepo.syncWithSystemClipboard()
                keyboardMode = KeyboardMode.CLIPBOARD
            }

            is KeyAction.OpenTextEdit -> {
                keyboardMode = KeyboardMode.TEXT_EDIT
            }

            is KeyAction.OpenSettings -> {
                onOpenSettingsInApp()
            }

            is KeyAction.SwitchToNextIme -> {
                onSwitchIme()
            }

            is KeyAction.CursorLeft -> onCursorMove(-1)
            is KeyAction.CursorRight -> onCursorMove(1)
            is KeyAction.CursorUp -> onCursorMove(-10)
            is KeyAction.CursorDown -> onCursorMove(10)
            is KeyAction.SelectAll -> onSelectAllText()
            is KeyAction.CopyText -> onCopyText()
            is KeyAction.CutText -> onCutText()
            is KeyAction.PasteText -> onPasteText()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("sv_keyboard_container")
    ) {
        // Wallpaper Background layer
        KeyboardBackground(settings = settings, palette = palette)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
            // Top Toolbar: Utilities & Quick Mode Switchers
            KeyboardTopBar(
                currentMode = keyboardMode,
                palette = palette,
                settings = settings,
                onModeChange = { keyboardMode = it },
                onOpenSettings = onOpenSettingsInApp
            )

            // Auto-Correct Suggestion Bar
            if (settings.suggestionsEnabled && keyboardMode == KeyboardMode.QWERTY) {
                SuggestionBar(
                    suggestions = suggestions,
                    palette = palette,
                    fontFamily = fontFamily,
                    onSelectSuggestion = { suggestion ->
                        hapticSoundManager.triggerFeedback(settings, view)
                        if (currentWordBuffer.isNotEmpty()) {
                            onDeleteSurroundingText(currentWordBuffer.length, 0)
                        }
                        onCommitText("$suggestion ")
                        currentWordBuffer = ""
                    }
                )
            }

            // Keyboard Body according to active mode
            when (keyboardMode) {
                KeyboardMode.QWERTY -> {
                    QwertyLayout(
                        settings = settings,
                        palette = palette,
                        shiftState = shiftState,
                        fontFamily = fontFamily,
                        onKeyAction = handleAction
                    )
                }

                KeyboardMode.SYMBOLS -> {
                    SymbolsLayout(
                        settings = settings,
                        palette = palette,
                        fontFamily = fontFamily,
                        onKeyAction = handleAction
                    )
                }

                KeyboardMode.MORE_SYMBOLS -> {
                    MoreSymbolsLayout(
                        settings = settings,
                        palette = palette,
                        fontFamily = fontFamily,
                        onKeyAction = handleAction
                    )
                }

                KeyboardMode.EMOJI -> {
                    EmojiKeyboard(
                        palette = palette,
                        settings = settings,
                        fontFamily = fontFamily,
                        onEmojiSelected = { emoji ->
                            hapticSoundManager.triggerFeedback(settings, view)
                            onCommitText(emoji)
                        },
                        onBackspace = {
                            handleAction(KeyAction.Backspace)
                        },
                        onBackToQwerty = {
                            keyboardMode = KeyboardMode.QWERTY
                        }
                    )
                }

                KeyboardMode.CLIPBOARD -> {
                    ClipboardDrawer(
                        clips = clips,
                        palette = palette,
                        settings = settings,
                        fontFamily = fontFamily,
                        onClipSelected = { text ->
                            hapticSoundManager.triggerFeedback(settings, view)
                            onCommitText(text)
                        },
                        onTogglePin = { id -> clipboardRepo.togglePin(id) },
                        onDeleteClip = { id -> clipboardRepo.deleteClip(id) },
                        onClearUnpinned = { clipboardRepo.clearUnpinned() },
                        onBackToQwerty = { keyboardMode = KeyboardMode.QWERTY }
                    )
                }

                KeyboardMode.TEXT_EDIT -> {
                    TextEditPad(
                        palette = palette,
                        settings = settings,
                        fontFamily = fontFamily,
                        onAction = handleAction,
                        onBackToQwerty = { keyboardMode = KeyboardMode.QWERTY }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardBackground(settings: KeyboardSettings, palette: KeyboardPalette) {
    when (settings.wallpaper) {
        KeyboardWallpaper.AURORA_GLASS -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.sv_wallpaper_aurora_1794219730447),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Frosted dark tint scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )
            }
        }

        KeyboardWallpaper.CYBER_GRID -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.sv_wallpaper_cyber_1794219746796),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.60f))
                )
            }
        }

        KeyboardWallpaper.GRADIENT_SUNSET -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3F1948), Color(0xFF9C27B0), Color(0xFFE91E63), Color(0xFFFF9800))
                        )
                    )
            )
        }

        KeyboardWallpaper.GRADIENT_COSMIC -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77), Color(0xFF778DA9))
                        )
                    )
            )
        }

        KeyboardWallpaper.GRADIENT_EMERALD -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF004D40), Color(0xFF00796B), Color(0xFF009688), Color(0xFF80CBC4))
                        )
                    )
            )
        }

        KeyboardWallpaper.NONE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.backgroundBrush)
            )
        }
    }
}

@Composable
private fun KeyboardTopBar(
    currentMode: KeyboardMode,
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    onModeChange: (KeyboardMode) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Mode toggle buttons
            TopBarIconBtn(
                icon = Icons.Outlined.SentimentSatisfiedAlt,
                label = "Emoji",
                isActive = currentMode == KeyboardMode.EMOJI,
                palette = palette,
                onClick = {
                    onModeChange(if (currentMode == KeyboardMode.EMOJI) KeyboardMode.QWERTY else KeyboardMode.EMOJI)
                }
            )

            TopBarIconBtn(
                icon = Icons.Outlined.ContentPaste,
                label = "Clipboard",
                isActive = currentMode == KeyboardMode.CLIPBOARD,
                palette = palette,
                onClick = {
                    onModeChange(if (currentMode == KeyboardMode.CLIPBOARD) KeyboardMode.QWERTY else KeyboardMode.CLIPBOARD)
                }
            )

            TopBarIconBtn(
                icon = Icons.Outlined.EditNote,
                label = "Cursor Pad",
                isActive = currentMode == KeyboardMode.TEXT_EDIT,
                palette = palette,
                onClick = {
                    onModeChange(if (currentMode == KeyboardMode.TEXT_EDIT) KeyboardMode.QWERTY else KeyboardMode.TEXT_EDIT)
                }
            )
        }

        // Right side: Quick Settings Studio
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // App Branding Badge
            Text(
                text = "SV",
                color = palette.accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            TopBarIconBtn(
                icon = Icons.Outlined.Tune,
                label = "Settings",
                isActive = false,
                palette = palette,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun TopBarIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    palette: KeyboardPalette,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (isActive) palette.accentColor.copy(alpha = 0.25f) else Color.Transparent)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) palette.accentColor else palette.specialTextColor.copy(alpha = 0.70f),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun SuggestionBar(
    suggestions: List<String>,
    palette: KeyboardPalette,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    onSelectSuggestion: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(suggestions) { candidate ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.suggestionBg)
                    .clickable { onSelectSuggestion(candidate) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = candidate,
                    color = palette.suggestionText,
                    fontSize = 13.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QwertyLayout(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    shiftState: ShiftState,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    onKeyAction: (KeyAction) -> Unit
) {
    val keyHeight = settings.keyHeight.heightDp.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        // Optional Number Row
        if (settings.numberRowEnabled) {
            Row(modifier = Modifier.fillMaxWidth().height(keyHeight * 0.85f)) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { num ->
                    KeyButton(
                        key = KeyModel(primaryText = num, action = KeyAction.Character(num)),
                        palette = palette,
                        settings = settings,
                        shiftState = shiftState,
                        fontFamily = fontFamily,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onKeyAction = onKeyAction
                    )
                }
            }
        }

        // Row 1: q w e r t y u i o p
        val row1 = listOf(
            Pair("q", "1"), Pair("w", "2"), Pair("e", "3"), Pair("r", "4"),
            Pair("t", "5"), Pair("y", "6"), Pair("u", "7"), Pair("i", "8"),
            Pair("o", "9"), Pair("p", "0")
        )
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            row1.forEach { (char, numHint) ->
                KeyButton(
                    key = KeyModel(primaryText = char, secondaryText = numHint, action = KeyAction.Character(char)),
                    palette = palette,
                    settings = settings,
                    shiftState = shiftState,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }
        }

        // Row 2: a s d f g h j k l
        val row2 = listOf(
            Pair("a", "@"), Pair("s", "#"), Pair("d", "\$"), Pair("f", "_"),
            Pair("g", "&"), Pair("h", "-"), Pair("j", "+"), Pair("k", "("), Pair("l", ")")
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(keyHeight)
                .padding(horizontal = 14.dp)
        ) {
            row2.forEach { (char, hint) ->
                KeyButton(
                    key = KeyModel(primaryText = char, secondaryText = hint, action = KeyAction.Character(char)),
                    palette = palette,
                    settings = settings,
                    shiftState = shiftState,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }
        }

        // Row 3: Shift, z x c v b n m, Backspace
        val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            // Shift key
            KeyButton(
                key = KeyModel(primaryText = "shift", action = KeyAction.Shift, isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = shiftState,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            row3.forEach { char ->
                KeyButton(
                    key = KeyModel(primaryText = char, action = KeyAction.Character(char)),
                    palette = palette,
                    settings = settings,
                    shiftState = shiftState,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }

            // Backspace key
            KeyButton(
                key = KeyModel(primaryText = "backspace", action = KeyAction.Backspace, isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = shiftState,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )
        }

        // Row 4: ?123, Emoji, Space, Period, Enter
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            KeyButton(
                key = KeyModel(primaryText = "?123", action = KeyAction.SwitchMode(KeyboardMode.SYMBOLS), isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = shiftState,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = "emoji", action = KeyAction.OpenEmoji, isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = shiftState,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.0f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            // Spacebar
            KeyButton(
                key = KeyModel(primaryText = "space", action = KeyAction.Space),
                palette = palette,
                settings = settings,
                shiftState = shiftState,
                fontFamily = fontFamily,
                modifier = Modifier.weight(4.2f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = ".", action = KeyAction.Character(".")),
                palette = palette,
                settings = settings,
                shiftState = shiftState,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.0f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            // Enter key
            KeyButton(
                key = KeyModel(primaryText = "enter", action = KeyAction.Enter(), isSpecial = true, isAccent = true),
                palette = palette,
                settings = settings,
                shiftState = shiftState,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.6f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )
        }
    }
}

@Composable
private fun SymbolsLayout(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    onKeyAction: (KeyAction) -> Unit
) {
    val keyHeight = settings.keyHeight.heightDp.dp
    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: 1 2 3 4 5 6 7 8 9 0
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { sym ->
                KeyButton(
                    key = KeyModel(primaryText = sym, action = KeyAction.Character(sym)),
                    palette = palette,
                    settings = settings,
                    shiftState = ShiftState.OFF,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }
        }

        // Row 2: @ # $ _ & - + ( ) /
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            listOf("@", "#", "\$", "_", "&", "-", "+", "(", ")", "/").forEach { sym ->
                KeyButton(
                    key = KeyModel(primaryText = sym, action = KeyAction.Character(sym)),
                    palette = palette,
                    settings = settings,
                    shiftState = ShiftState.OFF,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }
        }

        // Row 3: =/<, * " ' : ; ! ?, Backspace
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            KeyButton(
                key = KeyModel(primaryText = "=\\<", action = KeyAction.SwitchMode(KeyboardMode.MORE_SYMBOLS), isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            listOf("*", "\"", "'", ":", ";", "!", "?").forEach { sym ->
                KeyButton(
                    key = KeyModel(primaryText = sym, action = KeyAction.Character(sym)),
                    palette = palette,
                    settings = settings,
                    shiftState = ShiftState.OFF,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }

            KeyButton(
                key = KeyModel(primaryText = "backspace", action = KeyAction.Backspace, isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )
        }

        // Row 4: ABC, ,, Space, ., Enter
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            KeyButton(
                key = KeyModel(primaryText = "ABC", action = KeyAction.SwitchMode(KeyboardMode.QWERTY), isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = ",", action = KeyAction.Character(",")),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = "space", action = KeyAction.Space),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(4f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = ".", action = KeyAction.Character(".")),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = "enter", action = KeyAction.Enter(), isSpecial = true, isAccent = true),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.6f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )
        }
    }
}

@Composable
private fun MoreSymbolsLayout(
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    onKeyAction: (KeyAction) -> Unit
) {
    val keyHeight = settings.keyHeight.heightDp.dp
    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: ~ \ | ^ < > { } [ ]
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            listOf("~", "\\", "|", "^", "<", ">", "{", "}", "[", "]").forEach { sym ->
                KeyButton(
                    key = KeyModel(primaryText = sym, action = KeyAction.Character(sym)),
                    palette = palette,
                    settings = settings,
                    shiftState = ShiftState.OFF,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }
        }

        // Row 2: ° • ○ ● ★ ♠ ♣ ♥ ♦ §
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            listOf("°", "•", "○", "●", "★", "♠", "♣", "♥", "♦", "§").forEach { sym ->
                KeyButton(
                    key = KeyModel(primaryText = sym, action = KeyAction.Character(sym)),
                    palette = palette,
                    settings = settings,
                    shiftState = ShiftState.OFF,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }
        }

        // Row 3: ?123, € £ ¥ ¢ © ® ™ ÷ ×, Backspace
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            KeyButton(
                key = KeyModel(primaryText = "?123", action = KeyAction.SwitchMode(KeyboardMode.SYMBOLS), isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            listOf("€", "£", "¥", "¢", "©", "®", "™", "÷").forEach { sym ->
                KeyButton(
                    key = KeyModel(primaryText = sym, action = KeyAction.Character(sym)),
                    palette = palette,
                    settings = settings,
                    shiftState = ShiftState.OFF,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onKeyAction = onKeyAction
                )
            }

            KeyButton(
                key = KeyModel(primaryText = "backspace", action = KeyAction.Backspace, isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )
        }

        // Row 4: ABC, ,, Space, ., Enter
        Row(modifier = Modifier.fillMaxWidth().height(keyHeight)) {
            KeyButton(
                key = KeyModel(primaryText = "ABC", action = KeyAction.SwitchMode(KeyboardMode.QWERTY), isSpecial = true),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = "=", action = KeyAction.Character("=")),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = "space", action = KeyAction.Space),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(4f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = "%", action = KeyAction.Character("%")),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )

            KeyButton(
                key = KeyModel(primaryText = "enter", action = KeyAction.Enter(), isSpecial = true, isAccent = true),
                palette = palette,
                settings = settings,
                shiftState = ShiftState.OFF,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.6f).fillMaxHeight(),
                onKeyAction = onKeyAction
            )
        }
    }
}
