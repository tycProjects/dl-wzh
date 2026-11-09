package com.example.ui.keyboard

import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyBorderStyle
import com.example.model.KeyboardIconStyle
import com.example.model.KeyboardSettings

@Composable
fun KeyButton(
    key: KeyModel,
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    shiftState: ShiftState,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    onKeyAction: (KeyAction) -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "keyScale"
    )

    val cornerRadius = when (settings.keyBorderStyle) {
        KeyBorderStyle.BORDERLESS -> 10.dp
        KeyBorderStyle.BORDERED -> 8.dp
        KeyBorderStyle.LIQUID_GLASS -> 14.dp
    }
    val shape = RoundedCornerShape(cornerRadius)

    val baseBgColor = when {
        key.isAccent -> palette.accentColor
        key.isSpecial -> palette.specialKeyColor
        else -> palette.keyColor
    }

    val finalBgColor = if (isPressed) {
        baseBgColor.copy(alpha = (baseBgColor.alpha + 0.20f).coerceAtMost(0.95f))
    } else {
        baseBgColor
    }

    Box(
        modifier = modifier
            .padding(horizontal = 2.5.dp, vertical = 3.dp)
            .scale(scale)
            .clip(shape)
            .background(finalBgColor)
            .then(
                if (settings.keyBorderStyle != KeyBorderStyle.BORDERLESS) {
                    Modifier.border(
                        width = if (settings.keyBorderStyle == KeyBorderStyle.LIQUID_GLASS) 1.25.dp else 1.dp,
                        color = if (isPressed) palette.accentColor else palette.borderColor,
                        shape = shape
                    )
                } else Modifier
            )
            .then(
                if (settings.keyBorderStyle == KeyBorderStyle.LIQUID_GLASS) {
                    // Draw liquid glass gloss reflection across top half
                    Modifier.drawBehind {
                        drawRect(
                            brush = palette.glassGlossBrush,
                            size = size.copy(height = size.height * 0.45f)
                        )
                    }
                } else Modifier
            )
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onKeyAction(key.action)
                    },
                    onLongPress = {
                        if (key.secondaryText != null) {
                            onKeyAction(KeyAction.Character(key.secondaryText))
                        } else if (key.action is KeyAction.Character && key.action.alternate != null) {
                            onKeyAction(KeyAction.Character(key.action.alternate))
                        } else {
                            onKeyAction(key.action)
                        }
                    }
                )
            }
            .testTag("key_${key.primaryText}"),
        contentAlignment = Alignment.Center
    ) {
        // Show secondary label hint (e.g. number above letter on top row)
        if (key.secondaryText != null && !key.isSpecial) {
            Text(
                text = key.secondaryText,
                color = palette.textColor.copy(alpha = 0.55f),
                fontSize = 9.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 5.dp)
            )
        }

        // Key content: either custom icon or text
        KeyContent(
            key = key,
            palette = palette,
            settings = settings,
            shiftState = shiftState,
            fontFamily = fontFamily
        )
    }
}

@Composable
private fun KeyContent(
    key: KeyModel,
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    shiftState: ShiftState,
    fontFamily: FontFamily
) {
    val contentColor = when {
        key.isAccent -> if (palette.accentColor.luminance() > 0.5f) Color.Black else Color.White
        key.isSpecial -> palette.specialTextColor
        else -> palette.textColor
    }

    when (val action = key.action) {
        is KeyAction.Backspace -> {
            val icon = if (settings.iconStyle == KeyboardIconStyle.OUTLINED) {
                Icons.AutoMirrored.Outlined.Backspace
            } else {
                Icons.AutoMirrored.Filled.Backspace
            }
            Icon(
                imageVector = icon,
                contentDescription = "Backspace",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        is KeyAction.Shift -> {
            val isFilled = settings.iconStyle != KeyboardIconStyle.OUTLINED
            val (icon, tint) = when (shiftState) {
                ShiftState.CAPS_LOCK -> Pair(
                    Icons.Filled.KeyboardCapslock,
                    palette.accentColor
                )
                ShiftState.ON -> Pair(
                    if (isFilled) Icons.Filled.ArrowUpward else Icons.Outlined.ArrowUpward,
                    palette.accentColor
                )
                ShiftState.OFF -> Pair(
                    if (isFilled) Icons.Filled.ArrowUpward else Icons.Outlined.ArrowUpward,
                    contentColor.copy(alpha = 0.75f)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = "Shift",
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        is KeyAction.Enter -> {
            val icon = if (settings.iconStyle == KeyboardIconStyle.OUTLINED) {
                Icons.Outlined.KeyboardReturn
            } else {
                Icons.Filled.KeyboardReturn
            }
            Icon(
                imageVector = icon,
                contentDescription = "Enter",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        is KeyAction.OpenEmoji -> {
            val icon = if (settings.iconStyle == KeyboardIconStyle.OUTLINED) {
                Icons.Outlined.SentimentSatisfiedAlt
            } else {
                Icons.Filled.SentimentSatisfiedAlt
            }
            Icon(
                imageVector = icon,
                contentDescription = "Emoji",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        is KeyAction.OpenClipboard -> {
            val icon = if (settings.iconStyle == KeyboardIconStyle.OUTLINED) {
                Icons.Outlined.ContentPaste
            } else {
                Icons.Filled.ContentPaste
            }
            Icon(
                imageVector = icon,
                contentDescription = "Clipboard",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        is KeyAction.OpenSettings -> {
            val icon = if (settings.iconStyle == KeyboardIconStyle.OUTLINED) {
                Icons.Outlined.Tune
            } else {
                Icons.Filled.Tune
            }
            Icon(
                imageVector = icon,
                contentDescription = "Settings",
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
        }

        is KeyAction.OpenTextEdit -> {
            Icon(
                imageVector = Icons.Filled.EditNote,
                contentDescription = "Text Edit Navigation",
                tint = contentColor,
                modifier = Modifier.size(21.dp)
            )
        }

        is KeyAction.SwitchToNextIme -> {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Next Keyboard",
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
        }

        is KeyAction.Space -> {
            Text(
                text = "SV Keyboard",
                color = contentColor.copy(alpha = 0.60f),
                fontSize = 12.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium
            )
        }

        else -> {
            val displayChar = when {
                shiftState != ShiftState.OFF && key.primaryText.length == 1 && key.primaryText[0].isLetter() ->
                    key.primaryText.uppercase()
                else -> key.primaryText
            }

            Text(
                text = displayChar,
                color = contentColor,
                fontSize = if (displayChar.length > 2) 13.sp else 20.sp,
                fontFamily = fontFamily,
                fontWeight = if (key.isSpecial) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
