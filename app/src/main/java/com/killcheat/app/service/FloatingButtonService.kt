package com.killcheat.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.killcheat.app.R
import com.killcheat.app.state.CheatState
import com.killcheat.app.state.FloatState

class FloatingButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var floatView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private var toneGen: ToneGenerator? = null
    private val changeListener: () -> Unit = { applyFloatState() }

    companion object {
        private const val CHANNEL_ID = "killcheat_float"
        private const val NOTIF_ID = 2
    }

    override fun onCreate() {
        super.onCreate()
        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        FloatState.addListener(changeListener)
        showPanel()
    }

    private fun showPanel() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatView = LayoutInflater.from(this).inflate(R.layout.layout_float_button, null)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24; y = 320
        }

        windowManager!!.addView(floatView, params)
        applyFloatState()
        setupDrag()
        setupToggles()
    }

    private fun setupToggles() {
        val fv = floatView ?: return
        val swT = fv.findViewById<Switch>(R.id.float_sw_teleport)
        val swF = fv.findViewById<Switch>(R.id.float_sw_freeze)
        val swG = fv.findViewById<Switch>(R.id.float_sw_ghost)

        swT.isChecked = CheatState.isTeleportEnabled
        swF.isChecked = CheatState.isFreezeEnabled
        swG.isChecked = CheatState.isGhostEnabled

        swT.setOnCheckedChangeListener { _, c -> CheatState.isTeleportEnabled = c; beep(c) }
        swF.setOnCheckedChangeListener { _, c -> CheatState.isFreezeEnabled = c; beep(c) }
        swG.setOnCheckedChangeListener { _, c -> CheatState.isGhostEnabled = c; beep(c) }

        fv.findViewById<TextView>(R.id.float_close).setOnClickListener { stopSelf() }
    }

    private fun beep(on: Boolean) {
        toneGen?.startTone(
            if (on) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2, 100
        )
    }

    private fun setupDrag() {
        var initX = 0; var initY = 0
        var initTX = 0f; var initTY = 0f

        floatView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    initTX = event.rawX; initTY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initX + (event.rawX - initTX).toInt()
                    params.y = initY + (event.rawY - initTY).toInt()
                    windowManager?.updateViewLayout(floatView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun applyFloatState() {
        floatView?.alpha = FloatState.buttonOpacity / 100f
    }

    private fun createChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "KillCheat Float", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KillCheat Active")
            .setContentText("Float panel running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        FloatState.removeListener(changeListener)
        floatView?.let { windowManager?.removeView(it) }
        toneGen?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
