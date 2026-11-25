package com.livevideowallpaper.app

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.io.File

object WallpaperApplier {

    fun isActive(c: Context): Boolean {
        val info = WallpaperManager.getInstance(c).wallpaperInfo ?: return false
        return info.packageName == c.packageName && info.serviceName == VideoWallpaperService::class.java.name
    }

    /** Opens the system live-wallpaper preview. On Android 14+ the system dialog lets the user pick Home, Lock or Both. */
    fun liveWallpaperIntent(c: Context): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(c, VideoWallpaperService::class.java)
        )

    /** Sets a still frame of the video as lock screen wallpaper (documented WallpaperManager API). */
    fun setLockStill(c: Context, file: File, onDone: (Boolean, Int) -> Unit) {
        val app = c.applicationContext
        Bg.io.execute {
            val wm = WallpaperManager.getInstance(app)
            val result: Pair<Boolean, Int> = try {
                if (!wm.isWallpaperSupported || !wm.isSetWallpaperAllowed) {
                    Pair(false, R.string.msg_lock_unsupported)
                } else {
                    val bmp = ThumbnailLoader.frame(file, 2400, atMiddle = true)
                    if (bmp == null) Pair(false, R.string.msg_lock_fail) else {
                        wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
                        Pair(true, R.string.msg_lock_ok)
                    }
                }
            } catch (e: Exception) {
                Pair(false, R.string.msg_lock_fail)
            }
            onDone(result.first, result.second)
        }
    }
}
