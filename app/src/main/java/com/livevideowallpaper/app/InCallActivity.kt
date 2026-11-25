package com.livevideowallpaper.app

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Incoming / outgoing / ongoing call screen with a video or photo background. */
class InCallActivity : AppCompatActivity() {

    private lateinit var bg: BackgroundHost
    private lateinit var txtAvatar: TextView
    private lateinit var txtName: TextView
    private lateinit var txtStatus: TextView
    private lateinit var groupIncoming: View
    private lateinit var groupActive: View
    private lateinit var btnAccept: ImageView
    private lateinit var btnReject: ImageView
    private lateinit var btnEnd: ImageView
    private lateinit var btnMute: ImageView
    private lateinit var btnSpeaker: ImageView
    private lateinit var btnHold: ImageView
    private lateinit var ringAccept: View
    private lateinit var ringReject: View

    private val handler = Handler(Looper.getMainLooper())
    private val animators = ArrayList<Animator>()
    private val listener: () -> Unit = { update() }
    private val tick = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, 1000)
        }
    }

    private var shownNumber: String? = null
    private var displayName: String? = null
    private var closing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.showOverLock(this)
        setContentView(R.layout.activity_incall)
        Screen.edgeToEdge(this, findViewById(R.id.content))

        bg = BackgroundHost(findViewById<FrameLayout>(R.id.bgHost))
        txtAvatar = findViewById(R.id.txtAvatar)
        txtName = findViewById(R.id.txtName)
        txtStatus = findViewById(R.id.txtStatus)
        groupIncoming = findViewById(R.id.groupIncoming)
        groupActive = findViewById(R.id.groupActive)
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)
        btnEnd = findViewById(R.id.btnEnd)
        btnMute = findViewById(R.id.btnMute)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        btnHold = findViewById(R.id.btnHold)
        ringAccept = findViewById(R.id.ringAccept)
        ringReject = findViewById(R.id.ringReject)

        val glass = Prefs.btnStyle(this) == 1
        btnAccept.setBackgroundResource(if (glass) R.drawable.bg_btn_accept_glass else R.drawable.bg_btn_accept)
        btnReject.setBackgroundResource(if (glass) R.drawable.bg_btn_reject_glass else R.drawable.bg_btn_reject)
        btnEnd.setBackgroundResource(if (glass) R.drawable.bg_btn_reject_glass else R.drawable.bg_btn_reject)

        btnAccept.setOnClickListener { CallHub.primary()?.answer(VideoProfile.STATE_AUDIO_ONLY) }
        btnReject.setOnClickListener { endOrReject() }
        btnEnd.setOnClickListener { endOrReject() }
        btnMute.setOnClickListener {
            CallHub.service?.setMuted(!(CallHub.audio?.isMuted ?: false))
        }
        btnSpeaker.setOnClickListener {
            val speaker = CallHub.audio?.route == CallAudioState.ROUTE_SPEAKER
            CallHub.service?.setAudioRoute(
                if (speaker) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER
            )
        }
        btnHold.setOnClickListener {
            val c = CallHub.primary() ?: return@setOnClickListener
            if (c.state == Call.STATE_HOLDING) c.unhold() else c.hold()
        }
    }

    private fun endOrReject() {
        val c = CallHub.primary() ?: return
        if (c.state == Call.STATE_RINGING) c.reject(false, null) else c.disconnect()
    }

    override fun onStart() {
        super.onStart()
        closing = false
        bg.start(Prefs.bgPath(this, Prefs.SLOT_CALL))
        CallHub.addListener(listener)
        handler.post(tick)
        pulse(ringAccept)
        pulse(ringReject)
        val bounce = ObjectAnimator.ofFloat(btnAccept, View.TRANSLATION_Y, 0f, -10f * resources.displayMetrics.density)
        bounce.duration = 600
        bounce.repeatMode = ValueAnimator.REVERSE
        bounce.repeatCount = ValueAnimator.INFINITE
        bounce.start()
        animators.add(bounce)
    }

    override fun onStop() {
        super.onStop()
        CallHub.removeListener(listener)
        handler.removeCallbacksAndMessages(null)
        animators.forEach { it.cancel() }
        animators.clear()
        btnAccept.translationY = 0f
        bg.release()
    }

    private fun pulse(ring: View) {
        val a = ObjectAnimator.ofPropertyValuesHolder(
            ring,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0f)
        )
        a.duration = 1400
        a.repeatCount = ValueAnimator.INFINITE
        a.start()
        animators.add(a)
    }

    private fun setToggle(btn: ImageView, on: Boolean) {
        btn.setBackgroundResource(if (on) R.drawable.bg_ctrl_on else R.drawable.bg_ctrl)
        btn.imageTintList = ColorStateList.valueOf(if (on) 0xFF0D1B2A.toInt() else Color.WHITE)
    }

    private fun update() {
        val call = CallHub.primary()
        if (call == null) {
            txtStatus.text = "Call ended"
            if (!closing) {
                closing = true
                handler.postDelayed({ finish() }, 800)
            }
            return
        }
        val number = CallHub.numberOf(call)
        if (number != shownNumber) {
            shownNumber = number
            displayName = null
            Bg.io.execute {
                val n = ContactsRepo.nameFor(this, number)
                runOnUiThread { if (!isFinishing && !isDestroyed) { displayName = n; showName(number) } }
            }
        }
        showName(number)

        val ringing = call.state == Call.STATE_RINGING
        groupIncoming.visibility = if (ringing) View.VISIBLE else View.GONE
        groupActive.visibility = if (ringing) View.GONE else View.VISIBLE

        txtStatus.text = when (call.state) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
            Call.STATE_ACTIVE -> elapsed(call)
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "Call ended"
            else -> ""
        }
        setToggle(btnMute, CallHub.audio?.isMuted ?: false)
        setToggle(btnSpeaker, CallHub.audio?.route == CallAudioState.ROUTE_SPEAKER)
        setToggle(btnHold, call.state == Call.STATE_HOLDING)
    }

    private fun showName(number: String) {
        val n = displayName
        txtName.text = n ?: number.ifEmpty { "Unknown" }
        val first = (n ?: number).trim().firstOrNull()
        txtAvatar.text = (first?.uppercaseChar() ?: '?').toString()
    }

    private fun elapsed(call: Call): String {
        val t = call.details?.connectTimeMillis ?: 0L
        if (t <= 0L) return "Connected"
        val s = ((System.currentTimeMillis() - t) / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(s / 60, s % 60)
    }
}
