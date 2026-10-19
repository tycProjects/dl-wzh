package com.qrstudio.app.ui.scan

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrstudio.app.data.ScanFormats
import com.qrstudio.app.data.ScanHistoryRepository
import com.qrstudio.app.data.ScanRecord
import com.qrstudio.app.data.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanViewModel(
    private val app: Application,
    private val repository: ScanHistoryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    data class ScanResult(val rawValue: String, val format: Int)

    private val _result = MutableStateFlow<ScanResult?>(null)
    val result: StateFlow<ScanResult?> = _result.asStateFlow()

    private val _scanningPaused = MutableStateFlow(false)
    val scanningPaused: StateFlow<Boolean> = _scanningPaused.asStateFlow()

    val history: StateFlow<List<ScanRecord>> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun onBarcode(raw: String?, format: Int) {
        if (raw.isNullOrBlank() || _scanningPaused.value) return
        _scanningPaused.value = true
        _result.value = ScanResult(raw, format)
        if (settings.vibrate) vibrate()
        if (settings.sound) beep()
        viewModelScope.launch { repository.add(raw, ScanFormats.label(format)) }
    }

    fun dismissResult() {
        _result.value = null
        _scanningPaused.value = false
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clear()
            _events.tryEmit("تم مسح سجل العمليات")
        }
    }

    fun notify(message: String) {
        _events.tryEmit(message)
    }

    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        } catch (e: Exception) {
            // تجاهل — الاهتزاز ميزة اختيارية
        }
    }

    private fun beep() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 160)
            viewModelScope.launch {
                delay(400)
                tone.release()
            }
        } catch (e: Exception) {
            // تجاهل — الصوت ميزة اختيارية
        }
    }
}
