package com.livevideowallpaper.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File

/** Fills [container] with a muted looping video, a photo, or a default gradient. Call start() in onStart and release() in onStop. */
class BackgroundHost(private val container: FrameLayout) {
    private var player: ExoPlayer? = null

    fun start(path: String?) {
        release()
        container.removeAllViews()
        val ctx = container.context
        val f = path?.let { File(it) }
        if (f == null || !f.isFile) {
            container.setBackgroundResource(R.drawable.bg_default_gradient)
            return
        }
        container.background = null
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        if (BgRepository.isVideo(f)) {
            val pv = PlayerView(ctx)
            pv.useController = false
            pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            container.addView(pv, lp)
            val p = ExoPlayer.Builder(ctx).build()
            p.volume = 0f
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()
            p.repeatMode = Player.REPEAT_MODE_ONE
            p.setMediaItem(MediaItem.fromUri(Uri.fromFile(f)))
            p.prepare()
            p.playWhenReady = true
            pv.player = p
            player = p
        } else {
            val iv = ImageView(ctx)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            val m = ctx.resources.displayMetrics
            val bmp = decode(f, m.widthPixels, m.heightPixels)
            if (bmp != null) iv.setImageBitmap(bmp) else container.setBackgroundResource(R.drawable.bg_default_gradient)
            container.addView(iv, lp)
        }
    }

    fun release() {
        player?.release()
        player = null
    }

    private fun decode(f: File, w: Int, h: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, o)
        var s = 1
        while (o.outWidth / (s * 2) >= w && o.outHeight / (s * 2) >= h) s *= 2
        return BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = s })
    }
}

object Screen {
    /** Lets an activity appear above the lock screen and wake the display. */
    fun showOverLock(a: Activity) {
        if (Build.VERSION.SDK_INT >= 27) {
            a.setShowWhenLocked(true)
            a.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            a.window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    /** Draws behind the status/navigation bars and pads [content] so nothing is hidden. */
    fun edgeToEdge(a: Activity, content: View) {
        WindowCompat.setDecorFitsSystemWindows(a.window, false)
        a.window.statusBarColor = Color.TRANSPARENT
        a.window.navigationBarColor = Color.TRANSPARENT
        val pl = content.paddingLeft
        val pt = content.paddingTop
        val pr = content.paddingRight
        val pb = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(pl + b.left, pt + b.top, pr + b.right, pb + b.bottom)
            insets
        }
    }
}
