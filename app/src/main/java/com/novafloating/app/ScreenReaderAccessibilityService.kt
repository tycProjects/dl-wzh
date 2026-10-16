package com.novafloating.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenReaderAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ScreenReaderAccessibilityService? = null
            private set

        fun extractScreenText(): String? {
            val service = instance ?: return null
            return service.readActiveWindowText()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun readActiveWindowText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val collectedTexts = mutableListOf<String>()
        traverseNode(rootNode, collectedTexts)

        return cleanAndCombineTexts(collectedTexts)
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, collected: MutableList<String>) {
        if (node == null) return

        node.text?.toString()?.trim()?.let { text ->
            if (isValidScreenText(text) && !collected.contains(text)) {
                collected.add(text)
            }
        }

        node.contentDescription?.toString()?.trim()?.let { desc ->
            if (isValidScreenText(desc) && !collected.contains(desc)) {
                collected.add(desc)
            }
        }

        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i)
            traverseNode(child, collected)
        }
    }

    private fun isValidScreenText(text: String): Boolean {
        if (text.isEmpty()) return false
        if (text == "⠿" || text == "✕" || text.contains("SCAN & BACA LAYAR") || text.contains("NOVA FLOATING")) {
            return false
        }
        return true
    }

    private fun cleanAndCombineTexts(texts: List<String>): String {
        if (texts.isEmpty()) return ""

        val combined = texts.joinToString(" ")
        return if (combined.length > 450) {
            combined.substring(0, 450).trim() + "..."
        } else {
            combined
        }
    }
}
