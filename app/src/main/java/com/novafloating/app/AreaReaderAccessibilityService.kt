package com.novafloating.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AreaReaderAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AreaReaderAccessibilityService? = null
            private set

        fun extractTextInBounds(cropRect: Rect): String? {
            val service = instance ?: return null
            return service.readTextWithinBounds(cropRect)
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

    fun readTextWithinBounds(cropRect: Rect): String {
        val rootNode = rootInActiveWindow ?: return ""
        val collectedTexts = mutableListOf<String>()

        traverseNodeBounds(rootNode, cropRect, collectedTexts)

        return cleanAndCombineTexts(collectedTexts)
    }

    private fun traverseNodeBounds(
        node: AccessibilityNodeInfo?,
        cropRect: Rect,
        collected: MutableList<String>
    ) {
        if (node == null) return

        val nodeBounds = Rect()
        node.getBoundsInScreen(nodeBounds)

        if (Rect.intersects(cropRect, nodeBounds)) {
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
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            traverseNodeBounds(child, cropRect, collected)
        }
    }

    private fun isValidScreenText(text: String): Boolean {
        if (text.isEmpty()) return false
        if (text == "⠿" || text == "✔" || text == "✕" || text == "⚡" || text == "CROP" ||
            text.contains("KOTAK AREA SELEKSI") || text.contains("NOVA FLOATING") ||
            text.contains("Google AI Mode") || text.contains("In-App Overlay")
        ) {
            return false
        }
        return true
    }

    private fun cleanAndCombineTexts(texts: List<String>): String {
        if (texts.isEmpty()) return ""

        val combined = texts.joinToString(" ")
        return if (combined.length > 500) {
            combined.substring(0, 500).trim()
        } else {
            combined
        }
    }
}
