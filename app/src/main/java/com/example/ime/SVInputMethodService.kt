package com.example.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.KeyboardSettingsManager
import com.example.ui.keyboard.SVKeyboardView

class SVInputMethodService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val settingsManager = KeyboardSettingsManager.getInstance(applicationContext)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@SVInputMethodService)
            setViewTreeViewModelStoreOwner(this@SVInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@SVInputMethodService)

            setContent {
                val settings by settingsManager.settingsFlow.collectAsState()

                SVKeyboardView(
                    settings = settings,
                    onCommitText = { text ->
                        currentInputConnection?.commitText(text, 1)
                    },
                    onDeleteSurroundingText = { beforeLength, afterLength ->
                        val ic = currentInputConnection ?: return@SVKeyboardView
                        val selected = ic.getSelectedText(0)
                        if (!selected.isNullOrEmpty()) {
                            ic.commitText("", 1)
                        } else {
                            ic.deleteSurroundingText(beforeLength, afterLength)
                        }
                    },
                    onEnterAction = {
                        val ic = currentInputConnection ?: return@SVKeyboardView
                        val editorInfo = currentInputEditorInfo
                        val action = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                        if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                            ic.performEditorAction(action)
                        } else {
                            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                        }
                    },
                    onCursorMove = { delta ->
                        val ic = currentInputConnection ?: return@SVKeyboardView
                        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                        if (extracted != null) {
                            val newPos = (extracted.startOffset + extracted.selectionStart + delta)
                                .coerceIn(0, extracted.text.length)
                            ic.setSelection(newPos, newPos)
                        } else {
                            val keycode = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
                            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))
                        }
                    },
                    onSelectAllText = {
                        currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
                    },
                    onCopyText = {
                        currentInputConnection?.performContextMenuAction(android.R.id.copy)
                    },
                    onCutText = {
                        currentInputConnection?.performContextMenuAction(android.R.id.cut)
                    },
                    onPasteText = {
                        currentInputConnection?.performContextMenuAction(android.R.id.paste)
                    },
                    onSwitchIme = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            switchToNextInputMethod(false)
                        }
                    },
                    onOpenSettingsInApp = {
                        val intent = Intent(this@SVInputMethodService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        }
                        startActivity(intent)
                    }
                )
            }
        }

        return composeView
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
