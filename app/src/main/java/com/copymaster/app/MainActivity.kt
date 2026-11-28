package com.copymaster.app

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.graphics.Color

class MainActivity : Activity() {
    private lateinit var editor: EditText
    private lateinit var status: TextView
    private lateinit var count: TextView
    private lateinit var chunkInput: EditText
    private val clipboard by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            setBackgroundColor(Color.rgb(18, 22, 30))
        }
        root.addView(TextView(this).apply {
            text = "CopyMaster Pro"
            textSize = 26f
            setTextColor(Color.WHITE)
        }, lp(-1, 48))
        root.addView(TextView(this).apply {
            text = "Bàn phím riêng + Auto Paste • RAM only"
            textSize = 13f
            setTextColor(Color.rgb(150, 195, 170))
        }, lp(-1, 30))

        count = TextView(this).apply {
            text = "0 ký tự"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 8)
        }
        root.addView(count)

        editor = EditText(this).apply {
            hint = "Dán hoặc nhập văn bản dài tại đây…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 15f
            gravity = Gravity.TOP or Gravity.START
            minLines = 7
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(34, 40, 52))
            addTextChangedListener(SimpleTextWatcher {
                ClipboardBuffer.text = text.toString()
                updateCount()
            })
        }
        root.addView(editor, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(button("Dán từ Clipboard") {
            val item = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
            if (item == null) show("Clipboard trống.")
            else {
                val incoming = item.coerceToText(this).toString()
                editor.setText(incoming)
                editor.setSelection(editor.text.length)
                ClipboardBuffer.text = incoming
                show("Đã nhận ${incoming.length} ký tự vào RAM.")
            }
        }, lp(-1, 48))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chunkInput = EditText(this).apply {
            setText("20000")
            hint = "Ký tự/đoạn"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(10, 4, 10, 4)
            setBackgroundColor(Color.rgb(34, 40, 52))
        }
        row.addView(chunkInput, LinearLayout.LayoutParams(0, 48, 1f).apply { rightMargin = 8 })
        row.addView(button("Lưu cấu hình RAM") {
            ClipboardBuffer.text = editor.text.toString()
            ClipboardBuffer.chunkSize = chunkInput.text.toString().toIntOrNull()?.coerceIn(1, 500000) ?: 20000
            show("Đã cập nhật vùng đệm RAM.")
        }, LinearLayout.LayoutParams(0, 48, 1f))
        root.addView(row, lp(-1, 54))

        root.addView(button("Sao chép toàn bộ vào Clipboard") {
            ClipboardBuffer.text = editor.text.toString()
            ClipboardBuffer.chunkSize = chunkInput.text.toString().toIntOrNull()?.coerceIn(1, 500000) ?: 20000
            copyToClipboard(ClipboardBuffer.text)
            show("Đã sao chép ${ClipboardBuffer.text.length} ký tự. Chuyển sang ô nhập và dán.")
        }, lp(-1, 48))

        root.addView(button("Bật CopyMaster Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            show("Chọn CopyMaster Keyboard trong cài đặt bàn phím.")
        }, lp(-1, 48))

        root.addView(button("Chọn bàn phím CopyMaster") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }, lp(-1, 48))

        root.addView(button("Bật quyền Auto Paste") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            show("Trong Trợ năng, bật CopyMaster Auto Paste. Chỉ cấp quyền nếu bạn đồng ý.")
        }, lp(-1, 48))

        root.addView(button("Xóa dữ liệu RAM") {
            editor.text.clear()
            ClipboardBuffer.clear()
            try { clipboard.clearPrimaryClip() } catch (_: Exception) {}
            show("Đã xóa vùng đệm của ứng dụng và yêu cầu xóa clipboard.")
        }, lp(-1, 48))

        status = TextView(this).apply {
            text = "Sẵn sàng. Dữ liệu ứng dụng chỉ nằm trong RAM."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(2, 10, 2, 2)
        }
        root.addView(status, lp(-1, -2))
        setContentView(root)
        ClipboardBuffer.text = editor.text.toString()
    }

    private fun copyToClipboard(value: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("CopyMaster", value))
    }

    private fun updateCount() {
        val n = editor.text?.length ?: 0
        count.text = "%,d ký tự".format(n)
    }

    private fun show(message: String) { status.text = message }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)
}
