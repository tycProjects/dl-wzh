package com.puzzleblast.game.modules

import android.content.Context
import android.hardware.camera2.CameraManager

object FlashModule {
    suspend fun set(ctx: Context, on: Boolean): String {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "🔦 no flash"
            cm.setTorchMode(id, on)
            if (on) "🔦 flash on" else "🔦 flash off"
        } catch (e: Exception) {
            "🔦 failed: ${e.message}"
        }
    }
}
