package com.generated.aiaddi

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Membaca teks yang tampil di layar app lain secara langsung (tanpa screenshot).
 * Kata sandi (kolom password) dilewati.
 */
class AddiAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun readScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        if (root.packageName?.toString() == packageName) return ""
        val out = ArrayList<String>()

        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 40) return
            if (n.isVisibleToUser && !n.isPassword) {
                val t = (n.text ?: n.contentDescription)?.toString()?.trim()
                if (!t.isNullOrEmpty() && (out.isEmpty() || out.last() != t)) out.add(t)
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }

        walk(root, 0)
        return out.joinToString("\n").take(6000)
    }

    companion object {
        @Volatile var instance: AddiAccessibilityService? = null
    }
}
