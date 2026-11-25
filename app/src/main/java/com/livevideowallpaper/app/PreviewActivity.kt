package com.livevideowallpaper.app

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File

/** Full-screen muted preview and the "Apply wallpaper" screen. */
class PreviewActivity : BaseActivity() {

    companion object { const val EXTRA_PATH = "path" }

    private lateinit var file: File
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    private val liveLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        say(if (WallpaperApplier.isActive(this)) R.string.msg_live_active else R.string.msg_live_not_active)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_PATH)
        if (path == null || !File(path).isFile) { finish(); return }
        file = File(path)
        setContentView(R.layout.activity_preview)

        playerView = findViewById(R.id.playerView)
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        findViewById<TextView>(R.id.title).text = file.nameWithoutExtension
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnHome).setOnClickListener {
            Prefs.setSelectedPath(this, file.absolutePath)
            if (WallpaperApplier.isActive(this)) say(R.string.msg_home_updated) else launchLive()
        }
        findViewById<Button>(R.id.btnLock).setOnClickListener {
            WallpaperApplier.setLockStill(this, file) { _, msg -> sayAsync(msg) }
        }
        findViewById<Button>(R.id.btnBoth).setOnClickListener {
            Prefs.setSelectedPath(this, file.absolutePath)
            launchLive()
        }
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    releasePlayer()
                    file.delete()
                    if (Prefs.selectedPath(this) == file.absolutePath) Prefs.setSelectedPath(this, null)
                    finish()
                }.show()
        }
    }

    private fun launchLive() {
        try {
            liveLauncher.launch(WallpaperApplier.liveWallpaperIntent(this))
        } catch (e: ActivityNotFoundException) {
            say(R.string.msg_no_chooser)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!::file.isInitialized) return
        val p = ExoPlayer.Builder(this).build()
        p.volume = 0f
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()
        p.repeatMode = Player.REPEAT_MODE_ONE
        p.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
        p.prepare()
        p.playWhenReady = true
        playerView.player = p
        player = p
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        if (::playerView.isInitialized) playerView.player = null
        player?.release()
        player = null
    }
}
