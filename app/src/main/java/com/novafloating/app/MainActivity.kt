package com.novafloating.app

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var cardOverlay: MaterialCardView
    private lateinit var cardAccessibility: MaterialCardView
    private lateinit var btnTelegram: LinearLayout
    private lateinit var btnWhatsapp: LinearLayout

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStates()
        if (isOverlayPermissionGranted()) {
            Toast.makeText(this, "Izin Overlay Aktif!", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            proceedToStartService()
        } else {
            Toast.makeText(this, "Izin notifikasi diperlukan untuk foreground service", Toast.LENGTH_LONG).show()
            proceedToStartService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
    }

    private fun initViews() {
        btnStart = findViewById(R.id.btn_start_service)
        tvOverlayStatus = findViewById(R.id.tv_overlay_status)
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status)
        cardOverlay = findViewById(R.id.card_perm_overlay)
        cardAccessibility = findViewById(R.id.card_perm_accessibility)
        btnTelegram = findViewById(R.id.btn_contact_telegram)
        btnWhatsapp = findViewById(R.id.btn_contact_whatsapp)
    }

    private fun setupListeners() {
        btnStart.setOnClickListener { handleStartButtonClick() }
        cardOverlay.setOnClickListener { requestOverlayPermission() }
        cardAccessibility.setOnClickListener { requestAccessibilityPermission() }
        btnTelegram.setOnClickListener { openUrl("https://t.me/croxfloatingujian") }
        btnWhatsapp.setOnClickListener { openUrl("https://wa.me/6288211614094") }
    }

    private fun updatePermissionStates() {
        val hasOverlay = isOverlayPermissionGranted()
        val hasAccessibility = isAccessibilityServiceEnabled(this, AreaReaderAccessibilityService::class.java)

        if (hasOverlay) {
            tvOverlayStatus.text = "Izin Aktif: Siap Melayang"
            tvOverlayStatus.setTextColor(ContextCompat.getColor(this, R.color.cyan_neon))
        } else {
            tvOverlayStatus.text = "Belum Aktif (Klik untuk Izinkan)"
            tvOverlayStatus.setTextColor(ContextCompat.getColor(this, R.color.red_alert))
        }

        if (hasAccessibility) {
            tvAccessibilityStatus.text = "Layanan Aktif: Pembaca Area Siap"
            tvAccessibilityStatus.setTextColor(ContextCompat.getColor(this, R.color.cyan_neon))
        } else {
            tvAccessibilityStatus.text = "Belum Aktif (Klik untuk Pengaturan)"
            tvAccessibilityStatus.setTextColor(ContextCompat.getColor(this, R.color.purple_neon))
        }
    }

    private fun handleStartButtonClick() {
        if (!isOverlayPermissionGranted()) {
            AlertDialog.Builder(this)
                .setTitle("Izin Overlay Diperlukan")
                .setMessage("Aktifkan izin 'Tampilkan di atas aplikasi lain' agar widget NovaFloating dapat melayang di layar.")
                .setPositiveButton("Buka Pengaturan") { _, _ -> requestOverlayPermission() }
                .setNegativeButton("Batal", null)
                .show()
            return
        }

        if (!isAccessibilityServiceEnabled(this, AreaReaderAccessibilityService::class.java)) {
            AlertDialog.Builder(this)
                .setTitle("Layanan Aksesibilitas Belum Aktif")
                .setMessage("Aktifkan layanan 'NovaFloating Area Reader' agar sistem dapat mengekstrak teks di dalam kotak seleksi melayang.")
                .setPositiveButton("Buka Aksesibilitas") { _, _ -> requestAccessibilityPermission() }
                .setNegativeButton("Lanjutkan Tetap") { _, _ -> checkNotificationAndLaunch() }
                .show()
            return
        }

        checkNotificationAndLaunch()
    }

    private fun checkNotificationAndLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        proceedToStartService()
    }

    private fun proceedToStartService() {
        val serviceIntent = Intent(this, FloatingService::class.java).apply {
            action = FloatingService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "NovaFloating Area Reader Aktif!", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayLauncher.launch(intent)
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Cari dan aktifkan 'NovaFloating Area Reader'", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out android.accessibilityservice.AccessibilityService>
    ): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        for (enabled in enabledServices) {
            val sInfo = enabled.resolveInfo?.serviceInfo
            if (sInfo != null) {
                val current = "${sInfo.packageName}/${sInfo.name}"
                if (current.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
