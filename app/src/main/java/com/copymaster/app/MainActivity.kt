package com.copymaster.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color

class MainActivity : Activity() {
    private lateinit var editor: EditText
    private lateinit var status: TextView
    private lateinit var count: TextView
    private lateinit var chunkSize: EditText
    private var nextOffset = 0

    private val clipboard by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(18, 22, 30)
        window.navigationBarColor = Color.rgb(18, 22, 30)
        buildUi()
    }

    private fun buildUi() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
            setBackgroundColor(Color.rgb(18, 22, 30))
        }

        val title = TextView(this).apply {
            text = "CopyMaster"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }
        outer.addView(title, lp(-1, 52))

        val subtitle = TextView(this).apply {
            text = "Clipboard Bridge • RAM only"
            textSize = 13f
            setTextColor(Color.rgb(150, 190, 170))
        }
        outer.addView(subtitle, lp(-1, 30))

        count = TextView(this).apply {
            text = "0 ký tự"
            textSize = 15f
            setTextColor(Color.rgb(220, 225, 235))
            setPadding(0, 8, 0, 8)
        }
        outer.addView(count)

        editor = EditText(this).apply {
            hint = "Dán văn bản dài vào đây…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 15f
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 8
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(34, 40, 52))
            addTextChangedListener(SimpleTextWatcher { updateCount() })
        }
        outer.addView(editor, LinearLayout.LayoutParams(-1, 0, 1f))

        val pasteClipboard = makeButton("Dán từ Clipboard") {
            val clip = clipboard.primaryClip
            if (clip == null || clip.itemCount == 0) {
                setStatus("Clipboard đang trống.")
            } else {
                val text = clip.getItemAt(0).coerceToText(this)?.toString().orEmpty()
                editor.setText(text)
                editor.setSelection(editor.text.length)
                nextOffset = 0
                setStatus("Đã nhận ${text.length} ký tự vào RAM.")
            }
        }
        outer.addView(pasteClipboard, lp(-1, 50))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chunkSize = EditText(this).apply {
            setText("20000")
            hint = "Số ký tự/đoạn"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(34, 40, 52))
        }
        row.addView(chunkSize, LinearLayout.LayoutParams(0, 48, 1f).apply {
            rightMargin = 8
        })
        row.addView(makeButton("Sao chép toàn bộ") {
            val text = editor.text.toString()
            if (text.isEmpty()) setStatus("Chưa có văn bản.")
            else {
                putClipboard(text)
                setStatus("Đã đưa toàn bộ ${text.length} ký tự vào Clipboard. Hãy chuyển sang app đích và nhấn Dán.")
            }
        }, LinearLayout.LayoutParams(0, 48, 1f))
        outer.addView(row, lp(-1, 54))

        outer.addView(makeButton("Sao chép đoạn tiếp theo") {
            copyNextChunk()
        }, lp(-1, 50))

        outer.addView(makeButton("Đặt lại từ đầu") {
            nextOffset = 0
            setStatus("Đã đặt lại vị trí chia đoạn.")
        }, lp(-1, 46))

        outer.addView(makeButton("Xóa dữ liệu RAM") {
            editor.text.clear()
            nextOffset = 0
            // Clearing clipboard is best-effort; Android may restrict clipboard access.
            try { clipboard.clearPrimaryClip() } catch (_: Exception) {}
            setStatus("Đã xóa nội dung trong ứng dụng và yêu cầu xóa Clipboard.")
        }, lp(-1, 50).apply { topMargin = 4 })

        status = TextView(this).apply {
            text = "Sẵn sàng. Nội dung chỉ nằm trong RAM khi ứng dụng chạy."
            textSize = 12f
            setTextColor(Color.rgb(170, 180, 195))
            setPadding(2, 10, 2, 2)
        }
        outer.addView(status, lp(-1, -2))

        setContentView(outer)
    }

    private fun copyNextChunk() {
        val text = editor.text.toString()
        if (text.isEmpty()) {
            setStatus("Chưa có văn bản.")
            return
        }
        val size = chunkSize.text.toString().toIntOrNull()?.coerceIn(1, 500000) ?: 20000
        if (nextOffset >= text.length) {
            nextOffset = 0
            setStatus("Đã hết văn bản. Đã đặt lại về đầu.")
            return
        }
        val end = minOf(nextOffset + size, text.length)
        val part = text.substring(nextOffset, end)
        putClipboard(part)
        val from = nextOffset + 1
        nextOffset = end
        setStatus("Đã sao chép ký tự $from–$end / ${text.length}. Chuyển sang app đích và nhấn Dán.")
    }

    private fun putClipboard(text: String) {
        clipboard.setPrimaryClip(ClipData.newPlainText("CopyMaster", text))
    }

    private fun updateCount() {
        val n = editor.text?.length ?: 0
        count.text = "%,d ký tự".format(n)
        if (nextOffset > n) nextOffset = n
    }

    private fun setStatus(message: String) {
        status.text = message
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setOnClickListener { action() }
        }

    private fun lp(width: Int, height: Int) =
        LinearLayout.LayoutParams(width, height)
}
