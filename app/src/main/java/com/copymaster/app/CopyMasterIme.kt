package com.copymaster.app

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CopyMasterIme : InputMethodService() {
    private var offset = 0

    override fun onCreateInputView(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6, 6, 6, 6)
            setBackgroundColor(Color.rgb(27, 32, 42))
        }
        panel.addView(TextView(this).apply {
            text = "CopyMaster Keyboard · ${ClipboardBuffer.text.length} ký tự trong RAM"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(6, 4, 6, 8)
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(key("Chèn toàn bộ") {
            val value = ClipboardBuffer.text
            if (value.isNotEmpty()) currentInputConnection?.commitText(value, 1)
        }, weight())
        row.addView(key("Chèn đoạn tiếp") {
            val all = ClipboardBuffer.text
            if (offset >= all.length) offset = 0
            val size = ClipboardBuffer.chunkSize.coerceIn(1, 500000)
            val end = minOf(offset + size, all.length)
            if (end > offset) {
                currentInputConnection?.commitText(all.substring(offset, end), 1)
                offset = end
            }
        }, weight())
        panel.addView(row, LinearLayout.LayoutParams(-1, 52))
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(key("Đặt lại đoạn") { offset = 0 }, weight())
        row2.addView(key("Xóa vùng đệm") {
            ClipboardBuffer.clear()
            offset = 0
            requestHideSelf(0)
        }, weight())
        panel.addView(row2, LinearLayout.LayoutParams(-1, 48))
        return panel
    }

    private fun key(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 12f
        setOnClickListener { action() }
    }

    private fun weight() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
}
