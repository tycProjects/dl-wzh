package com.zewmod.hack

import android.graphics.Color
import android.graphics.PointF
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/**
 * ZewMod — Hack Feature Definitions v4
 *
 * EnemyData bây giờ lưu WORLD POSITIONS 3D thay vì screen coords.
 * Screen coords tính real-time trong onDraw mỗi frame via CameraProjection.
 * Khi user xoay camera → CameraProjection.updateMatrices() ngay lập tức
 * → worldToScreen() cho kết quả mới trong cùng frame → ESP selalu tepat.
 */

// ─── ENEMY DATA ─────────────────────────────────────────────────────────────

data class EnemyData(
    val id: Int,
    val name: String,
    val teamId: Int,
    val hp: Int,
    val isLocalPlayer: Boolean,

    // World positions 3D — update dari patch per-frame
    val rootWorld:        FloatArray = floatArrayOf(0f, 0f, 0f),
    val headWorld:        FloatArray = floatArrayOf(0f, 0f, 0f),
    val spineWorld:       FloatArray = floatArrayOf(0f, 0f, 0f),
    val shoulderWorld:    FloatArray = floatArrayOf(0f, 0f, 0f),
    val hipsWorld:        FloatArray = floatArrayOf(0f, 0f, 0f),
    val ankleLeftWorld:   FloatArray = floatArrayOf(0f, 0f, 0f),
    val ankleRightWorld:  FloatArray = floatArrayOf(0f, 0f, 0f),
    val toeLeftWorld:     FloatArray = floatArrayOf(0f, 0f, 0f),
    val toeRightWorld:    FloatArray = floatArrayOf(0f, 0f, 0f),

    // Legacy screen coords — digunakan sebagai fallback jika world tidak tersedia
    // Sẽ di-override bởi worldToScreen() dalam onDraw jika hasWorldPos = true
    val rootScreen:       PointF = PointF(0f, 0f),
    val headScreen:       PointF = PointF(0f, 0f),
    val spineScreen:      PointF = PointF(0f, 0f),
    val shoulderScreen:   PointF = PointF(0f, 0f),
    val hipsScreen:       PointF = PointF(0f, 0f),
    val ankleLeftScreen:  PointF = PointF(0f, 0f),
    val ankleRightScreen: PointF = PointF(0f, 0f),
    val toeLeftScreen:    PointF = PointF(0f, 0f),
    val toeRightScreen:   PointF = PointF(0f, 0f),

    // Mode flag — true = dùng worldToScreen real-time (FIX utama)
    val hasWorldPos: Boolean = false,

    val isVisible: Boolean = true,
    val isPrepareAttack: Boolean = false,

    // rootPos[2] = z-depth untuk distance
    val rootPos: FloatArray = floatArrayOf(0f, 0f, 0f),
    val forwardVec: FloatArray = floatArrayOf(0f, 0f, 1f),
)

// ─── FEATURE CONFIGS ────────────────────────────────────────────────────────

data class AimbotConfig(
    var enabled: Boolean = false,
    var targetBone: TargetBone = TargetBone.HEAD,
    var fovDegrees: Float = 45f,
    var smoothing: Float = 0.3f,
    var aimKey: Int = 0,
    var visibleOnly: Boolean = true,
    var teamCheck: Boolean = true,
    var predictMovement: Boolean = false,
)

data class EspConfig(
    var enabled: Boolean = false,
    var boxEsp: Boolean = true,
    var skeletonEsp: Boolean = true,
    var nameEsp: Boolean = true,
    var hpEsp: Boolean = true,
    var distanceEsp: Boolean = true,
    var lineEsp: Boolean = true,
    var colorEnemy: Int = Color.RED,
    var colorTeam: Int = Color.GREEN,
    var lineWidth: Float = 2f,
    var chams: Boolean = false,
    var chamsColorEnemy: Int = Color.argb(180, 255, 0, 0),
    var chamsColorTeam: Int = Color.argb(120, 0, 255, 0),
)

data class ProxyConfig(
    var enabled: Boolean = false,
    var localPort: Int = 7777,
    var remoteHost: String = "127.0.0.1",
    var remotePort: Int = 7778,
    var frameInterval: Int = 1,
)

enum class TargetBone {
    HEAD, CHEST, HIPS, RANDOM_BONE
}

// ─── HACK STATE ─────────────────────────────────────────────────────────────

object HackState {
    var aimbot = AimbotConfig()
    var esp    = EspConfig()
    var proxy  = ProxyConfig()

    var localPlayerId: Int = -1
    var screenWidth: Int   = 1080
    var screenHeight: Int  = 1920

    // Thread-safe: ProxyRelay (background) write, Choreographer (main) read
    private val _enemies = AtomicReference<List<EnemyData>>(emptyList())

    fun setEnemies(list: List<EnemyData>) = _enemies.set(list)
    fun getEnemies(): List<EnemyData>      = _enemies.get()

    fun reset() {
        _enemies.set(emptyList())
        localPlayerId = -1
    }
}

// ─── PROJECTED BONE DATA ───────────────────────────────────────────────────
//
// Dihitung SEKALI per enemy per frame dalam onDraw — bukan per bone call
// Simpan hasil worldToScreen ke struct ini agar tiap draw step
// (box, skeleton, hp, name) pakai data yang sama, tanpa re-project

data class ProjectedEnemy(
    val src: EnemyData,
    val head:      PointF,
    val root:      PointF,
    val spine:     PointF,
    val shoulder:  PointF,
    val hips:      PointF,
    val ankleL:    PointF,
    val ankleR:    PointF,
    val toeL:      PointF,
    val toeR:      PointF,

    // Bounding box — dihitung dari bone screen coords
    val boxX: Float,
    val boxY: Float,
    val boxW: Float,
    val boxH: Float,

    // Foot midpoint untuk snapline
    val footX: Float,
    val footY: Float,

    val onScreen: Boolean,   // false = di belakang kamera atau diluar layar
)

// ─── AIMBOT LOGIC ───────────────────────────────────────────────────────────

object Aimbot {

    fun getBestTarget(
        cfg: AimbotConfig,
        enemies: List<EnemyData>,
        screenW: Int,
        screenH: Int
    ): EnemyData? {
        val cx        = screenW / 2f
        val cy        = screenH / 2f
        val fovRadius = (cfg.fovDegrees / 180f) * screenW * 0.5f
        val localTeam = HackState.getEnemies().find { it.isLocalPlayer }?.teamId ?: -1

        return enemies
            .filter { e ->
                !e.isLocalPlayer &&
                (!cfg.teamCheck || localTeam == -1 || e.teamId != localTeam) &&
                (!cfg.visibleOnly || e.isVisible) &&
                e.hp > 0
            }
            .minByOrNull { e ->
                val s = getBoneScreenFresh(e, cfg.targetBone)
                val dx = s.x - cx; val dy = s.y - cy
                dx * dx + dy * dy
            }
            ?.takeIf { e ->
                val s = getBoneScreenFresh(e, cfg.targetBone)
                val dx = s.x - cx; val dy = s.y - cy
                Math.sqrt((dx * dx + dy * dy).toDouble()) <= fovRadius
            }
    }

    // Selalu project ulang dari world position — tidak pakai stale screen data
    fun getBoneScreenFresh(e: EnemyData, bone: TargetBone): PointF {
        return if (e.hasWorldPos) {
            when (bone) {
                TargetBone.HEAD  -> CameraProjection.worldToScreen(e.headWorld)
                TargetBone.CHEST -> CameraProjection.worldToScreen(e.spineWorld)
                TargetBone.HIPS  -> CameraProjection.worldToScreen(e.hipsWorld)
                else             -> CameraProjection.worldToScreen(e.headWorld)
            }
        } else {
            when (bone) {
                TargetBone.HEAD  -> e.headScreen
                TargetBone.CHEST -> e.spineScreen
                TargetBone.HIPS  -> e.hipsScreen
                else             -> e.headScreen
            }
        }
    }

    fun computeAimDelta(
        targetScreen: PointF,
        screenW: Int,
        screenH: Int,
        smooth: Float
    ): Pair<Float, Float> {
        val rawDx = targetScreen.x - screenW / 2f
        val rawDy = targetScreen.y - screenH / 2f
        return Pair(rawDx * (1f - smooth), rawDy * (1f - smooth))
    }
}

// ─── ESP RENDERER ──────────────────────────────────────────────────────────

object EspRenderer {

    /**
     * Project SEMUA bones enemy ke screen coords dalam satu call.
     * Dihitung di awal onDraw sebelum draw steps.
     *
     * Nếu hasWorldPos = true → CameraProjection.worldToScreen (real-time, selalu fresh)
     * Nếu hasWorldPos = false → legacy screen coords dari UDP (fallback)
     *
     * @param screenW viewport width
     * @param screenH viewport height
     */
    fun project(e: EnemyData, screenW: Int, screenH: Int): ProjectedEnemy {
        val head: PointF
        val root: PointF
        val spine: PointF
        val shoulder: PointF
        val hips: PointF
        val ankleL: PointF
        val ankleR: PointF
        val toeL: PointF
        val toeR: PointF

        if (e.hasWorldPos) {
            // ── MODE BARU: world positions → project real-time ──
            head     = CameraProjection.worldToScreen(e.headWorld)
            root     = CameraProjection.worldToScreen(e.rootWorld)
            spine    = CameraProjection.worldToScreen(e.spineWorld)
            shoulder = CameraProjection.worldToScreen(e.shoulderWorld)
            hips     = CameraProjection.worldToScreen(e.hipsWorld)
            ankleL   = CameraProjection.worldToScreen(e.ankleLeftWorld)
            ankleR   = CameraProjection.worldToScreen(e.ankleRightWorld)
            toeL     = CameraProjection.worldToScreen(e.toeLeftWorld)
            toeR     = CameraProjection.worldToScreen(e.toeRightWorld)
        } else {
            // ── FALLBACK: legacy screen coords dari UDP ──
            head     = e.headScreen
            root     = e.rootScreen
            spine    = e.spineScreen
            shoulder = e.shoulderScreen
            hips     = e.hipsScreen
            ankleL   = e.ankleLeftScreen
            ankleR   = e.ankleRightScreen
            toeL     = e.toeLeftScreen
            toeR     = e.toeRightScreen
        }

        // Di belakang kamera
        if (head.x < 0f && head.y < 0f) {
            return ProjectedEnemy(e, head, root, spine, shoulder, hips, ankleL, ankleR,
                toeL, toeR, 0f, 0f, 0f, 0f, 0f, 0f, false)
        }

        // Bounding box dari semua bone yang valid
        val pts = listOf(head, spine, hips, ankleL, ankleR, toeL, toeR, shoulder)
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in pts) {
            if (p.x <= 0f && p.y <= 0f) continue
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
        }
        if (minX == Float.MAX_VALUE) minX = 0f
        if (minY == Float.MAX_VALUE) minY = 0f
        if (maxX == -Float.MAX_VALUE) maxX = 0f
        if (maxY == -Float.MAX_VALUE) maxY = 0f

        val bx = minX - 4f
        val by = minY - 6f
        val bw = (maxX - minX) + 8f
        val bh = (maxY - minY) + 8f

        val footX = (ankleL.x + ankleR.x) * 0.5f
        val footY = maxOf(toeL.y, toeR.y)

        // Onscreen check — ít nhất head hoặc hips phải trong viewport
        val onScreen = head.x in 0f..screenW.toFloat() && head.y in 0f..screenH.toFloat() ||
                       hips.x in 0f..screenW.toFloat() && hips.y in 0f..screenH.toFloat()

        return ProjectedEnemy(e, head, root, spine, shoulder, hips, ankleL, ankleR,
            toeL, toeR, bx, by, bw, bh, footX, footY, onScreen)
    }

    // Distance dalam meter (z-depth dari worldPos atau rootPos)
    fun distanceM(e: EnemyData): Int {
        val z = if (e.hasWorldPos) {
            CameraProjection.worldToScreen(e.rootWorld).let { _ ->
                // Hitung jarak 3D dari asal (local player ~= origin)
                Math.sqrt((e.rootWorld[0] * e.rootWorld[0] +
                           e.rootWorld[2] * e.rootWorld[2]).toDouble()).toFloat()
            }
        } else {
            Math.abs(e.rootPos.getOrElse(2) { 0f })
        }
        return z.toInt()
    }
}
