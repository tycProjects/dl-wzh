package com.example.screenrecorder

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.screenrecorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var projectionManager: MediaProjectionManager
    private var isRecording = false

    private val screenCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                startRecordingService(result.resultCode, result.data!!)
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
                updateButtonState(false)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        updateButtonState(RecordingService.isRunning)

        binding.btnRecord.setOnClickListener {
            if (RecordingService.isRunning) stopRecordingService() else requestScreenCapture()
        }
    }

    private fun requestScreenCapture() {
        // No microphone permission is requested because this build records
        // pure screen video. That avoids an unnecessary permission prompt.
        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun startRecordingService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(RecordingService.EXTRA_RESULT_DATA, data)
        }

        ContextCompat.startForegroundService(this, serviceIntent)
        isRecording = true
        updateButtonState(true)

        // Immediately hide the recorder UI so it doesn't become part of the capture.
        moveTaskToBack(true)
    }

    private fun stopRecordingService() {
        val serviceIntent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        startService(serviceIntent)
        isRecording = false
        updateButtonState(false)
    }

    private fun updateButtonState(recording: Boolean) {
        isRecording = recording
        binding.btnRecord.text = getString(
            if (recording) R.string.stop_recording else R.string.start_recording
        )
        binding.tvStatus.text = getString(
            if (recording) R.string.status_recording else R.string.status_idle
        )
    }

    override fun onResume() {
        super.onResume()
        updateButtonState(RecordingService.isRunning)
    }
}
