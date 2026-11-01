package com.zewmod.hack

import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import com.zewmod.core.WirelessAdbBridge

/**
 * ZewMod — Aimbot Input Injector v2
 *
 * FIX:
 *   - HackState.enemies → HackState.getEnemies()
 *   - Aimbot.getBoneScreen() → Aimbot.getBoneScreenFresh()
 *   - Aimbot.getBestTarget() → signature baru (tanpa cameraForward param)
 *   - screenW/screenH: Int explicit để tránh FloatArray type mismatch
 */
object AimbotInputInjector {

    private const val TAG              = "ZewMod.Aimbot.Injector"
    private const val SWIPE_DURATION_MS = 8

    fun applyDelta(dx: Float, dy: Float, screenW: Int, screenH: Int) {
        if (!WirelessAdbBridge.isConnected) return
        if (Math.abs(dx) < 1f && Math.abs(dy) < 1f) return

        val cx = screenW / 2
        val cy = screenH / 2
        val tx = (cx + dx).toInt().coerceIn(0, screenW)
        val ty = (cy + dy).toInt().coerceIn(0, screenH)

        Thread {
            try {
                WirelessAdbBridge.execCmd(
                    "input touchscreen swipe $cx $cy $tx $ty $SWIPE_DURATION_MS"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Input inject failed: ${e.message}")
            }
        }.start()
    }

    fun buildSwipeEvent(
        fromX: Float, fromY: Float,
        toX: Float,   toY: Float
    ): List<MotionEvent> {
        val now = SystemClock.uptimeMillis()
        val src = InputDevice.SOURCE_TOUCHSCREEN

        val down = MotionEvent.obtain(now, now,
            MotionEvent.ACTION_DOWN, fromX, fromY, 0).also { it.source = src }
        val move = MotionEvent.obtain(now, now + SWIPE_DURATION_MS,
            MotionEvent.ACTION_MOVE, toX, toY, 0).also { it.source = src }
        val up   = MotionEvent.obtain(now, now + SWIPE_DURATION_MS + 1,
            MotionEvent.ACTION_UP, toX, toY, 0).also { it.source = src }

        return listOf(down, move, up)
    }

    /**
     * Tick per-frame — dipanggil dari ModService loop.
     *
     * FIX 1: HackState.getEnemies() bukan HackState.enemies
     * FIX 2: Aimbot.getBestTarget() tanpa cameraForward (param dihapus di v4)
     * FIX 3: Aimbot.getBoneScreenFresh() bukan getBoneScreen()
     * FIX 4: screenW, screenH sebagai Int explicit
     */
    fun tick(screenW: Int, screenH: Int) {
        val cfg = HackState.aimbot
        if (!cfg.enabled) return

        // FIX 1+2: getEnemies(), signature baru
        val target = Aimbot.getBestTarget(
            cfg,
            HackState.getEnemies(),
            screenW,
            screenH
        ) ?: return

        // FIX 3: getBoneScreenFresh bukan getBoneScreen
        val boneScreen = Aimbot.getBoneScreenFresh(target, cfg.targetBone)

        val (dx, dy) = Aimbot.computeAimDelta(boneScreen, screenW, screenH, cfg.smoothing)
        applyDelta(dx, dy, screenW, screenH)
    }
}
