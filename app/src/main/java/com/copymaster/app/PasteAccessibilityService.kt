package com.copymaster.app

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Accessibility is used only after the user explicitly taps the notification action
 * "Auto Paste". It does not monitor or transmit text.
 */
class PasteAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    fun pasteNow(): Boolean {
        val value = ClipboardBuffer.text
        if (value.isEmpty()) {
            Toast.makeText(this, "CopyMaster chưa có văn bản trong RAM.", Toast.LENGTH_SHORT).show()
            return false
        }
        val root = rootInActiveWindow
        val target = findEditable(root)
        if (target == null) {
            Toast.makeText(this, "Không tìm thấy ô nhập đang được chọn. Hãy chạm vào ô nhập trước.", Toast.LENGTH_LONG).show()
            return false
        }

        // First attempt direct accessibility text insertion. Some apps reject this action,
        // impose their own length limits, or expose no editable node.
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        val setOk = try {
            target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (_: Exception) { false }

        if (!setOk) {
            // Fallback: put full text on system clipboard. User may then tap Paste.
            try {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("CopyMaster", value))
                target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                target.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            } catch (_: Exception) {
                Toast.makeText(this, "Ứng dụng đích không cho phép chèn tự động.", Toast.LENGTH_LONG).show()
                return false
            }
        }
        Toast.makeText(this, "Đã gửi yêu cầu chèn văn bản. Hãy kiểm tra nội dung.", Toast.LENGTH_LONG).show()
        return true
    }

    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.isEnabled && node.isFocused) return node
        for (i in 0 until node.childCount) {
            val found = findEditable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    companion object {
        @Volatile var activeService: PasteAccessibilityService? = null
    }
}
