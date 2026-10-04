package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JnmfApp
import com.example.model.AppPreferences
import com.example.model.AppUiColor
import com.example.model.AppLanguage
import com.example.model.AudioEngineStatusInfo
import com.example.model.AudioTestStatus
import com.example.model.AudioTestType
import com.example.model.EqualizerBandState
import com.example.model.EqualizerCompatibilityReport
import com.example.model.EqualizerPresetType
import com.example.model.KickedSong
import com.example.model.Playlist
import com.example.model.PossibleDuplicateGroup
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.scanner.MusicScanner
import com.example.service.MusicService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JnmfApp
    private val repository = app.repository
    private val audioEngine = app.audioEngine

    // Audio Engine Hardware Status & Diagnostics
    val audioEngineStatus: StateFlow<AudioEngineStatusInfo> = audioEngine.engineStatus
    val equalizerCompatibilityReport: StateFlow<EqualizerCompatibilityReport> = audioEngine.eqCompatibilityReport
    private val _audioTestResults = MutableStateFlow<Map<AudioTestType, AudioTestStatus>>(emptyMap())
    val audioTestResults: StateFlow<Map<AudioTestType, AudioTestStatus>> = _audioTestResults.asStateFlow()

    // Library & Playlists
    val songs: StateFlow<List<Song>> = repository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val kickedSongs: StateFlow<List<KickedSong>> = repository.kickedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<AppPreferences> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    // Currently Selected Playlist in Playlist Screen
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    val songsInSelectedPlaylist: StateFlow<List<Song>> = _selectedPlaylist
        .flatMapLatest { pl ->
            if (pl != null) repository.getSongsForPlaylist(pl.id) else flowOf<List<Song>>(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player State delegated from AudioEngine
    val currentSong: StateFlow<Song?> = audioEngine.currentSong
    val isPlaying: StateFlow<Boolean> = audioEngine.isPlaying
    val currentPositionMs: StateFlow<Long> = audioEngine.currentPositionMs
    val durationMs: StateFlow<Long> = audioEngine.durationMs
    val repeatMode: StateFlow<RepeatMode> = audioEngine.repeatMode
    val isShuffle: StateFlow<Boolean> = audioEngine.isShuffle

    // Equalizer & Bass Boost & DSP
    val equalizerBands: StateFlow<List<EqualizerBandState>> = audioEngine.equalizerBands
    val equalizerEnabled: StateFlow<Boolean> = audioEngine.equalizerEnabled
    val currentPreset: StateFlow<EqualizerPresetType> = audioEngine.currentPreset
    val bassBoostStrength: StateFlow<Int> = audioEngine.bassBoostStrength
    val bassBoostEnabled: StateFlow<Boolean> = audioEngine.bassBoostEnabled
    val stereoWidthStrength: StateFlow<Int> = audioEngine.stereoWidthStrength
    val depth3DStrength: StateFlow<Int> = audioEngine.depth3DStrength
    val clarityStrength: StateFlow<Int> = audioEngine.clarityStrength
    val limiterEnabled: StateFlow<Boolean> = audioEngine.limiterEnabled
    val twsHeadsetMode: StateFlow<Boolean> = audioEngine.twsHeadsetMode

    // 3-Band Equalizer (Bass Shelf <= 250Hz, Mids Peaking ~1000Hz, Treble Shelf >= 4000Hz)
    val bassGainDb: StateFlow<Float> = audioEngine.bassGainDb
    val midGainDb: StateFlow<Float> = audioEngine.midGainDb
    val trebleGainDb: StateFlow<Float> = audioEngine.trebleGainDb

    // Duplicate Detection state
    private val _possibleDuplicates = MutableStateFlow<List<PossibleDuplicateGroup>>(emptyList())
    val possibleDuplicates: StateFlow<List<PossibleDuplicateGroup>> = _possibleDuplicates.asStateFlow()

    private val _isDuplicateScanning = MutableStateFlow(false)
    val isDuplicateScanning: StateFlow<Boolean> = _isDuplicateScanning.asStateFlow()

    private val _duplicateScanCompleted = MutableStateFlow(false)
    val duplicateScanCompleted: StateFlow<Boolean> = _duplicateScanCompleted.asStateFlow()

    // UI state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _nowPlayingOpen = MutableStateFlow(false)
    val nowPlayingOpen: StateFlow<Boolean> = _nowPlayingOpen.asStateFlow()

    private val _songForAddToPlaylist = MutableStateFlow<Song?>(null)
    val songForAddToPlaylist: StateFlow<Song?> = _songForAddToPlaylist.asStateFlow()

    private val _songToKick = MutableStateFlow<Song?>(null)
    val songToKick: StateFlow<Song?> = _songToKick.asStateFlow()

    init {
        // Initial duplicate analysis after loading
        viewModelScope.launch {
            scanDuplicates()
        }
    }

    fun playSong(song: Song) {
        val currentList = songs.value
        val idx = currentList.indexOfFirst { it.id == song.id }
        if (idx != -1) {
            audioEngine.playQueue(currentList, idx)
        } else {
            audioEngine.playSong(song)
        }
        MusicService.start(getApplication())
    }

    fun previewSong(song: Song) {
        audioEngine.playSong(song)
        MusicService.start(getApplication())
    }

    fun playQueue(queue: List<Song>, startIndex: Int = 0) {
        audioEngine.playQueue(queue, startIndex)
        MusicService.start(getApplication())
    }

    fun togglePlayPause() {
        audioEngine.togglePlayPause()
        MusicService.start(getApplication())
    }

    fun playNext() {
        audioEngine.playNext()
    }

    fun playPrevious() {
        audioEngine.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        audioEngine.toggleShuffle()
    }

    fun cycleRepeatMode() {
        audioEngine.cycleRepeatMode()
    }

    // --- Audio DSP Controls ---

    fun setEqualizerEnabled(enabled: Boolean) {
        audioEngine.setEqualizerEnabled(enabled)
        persistPreferences()
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        audioEngine.setBassBoostEnabled(enabled)
        persistPreferences()
    }

    fun setBassBoostStrength(strength: Int) {
        audioEngine.setBassBoostStrength(strength)
        persistPreferences()
    }

    fun setStereoWidthStrength(strength: Int) {
        audioEngine.setStereoWidthStrength(strength)
        persistPreferences()
    }

    fun setDepth3DStrength(strength: Int) {
        audioEngine.setDepth3DStrength(strength)
        persistPreferences()
    }

    fun setClarityStrength(strength: Int) {
        audioEngine.setClarityStrength(strength)
        persistPreferences()
    }

    fun setLimiterEnabled(enabled: Boolean) {
        audioEngine.setLimiterEnabled(enabled)
        persistPreferences()
    }

    fun setTwsHeadsetMode(enabled: Boolean) {
        audioEngine.setTwsHeadsetMode(enabled)
        persistPreferences()
    }

    fun refreshAudioHardwareStatus() {
        audioEngine.detectAudioHardwareCapabilities()
    }

    fun runStrictEqualizerCompatibilityCheck() {
        audioEngine.runStrictEqualizerCompatibilityCheck()
    }

    fun runAudioTest(type: AudioTestType) {
        val result = audioEngine.testAudioEffect(type)
        val current = _audioTestResults.value.toMutableMap()
        current[type] = result
        _audioTestResults.value = current
    }

    fun runAllAudioTests() {
        val results = mutableMapOf<AudioTestType, AudioTestStatus>()
        AudioTestType.entries.forEach { type ->
            results[type] = audioEngine.testAudioEffect(type)
        }
        _audioTestResults.value = results
    }

    fun setBandLevel(bandIndex: Short, levelMilliBels: Short) {
        audioEngine.setBandLevel(bandIndex, levelMilliBels)
    }

    fun setBassGain(gainDb: Float) {
        audioEngine.setBassGain(gainDb)
    }

    fun setMidGain(gainDb: Float) {
        audioEngine.setMidGain(gainDb)
    }

    fun setTrebleGain(gainDb: Float) {
        audioEngine.setTrebleGain(gainDb)
    }

    fun setPreset(preset: EqualizerPresetType) {
        audioEngine.setPreset(preset)
        persistPreferences()
    }

    fun setUiColor(uiColor: AppUiColor): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            val updated = preferences.value.copy(uiColor = uiColor)
            repository.savePreferences(updated)
        }
    }

    fun setLanguage(language: AppLanguage): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            val updated = preferences.value.copy(language = language)
            repository.savePreferences(updated)
        }
    }

    fun updatePreferences(newPrefs: AppPreferences) {
        viewModelScope.launch {
            repository.savePreferences(newPrefs)
        }
    }

    private fun persistPreferences() {
        viewModelScope.launch {
            val current = preferences.value
            val updated = current.copy(
                bassBoostStrength = audioEngine.bassBoostStrength.value,
                stereoWidthStrength = audioEngine.stereoWidthStrength.value,
                depth3DStrength = audioEngine.depth3DStrength.value,
                clarityStrength = audioEngine.clarityStrength.value,
                limiterEnabled = audioEngine.limiterEnabled.value,
                twsHeadsetMode = audioEngine.twsHeadsetMode.value,
                eqEnabled = audioEngine.equalizerEnabled.value,
                eqPreset = audioEngine.currentPreset.value,
                pauseOnUnplug = current.pauseOnUnplug,
                uiColor = current.uiColor,
                language = current.language
            )
            repository.savePreferences(updated)
        }
    }

    // --- Kicked Songs (Hidden from JNMF Only) ---

    fun promptKickSong(song: Song?) {
        _songToKick.value = song
    }

    fun confirmKickSong(song: Song, notes: String? = null) {
        viewModelScope.launch {
            repository.kickSong(song, notes)
            // If the kicked song is currently playing, skip to next
            if (currentSong.value?.id == song.id) {
                audioEngine.playNext()
            }
            _songToKick.value = null
            // Re-run duplicates in background
            scanDuplicates()
        }
    }

    fun restoreKickedSong(kickedId: Long) {
        viewModelScope.launch {
            repository.restoreKickedSong(kickedId)
            scanDuplicates()
        }
    }

    fun updateKickedSong(kickedSong: KickedSong) {
        viewModelScope.launch {
            repository.updateKickedSong(kickedSong)
        }
    }

    // --- Duplicate Detection & Manual Delete ---

    fun scanDuplicates() {
        viewModelScope.launch {
            _isDuplicateScanning.value = true
            _duplicateScanCompleted.value = false
            try {
                val results = repository.findDuplicates()
                _possibleDuplicates.value = results
            } finally {
                _isDuplicateScanning.value = false
                _duplicateScanCompleted.value = true
            }
        }
    }

    /**
     * PERMANENT DELETION: Only invoked from Possible Duplicates screen upon explicit user confirmation!
     */
    fun deleteDuplicateFilePermanently(song: Song) {
        viewModelScope.launch {
            if (currentSong.value?.id == song.id) {
                audioEngine.pause()
            }
            repository.deleteDuplicateFilePermanently(song)
            // Remove any duplicate groups involving this song
            _possibleDuplicates.value = _possibleDuplicates.value.filter { group ->
                group.songA.id != song.id && group.songB.id != song.id
            }
        }
    }

    // --- Media Scanning & File Picker ---

    fun scanDeviceAudio() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanDeviceAndSync()
                scanDuplicates()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun importAudioUri(uri: Uri) {
        viewModelScope.launch {
            val song = MusicScanner.parseSongFromUri(getApplication(), uri)
            if (song != null) {
                repository.insertSong(song)
                scanDuplicates()
            }
        }
    }

    fun reloadDemoTracks() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.checkAndSeedDemoTracks(force = true)
                scanDuplicates()
            } finally {
                _isScanning.value = false
            }
        }
    }

    // --- Playlists ---

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, newName)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            if (_selectedPlaylist.value?.id == id) {
                _selectedPlaylist.value = null
            }
            repository.deletePlaylist(id)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun reorderPlaylist(playlistId: Long, songIdsInOrder: List<Long>) {
        viewModelScope.launch {
            repository.reorderPlaylist(playlistId, songIdsInOrder)
        }
    }

    fun promptAddToPlaylist(song: Song) {
        _songForAddToPlaylist.value = song
    }

    fun dismissAddToPlaylist() {
        _songForAddToPlaylist.value = null
    }

    // --- UI Navigation ---

    fun openNowPlaying() {
        _nowPlayingOpen.value = true
    }

    fun closeNowPlaying() {
        _nowPlayingOpen.value = false
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
