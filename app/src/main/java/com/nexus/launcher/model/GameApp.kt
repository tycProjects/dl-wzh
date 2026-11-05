package com.nexus.launcher.model

import android.graphics.drawable.Drawable

data class GameApp(
    val packageName: String,
    val name: String,
    val icon: Drawable? = null,
    var playTimeMinutes: Long = 0L,
    var lastPlayed: Long = 0L
) {
    fun getFormattedPlayTime(): String {
        val hours = playTimeMinutes / 60
        val mins = playTimeMinutes % 60
        return when {
            hours > 0 -> "$hours jam $mins mnt"
            else -> "$mins mnt"
        }
    }
}
