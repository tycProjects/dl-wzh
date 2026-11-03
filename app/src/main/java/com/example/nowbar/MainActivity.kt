package com.example.nowbar

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private val channelId = "now_bar_channel"
    private val notifId = 1
    private val pickLogoCode = 100
    private val permCode = 200

    private lateinit var textInput: EditText
    private lateinit var preview: ImageView

    private val prefs by lazy { getSharedPreferences("nowbar", MODE_PRIVATE) }
    private val logoFile by lazy { File(filesDir, "logo.png") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), permCode)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        // Keep content clear of system bars (edge-to-edge on Android 15+)
        root.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                dp(24), insets.systemWindowInsetTop + dp(24),
                dp(24), insets.systemWindowInsetBottom + dp(24)
            )
            insets
        }

        val title = TextView(this).apply {
            text = "Now Bar Custom"
            textSize = 24f
            setTextColor(Color.BLACK)
        }

        textInput = EditText(this).apply {
            hint = "Text"
            setText(prefs.getString("text", "mustafa"))
            setSingleLine(true)
        }

        preview = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96)).apply {
                topMargin = dp(16)
            }
            setBackgroundColor(Color.RED)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        refreshPreview()

        val pickBtn = Button(this).apply {
            text = "Choose logo"
            setOnClickListener {
                val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                startActivityForResult(i, pickLogoCode)
            }
        }

        val resetBtn = Button(this).apply {
            text = "Reset logo"
            setOnClickListener {
                logoFile.delete()
                refreshPreview()
            }
        }

        val showBtn = Button(this).apply {
            text = "Show in Now Bar"
            setOnClickListener { showNowBar() }
        }

        val stopBtn = Button(this).apply {
            text = "Stop"
            setOnClickListener {
                getSystemService(NotificationManager::class.java).cancel(notifId)
            }
        }

        val wide = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        root.addView(title, LinearLayout.LayoutParams(wide, wrap))
        root.addView(textInput, LinearLayout.LayoutParams(wide, wrap).apply { topMargin = dp(16) })
        root.addView(preview)
        root.addView(pickBtn, LinearLayout.LayoutParams(wide, wrap).apply { topMargin = dp(8) })
        root.addView(resetBtn, LinearLayout.LayoutParams(wide, wrap))
        root.addView(showBtn, LinearLayout.LayoutParams(wide, wrap).apply { topMargin = dp(16) })
        root.addView(stopBtn, LinearLayout.LayoutParams(wide, wrap))

        setContentView(root)
    }

    private fun loadLogo(): Bitmap? =
        if (logoFile.exists()) BitmapFactory.decodeFile(logoFile.absolutePath) else null

    private fun refreshPreview() {
        val bmp = loadLogo()
        if (bmp != null) preview.setImageBitmap(bmp)
        else preview.setImageResource(android.R.drawable.ic_dialog_info)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickLogoCode && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val src = contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return
                val scaled = Bitmap.createScaledBitmap(src, 128, 128, true)
                logoFile.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
                refreshPreview()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNowBar() {
        val text = textInput.text.toString().trim().ifEmpty { "mustafa" }
        prefs.edit().putString("text", text).apply()

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Now Bar", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val logo = loadLogo()
        val builder = Notification.Builder(this, channelId)
            .setContentTitle(text)
            .setContentText(text)
            .setColor(Color.RED)
            .setOngoing(true)

        if (logo != null) {
            builder.setSmallIcon(Icon.createWithBitmap(logo))
            builder.setLargeIcon(logo)
        } else {
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        }

        if (Build.VERSION.SDK_INT >= 36) {
            builder.setShortCriticalText(text)
        }

        nm.notify(notifId, builder.build())
    }
}