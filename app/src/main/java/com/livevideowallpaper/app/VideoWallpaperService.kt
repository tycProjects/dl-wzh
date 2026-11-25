package com.livevideowallpaper.app

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * Live wallpaper that plays the selected video silently.
 * The player exists only while the surface exists, plays only while the wallpaper is visible,
 * and is fully released otherwise.
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var player: ExoPlayer? = null
        private var surfaceReady = false
        private var visible = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            Prefs.sp(applicationContext).registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            setupPlayer(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            val p = player
            if (isVisible) {
                if (p == null) {
                    if (surfaceReady) setupPlayer(surfaceHolder)
                } else {
                    p.playWhenReady = allowedToPlay()
                }
            } else {
                p?.playWhenReady = false
            }
        }

        override fun onDestroy() {
            Prefs.sp(applicationContext).unregisterOnSharedPreferenceChangeListener(this)
            releasePlayer()
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            if (key != Prefs.KEY_PATH && key != Prefs.KEY_LOOP && key != Prefs.KEY_FIT) return
            if (surfaceReady) {
                releasePlayer()
                setupPlayer(surfaceHolder)
            }
        }

        private fun allowedToPlay(): Boolean {
            if (!visible) return false
            if (Prefs.pauseInSaver(applicationContext)) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isPowerSaveMode) return false
            }
            return true
        }

        private fun setupPlayer(holder: SurfaceHolder) {
            releasePlayer()
            val path = Prefs.selectedPath(applicationContext) ?: return
            val file = File(path)
            if (!file.isFile) return

            val p = ExoPlayer.Builder(applicationContext).build()
            p.volume = 0f // silent
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true) // audio is never even decoded
                .build()
            p.repeatMode = if (Prefs.loop(applicationContext)) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            p.videoScalingMode =
                if (Prefs.fit(applicationContext)) C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                else C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            p.setVideoSurfaceHolder(holder)
            p.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            p.prepare()
            p.playWhenReady = allowedToPlay()
            player = p
        }

        private fun releasePlayer() {
            player?.let {
                it.clearVideoSurface()
                it.release()
            }
            player = null
        }
    }
}
