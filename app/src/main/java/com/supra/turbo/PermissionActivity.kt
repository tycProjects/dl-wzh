package com.supra.turbo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import rikka.shizuku.Shizuku

/** Màn hình cấp quyền: tự hiện khi vào app nếu còn thiếu quyền, tự đóng khi đã đủ. */
class PermissionActivity : AppCompatActivity() {

    private val listener = Shizuku.OnRequestPermissionResultListener { _, _ -> runOnUiThread { refresh() } }
    private lateinit var st1: TextView; private lateinit var st2: TextView; private lateinit var st3: TextView
    private lateinit var b1: Button; private lateinit var b2: Button; private lateinit var b3: Button

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_permission)
        st1 = findViewById(R.id.st1); st2 = findViewById(R.id.st2); st3 = findViewById(R.id.st3)
        b1 = findViewById(R.id.b1); b2 = findViewById(R.id.b2); b3 = findViewById(R.id.b3)
        findViewById<TextView>(R.id.tvCopyright).text =
            getString(R.string.copyright, getString(R.string.owner_name))
        Shizuku.addRequestPermissionResultListener(listener)

        b1.setOnClickListener {
            try {
                if (!Shizuku.pingBinder()) {
                    val i = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (i != null) startActivity(i) else toast("Chưa cài Shizuku. Hãy cài app Shizuku trước.")
                } else Shizuku.requestPermission(200)
            } catch (t: Throwable) { toast("Shizuku chưa chạy") }
        }
        b2.setOnClickListener {
            try { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))) }
            catch (_: Exception) { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        }
        b3.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun row(ok: Boolean, st: TextView, b: Button) {
        st.text = if (ok) "✔ Đã cấp" else "Chưa cấp"
        st.setTextColor(if (ok) 0xFF2EE6A6.toInt() else 0xFFFF4D6A.toInt())
        b.visibility = if (ok) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun refresh() {
        row(SupraEngine.hasShizuku(), st1, b1)
        row(SupraEngine.usageGranted(this), st2, b2)
        row(SupraEngine.notifGranted(this), st3, b3)
        if (SupraEngine.allGranted(this)) finish()
    }

    override fun onResume() { super.onResume(); refresh() }
    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r); refresh()
    }
    override fun onDestroy() { Shizuku.removeRequestPermissionResultListener(listener); super.onDestroy() }
    @Suppress("OVERRIDE_DEPRECATION") override fun onBackPressed() { finishAffinity() }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
