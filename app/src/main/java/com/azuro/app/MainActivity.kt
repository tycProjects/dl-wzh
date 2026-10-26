package com.azuro.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var key: EditText
    private lateinit var model: EditText

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = getSharedPreferences("azuro", MODE_PRIVATE)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(48), dp(24), dp(24)); setBackgroundColor(Color.parseColor("#F2F7FF"))
        }
        root.addView(ImageView(this).apply { setImageResource(R.drawable.logo) }, LinearLayout.LayoutParams(dp(120), dp(120)))
        root.addView(TextView(this).apply {
            text = "Azuro"; textSize = 28f; setTextColor(Color.parseColor("#0A3AB8")); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Trợ lý AI nhìn màn hình, chat qua nút nổi và vẽ chỉ dẫn lên màn hình."
            gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(24))
        })
        key = EditText(this).apply {
            hint = "Anthropic API key (sk-ant-...)"; setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(p.getString("key", BuildConfig.DEFAULT_KEY))
        }
        model = EditText(this).apply { hint = "Model"; setSingleLine(); setText(p.getString("model", "claude-sonnet-5-5")) }
        root.addView(key, LinearLayout.LayoutParams(-1, -2))
        root.addView(model, LinearLayout.LayoutParams(-1, -2))
        root.addView(Button(this).apply {
            text = "Bật Azuro"
            setOnClickListener {
                p.edit().putString("key", key.text.toString().trim()).putString("model", model.text.toString().trim()).apply()
                go()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun go() {
        if (key.text.isBlank()) { Toast.makeText(this, "Nhập API key trước", Toast.LENGTH_SHORT).show(); return }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Cho phép Azuro hiển thị trên ứng dụng khác, rồi bấm lại", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1); return
        }
        startActivityForResult(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent(), 100)
    }

    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) { go() }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 100 && res == RESULT_OK && data != null) {
            startForegroundService(Intent(this, OverlayService::class.java).putExtra("code", res).putExtra("data", data))
            moveTaskToBack(true)
        }
    }
}
