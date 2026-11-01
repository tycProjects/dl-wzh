package com.zewmod.hack

import android.graphics.PointF
import java.util.concurrent.atomic.AtomicReference

/**
 * ZewMod — CameraProjection
 *
 * ROOT CAUSE của ESP trượt khỏi địch khi xoay:
 *
 *   TRƯỚC: Patch gọi Unity WorldToScreenPoint() → gửi tọa độ 2D qua UDP
 *          → ProxyRelay nhận → HackState update → overlay render
 *          Chuỗi: UDP roundtrip ~3–10ms + thread switch + 1 frame delay
 *          → Khi user xoay nhanh, screen coords đã CŨ, chưa reflect camera mới
 *          → ESP trễ, trượt khỏi địch
 *
 *   SAU:   Patch gửi world positions 3D + camera matrix mỗi frame
 *          → Overlay tự tính WorldToScreen trong VSYNC frame hiện tại
 *          → Camera matrix selalu baru nhất (từ cùng frame Unity render)
 *          → Không bao giờ stale, ESP dính chặt địch dù xoay bao nhanh
 *
 *
 * Pipeline baru:
 *
 *   Unity Game (IL2CPP patch):
 *     Camera.main.worldToCameraMatrix  → gửi 4×4 matrix qua UDP
 *     Camera.main.projectionMatrix     → gửi 4×4 matrix qua UDP
 *     enemy.transform.position         → gửi Vector3 per enemy per bone
 *
 *   ProxyRelay (background thread):
 *     Terima packet → parse matrix + world positions
 *     → CameraProjection.updateMatrix(view, proj)
 *     → HackState.setEnemies(list dengan worldPos, screen = PointF(0,0))
 *
 *   EspOverlayView.onDraw (Choreographer, main thread):
 *     Untuk tiap enemy, tiap bone → worldToScreen(worldPos) real-time
 *     → Hasil langsung dipakai di frame ini, selalu fresh
 *
 *
 * Format packet baru dari patch:
 *
 *   |CAM_VIEW|m00,m01,...,m15|CAM_PROJ|m00,...,m15|
 *   |SCREEN_W|1080|SCREEN_H|1920|
 *
 *   Per enemy:
 *   |ENEMY|id|HP|hp|NAME|nick|TEAM|tid|SELF|0|
 *   |ROOT_WORLD|x,y,z|HEAD_WORLD|x,y,z|SPINE_WORLD|x,y,z|
 *   |SHOULDER_WORLD|x,y,z|HIPS_WORLD|x,y,z|
 *   |L_ANKLE_WORLD|x,y,z|R_ANKLE_WORLD|x,y,z|
 *   |L_TOE_WORLD|x,y,z|R_TOE_WORLD|x,y,z|
 *   |VIS|1|
 */
object CameraProjection {

    // ── Camera matrices (diterima dari patch per-frame via UDP) ──────────────

    private val _viewMatrix = AtomicReference(FloatArray(16).also { identityMatrix(it) })
    private val _projMatrix = AtomicReference(FloatArray(16).also { identityMatrix(it) })

    // Screen dimensions — update dari packet
    @Volatile var screenW: Int = 1080
    @Volatile var screenH: Int = 1920

    // Combined VP matrix cache — recomputed ketika view atau proj berubah
    private val _vpMatrix = AtomicReference(FloatArray(16).also { identityMatrix(it) })

    // ── Matrix Update (dari ProxyRelay background thread) ────────────────────

    fun updateViewMatrix(m: FloatArray) {
        _viewMatrix.set(m.copyOf())
        recomputeVP()
    }

    fun updateProjMatrix(m: FloatArray) {
        _projMatrix.set(m.copyOf())
        recomputeVP()
    }

    fun updateMatrices(view: FloatArray, proj: FloatArray, sw: Int, sh: Int) {
        _viewMatrix.set(view.copyOf())
        _projMatrix.set(proj.copyOf())
        screenW = sw
        screenH = sh
        recomputeVP()
    }

    private fun recomputeVP() {
        val vp = FloatArray(16)
        multiplyMatrix(_projMatrix.get(), _viewMatrix.get(), vp)
        _vpMatrix.set(vp)
    }

    // ── World → Screen Projection (dipanggil dari onDraw, TIAP FRAME) ────────
    //
    // Ekuivalen dengan:
    //   Vector3 screenPos = Camera.main.WorldToScreenPoint(worldPos);
    //
    // tapi dihitung di sisi overlay dengan matrix terbaru,
    // tidak menunggu UDP roundtrip.

    fun worldToScreen(wx: Float, wy: Float, wz: Float): PointF {
        val vp = _vpMatrix.get()

        // Clip space: vp * [wx, wy, wz, 1]
        val cx = vp[0]*wx + vp[4]*wy + vp[8]*wz  + vp[12]
        val cy = vp[1]*wx + vp[5]*wy + vp[9]*wz  + vp[13]
        // val cz = vp[2]*wx + vp[6]*wy + vp[10]*wz + vp[14]
        val cw = vp[3]*wx + vp[7]*wy + vp[11]*wz + vp[15]

        // Di belakang kamera — jangan render
        if (cw < 0.001f) return PointF(-1f, -1f)

        // NDC: [-1, 1]
        val ndcX =  cx / cw
        val ndcY = -cy / cw    // flip Y: OpenGL = bottom-up, screen = top-down

        // Screen pixels
        val sx = (ndcX + 1f) * 0.5f * screenW
        val sy = (ndcY + 1f) * 0.5f * screenH

        return PointF(sx, sy)
    }

    fun worldToScreen(pos: FloatArray): PointF =
        worldToScreen(pos[0], pos[1], pos[2])

    // ── Matrix Math ─────────────────────────────────────────────────────────

    /**
     * Matrix multiply: out = a * b
     * Column-major 4×4 (đúng với Unity / OpenGL convention)
     */
    private fun multiplyMatrix(a: FloatArray, b: FloatArray, out: FloatArray) {
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += a[row + k * 4] * b[k + col * 4]
                }
                out[row + col * 4] = sum
            }
        }
    }

    private fun identityMatrix(m: FloatArray) {
        m.fill(0f)
        m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
    }
}
