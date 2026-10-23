package com.killcheat.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.killcheat.app.R
import com.killcheat.app.util.LanguageManager

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "killcheat_auth"
        private const val KEY_SAVED  = "saved_key"
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = LanguageManager.getSavedLanguage(newBase)
        super.attachBaseContext(LanguageManager.applyLanguage(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val title = findViewById<TextView>(R.id.splash_title)
        val logo  = findViewById<ImageView>(R.id.splash_logo)

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration  = 1200
            fillAfter = true
        }
        title.startAnimation(fadeIn)
        logo.startAnimation(fadeIn)

        Handler(Looper.getMainLooper()).postDelayed({
            // Route: if saved key exists → LoginActivity handles auto-verify
            //        no key → LoginActivity shows manual login
            // Either way, always go through LoginActivity first
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500)
    }
}
