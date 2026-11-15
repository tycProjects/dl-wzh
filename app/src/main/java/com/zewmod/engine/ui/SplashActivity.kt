package com.zewmod.engine.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zewmod.engine.R
import com.zewmod.engine.databinding.ActivitySplashBinding
import com.zewmod.engine.util.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Logo animation
        binding.splashLogo.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.splash_logo_enter)
        )
        binding.splashTagline.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.splash_fade_in)
        )

        // Version
        binding.splashVersion.text = "v1.0 • ZewMod Engine"

        lifecycleScope.launch {
            delay(1800L)
            navigateNext()
        }
    }

    private fun navigateNext() {
        val destination = if (PermissionHelper.allGranted(this)) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, PermissionActivity::class.java)
        }
        destination.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(destination)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
