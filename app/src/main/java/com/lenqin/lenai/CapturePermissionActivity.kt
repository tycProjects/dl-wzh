package com.lenqin.lenai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat

class CapturePermissionActivity : Activity() {
    companion object {
        private const val REQUEST_SCREEN_CAPTURE = 701
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
    }

    @Deprecated("Uses Android activity result callback for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_START_PROJECTION
                    putExtra(OverlayService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(OverlayService.EXTRA_RESULT_DATA, data)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
                Toast.makeText(this, "Izin baca layar aktif. Buka Chrome atau aplikasi tujuan.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Izin baca layar tidak diberikan.", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
