package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.RadioPlaybackController
import com.example.ui.JnmfApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val openVideoPlayerTrigger = MutableStateFlow(0L)
    private val openRadioSchedulerTrigger = MutableStateFlow(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicController.bindAppContext(applicationContext)
        RadioPlaybackController.bindAppContext(applicationContext)
        handleIncomingIntent(intent)
        enableEdgeToEdge()
        setContent {
            val openVideoToken by openVideoPlayerTrigger.collectAsState()
            val openRadioToken by openRadioSchedulerTrigger.collectAsState()
            JnmfApp(
                openVideoPlayerToken = openVideoToken,
                openRadioSchedulerToken = openRadioToken
            )
        }
    }

    override fun onStop() {
        MusicController.flushPlaybackSnapshotNow()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(PlaybackService.EXTRA_OPEN_VIDEO_PLAYER, false) == true) {
            openVideoPlayerTrigger.value = System.nanoTime()
            intent.removeExtra(PlaybackService.EXTRA_OPEN_VIDEO_PLAYER)
        }
        if (intent?.getBooleanExtra(PlaybackService.EXTRA_OPEN_RADIO_SCHEDULER, false) == true) {
            openRadioSchedulerTrigger.value = System.nanoTime()
            intent.removeExtra(PlaybackService.EXTRA_OPEN_RADIO_SCHEDULER)
        }
    }
}
