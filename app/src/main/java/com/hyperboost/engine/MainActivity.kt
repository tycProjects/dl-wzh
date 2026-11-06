// app/src/main/java/com/hyperboost/engine/MainActivity.kt
package com.hyperboost.engine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.hyperboost.engine.ui.FfViewModel
import com.hyperboost.engine.ui.FreeFireApp
import com.hyperboost.engine.ui.MainViewModel
import com.hyperboost.engine.ui.theme.HyperBoostTheme

/** LSUPRA Game Turbo: edge-to-edge Compose host. */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val ffViewModel: FfViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        checkPostNotificationPermission()

        setContent {
            HyperBoostTheme {
                FreeFireApp(
                    viewModel = viewModel,
                    ff = ffViewModel,
                    onOpenOverlaySettings = { openOverlaySettings() },
                    onOpenAccessibilitySettings = { openAccessibilitySettings() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setUiVisible(true)
    }

    override fun onStop() {
        viewModel.setUiVisible(false)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reevaluateShizuku()
        ffViewModel.refresh()
        viewModel.refreshDisplayConfig()
    }

    private fun checkPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openOverlaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) } catch (_: Exception) {}
        }
    }

    private fun openAccessibilitySettings() {
        try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {}
    }
}
