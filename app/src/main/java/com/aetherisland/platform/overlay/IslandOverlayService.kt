package com.aetherisland.platform.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Overlay lifecycle shell. WindowManager attachment is implemented in the overlay milestone. */
class IslandOverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
