package com.ryuzen.tas

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var shizukuStatus: TextView
    private lateinit var ramStatus: TextView
    private lateinit var thermalStatus: TextView
    private lateinit var cpuStatus: TextView
    private lateinit var logStatus: TextView
    private lateinit var shizuku: ShizukuManager
    private lateinit var performance: PerformanceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        shizuku = ShizukuManager(this)
        performance = PerformanceManager(this)

        shizukuStatus = findViewById(R.id.shizukuStatus)
        ramStatus = findViewById(R.id.ramStatus)
        thermalStatus = findViewById(R.id.thermalStatus)
        cpuStatus = findViewById(R.id.cpuStatus)
        logStatus = findViewById(R.id.logStatus)

        findViewById<Button>(R.id.connectShizuku).setOnClickListener {
            shizuku.requestPermission()
            updateShizukuStatus()
        }
        findViewById<Button>(R.id.refreshStats).setOnClickListener {
            refreshStats()
        }
        findViewById<Button>(R.id.applyProfile).setOnClickListener {
            applySelectedProfile()
        }
        findViewById<Button>(R.id.openShizuku).setOnClickListener {
            val launch = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (launch != null) startActivity(launch)
            else logStatus.text = "Install Shizuku from its official project first."
        }

        refreshStats()
        updateShizukuStatus()

        lifecycleScope.launch(Dispatchers.Main) {
            while (isActive) {
                refreshStats()
                updateShizukuStatus()
                delay(3000)
            }
        }
    }

    private fun updateShizukuStatus() {
        shizukuStatus.text = if (shizuku.isConnected()) {
            if (shizuku.hasPermission()) "SHIZUKU: CONNECTED • PERMISSION GRANTED"
            else "SHIZUKU: CONNECTED • PERMISSION NEEDED"
        } else "SHIZUKU: NOT RUNNING"
    }

    private fun refreshStats() {
        ramStatus.text = "RAM: ${performance.getRamUsage()} used • ${performance.getAvailableRam()} available"
        thermalStatus.text = "THERMAL: ${performance.getThermalStatus()}"
        cpuStatus.text = "CPU: ${performance.getCpuSummary()}"
    }

    private fun applySelectedProfile() {
        val group = findViewById<RadioGroup>(R.id.profileGroup)
        val message = when (group.checkedRadioButtonId) {
            R.id.profileGame -> {
                performance.requestGameMode()
                "Game focus requested. Android may ignore unsupported game-mode requests."
            }
            R.id.profileSaver -> {
                performance.openBatterySettings()
                "Battery settings opened; enable Battery Saver there if desired."
            }
            else -> "Balanced profile selected. No privileged kernel changes made."
        }
        logStatus.text = message
    }
}
