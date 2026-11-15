package com.zewmod.engine.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zewmod.engine.R
import com.zewmod.engine.ZewModApp
import com.zewmod.engine.bridge.ZewModBridge
import com.zewmod.engine.databinding.ActivityMainBinding
import com.zewmod.engine.service.FloatingService
import com.zewmod.engine.service.ZewModVpnService
import com.zewmod.engine.ui.fragments.*
import com.zewmod.engine.util.PermissionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Double-check permissions
        if (!PermissionHelper.allGranted(this)) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }

        setupNavigation()
        setupStatusBar()
        observeEngineState()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val frag: Fragment = when (item.itemId) {
                R.id.nav_home     -> HomeFragment()
                R.id.nav_features -> FeatureFragment()
                R.id.nav_server   -> ServerFragment()
                R.id.nav_settings -> SettingsFragment()
                else              -> HomeFragment()
            }
            loadFragment(frag)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_enter,
                R.anim.fragment_exit,
                R.anim.fragment_enter,
                R.anim.fragment_exit
            )
            .replace(R.id.main_fragment_container, fragment)
            .commit()
    }

    private fun setupStatusBar() {
        // Update status bar badge
        lifecycleScope.launch {
            ZewModBridge.status.collectLatest { status ->
                binding.mainStatusBadge.text = status
                val color = when {
                    status.contains("ACTIVE")   -> getColor(R.color.zew_green)
                    status.contains("LỖI")      -> getColor(R.color.zew_red)
                    else                        -> getColor(R.color.zew_yellow)
                }
                binding.mainStatusDot.setBackgroundColor(color)
            }
        }
    }

    private fun observeEngineState() {
        lifecycleScope.launch {
            ZewModBridge.engineRunning.collectLatest { running ->
                binding.fabLaunch.apply {
                    if (running) {
                        setImageResource(R.drawable.ic_stop)
                        backgroundTintList = getColorStateList(R.color.zew_red)
                        contentDescription = "Dừng Engine"
                    } else {
                        setImageResource(R.drawable.ic_play)
                        backgroundTintList = getColorStateList(R.color.zew_accent)
                        contentDescription = "Khởi chạy"
                    }
                }
            }
        }

        binding.fabLaunch.setOnClickListener {
            if (ZewModBridge.engineRunning.value) {
                stopEngine()
            } else {
                startEngine()
            }
        }
    }

    private fun startEngine() {
        val app = applicationContext as ZewModApp
        val config = app.config

        // Start VPN service
        val vpnIntent = Intent(this, ZewModVpnService::class.java)
        startForegroundService(vpnIntent)

        // Start floating service
        val floatIntent = Intent(this, FloatingService::class.java)
        startForegroundService(floatIntent)

        // Animate FAB
        binding.fabLaunch.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.scale_up)
        )
    }

    private fun stopEngine() {
        // Stop VPN
        val vpnStop = Intent(this, ZewModVpnService::class.java).apply {
            action = ZewModVpnService.ACTION_STOP
        }
        startService(vpnStop)

        // Stop floating
        val floatStop = Intent(this, FloatingService::class.java).apply {
            action = FloatingService.ACTION_STOP_FLOAT
        }
        startService(floatStop)

        ZewModBridge.stop()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions
        if (!PermissionHelper.allGranted(this)) {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
    }
}
