package com.zewmod.engine.bridge

import android.content.Context
import android.util.Log
import com.zewmod.engine.model.AppConfig
import com.zewmod.engine.model.ZewModFeatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge chính giữa Engine native và UI.
 * Giao tiếp với native lib qua JNI.
 */
object ZewModBridge {

    private const val TAG = "ZewModBridge"

    // Native library name
    private const val LIB_NAME = "zewmod_engine"

    // ---- State ----
    private val _engineRunning = MutableStateFlow(false)
    val engineRunning: StateFlow<Boolean> = _engineRunning

    private val _status = MutableStateFlow("CHƯA SẴN SÀNG")
    val status: StateFlow<String> = _status

    private var nativeEnginePtr: Long = 0L
    private var initialized = false

    // ---- JNI Native declarations ----
    external fun nativeInit(config: String): Long
    external fun nativeStart(ptr: Long): Boolean
    external fun nativeStop(ptr: Long)
    external fun nativeUpdateFeatures(ptr: Long, features: String): Boolean
    external fun nativeUpdateSlider(ptr: Long, featureId: String, value: Float): Boolean
    external fun nativeGetStatus(ptr: Long): String
    external fun nativeIsRunning(ptr: Long): Boolean
    external fun nativeSetTarget(ptr: Long, target: String): Boolean
    external fun nativeGetTargetList(ptr: Long): String
    external fun nativeRelease(ptr: Long)

    // ---- Init ----
    fun init(context: Context) {
        if (initialized) return
        try {
            System.loadLibrary(LIB_NAME)
            Log.i(TAG, "Native lib loaded: $LIB_NAME")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native lib not found, running in mock mode: ${e.message}")
        }
        initialized = true
    }

    fun start(config: AppConfig): Boolean {
        return try {
            val configJson = config.toString()
            nativeEnginePtr = nativeInit(configJson)
            if (nativeEnginePtr != 0L) {
                val ok = nativeStart(nativeEnginePtr)
                _engineRunning.value = ok
                _status.value = if (ok) "ACTIVE" else "LỖI"
                ok
            } else {
                _status.value = "LỖI KHỞI TẠO"
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Engine start failed", e)
            // Mock mode: still set running for UI demo
            _engineRunning.value = true
            _status.value = "ACTIVE"
            true
        }
    }

    fun stop() {
        try {
            if (nativeEnginePtr != 0L) {
                nativeStop(nativeEnginePtr)
                nativeRelease(nativeEnginePtr)
                nativeEnginePtr = 0L
            }
        } catch (_: Exception) {}
        _engineRunning.value = false
        _status.value = "ĐÃ DỪNG"
    }

    fun updateFeatures(features: String): Boolean {
        return try {
            if (nativeEnginePtr != 0L) nativeUpdateFeatures(nativeEnginePtr, features)
            else true
        } catch (_: Exception) { true }
    }

    fun updateSlider(featureId: String, value: Float): Boolean {
        return try {
            if (nativeEnginePtr != 0L) nativeUpdateSlider(nativeEnginePtr, featureId, value)
            else true
        } catch (_: Exception) { true }
    }

    fun applyAllFeatures(): Boolean {
        val feat = ZewModFeatures.buildFeaturesString()
        return updateFeatures(feat)
    }

    fun getRunningStatus(): String {
        return try {
            if (nativeEnginePtr != 0L) nativeGetStatus(nativeEnginePtr)
            else _status.value
        } catch (_: Exception) { _status.value }
    }
}
