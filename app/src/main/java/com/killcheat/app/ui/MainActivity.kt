package com.killcheat.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.killcheat.app.R
import com.killcheat.app.adapter.MainPagerAdapter
import com.killcheat.app.service.CheatService
import com.killcheat.app.service.FloatingButtonService
import com.killcheat.app.util.LanguageManager

class MainActivity : AppCompatActivity() {

    companion object {
        private const val OVERLAY_REQUEST = 1001
    }

    private lateinit var viewPager: ViewPager2

    override fun attachBaseContext(newBase: Context) {
        val lang = LanguageManager.getSavedLanguage(newBase)
        super.attachBaseContext(LanguageManager.applyLanguage(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = MainPagerAdapter(this)
        viewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> getString(R.string.tab_features)
                1 -> getString(R.string.tab_float)
                2 -> getString(R.string.tab_settings)
                else -> ""
            }
        }.attach()

        // Language picker — top right
        findViewById<TextView>(R.id.btn_language).setOnClickListener {
            showLanguagePicker()
        }
    }

    private fun showLanguagePicker() {
        val codes = LanguageManager.LANGUAGES.keys.toTypedArray()
        val names = LanguageManager.LANGUAGES.values.toTypedArray()
        val current = LanguageManager.getSavedLanguage(this)
        val currentIdx = codes.indexOf(current).coerceAtLeast(0)

        AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.language))
            .setSingleChoiceItems(names, currentIdx) { dialog, which ->
                val chosen = codes[which]
                LanguageManager.saveLanguage(this, chosen)
                dialog.dismiss()
                // Restart activity to apply
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            .show()
    }

    fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_REQUEST)
        } else {
            startFloatingService()
        }
    }

    private fun startFloatingService() {
        startForegroundService(Intent(this, FloatingButtonService::class.java))
    }

    fun navigateToFloatTab() {
        viewPager.setCurrentItem(1, true)
    }

    @Deprecated("onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQUEST) {
            if (Settings.canDrawOverlays(this)) startFloatingService()
            else Toast.makeText(this, getString(R.string.overlay_denied), Toast.LENGTH_SHORT).show()
        }
    }
}
