package vn.example.khoamanhinh

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    // Đổi mã PIN tại đây trước khi biên dịch APK.
    private val configuredPin = "2468"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(0xFF101827.toInt())
        }
        val title = TextView(this).apply {
            text = "THIẾT BỊ ĐANG KHÓA"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        val hint = TextView(this).apply {
            text = "Nhập mã PIN để tiếp tục"
            textSize = 16f
            setTextColor(0xFFD1D5DB.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 24)
        }
        val pin = EditText(this).apply {
            hint = "Mã PIN"
            inputType =  (android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            setSingleLine(true)
        }
        val unlock = Button(this).apply { text = "MỞ KHÓA" }
        unlock.setOnClickListener {
            if (pin.text.toString() == configuredPin) {
                Toast.makeText(this, "Đã mở khóa ứng dụng", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                pin.text.clear()
                pin.error = "Mã PIN không đúng"
            }
        }

        root.addView(title)
        root.addView(hint)
        root.addView(pin, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        root.addView(unlock)
        setContentView(root)
    }

    override fun onBackPressed() {
        // Giữ người dùng ở màn hình khóa của ứng dụng.
        Toast.makeText(this, "Hãy nhập mã PIN để đóng màn hình khóa", Toast.LENGTH_SHORT).show()
    }
}
