package com.example.ui.keyboard

enum class KeyboardMode {
    QWERTY,
    SYMBOLS,
    MORE_SYMBOLS,
    EMOJI,
    CLIPBOARD,
    TEXT_EDIT
}

enum class ShiftState {
    OFF,
    ON,
    CAPS_LOCK
}

sealed class KeyAction {
    data class Character(val char: String, val alternate: String? = null) : KeyAction()
    object Backspace : KeyAction()
    object Shift : KeyAction()
    data class SwitchMode(val mode: KeyboardMode) : KeyAction()
    object Space : KeyAction()
    data class Enter(val label: String = "return") : KeyAction()
    object OpenEmoji : KeyAction()
    object OpenClipboard : KeyAction()
    object OpenSettings : KeyAction()
    object OpenTextEdit : KeyAction()
    object SwitchToNextIme : KeyAction()
    object CursorLeft : KeyAction()
    object CursorRight : KeyAction()
    object CursorUp : KeyAction()
    object CursorDown : KeyAction()
    object SelectAll : KeyAction()
    object CopyText : KeyAction()
    object CutText : KeyAction()
    object PasteText : KeyAction()
}

data class KeyModel(
    val primaryText: String,
    val secondaryText: String? = null,
    val action: KeyAction,
    val weight: Float = 1.0f,
    val isSpecial: Boolean = false,
    val isAccent: Boolean = false
)
