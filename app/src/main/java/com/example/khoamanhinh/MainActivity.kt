package com.example.khoamanhinh

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    // PIN thử nghiệm mặc định. Hãy đổi trước khi chia sẻ APK.
    private val unlockPin = "2468"
    private lateinit var pinInput: EditText
    private lateinit var statusText: TextView
    private lateinit var dpm: DevicePolicyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(16, 42, 67)
        window.navigationBarColor = Color.rgb(16, 42, 67)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        buildScreen()
        configureKioskIfDeviceOwner()
    }

    private fun buildScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(16, 42, 67))
        }

        val title = TextView(this).apply {
            text = "ĐIỆN THOẠI ĐANG KHÓA"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val hint = TextView(this).apply {
            text = "Nhập mã PIN để thoát màn hình này"
            textSize = 17f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 20)
        }
        pinInput = EditText(this).apply {
            hint = "Mã PIN"
            inputType = 2
            textSize = 22f
            gravity = Gravity.CENTER
            setSingleLine(true)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.WHITE)
            contentDescription = "Ô nhập mã PIN"
        }
        val unlock = Button(this).apply {
            text = "MỞ KHÓA"
            textSize = 18f
            setOnClickListener { checkPin() }
        }
        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.YELLOW)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        layout.addView(title, matchWrap())
        layout.addView(hint, matchWrap())
        layout.addView(pinInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 60
        ))
        layout.addView(unlock, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 58
        ).apply { topMargin = 16 })
        layout.addView(statusText, matchWrap())
        setContentView(layout)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun configureKioskIfDeviceOwner() {
        if (!dpm.isLockTaskPermitted(packageName)) {
            statusText.text =
                "Chưa bật kiosk. APK thường không thể thay màn hình khóa hệ thống."
            return
        }

        try {
            startLockTask()
            statusText.text = "Kiosk đang hoạt động theo cấu hình quản trị của thiết bị."
        } catch (_: Exception) {
            statusText.text = "Không bật được kiosk trên thiết bị này."
        }
    }

    private fun checkPin() {
        if (pinInput.text.toString() != unlockPin) {
            pinInput.text.clear()
            Toast.makeText(this, "Sai mã PIN", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (dpm.isLockTaskPermitted(packageName)) stopLockTask()
        } catch (_: Exception) { }

        Toast.makeText(this, "Đã mở khóa màn hình ứng dụng", Toast.LENGTH_SHORT).show()
        finish()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        Toast.makeText(this, "Hãy nhập đúng mã PIN để thoát màn hình này", Toast.LENGTH_SHORT).show()
    }
}
