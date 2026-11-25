package com.livevideowallpaper.app

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.FrameLayout

/** Video lock screen shown above the system lock screen. Swipe up to unlock (PIN/pattern is still asked by Android). */
class LockVideoActivity : AppCompatActivity() {

    private lateinit var bg: BackgroundHost
    private lateinit var content: View
    private var downY = 0f

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.showOverLock(this)
        setContentView(R.layout.activity_lock)
        content = findViewById(R.id.content)
        Screen.edgeToEdge(this, content)
        bg = BackgroundHost(findViewById<FrameLayout>(R.id.bgHost))

        val bar = findViewById<StatusBarView>(R.id.statusBar)
        bar.style = Prefs.battStyle(this)
        bar.notifAnim = Prefs.notifAnim(this)
        if (Prefs.statusOn(this)) {
            bar.visibility = View.VISIBLE
            // our bar replaces the system status bar on this screen; keep it below any inset that remains
            WindowCompat.getInsetsController(window, content).hide(WindowInsetsCompat.Type.statusBars())
            ViewCompat.setOnApplyWindowInsetsListener(bar) { v, insets ->
                val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                (v.layoutParams as FrameLayout.LayoutParams).topMargin = top
                v.requestLayout()
                insets
            }
        } else {
            bar.visibility = View.GONE
        }

        ContextCompat.registerReceiver(
            this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val threshold = 140 * resources.displayMetrics.density
        findViewById<View>(R.id.root).setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    val dy = e.rawY - downY
                    if (dy < 0) content.translationY = dy
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dy = e.rawY - downY
                    if (e.actionMasked == MotionEvent.ACTION_UP && dy < -threshold) unlock()
                    else content.animate().translationY(0f).setDuration(150).start()
                    true
                }
                else -> false
            }
        }
        findViewById<View>(R.id.btnUnlock).setOnClickListener { unlock() }
    }

    override fun onStart() {
        super.onStart()
        bg.start(Prefs.bgPath(this, Prefs.SLOT_LOCK) ?: Prefs.selectedPath(this))
    }

    override fun onResume() {
        super.onResume()
        val km = getSystemService(KeyguardManager::class.java)
        if (!km.isKeyguardLocked) finish()
    }

    override fun onStop() {
        super.onStop()
        bg.release()
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenOff) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun unlock() {
        val km = getSystemService(KeyguardManager::class.java)
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() { finish() }
            override fun onDismissError() { finish() }
            override fun onDismissCancelled() {
                content.animate().translationY(0f).setDuration(150).start()
            }
        })
    }
}
