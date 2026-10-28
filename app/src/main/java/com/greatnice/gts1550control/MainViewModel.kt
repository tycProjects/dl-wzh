package com.greatnice.gts1550control

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.greatnice.gts1550control.bluetooth.BtController
import com.greatnice.gts1550control.media.PlaybackControls
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioUi(
    val volume: Int = 0,
    val max: Int = 15,
    val btRoute: String? = null,
    val musicActive: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val bt = BtController(app)
    val playback = PlaybackControls(app)

    val btState = bt.state
    private val _audio = MutableStateFlow(AudioUi())
    val audio: StateFlow<AudioUi> = _audio.asStateFlow()

    fun refreshAudio() {
        _audio.value = AudioUi(
            volume = playback.volume(),
            max = playback.maxVolume,
            btRoute = playback.bluetoothOutputName(),
            musicActive = playback.isMusicActive()
        )
    }

    fun setVolume(v: Int) {
        playback.setVolume(v)
        refreshAudio()
    }

    override fun onCleared() {
        bt.release()
    }
}
