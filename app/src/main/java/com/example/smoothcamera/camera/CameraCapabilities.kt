package com.example.smoothcamera.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import androidx.camera.core.CameraInfo
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapabilities

/** Hardware facts only: UI never assumes a capability that isn't reported by the device. */
class CameraCapabilities(context: Context) {
    private val manager = context.getSystemService(CameraManager::class.java)

    data class LensInfo(val id: String, val facing: Int, val focalLengths: FloatArray, val hasFlash: Boolean)

    fun lenses(): List<LensInfo> = runCatching {
        manager.cameraIdList.mapNotNull { id ->
            val c = manager.getCameraCharacteristics(id)
            val facing = c.get(CameraCharacteristics.LENS_FACING) ?: return@mapNotNull null
            val focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf()
            LensInfo(id, facing, focal, c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true)
        }
    }.getOrDefault(emptyList())

    fun camera2Fps(id: String): List<Int> = runCatching {
        val ranges = manager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES).orEmpty()
        ranges.flatMap { listOf(it.lower, it.upper) }
            .filter { it >= 24 }
            .distinct()
            .sorted()
    }.getOrDefault(emptyList())

    fun sizes(id: String): List<Size> = runCatching {
        val map = manager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return emptyList()
        map.getOutputSizes(android.graphics.ImageFormat.JPEG).toList()
    }.getOrDefault(emptyList())

    fun manualSupported(id: String): Boolean = runCatching {
        val c = manager.getCameraCharacteristics(id)
        c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
        ) == true
    }.getOrDefault(false)

    companion object {
        fun supportedFps(cameraInfo: CameraInfo, sessionConfig: androidx.camera.core.SessionConfig): List<Range<Int>> =
            runCatching { cameraInfo.getSupportedFrameRateRanges(sessionConfig).toList() }
                .getOrDefault(emptyList())
                .sortedWith(compareBy<Range<Int>> { it.upper }.thenBy { it.lower })

        fun highSpeedCapabilities(cameraInfo: CameraInfo): VideoCapabilities? =
            runCatching { Recorder.getHighSpeedVideoCapabilities(cameraInfo) }.getOrNull()
    }
}
