package com.example.playback

import android.content.Context
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.DjModeSettings
import com.example.data.repository.EqSavedState
import com.example.data.repository.SavedMusicSession
import com.example.data.repository.SettingsRepository
import com.example.ui.i18n.JnmfStrings
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatState {
    OFF, ALL, ONE
}

enum class QueueSourceType {
    CUSTOM,
    LIBRARY,
    FAVORITES,
    PLAYLIST;

    companion object {
        fun fromNameOrDefault(name: String?): QueueSourceType {
            return entries.find { it.name == name } ?: CUSTOM
        }
    }
}

enum class PlaybackErrorCode {
    AUDIO_MISSING,
    AUDIO_UNSUPPORTED,
    CANNOT_PLAY_TRACK,
    GENERIC
}

data class CrossfadeOverlapSample(
    val progressFraction: Float,
    val outgoingVolume: Float,
    val incomingVolume: Float,
    val outgoingSongId: Long,
    val incomingSongId: Long,
    val bothDecksActive: Boolean
)

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val isShuffle: Boolean = false,
    val repeatState: RepeatState = RepeatState.ALL,
    val isReverseQueue: Boolean = false,
    val djModeEnabled: Boolean = false,
    val djTransitionSeconds: Int = DjModeSettings.DEFAULT_TRANSITION_SECONDS,
    val isDjTransitionActive: Boolean = false,
    val djOutgoingSong: Song? = null,
    val djOutgoingVolume: Float = 0f,
    val djIncomingVolume: Float = 1f,
    val errorCode: PlaybackErrorCode? = null,
    val errorMessage: String? = null
)

object MusicController {
    private const val TAG = "JNMF_MusicController"

    // Redmi 10C (Snapdragon 680, 4 GB RAM) compact local-audio buffer bounds
    private const val DECK_MIN_BUFFER_MS = 5_000
    private const val DECK_MAX_BUFFER_MS = 15_000
    private const val DECK_BUFFER_FOR_PLAYBACK_MS = 250
    private const val DECK_BUFFER_AFTER_REBUFFER_MS = 500

    // DJ Crossfade timing thresholds
    private const val MIN_TRACK_DURATION_FOR_CROSSFADE_MS = 6_000L
    private const val PRELOAD_LEAD_TIME_MS = 4_000L
    private const val MAX_INCOMING_READY_WAIT_MS = 1_500L
    private const val AUDIO_FOCUS_DUCK_MULTIPLIER = 0.25f

    private data class NextTrackCandidate(
        val song: Song,
        val index: Int
    )

    private data class PreloadedDeckCandidate(
        val deck: ExoPlayer,
        val song: Song,
        val queueIndex: Int,
        val forTrackToken: Long
    )

    // Dual-deck player slots (strictly <= 2 decks at all times)
    @Volatile
    private var deckA: ExoPlayer? = null
    @Volatile
    private var deckB: ExoPlayer? = null
    private var deckAListener: Player.Listener? = null
    private var deckBListener: Player.Listener? = null
    private var deckBCreatedInternally: Boolean = false
    private var secondaryDeckFactory: (() -> ExoPlayer)? = null
    private var secondaryDeckDisposer: ((ExoPlayer) -> Unit)? = null

    @Volatile
    private var activePlayer: ExoPlayer? = null
    @Volatile
    private var outgoingCrossfadePlayer: ExoPlayer? = null

    private val _activeExoPlayerFlow = MutableStateFlow<ExoPlayer?>(null)
    val activeExoPlayerFlow: StateFlow<ExoPlayer?> = _activeExoPlayerFlow.asStateFlow()

    val equalizerManager = EqualizerManager()

    @Volatile
    private var appContext: Context? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var djTransitionJob: Job? = null

    @Volatile
    private var isDjTransitioning: Boolean = false
    private var currentTrackInstanceToken: Long = 0L
    private var lastDjTriggeredInstanceToken: Long = -1L
    private var preloadedCandidate: PreloadedDeckCandidate? = null
    private val failedPreloadSongIdsForToken = mutableSetOf<Long>()

    // Independent deck base gains (0.0..1.0) multiplied by audioFocusGainMultiplier
    @Volatile
    private var currentActiveBaseGain: Float = 1.0f
    @Volatile
    private var currentOutgoingBaseGain: Float = 0.0f
    @Volatile
    private var audioFocusGainMultiplier: Float = 1.0f
    @Volatile
    private var hasAudioFocus: Boolean = false
    @Volatile
    private var resumeOnAudioFocusGain: Boolean = false
    private var audioFocusRequestOreo: AudioFocusRequest? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (Looper.myLooper() == Looper.getMainLooper()) {
            handleAudioFocusChange(focusChange)
        } else {
            mainHandler.post { handleAudioFocusChange(focusChange) }
        }
    }

    internal var djCrossfadeStepDelayMs: Long = 50L
    internal val recordedCrossfadeVolumes = CopyOnWriteArrayList<Float>()
    internal val recordedOverlapSamples = CopyOnWriteArrayList<CrossfadeOverlapSample>()

    private var originalQueue: List<Song> = emptyList()

    var activeQueueSourceType: QueueSourceType = QueueSourceType.CUSTOM
        private set
    var activeQueuePlaylistId: Long = -1L
        private set

    // Smart Shuffle state: non-repeating cycle history + randomized remaining pool + back-navigation history
    private const val MAX_SHUFFLE_BACK_HISTORY = 500
    private val shuffleCyclePlayedIds = LinkedHashSet<Long>()
    private val shuffleRemainingIds = ArrayDeque<Long>()
    private val shuffleHistoryBackStack = ArrayDeque<Long>()
    private var pendingNextCycleOrder: List<Long>? = null
    private var shuffleCompletedCyclesCount: Int = 0
    private var shuffleActivationEventCount: Int = 0

    /**
     * Builds a memory-efficient Media3 ExoPlayer configured for dual-deck music playback
     * on budget hardware (Redmi 10C / Snapdragon 680 / 4 GB RAM).
     * Audio focus is coordinated centrally by [MusicController] so Deck B never steals focus from Deck A.
     */
    @OptIn(UnstableApi::class)
    fun buildMusicExoPlayer(context: Context): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DECK_MIN_BUFFER_MS,
                DECK_MAX_BUFFER_MS,
                DECK_BUFFER_FOR_PLAYBACK_MS,
                DECK_BUFFER_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val musicAudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(musicAudioAttributes, /* handleAudioFocus = */ false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }

    /**
     * Binds application context once so playback modes, equalizer settings, audio focus, and
     * paused session snapshots persist reliably across screen closures and restarts.
     */
    fun bindAppContext(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        equalizerManager.onSettingsChanged = { eqState ->
            val bandsCsv = eqState.bands.joinToString(",") { it.currentLevelMilliBel.toString() }
            SettingsRepository.saveEqSettingsSync(
                ctx,
                EqSavedState(
                    enabled = eqState.isEnabled,
                    bass = eqState.bassStrength,
                    virtualizer = eqState.virtualizerStrength,
                    preset = eqState.currentPreset,
                    clarity = eqState.clarityStrength,
                    customBandsCsv = bandsCsv
                )
            )
        }
    }

    private fun requestMusicAudioFocus(): Boolean {
        val ctx = appContext ?: return true
        if (hasAudioFocus) return true
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        val result = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequestOreo ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        PlatformAudioAttributes.Builder()
                            .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                            .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(audioFocusListener, mainHandler)
                    .build()
                    .also { audioFocusRequestOreo = it }
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioFocus request warning: ${e.message}")
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        if (hasAudioFocus) {
            audioFocusGainMultiplier = 1.0f
            applyCurrentDeckVolumes()
        }
        return hasAudioFocus
    }

    private fun abandonMusicAudioFocus() {
        resumeOnAudioFocusGain = false
        audioFocusGainMultiplier = 1.0f
        if (!hasAudioFocus) return
        hasAudioFocus = false
        val ctx = appContext ?: return
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequestOreo?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioFocus abandon warning: ${e.message}")
        }
    }

    internal fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                resumeOnAudioFocusGain = false
                audioFocusGainMultiplier = 1.0f
                if (_state.value.isPlaying) {
                    pauseInternal(abandonFocus = true)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val wasPlaying = _state.value.isPlaying
                hasAudioFocus = false
                audioFocusGainMultiplier = 1.0f
                if (wasPlaying) {
                    pauseInternal(abandonFocus = false)
                    resumeOnAudioFocusGain = true
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                audioFocusGainMultiplier = AUDIO_FOCUS_DUCK_MULTIPLIER
                applyCurrentDeckVolumes()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                audioFocusGainMultiplier = 1.0f
                applyCurrentDeckVolumes()
                if (resumeOnAudioFocusGain) {
                    resumeOnAudioFocusGain = false
                    resume()
                }
            }
        }
    }

    /**
     * Restores persisted shuffle, repeat, and reverse-queue preferences without triggering playback.
     */
    fun restorePlaybackModes(
        isShuffle: Boolean,
        repeatState: RepeatState,
        isReverseQueue: Boolean
    ) {
        val previousShuffle = _state.value.isShuffle
        val previousReverse = _state.value.isReverseQueue
        _state.value = _state.value.copy(
            isShuffle = isShuffle,
            repeatState = repeatState,
            isReverseQueue = isReverseQueue
        )
        if (!isDjTransitioning) {
            clearStandbyPreload()
        }
        if (previousReverse != isReverseQueue && originalQueue.isNotEmpty()) {
            val currentSong = _state.value.currentSong
            val updatedQueue = buildEffectiveQueue()
            val newIndex = if (currentSong != null) {
                updatedQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
            } else {
                0
            }
            _state.value = _state.value.copy(
                queue = updatedQueue,
                queueIndex = newIndex
            )
        }
        if (isShuffle && !previousShuffle) {
            val curr = _state.value.currentSong
            if (curr != null) {
                initSmartShuffleCycleForCurrentSong(
                    queue = _state.value.queue,
                    currentSongId = curr.id,
                    clearBackHistory = true
                )
            }
        } else if (!isShuffle && previousShuffle) {
            resetSmartShuffleState()
        }
    }

    /**
     * Updates DJ Mode ON/OFF and transition duration (3, 5, 10, or 20 seconds) immediately
     * without restarting active playback. Releases idle secondary deck resources when turned OFF.
     */
    fun setDjModeSettings(
        enabled: Boolean,
        transitionSeconds: Int = _state.value.djTransitionSeconds,
        persist: Boolean = true
    ) {
        val normalizedSeconds = DjModeSettings.normalizeSeconds(transitionSeconds)
        val previousSeconds = _state.value.djTransitionSeconds
        if (!enabled) {
            cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
            lastDjTriggeredInstanceToken = -1L
            releaseIdleSecondaryDeckIfPossible()
        } else if (normalizedSeconds != previousSeconds && !isDjTransitioning) {
            lastDjTriggeredInstanceToken = -1L
        }
        _state.value = _state.value.copy(
            djModeEnabled = enabled,
            djTransitionSeconds = normalizedSeconds,
            isDjTransitionActive = if (enabled) _state.value.isDjTransitionActive else false,
            djOutgoingSong = if (enabled) _state.value.djOutgoingSong else null,
            djOutgoingVolume = if (enabled) _state.value.djOutgoingVolume else 0f,
            djIncomingVolume = if (enabled) _state.value.djIncomingVolume else 1f
        )
        if (persist) {
            appContext?.let { ctx ->
                SettingsRepository.saveDjModeSettingsSync(
                    context = ctx,
                    enabled = enabled,
                    transitionSeconds = normalizedSeconds
                )
            }
        }
    }

    private fun setDeckRawVolume(deck: ExoPlayer?, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        try {
            deck?.volume = clamped
        } catch (e: Exception) {
            Log.w(TAG, "Could not set ExoPlayer deck volume: ${e.message}")
        }
    }

    private fun applyCurrentDeckVolumes() {
        val focusMult = audioFocusGainMultiplier.coerceIn(0f, 1f)
        activePlayer?.let { active ->
            setDeckRawVolume(active, currentActiveBaseGain * focusMult)
        }
        outgoingCrossfadePlayer?.let { outgoing ->
            if (outgoing !== activePlayer) {
                val outVol = if (outgoing.mediaItemCount > 0) {
                    currentOutgoingBaseGain * focusMult
                } else {
                    0f
                }
                setDeckRawVolume(outgoing, outVol)
            }
        }
    }

    private fun stopAndClearDeck(deck: ExoPlayer?) {
        if (deck == null) return
        try {
            deck.volume = 0f
            deck.playWhenReady = false
            deck.stop()
            deck.clearMediaItems()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping/clearing standby deck: ${e.message}")
        }
    }

    private fun clearStandbyPreload() {
        val preloaded = preloadedCandidate
        preloadedCandidate = null
        if (preloaded != null && preloaded.deck !== activePlayer && preloaded.deck !== outgoingCrossfadePlayer) {
            stopAndClearDeck(preloaded.deck)
        }
    }

    private fun cancelActiveDjTransitionAndPreload(restoreActiveVolume: Boolean) {
        djTransitionJob?.cancel()
        djTransitionJob = null
        isDjTransitioning = false
        preloadedCandidate = null
        failedPreloadSongIdsForToken.clear()

        val active = activePlayer
        val other = if (active === deckA) deckB else deckA
        outgoingCrossfadePlayer = null
        if (other != null && other !== active) {
            stopAndClearDeck(other)
        }

        if (restoreActiveVolume) {
            currentActiveBaseGain = 1.0f
            currentOutgoingBaseGain = 0.0f
            applyCurrentDeckVolumes()
            try {
                val sid = active?.audioSessionId ?: 0
                if (sid > 0) {
                    equalizerManager.attachAudioSession(sid)
                }
            } catch (_: Exception) {
            }
        }

        if (_state.value.isDjTransitionActive || _state.value.djOutgoingSong != null) {
            _state.value = _state.value.copy(
                isDjTransitionActive = false,
                djOutgoingSong = null,
                djOutgoingVolume = 0f,
                djIncomingVolume = 1f
            )
        }
    }

    private fun releaseIdleSecondaryDeckIfPossible() {
        val secondary = deckB ?: return
        if (activePlayer === secondary || outgoingCrossfadePlayer === secondary) {
            return
        }
        deckBListener?.let { listener ->
            try {
                secondary.removeListener(listener)
            } catch (_: Exception) {
            }
        }
        deckBListener = null
        stopAndClearDeck(secondary)
        if (deckBCreatedInternally) {
            try {
                secondary.release()
            } catch (_: Exception) {
            }
        } else {
            secondaryDeckDisposer?.invoke(secondary)
        }
        deckBCreatedInternally = false
        deckB = null
    }

    private fun peekStandbyDeck(): ExoPlayer? {
        val active = activePlayer ?: return null
        return if (active === deckA) deckB else deckA
    }

    private fun getOrCreateStandbyDeck(): ExoPlayer? {
        val active = activePlayer ?: return null
        if (active === deckB) {
            return deckA
        }
        // active === deckA -> ensure deckB exists
        deckB?.let { return it }

        val created: ExoPlayer = when {
            secondaryDeckFactory != null -> {
                deckBCreatedInternally = false
                secondaryDeckFactory!!.invoke()
            }
            appContext != null -> {
                deckBCreatedInternally = true
                buildMusicExoPlayer(appContext!!)
            }
            else -> return null
        }

        if (created === deckA) {
            return null
        }
        val listener = createDeckListener(created)
        deckBListener = listener
        created.addListener(listener)
        setDeckRawVolume(created, 0f)
        deckB = created
        return created
    }

    /**
     * Restores the last active track, queue, playback position, and Smart Shuffle state in PAUSED
     * state after app closure, process death, or device restart. Never auto-starts playback.
     */
    fun restorePausedSession(
        song: Song,
        queue: List<Song>,
        positionMs: Long,
        savedShufflePlayedIds: List<Long> = emptyList(),
        savedShuffleRemainingIds: List<Long> = emptyList(),
        savedShuffleHistoryIds: List<Long> = emptyList(),
        sourceType: QueueSourceType = QueueSourceType.CUSTOM,
        sourcePlaylistId: Long = -1L
    ) {
        if (_state.value.currentSong != null) {
            return
        }
        activeQueueSourceType = sourceType
        activeQueuePlaylistId = sourcePlaylistId
        originalQueue = if (queue.isNotEmpty()) queue else listOf(song)
        val effectiveQueue = buildEffectiveQueue()
        val index = effectiveQueue.indexOfFirst { it.id == song.id }.let {
            if (it >= 0) it else 0
        }
        val clampedPos = if (song.durationMs > 0L) {
            positionMs.coerceIn(0L, song.durationMs)
        } else {
            positionMs.coerceAtLeast(0L)
        }

        if (_state.value.isShuffle) {
            val validIds = distinctQueueSongIds(effectiveQueue)
            val validIdSet = validIds.toHashSet()
            shuffleCyclePlayedIds.clear()
            shuffleRemainingIds.clear()
            shuffleHistoryBackStack.clear()
            pendingNextCycleOrder = null
            shuffleCompletedCyclesCount = 0

            if (savedShufflePlayedIds.isNotEmpty() || savedShuffleRemainingIds.isNotEmpty()) {
                for (id in savedShufflePlayedIds) {
                    if (id in validIdSet) {
                        shuffleCyclePlayedIds.add(id)
                    }
                }
                shuffleCyclePlayedIds.add(song.id)

                val remainingSeen = HashSet<Long>()
                for (id in savedShuffleRemainingIds) {
                    if (id in validIdSet && id !in shuffleCyclePlayedIds && remainingSeen.add(id)) {
                        shuffleRemainingIds.addLast(id)
                    }
                }
                val newUnaccounted = validIds.filter {
                    it !in shuffleCyclePlayedIds && it !in remainingSeen
                }.shuffled()
                for (newId in newUnaccounted) {
                    shuffleRemainingIds.addLast(newId)
                }

                for (histId in savedShuffleHistoryIds.takeLast(MAX_SHUFFLE_BACK_HISTORY)) {
                    if (histId in validIdSet && shuffleHistoryBackStack.lastOrNull() != histId) {
                        shuffleHistoryBackStack.addLast(histId)
                    }
                }
            } else {
                initSmartShuffleCycleForCurrentSong(
                    queue = effectiveQueue,
                    currentSongId = song.id,
                    clearBackHistory = true
                )
            }
        }

        _state.value = _state.value.copy(
            currentSong = song,
            queue = effectiveQueue,
            queueIndex = index,
            currentPositionMs = clampedPos,
            durationMs = song.durationMs,
            isPlaying = false,
            errorCode = null,
            errorMessage = null
        )

        val player = activePlayer
        if (player != null && player.mediaItemCount == 0) {
            try {
                player.setMediaItem(buildMediaItem(song), clampedPos)
                player.prepare()
                player.playWhenReady = false
            } catch (e: Exception) {
                Log.w(TAG, "Could not cue restored session on ExoPlayer: ${e.message}")
            }
        }
    }

    /**
     * Synchronizes updated library metadata (such as manual remix classifications) in-place
     * without interrupting active playback, resetting position, or recreating ExoPlayer.
     */
    fun syncLibraryMetadata(updatedSongs: List<Song>) {
        if (updatedSongs.isEmpty() || _state.value.currentSong == null) return
        val byId = updatedSongs.associateBy { it.id }
        if (originalQueue.isNotEmpty()) {
            originalQueue = originalQueue.map { existing -> byId[existing.id] ?: existing }
        }
        val currentQueue = _state.value.queue
        val syncedQueue = if (currentQueue.isNotEmpty()) {
            currentQueue.map { existing -> byId[existing.id] ?: existing }
        } else {
            currentQueue
        }
        val curr = _state.value.currentSong
        val syncedCurrent = if (curr != null) byId[curr.id] ?: curr else null
        if (syncedCurrent != curr || syncedQueue != currentQueue) {
            _state.value = _state.value.copy(
                currentSong = syncedCurrent,
                queue = syncedQueue
            )
        }
    }

    /**
     * Updates the active playback queue when songs are added, removed, hidden, or reordered
     * during playback. Preserves the currently playing track without interruption and updates
     * the remaining Smart Shuffle pool without repeating already-played songs in the current cycle.
     */
    fun updateActiveQueueSongs(updatedSongs: List<Song>) {
        val currentSong = _state.value.currentSong ?: return
        if (updatedSongs.isEmpty()) {
            // Keep current song playable as a singleton queue until playback ends or changes
            originalQueue = listOf(currentSong)
            val singleQueue = listOf(currentSong)
            if (_state.value.isShuffle) {
                reconcileSmartShuffleWithQueue(singleQueue, currentSong.id)
            }
            if (!isDjTransitioning) {
                clearStandbyPreload()
            }
            _state.value = _state.value.copy(
                queue = singleQueue,
                queueIndex = 0
            )
            savePlaybackSnapshotAsync()
            return
        }

        // Fast-path check if queue IDs & order are unchanged
        val sameIdsAndOrder = originalQueue.size == updatedSongs.size &&
            originalQueue.indices.all { idx -> originalQueue[idx].id == updatedSongs[idx].id }
        if (sameIdsAndOrder) {
            syncLibraryMetadata(updatedSongs)
            return
        }

        originalQueue = updatedSongs
        val effectiveQueue = buildEffectiveQueue()
        val updatedCurrentSong = effectiveQueue.find { it.id == currentSong.id } ?: currentSong
        val resolvedIndex = effectiveQueue.indexOfFirst { it.id == currentSong.id }.let { idx ->
            if (idx >= 0) idx else _state.value.queueIndex.coerceIn(0, effectiveQueue.lastIndex)
        }

        if (_state.value.isShuffle) {
            reconcileSmartShuffleWithQueue(effectiveQueue, currentSong.id)
        }

        if (!isDjTransitioning) {
            val preload = preloadedCandidate
            if (preload != null) {
                val validIds = effectiveQueue.mapTo(HashSet(effectiveQueue.size)) { it.id }
                val preloadStillExpected = preload.song.id in validIds && (
                    !_state.value.isShuffle ||
                        preload.song.id == shuffleRemainingIds.firstOrNull()
                    )
                if (!preloadStillExpected) {
                    clearStandbyPreload()
                } else {
                    val newPreloadIdx = effectiveQueue.indexOfFirst { it.id == preload.song.id }
                    if (newPreloadIdx >= 0 && newPreloadIdx != preload.queueIndex) {
                        preloadedCandidate = preload.copy(queueIndex = newPreloadIdx)
                    }
                }
            }
        }

        _state.value = _state.value.copy(
            currentSong = updatedCurrentSong,
            queue = effectiveQueue,
            queueIndex = resolvedIndex
        )
        savePlaybackSnapshotAsync()
    }

    /**
     * Appends a song to the active queue and includes it in the remaining Smart Shuffle cycle
     * (unless it was already played in the current cycle) without interrupting playback.
     */
    fun addSongToQueue(song: Song) {
        val base = if (originalQueue.isNotEmpty()) originalQueue else _state.value.queue
        if (base.any { it.id == song.id }) return
        if (_state.value.currentSong == null) return
        updateActiveQueueSongs(base + song)
    }

    /**
     * Removes a song by ID from the active queue and prunes it from Smart Shuffle remaining/history
     * pools without interrupting active playback.
     */
    fun removeSongFromQueue(songId: Long) {
        val base = if (originalQueue.isNotEmpty()) originalQueue else _state.value.queue
        if (base.none { it.id == songId }) return
        if (_state.value.currentSong == null) return
        updateActiveQueueSongs(base.filterNot { it.id == songId })
    }

    /**
     * Persists the current track, queue IDs, playback position, and modes immediately.
     */
    private const val MAX_PERSISTED_QUEUE_WINDOW = 2_000
    private var cachedPersistedQueueSource: List<Song>? = null
    private var cachedPersistedQueueSongId: Long = -1L
    private var cachedPersistedQueueCsv: String = ""

    private fun resolveBoundedQueueCsv(sourceQueue: List<Song>, songId: Long): String {
        if (sourceQueue === cachedPersistedQueueSource &&
            songId == cachedPersistedQueueSongId &&
            cachedPersistedQueueCsv.isNotEmpty()
        ) {
            return cachedPersistedQueueCsv
        }
        val csv = if (sourceQueue.isEmpty()) {
            songId.toString()
        } else if (sourceQueue.size <= MAX_PERSISTED_QUEUE_WINDOW) {
            sourceQueue.joinToString(",") { it.id.toString() }
        } else {
            val activeIdx = sourceQueue.indexOfFirst { it.id == songId }.coerceAtLeast(0)
            val half = MAX_PERSISTED_QUEUE_WINDOW / 2
            val start = (activeIdx - half).coerceAtLeast(0)
            val end = (start + MAX_PERSISTED_QUEUE_WINDOW).coerceAtMost(sourceQueue.size)
            sourceQueue.subList(start, end).joinToString(",") { it.id.toString() }
        }
        cachedPersistedQueueSource = sourceQueue
        cachedPersistedQueueSongId = songId
        cachedPersistedQueueCsv = csv
        return csv
    }

    private fun persistPlaybackSnapshotInternal(commitSynchronously: Boolean) {
        val ctx = appContext ?: return
        val currentState = _state.value
        val song = currentState.currentSong
        if (song == null) {
            SettingsRepository.saveMusicModesSync(
                context = ctx,
                isShuffle = currentState.isShuffle,
                repeatState = currentState.repeatState,
                isReverseQueue = currentState.isReverseQueue,
                commitSynchronously = commitSynchronously
            )
            return
        }

        var latestPos = currentState.currentPositionMs
        try {
            activePlayer?.let { player ->
                if (player.mediaItemCount > 0) {
                    val pPos = player.currentPosition.coerceAtLeast(0L)
                    if (pPos > 0L || player.playbackState == Player.STATE_READY) {
                        latestPos = pPos
                    }
                }
            }
        } catch (_: Exception) {
        }

        val sourceQueue = if (originalQueue.isNotEmpty()) originalQueue else currentState.queue
        val queueCsv = resolveBoundedQueueCsv(sourceQueue, song.id)

        SettingsRepository.saveMusicModesAndSessionSnapshot(
            context = ctx,
            isShuffle = currentState.isShuffle,
            repeatState = currentState.repeatState,
            isReverseQueue = currentState.isReverseQueue,
            session = SavedMusicSession(
                songId = song.id,
                positionMs = latestPos,
                durationMs = currentState.durationMs.coerceAtLeast(song.durationMs),
                title = song.title,
                artist = song.artist,
                album = song.album,
                contentUri = song.contentUri.toString(),
                albumArtUri = song.albumArtUri?.toString(),
                remixCategoryName = song.remixCategory.name,
                fileName = song.fileName,
                queueSongIds = emptyList(),
                shufflePlayedIds = if (currentState.isShuffle) shuffleCyclePlayedIds.toList() else emptyList(),
                shuffleRemainingIds = if (currentState.isShuffle) shuffleRemainingIds.toList() else emptyList(),
                shuffleHistoryIds = if (currentState.isShuffle) shuffleHistoryBackStack.toList() else emptyList(),
                queueSourceType = activeQueueSourceType.name,
                queueSourcePlaylistId = activeQueuePlaylistId
            ),
            commitSynchronously = commitSynchronously,
            precomputedQueueCsv = queueCsv
        )
    }

    /**
     * Persists the current track, queue IDs, playback position, and modes immediately to disk.
     */
    fun flushPlaybackSnapshotNow() {
        persistPlaybackSnapshotInternal(commitSynchronously = true)
    }

    /**
     * Updates the in-memory session cache immediately and flushes to disk asynchronously
     * without blocking the UI thread during Play/Pause, Next/Previous, Seek, or progress ticks.
     */
    private fun savePlaybackSnapshotAsync() {
        persistPlaybackSnapshotInternal(commitSynchronously = false)
    }

    private fun persistModesIfBound() {
        val ctx = appContext ?: return
        val s = _state.value
        SettingsRepository.saveMusicModesSync(
            context = ctx,
            isShuffle = s.isShuffle,
            repeatState = s.repeatState,
            isReverseQueue = s.isReverseQueue,
            commitSynchronously = false
        )
    }

    internal fun activeDeckForTesting(): ExoPlayer? = activePlayer
    internal fun outgoingDeckForTesting(): ExoPlayer? = outgoingCrossfadePlayer
    internal fun standbyDeckForTesting(): ExoPlayer? = peekStandbyDeck()
    internal fun totalAllocatedDecksForTesting(): Int = listOfNotNull(deckA, deckB).distinct().size
    internal fun simultaneouslyActiveDeckCountForTesting(): Int {
        return listOfNotNull(deckA, deckB)
            .distinct()
            .count { p -> p.mediaItemCount > 0 && (p.isPlaying || p.playWhenReady) }
    }
    internal fun smartShuffleCyclePlayedIdsForTesting(): List<Long> = shuffleCyclePlayedIds.toList()
    internal fun smartShuffleRemainingIdsForTesting(): List<Long> = shuffleRemainingIds.toList()
    internal fun smartShuffleBackHistoryIdsForTesting(): List<Long> = shuffleHistoryBackStack.toList()
    internal fun smartShuffleCompletedCyclesForTesting(): Int = shuffleCompletedCyclesCount
    internal fun smartShuffleActivationCountForTesting(): Int = shuffleActivationEventCount

    /**
     * Resets in-memory singleton state for deterministic cold-restart / process-death testing.
     */
    internal fun resetInMemoryStateForTesting() {
        cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
        abandonMusicAudioFocus()
        detachPlayer()
        originalQueue = emptyList()
        activeQueueSourceType = QueueSourceType.CUSTOM
        activeQueuePlaylistId = -1L
        resetSmartShuffleState()
        shuffleActivationEventCount = 0
        cachedPersistedQueueSource = null
        cachedPersistedQueueSongId = -1L
        cachedPersistedQueueCsv = ""
        appContext = null
        currentTrackInstanceToken = 0L
        lastDjTriggeredInstanceToken = -1L
        djCrossfadeStepDelayMs = 50L
        recordedCrossfadeVolumes.clear()
        recordedOverlapSamples.clear()
        equalizerManager.onSettingsChanged = null
        _state.value = PlaybackState()
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        val currentErrCode = _state.value.errorCode
        if (currentErrCode != null) {
            _state.value = _state.value.copy(
                errorMessage = resolveErrorMessage(currentErrCode, lang)
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorCode = null, errorMessage = null)
    }

    private fun resolveErrorMessage(code: PlaybackErrorCode, lang: AppLanguage = _language.value): String {
        return when (code) {
            PlaybackErrorCode.AUDIO_MISSING -> JnmfStrings.errorAudioMissing(lang)
            PlaybackErrorCode.AUDIO_UNSUPPORTED -> JnmfStrings.errorAudioUnsupported(lang)
            PlaybackErrorCode.CANNOT_PLAY_TRACK -> JnmfStrings.errorCannotPlayTrack(lang)
            PlaybackErrorCode.GENERIC -> JnmfStrings.errorPlaybackGeneric(lang)
        }
    }

    private fun mapPlaybackExceptionCode(error: PlaybackException): PlaybackErrorCode {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlaybackErrorCode.AUDIO_MISSING
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> PlaybackErrorCode.AUDIO_UNSUPPORTED
            else -> PlaybackErrorCode.GENERIC
        }
    }

    fun buildMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.albumArtUri)
            .setDisplayTitle(song.title)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun createDeckListener(player: ExoPlayer): Player.Listener {
        return object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (player !== activePlayer) {
                    return
                }
                if (isPlaying) {
                    _state.value = _state.value.copy(isPlaying = true)
                    startProgressTracker()
                } else {
                    if (!isDjTransitioning) {
                        stopProgressTracker()
                    }
                    if (!player.playWhenReady ||
                        player.playbackState == Player.STATE_IDLE ||
                        player.playbackState == Player.STATE_ENDED
                    ) {
                        _state.value = _state.value.copy(isPlaying = false)
                    }
                    savePlaybackSnapshotAsync()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (player !== activePlayer) {
                    return
                }
                if (!playWhenReady) {
                    if (!isDjTransitioning) {
                        stopProgressTracker()
                    }
                    _state.value = _state.value.copy(isPlaying = false)
                } else if (
                    _state.value.currentSong != null &&
                    (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING)
                ) {
                    _state.value = _state.value.copy(isPlaying = true)
                    startProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (player !== activePlayer) {
                    return
                }
                if (mediaItem != null) {
                    val mediaId = mediaItem.mediaId.toLongOrNull()
                    val currentQueue = _state.value.queue
                    val matchingSong = currentQueue.find { it.id == mediaId }
                        ?: _state.value.currentSong
                    if (matchingSong != null) {
                        val newIndex = currentQueue.indexOfFirst { it.id == matchingSong.id }
                        _state.value = _state.value.copy(
                            currentSong = matchingSong,
                            queueIndex = if (newIndex >= 0) newIndex else _state.value.queueIndex,
                            errorCode = null,
                            errorMessage = null
                        )
                        savePlaybackSnapshotAsync()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (player === outgoingCrossfadePlayer && player !== activePlayer) {
                    if (playbackState == Player.STATE_ENDED) {
                        // Outgoing deck finished its tail during overlap; stop quietly without double-skipping
                        stopAndClearDeck(player)
                    }
                    return
                }
                if (player !== activePlayer) {
                    return
                }
                when (playbackState) {
                    Player.STATE_READY -> {
                        val duration = player.duration.coerceAtLeast(0L)
                        _state.value = _state.value.copy(
                            durationMs = if (duration > 0L) duration else _state.value.durationMs,
                            isPlaying = player.playWhenReady,
                            errorCode = null,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        handleTrackEnded()
                    }
                    Player.STATE_IDLE -> {
                        if (!player.playWhenReady) {
                            _state.value = _state.value.copy(isPlaying = false)
                        }
                    }
                    Player.STATE_BUFFERING -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Case 1: Standby deck failed during advance preload or before taking over as activePlayer
                if (player !== activePlayer) {
                    val failedPreloadSongId = preloadedCandidate
                        ?.takeIf { it.deck === player }
                        ?.song?.id
                        ?: player.currentMediaItem?.mediaId?.toLongOrNull()
                    Log.w(TAG, "Standby DJ deck error on songId=$failedPreloadSongId: ${error.message}")
                    if (failedPreloadSongId != null) {
                        failedPreloadSongIdsForToken.add(failedPreloadSongId)
                    }
                    if (preloadedCandidate?.deck === player) {
                        preloadedCandidate = null
                    }
                    if (outgoingCrossfadePlayer === player) {
                        outgoingCrossfadePlayer = null
                    }
                    stopAndClearDeck(player)
                    return
                }

                // Case 2: Incoming deck failed right after crossfade started while outgoing deck is still viable
                val fallbackOutgoing = outgoingCrossfadePlayer
                val outgoingSong = _state.value.djOutgoingSong
                if (isDjTransitioning &&
                    fallbackOutgoing != null &&
                    fallbackOutgoing !== player &&
                    outgoingSong != null &&
                    fallbackOutgoing.playbackState != Player.STATE_ENDED &&
                    fallbackOutgoing.playbackState != Player.STATE_IDLE &&
                    fallbackOutgoing.playerError == null
                ) {
                    val failedIncomingId = _state.value.currentSong?.id
                    Log.w(TAG, "Incoming DJ deck failed mid-crossfade (songId=$failedIncomingId); keeping outgoing song playing")
                    djTransitionJob?.cancel()
                    djTransitionJob = null
                    isDjTransitioning = false
                    outgoingCrossfadePlayer = null
                    stopAndClearDeck(player)

                    activePlayer = fallbackOutgoing
                    _activeExoPlayerFlow.value = fallbackOutgoing
                    currentActiveBaseGain = 1.0f
                    currentOutgoingBaseGain = 0.0f
                    applyCurrentDeckVolumes()

                    if (failedIncomingId != null) {
                        failedPreloadSongIdsForToken.add(failedIncomingId)
                        if (_state.value.isShuffle) {
                            shuffleCyclePlayedIds.remove(failedIncomingId)
                            shuffleRemainingIds.remove(failedIncomingId)
                            if (shuffleHistoryBackStack.lastOrNull() == outgoingSong.id) {
                                shuffleHistoryBackStack.removeLast()
                            }
                        }
                    }
                    lastDjTriggeredInstanceToken = -1L

                    val q = _state.value.queue
                    val restoredIdx = q.indexOfFirst { it.id == outgoingSong.id }.coerceAtLeast(0)
                    _state.value = _state.value.copy(
                        currentSong = outgoingSong,
                        queueIndex = restoredIdx,
                        currentPositionMs = fallbackOutgoing.currentPosition.coerceAtLeast(0L),
                        durationMs = fallbackOutgoing.duration.takeIf { it > 0L } ?: outgoingSong.durationMs,
                        isPlaying = fallbackOutgoing.playWhenReady,
                        isDjTransitionActive = false,
                        djOutgoingSong = null,
                        djOutgoingVolume = 0f,
                        djIncomingVolume = 1f
                    )
                    return
                }

                // Case 3: Active primary player encountered an unrecoverable error
                Log.e(TAG, "Active ExoPlayer playback error [${error.errorCode}]: ${error.message}", error)
                cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
                player.playWhenReady = false
                abandonMusicAudioFocus()
                val code = mapPlaybackExceptionCode(error)
                _state.value = _state.value.copy(
                    isPlaying = false,
                    errorCode = code,
                    errorMessage = resolveErrorMessage(code)
                )
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (player === activePlayer && audioSessionId > 0) {
                    try {
                        equalizerManager.attachAudioSession(audioSessionId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed attaching audio session: ${e.message}")
                    }
                }
            }
        }
    }

    fun attachPlayer(
        player: ExoPlayer,
        secondaryPlayerFactory: (() -> ExoPlayer)? = null,
        secondaryPlayerDisposer: ((ExoPlayer) -> Unit)? = null
    ) {
        detachPlayer()

        this.deckA = player
        this.secondaryDeckFactory = secondaryPlayerFactory
        this.secondaryDeckDisposer = secondaryPlayerDisposer
        this.activePlayer = player
        this._activeExoPlayerFlow.value = player

        val listener = createDeckListener(player)
        deckAListener = listener
        player.addListener(listener)
        currentActiveBaseGain = 1.0f
        currentOutgoingBaseGain = 0.0f
        applyCurrentDeckVolumes()

        try {
            val sid = player.audioSessionId
            if (sid > 0) {
                equalizerManager.attachAudioSession(sid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking initial audioSessionId: ${e.message}")
        }

        val pendingSong = _state.value.currentSong
        if (pendingSong != null && player.mediaItemCount == 0) {
            try {
                val startPos = _state.value.currentPositionMs.coerceAtLeast(0L)
                val shouldPlay = _state.value.isPlaying
                player.setMediaItem(buildMediaItem(pendingSong), startPos)
                player.prepare()
                player.playWhenReady = shouldPlay
                if (shouldPlay) {
                    requestMusicAudioFocus()
                    player.play()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed preparing pending song on attachPlayer: ${e.message}", e)
            }
        }
    }

    fun detachPlayer() {
        cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
        progressJob?.cancel()
        progressJob = null

        deckAListener?.let { old ->
            try {
                deckA?.removeListener(old)
            } catch (_: Exception) {
            }
        }
        deckAListener = null

        deckBListener?.let { old ->
            try {
                deckB?.removeListener(old)
            } catch (_: Exception) {
            }
        }
        deckBListener = null

        val oldDeckB = deckB
        val wasInternalDeckB = deckBCreatedInternally
        val disposer = secondaryDeckDisposer

        deckA = null
        deckB = null
        deckBCreatedInternally = false
        secondaryDeckFactory = null
        secondaryDeckDisposer = null
        activePlayer = null
        outgoingCrossfadePlayer = null
        _activeExoPlayerFlow.value = null

        if (oldDeckB != null) {
            if (wasInternalDeckB) {
                try {
                    oldDeckB.release()
                } catch (_: Exception) {
                }
            } else {
                disposer?.invoke(oldDeckB)
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var tickCounter = 0
            while (isActive) {
                try {
                    activePlayer?.let { p ->
                        if (p.playbackState == Player.STATE_READY || p.isPlaying) {
                            val pos = p.currentPosition.coerceAtLeast(0L)
                            val rawDur = p.duration.coerceAtLeast(0L)
                            val effectiveDur = if (rawDur > 0L) rawDur else _state.value.durationMs
                            _state.value = _state.value.copy(
                                currentPositionMs = pos,
                                durationMs = effectiveDur
                            )
                            evaluatePlaybackPositionForDjTransition(pos, effectiveDur)
                            tickCounter++
                            if (tickCounter >= 25) {
                                tickCounter = 0
                                savePlaybackSnapshotAsync()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error tracking progress: ${e.message}")
                }
                delay(200)
            }
        }
    }

    internal fun computeEffectiveTransitionMs(durationMs: Long, configuredSeconds: Int): Long? {
        if (durationMs < MIN_TRACK_DURATION_FOR_CROSSFADE_MS) {
            return null
        }
        val targetMs = DjModeSettings.normalizeSeconds(configuredSeconds) * 1_000L
        val maxSafeForTrackMs = (durationMs - 2_500L).coerceAtMost(durationMs / 2L)
        val effectiveMs = minOf(targetMs, maxSafeForTrackMs)
        return if (effectiveMs >= 1_500L) effectiveMs else null
    }

    internal fun shouldTriggerDjTransition(positionMs: Long, durationMs: Long): Boolean {
        val s = _state.value
        if (!s.djModeEnabled || !s.isPlaying || isDjTransitioning) return false
        if (s.currentSong == null || durationMs <= 0L) return false
        if (lastDjTriggeredInstanceToken == currentTrackInstanceToken) return false

        val effectiveTransitionMs = computeEffectiveTransitionMs(durationMs, s.djTransitionSeconds) ?: return false
        if (positionMs < durationMs - effectiveTransitionMs) return false

        return selectNextCandidateForAutoAdvance(failedPreloadSongIdsForToken) != null
    }

    private fun distinctQueueSongIds(queue: List<Song>): List<Long> {
        if (queue.isEmpty()) return emptyList()
        val seen = HashSet<Long>(queue.size.coerceAtLeast(4))
        val result = ArrayList<Long>(queue.size)
        for (song in queue) {
            if (seen.add(song.id)) {
                result.add(song.id)
            }
        }
        return result
    }

    private fun buildRandomizedCycleOrder(eligibleIds: List<Long>, avoidFirstId: Long?): List<Long> {
        if (eligibleIds.size <= 1) return eligibleIds.toList()
        val shuffled = eligibleIds.toMutableList()
        shuffled.shuffle()
        if (avoidFirstId != null && shuffled.first() == avoidFirstId) {
            val swapIdx = (1 until shuffled.size).random()
            val tmp = shuffled[0]
            shuffled[0] = shuffled[swapIdx]
            shuffled[swapIdx] = tmp
        }
        return shuffled
    }

    private fun resetSmartShuffleState() {
        shuffleCyclePlayedIds.clear()
        shuffleRemainingIds.clear()
        shuffleHistoryBackStack.clear()
        pendingNextCycleOrder = null
        shuffleCompletedCyclesCount = 0
    }

    private fun initSmartShuffleCycleForCurrentSong(
        queue: List<Song>,
        currentSongId: Long,
        clearBackHistory: Boolean
    ) {
        shuffleCyclePlayedIds.clear()
        shuffleRemainingIds.clear()
        pendingNextCycleOrder = null
        if (clearBackHistory) {
            shuffleHistoryBackStack.clear()
            shuffleCompletedCyclesCount = 0
        }
        val allIds = distinctQueueSongIds(queue)
        shuffleCyclePlayedIds.add(currentSongId)
        val others = allIds.filter { it != currentSongId }.shuffled()
        shuffleRemainingIds.addAll(others)
    }

    private fun pushToShuffleBackHistory(songId: Long) {
        if (songId <= 0L) return
        if (shuffleHistoryBackStack.lastOrNull() == songId) return
        if (shuffleHistoryBackStack.size >= MAX_SHUFFLE_BACK_HISTORY) {
            shuffleHistoryBackStack.removeFirst()
        }
        shuffleHistoryBackStack.addLast(songId)
    }

    private fun ensureSmartShuffleInitialized(queue: List<Song>, currentSongId: Long?) {
        if (shuffleCyclePlayedIds.isEmpty() && shuffleRemainingIds.isEmpty()) {
            val seedId = currentSongId ?: queue.firstOrNull()?.id ?: return
            initSmartShuffleCycleForCurrentSong(queue, seedId, clearBackHistory = false)
        }
    }

    private fun reconcileSmartShuffleWithQueue(effectiveQueue: List<Song>, currentSongId: Long?) {
        if (!_state.value.isShuffle) return
        val validIds = distinctQueueSongIds(effectiveQueue)
        if (validIds.isEmpty()) {
            resetSmartShuffleState()
            return
        }
        val validIdSet = validIds.toHashSet()

        if (shuffleCyclePlayedIds.isEmpty() && shuffleRemainingIds.isEmpty()) {
            val seedId = currentSongId?.takeIf { it in validIdSet } ?: validIds.first()
            initSmartShuffleCycleForCurrentSong(effectiveQueue, seedId, clearBackHistory = false)
            return
        }

        // 1. Remove any deleted/removed songs from remaining queue, current cycle history, and back-stack
        shuffleRemainingIds.removeAll { it !in validIdSet }
        shuffleCyclePlayedIds.retainAll(validIdSet)
        if (currentSongId != null && currentSongId in validIdSet) {
            shuffleCyclePlayedIds.add(currentSongId)
            shuffleRemainingIds.remove(currentSongId)
        }
        if (shuffleHistoryBackStack.isNotEmpty()) {
            val filteredHistory = ArrayList<Long>(shuffleHistoryBackStack.size)
            for (id in shuffleHistoryBackStack) {
                if (id in validIdSet && filteredHistory.lastOrNull() != id) {
                    filteredHistory.add(id)
                }
            }
            shuffleHistoryBackStack.clear()
            shuffleHistoryBackStack.addAll(filteredHistory)
        }
        pendingNextCycleOrder = null

        // 2. Insert newly added songs into remaining shuffle pool at random positions without repeating played songs
        val remainingSet = shuffleRemainingIds.toHashSet()
        val newlyAddedIds = validIds.filter { id ->
            id !in shuffleCyclePlayedIds && id !in remainingSet
        }.shuffled()

        if (newlyAddedIds.isNotEmpty()) {
            val mutableRemaining = shuffleRemainingIds.toMutableList()
            for (newId in newlyAddedIds) {
                val insertPos = (0..mutableRemaining.size).random()
                mutableRemaining.add(insertPos, newId)
            }
            shuffleRemainingIds.clear()
            shuffleRemainingIds.addAll(mutableRemaining)
        }
    }

    private fun peekNextSmartShuffleCandidate(
        queue: List<Song>,
        currentSongId: Long?,
        excludeSongIds: Set<Long> = emptySet()
    ): NextTrackCandidate? {
        if (queue.isEmpty()) return null
        ensureSmartShuffleInitialized(queue, currentSongId)

        val songsById = queue.associateBy { it.id }
        shuffleRemainingIds.removeAll { it !in songsById }

        // 1. Select next unplayed shuffled song in the current cycle
        val nextInCurrentCycleId = shuffleRemainingIds.firstOrNull { id ->
            id !in excludeSongIds && id !in shuffleCyclePlayedIds && songsById.containsKey(id)
        }
        if (nextInCurrentCycleId != null) {
            val song = songsById[nextInCurrentCycleId] ?: return null
            val idx = queue.indexOfFirst { it.id == nextInCurrentCycleId }.coerceAtLeast(0)
            return NextTrackCandidate(song, idx)
        }

        // 2. Every eligible song in the current cycle has played: prepare a new randomized cycle
        //    avoiding immediate repetition of the last song from the previous cycle when possible.
        val eligibleIds = distinctQueueSongIds(queue).filter { it !in excludeSongIds }
        if (eligibleIds.isEmpty()) return null

        val existingPending = pendingNextCycleOrder
        val validPending = if (
            existingPending != null &&
            existingPending.isNotEmpty() &&
            existingPending.first() in songsById &&
            existingPending.first() !in excludeSongIds &&
            (eligibleIds.size <= 1 || existingPending.first() != currentSongId)
        ) {
            existingPending.filter { it in songsById }
        } else {
            buildRandomizedCycleOrder(eligibleIds = eligibleIds, avoidFirstId = currentSongId).also {
                pendingNextCycleOrder = it
            }
        }

        val chosenId = validPending.firstOrNull { it !in excludeSongIds && songsById.containsKey(it) }
            ?: return null
        val song = songsById[chosenId] ?: return null
        val idx = queue.indexOfFirst { it.id == chosenId }.coerceAtLeast(0)
        return NextTrackCandidate(song, idx)
    }

    private fun commitSmartShuffleTrackActivation(
        queue: List<Song>,
        previousSongId: Long?,
        activatedSongId: Long,
        brokenSkippedIds: Set<Long> = emptySet()
    ) {
        if (!_state.value.isShuffle || _state.value.repeatState == RepeatState.ONE) return
        ensureSmartShuffleInitialized(queue, previousSongId)
        shuffleActivationEventCount++

        if (previousSongId != null && (previousSongId != activatedSongId || queue.size == 1)) {
            pushToShuffleBackHistory(previousSongId)
        }

        if (brokenSkippedIds.isNotEmpty()) {
            shuffleRemainingIds.removeAll { it in brokenSkippedIds }
        }

        val wasInCurrentCycleRemaining = shuffleRemainingIds.remove(activatedSongId) &&
            activatedSongId !in shuffleCyclePlayedIds

        if (wasInCurrentCycleRemaining) {
            shuffleCyclePlayedIds.add(activatedSongId)
            pendingNextCycleOrder = null
        } else {
            shuffleCompletedCyclesCount++
            val allIds = distinctQueueSongIds(queue)
            val cycleOrder = pendingNextCycleOrder
                ?.takeIf { it.firstOrNull() == activatedSongId }
                ?.filter { it in allIds }
                ?: run {
                    val rest = allIds.filter { it != activatedSongId }.shuffled()
                    listOf(activatedSongId) + rest
                }
            pendingNextCycleOrder = null
            shuffleCyclePlayedIds.clear()
            shuffleCyclePlayedIds.add(activatedSongId)
            shuffleRemainingIds.clear()
            for (id in cycleOrder) {
                if (id != activatedSongId && id !in shuffleCyclePlayedIds) {
                    shuffleRemainingIds.addLast(id)
                }
            }
            val inRemaining = shuffleRemainingIds.toHashSet()
            val missing = allIds.filter { it != activatedSongId && it !in inRemaining }.shuffled()
            shuffleRemainingIds.addAll(missing)
        }
    }

    private fun selectNextCandidateForAutoAdvance(
        excludeSongIds: Set<Long> = emptySet()
    ): NextTrackCandidate? {
        val s = _state.value
        var queue = s.queue
        if (queue.isEmpty()) {
            val curr = s.currentSong ?: return null
            queue = listOf(curr)
        }
        val currentIndex = if (s.queueIndex in queue.indices) s.queueIndex else 0

        return when {
            s.repeatState == RepeatState.ONE -> {
                val song = queue[currentIndex]
                if (song.id in excludeSongIds) null else NextTrackCandidate(song, currentIndex)
            }
            s.isShuffle -> {
                val currentId = s.currentSong?.id ?: queue.getOrNull(currentIndex)?.id
                peekNextSmartShuffleCandidate(
                    queue = queue,
                    currentSongId = currentId,
                    excludeSongIds = excludeSongIds
                )
            }
            s.repeatState == RepeatState.OFF -> {
                var nextIdx = currentIndex + 1
                while (nextIdx < queue.size && queue[nextIdx].id in excludeSongIds) {
                    nextIdx++
                }
                if (nextIdx in queue.indices) {
                    NextTrackCandidate(queue[nextIdx], nextIdx)
                } else {
                    null
                }
            }
            else -> {
                var offset = 1
                var found: NextTrackCandidate? = null
                while (offset <= queue.size) {
                    val idx = (currentIndex + offset) % queue.size
                    if (queue[idx].id !in excludeSongIds) {
                        found = NextTrackCandidate(queue[idx], idx)
                        break
                    }
                    offset++
                }
                found
            }
        }
    }

    private fun preloadCandidateOnStandbyDeck(candidate: NextTrackCandidate): ExoPlayer? {
        val standby = getOrCreateStandbyDeck() ?: return null
        if (standby === activePlayer) return null
        return try {
            setDeckRawVolume(standby, 0f)
            standby.playWhenReady = false
            standby.setMediaItem(buildMediaItem(candidate.song), 0L)
            standby.prepare()
            preloadedCandidate = PreloadedDeckCandidate(
                deck = standby,
                song = candidate.song,
                queueIndex = candidate.index,
                forTrackToken = currentTrackInstanceToken
            )
            standby
        } catch (e: Exception) {
            Log.w(TAG, "Failed preloading next DJ track (${candidate.song.title}): ${e.message}")
            failedPreloadSongIdsForToken.add(candidate.song.id)
            stopAndClearDeck(standby)
            preloadedCandidate = null
            null
        }
    }

    /**
     * Evaluates playback progress to:
     * 1. Preload the next track in advance on the silent standby deck (~4s before crossfade).
     * 2. Start a genuine overlapping dual-deck equal-power crossfade at (duration - transitionSeconds).
     */
    internal fun evaluatePlaybackPositionForDjTransition(positionMs: Long, durationMs: Long) {
        val s = _state.value
        if (!s.djModeEnabled || !s.isPlaying || isDjTransitioning) return
        val outgoingSong = s.currentSong ?: return
        if (durationMs <= 0L) return
        if (lastDjTriggeredInstanceToken == currentTrackInstanceToken) return

        val effectiveTransitionMs = computeEffectiveTransitionMs(durationMs, s.djTransitionSeconds) ?: return
        val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)

        // Stage 1: Advance preload on silent standby deck before crossfade point arrives
        if (remainingMs > effectiveTransitionMs) {
            if (remainingMs <= effectiveTransitionMs + PRELOAD_LEAD_TIME_MS) {
                val existingPreload = preloadedCandidate
                val isPreloadValid = existingPreload != null &&
                    existingPreload.forTrackToken == currentTrackInstanceToken &&
                    existingPreload.song.id !in failedPreloadSongIdsForToken &&
                    existingPreload.deck !== activePlayer &&
                    existingPreload.deck.playerError == null &&
                    existingPreload.deck.mediaItemCount > 0

                if (!isPreloadValid) {
                    val candidate = selectNextCandidateForAutoAdvance(failedPreloadSongIdsForToken)
                    if (candidate != null) {
                        preloadCandidateOnStandbyDeck(candidate)
                    }
                }
            }
            return
        }

        // Stage 2: Start overlapping dual-deck crossfade
        val outgoingDeck = activePlayer ?: return
        val validPreload = preloadedCandidate?.takeIf {
            it.forTrackToken == currentTrackInstanceToken &&
                it.song.id !in failedPreloadSongIdsForToken &&
                it.deck !== outgoingDeck &&
                it.deck.playerError == null &&
                it.deck.mediaItemCount > 0
        }

        val candidate: NextTrackCandidate
        val incomingDeck: ExoPlayer
        if (validPreload != null) {
            candidate = NextTrackCandidate(validPreload.song, validPreload.queueIndex)
            incomingDeck = validPreload.deck
        } else {
            candidate = selectNextCandidateForAutoAdvance(failedPreloadSongIdsForToken) ?: return
            incomingDeck = preloadCandidateOnStandbyDeck(candidate) ?: return
        }

        startOverlappingCrossfade(
            outgoingDeck = outgoingDeck,
            incomingDeck = incomingDeck,
            outgoingSong = outgoingSong,
            candidate = candidate,
            targetDurationMs = effectiveTransitionMs,
            outgoingRemainingMs = remainingMs
        )
    }

    private fun startOverlappingCrossfade(
        outgoingDeck: ExoPlayer,
        incomingDeck: ExoPlayer,
        outgoingSong: Song,
        candidate: NextTrackCandidate,
        targetDurationMs: Long,
        outgoingRemainingMs: Long
    ) {
        if (outgoingDeck === incomingDeck) return

        isDjTransitioning = true
        val originatingToken = currentTrackInstanceToken
        lastDjTriggeredInstanceToken = originatingToken
        recordedCrossfadeVolumes.clear()
        recordedOverlapSamples.clear()

        val stepDelay = djCrossfadeStepDelayMs

        // Deterministic synchronous ramp path for zero-delay unit tests
        if (stepDelay <= 0L) {
            try {
                if (incomingDeck.playerError != null || candidate.song.id in failedPreloadSongIdsForToken) {
                    failedPreloadSongIdsForToken.add(candidate.song.id)
                    stopAndClearDeck(incomingDeck)
                    isDjTransitioning = false
                    lastDjTriggeredInstanceToken = -1L
                    return
                }

                VideoPlaybackController.onMusicPlaybackStarted()
                requestMusicAudioFocus()

                setDeckRawVolume(incomingDeck, 0f)
                incomingDeck.playWhenReady = true
                incomingDeck.play()

                val brokenIdsBeforeCommit = failedPreloadSongIdsForToken.toSet()
                outgoingCrossfadePlayer = outgoingDeck
                activePlayer = incomingDeck
                _activeExoPlayerFlow.value = incomingDeck
                currentTrackInstanceToken++
                preloadedCandidate = null
                failedPreloadSongIdsForToken.clear()

                commitSmartShuffleTrackActivation(
                    queue = _state.value.queue,
                    previousSongId = outgoingSong.id,
                    activatedSongId = candidate.song.id,
                    brokenSkippedIds = brokenIdsBeforeCommit
                )

                _state.value = _state.value.copy(
                    currentSong = candidate.song,
                    queueIndex = candidate.index,
                    currentPositionMs = incomingDeck.currentPosition.coerceAtLeast(0L),
                    durationMs = candidate.song.durationMs,
                    isPlaying = true,
                    isDjTransitionActive = true,
                    djOutgoingSong = outgoingSong,
                    djOutgoingVolume = 1.0f,
                    djIncomingVolume = 0.0f,
                    errorCode = null,
                    errorMessage = null
                )

                val syncSteps = (targetDurationMs / 250L).toInt().coerceIn(12, 80)
                for (step in 1..syncSteps) {
                    val fraction = step.toFloat() / syncSteps.toFloat()
                    val angle = fraction * (PI / 2.0)
                    val outGain = cos(angle).toFloat().coerceIn(0f, 1f)
                    val inGain = sin(angle).toFloat().coerceIn(0f, 1f)
                    currentOutgoingBaseGain = outGain
                    currentActiveBaseGain = inGain
                    applyCurrentDeckVolumes()

                    val bothActive = outgoingDeck.playWhenReady &&
                        incomingDeck.playWhenReady &&
                        outgoingDeck.mediaItemCount > 0 &&
                        incomingDeck.mediaItemCount > 0
                    recordedCrossfadeVolumes.add(outGain)
                    recordedOverlapSamples.add(
                        CrossfadeOverlapSample(
                            progressFraction = fraction,
                            outgoingVolume = outgoingDeck.volume,
                            incomingVolume = incomingDeck.volume,
                            outgoingSongId = outgoingSong.id,
                            incomingSongId = candidate.song.id,
                            bothDecksActive = bothActive
                        )
                    )
                }

                currentActiveBaseGain = 1.0f
                currentOutgoingBaseGain = 0.0f
                applyCurrentDeckVolumes()
                if (incomingDeck.audioSessionId > 0) {
                    equalizerManager.attachAudioSession(incomingDeck.audioSessionId)
                }
                stopAndClearDeck(outgoingDeck)
                outgoingCrossfadePlayer = null
                isDjTransitioning = false
                _state.value = _state.value.copy(
                    isDjTransitionActive = false,
                    djOutgoingSong = null,
                    djOutgoingVolume = 0f,
                    djIncomingVolume = 1f
                )
                savePlaybackSnapshotAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Error in synchronous DJ crossfade: ${e.message}", e)
                cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
            }
            return
        }

        // Real-time asynchronous dual-deck overlap coroutine
        djTransitionJob?.cancel()
        djTransitionJob = scope.launch {
            var committedToIncoming = false
            var latestIncomingDeck: ExoPlayer? = incomingDeck
            try {
                VideoPlaybackController.onMusicPlaybackStarted()
                requestMusicAudioFocus()

                var currentCandidate: NextTrackCandidate? = candidate
                var readyIncomingDeck: ExoPlayer? = incomingDeck
                var totalWaitedMs = 0L
                val maxAttempts = _state.value.queue.size.coerceAtLeast(1)
                var attempts = 0

                while (isActive &&
                    _state.value.djModeEnabled &&
                    _state.value.isPlaying &&
                    activePlayer === outgoingDeck &&
                    currentCandidate != null &&
                    readyIncomingDeck != null &&
                    attempts < maxAttempts
                ) {
                    attempts++
                    val cand = currentCandidate
                    val deck = readyIncomingDeck
                    latestIncomingDeck = deck

                    // Start incoming deck silently first so we can verify readiness before fading out outgoing deck
                    if (deck.playbackState == Player.STATE_IDLE && deck.mediaItemCount > 0) {
                        deck.prepare()
                    }
                    setDeckRawVolume(deck, 0f)
                    deck.playWhenReady = true
                    deck.play()

                    var waitedForCandidateMs = 0L
                    while (isActive &&
                        deck.mediaItemCount > 0 &&
                        (deck.playbackState == Player.STATE_IDLE || deck.playbackState == Player.STATE_BUFFERING) &&
                        deck.playerError == null &&
                        cand.song.id !in failedPreloadSongIdsForToken &&
                        waitedForCandidateMs < MAX_INCOMING_READY_WAIT_MS
                    ) {
                        delay(40L)
                        waitedForCandidateMs += 40L
                        totalWaitedMs += 40L
                    }

                    if (!isActive || !_state.value.djModeEnabled || !_state.value.isPlaying || activePlayer !== outgoingDeck) {
                        stopAndClearDeck(deck)
                        return@launch
                    }

                    val isDeckReady = deck.playbackState == Player.STATE_READY &&
                        deck.playerError == null &&
                        cand.song.id !in failedPreloadSongIdsForToken &&
                        deck.mediaItemCount > 0

                    if (isDeckReady) {
                        break
                    }

                    // Candidate failed to reach STATE_READY (missing file, broken codec, or load timeout):
                    // Keep outgoingDeck playing smoothly at 1.0f and advance to the next valid song in queue.
                    Log.w(TAG, "Skipping unready/broken DJ candidate (${cand.song.title}); keeping outgoing track playing")
                    failedPreloadSongIdsForToken.add(cand.song.id)
                    if (preloadedCandidate?.song?.id == cand.song.id) {
                        preloadedCandidate = null
                    }
                    stopAndClearDeck(deck)

                    val nextCand = selectNextCandidateForAutoAdvance(failedPreloadSongIdsForToken)
                    if (nextCand == null) {
                        currentCandidate = null
                        readyIncomingDeck = null
                        break
                    }
                    val nextDeck = preloadCandidateOnStandbyDeck(nextCand)
                    if (nextDeck == null) {
                        currentCandidate = null
                        readyIncomingDeck = null
                        break
                    }
                    currentCandidate = nextCand
                    readyIncomingDeck = nextDeck
                }

                val verifiedCandidate = currentCandidate
                val verifiedIncomingDeck = readyIncomingDeck
                if (verifiedCandidate == null ||
                    verifiedIncomingDeck == null ||
                    verifiedIncomingDeck.playbackState != Player.STATE_READY ||
                    verifiedIncomingDeck.playerError != null ||
                    verifiedCandidate.song.id in failedPreloadSongIdsForToken
                ) {
                    if (verifiedIncomingDeck != null && verifiedIncomingDeck !== activePlayer) {
                        stopAndClearDeck(verifiedIncomingDeck)
                    }
                    lastDjTriggeredInstanceToken = -1L
                    return@launch
                }

                latestIncomingDeck = verifiedIncomingDeck

                // Both decks are now simultaneously active! Swap active deck reference without resetting either player.
                val brokenIdsBeforeCommit = failedPreloadSongIdsForToken.toSet()
                committedToIncoming = true
                outgoingCrossfadePlayer = outgoingDeck
                activePlayer = verifiedIncomingDeck
                _activeExoPlayerFlow.value = verifiedIncomingDeck
                currentTrackInstanceToken++
                preloadedCandidate = null
                failedPreloadSongIdsForToken.clear()
                currentOutgoingBaseGain = 1.0f
                currentActiveBaseGain = 0.0f
                applyCurrentDeckVolumes()

                commitSmartShuffleTrackActivation(
                    queue = _state.value.queue,
                    previousSongId = outgoingSong.id,
                    activatedSongId = verifiedCandidate.song.id,
                    brokenSkippedIds = brokenIdsBeforeCommit
                )

                _state.value = _state.value.copy(
                    currentSong = verifiedCandidate.song,
                    queueIndex = verifiedCandidate.index,
                    currentPositionMs = verifiedIncomingDeck.currentPosition.coerceAtLeast(0L),
                    durationMs = verifiedIncomingDeck.duration.takeIf { it > 0L } ?: verifiedCandidate.song.durationMs,
                    isPlaying = true,
                    isDjTransitionActive = true,
                    djOutgoingSong = outgoingSong,
                    djOutgoingVolume = 1.0f,
                    djIncomingVolume = 0.0f,
                    errorCode = null,
                    errorMessage = null
                )
                savePlaybackSnapshotAsync()
                startProgressTracker()

                val effectiveRampMs = if (outgoingRemainingMs > 0L) {
                    minOf(targetDurationMs, (outgoingRemainingMs - totalWaitedMs).coerceAtLeast(600L))
                } else {
                    targetDurationMs
                }

                val rampStartMs = SystemClock.elapsedRealtime()
                var switchedEqualizerSession = false
                var uiUpdateTick = 0

                while (isActive && _state.value.djModeEnabled && _state.value.isPlaying && activePlayer === verifiedIncomingDeck) {
                    val elapsedMs = (SystemClock.elapsedRealtime() - rampStartMs).coerceAtLeast(0L)
                    val fraction = (elapsedMs.toFloat() / effectiveRampMs.toFloat()).coerceIn(0f, 1f)
                    val angle = fraction * (PI / 2.0)
                    val outGain = cos(angle).toFloat().coerceIn(0f, 1f)
                    val inGain = sin(angle).toFloat().coerceIn(0f, 1f)

                    currentOutgoingBaseGain = outGain
                    currentActiveBaseGain = inGain
                    applyCurrentDeckVolumes()

                    val bothActive = outgoingDeck.playWhenReady &&
                        verifiedIncomingDeck.playWhenReady &&
                        verifiedIncomingDeck.mediaItemCount > 0 &&
                        (outgoingDeck.mediaItemCount > 0 || fraction >= 0.95f)

                    recordedCrossfadeVolumes.add(outGain)
                    recordedOverlapSamples.add(
                        CrossfadeOverlapSample(
                            progressFraction = fraction,
                            outgoingVolume = outgoingDeck.volume,
                            incomingVolume = verifiedIncomingDeck.volume,
                            outgoingSongId = outgoingSong.id,
                            incomingSongId = verifiedCandidate.song.id,
                            bothDecksActive = bothActive
                        )
                    )

                    if (!switchedEqualizerSession && fraction >= 0.5f) {
                        switchedEqualizerSession = true
                        val sid = verifiedIncomingDeck.audioSessionId
                        if (sid > 0) {
                            equalizerManager.attachAudioSession(sid)
                        }
                    }

                    uiUpdateTick++
                    if (uiUpdateTick % 4 == 0 || fraction >= 1.0f) {
                        _state.value = _state.value.copy(
                            djOutgoingVolume = outGain,
                            djIncomingVolume = inGain
                        )
                    }

                    if (fraction >= 1.0f) {
                        break
                    }
                    delay(stepDelay)
                }
            } finally {
                val finalIncoming = latestIncomingDeck
                if (committedToIncoming && finalIncoming != null && activePlayer === finalIncoming) {
                    currentActiveBaseGain = 1.0f
                    currentOutgoingBaseGain = 0.0f
                    applyCurrentDeckVolumes()
                    val sid = finalIncoming.audioSessionId
                    if (sid > 0) {
                        equalizerManager.attachAudioSession(sid)
                    }
                    if (outgoingDeck !== activePlayer) {
                        stopAndClearDeck(outgoingDeck)
                    }
                    outgoingCrossfadePlayer = null
                    if (!_state.value.djModeEnabled) {
                        releaseIdleSecondaryDeckIfPossible()
                    }
                } else if (!committedToIncoming && finalIncoming != null && finalIncoming !== activePlayer) {
                    stopAndClearDeck(finalIncoming)
                    currentActiveBaseGain = 1.0f
                    applyCurrentDeckVolumes()
                }
                isDjTransitioning = false
                if (_state.value.isDjTransitionActive || _state.value.djOutgoingSong != null) {
                    _state.value = _state.value.copy(
                        isDjTransitionActive = false,
                        djOutgoingSong = null,
                        djOutgoingVolume = 0f,
                        djIncomingVolume = 1f
                    )
                }
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
        try {
            activePlayer?.let { p ->
                if (_state.value.currentSong != null && p.mediaItemCount > 0) {
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    if (pos > 0L || p.playbackState == Player.STATE_READY) {
                        _state.value = _state.value.copy(currentPositionMs = pos)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in stopProgressTracker: ${e.message}")
        }
    }

    fun playSong(
        song: Song,
        newQueue: List<Song> = listOf(song),
        sourceType: QueueSourceType = QueueSourceType.CUSTOM,
        sourcePlaylistId: Long = -1L
    ) {
        cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
        currentTrackInstanceToken++
        lastDjTriggeredInstanceToken = -1L
        VideoPlaybackController.onMusicPlaybackStarted()
        requestMusicAudioFocus()
        activeQueueSourceType = sourceType
        activeQueuePlaylistId = sourcePlaylistId
        originalQueue = newQueue.ifEmpty { listOf(song) }

        val effectiveQueue = buildEffectiveQueue()
        val index = effectiveQueue.indexOfFirst { it.id == song.id }.let {
            if (it >= 0) it else 0
        }

        if (_state.value.isShuffle) {
            initSmartShuffleCycleForCurrentSong(
                queue = effectiveQueue,
                currentSongId = song.id,
                clearBackHistory = true
            )
        } else {
            resetSmartShuffleState()
        }

        _state.value = _state.value.copy(
            queue = effectiveQueue,
            queueIndex = index,
            currentSong = song,
            currentPositionMs = 0L,
            durationMs = song.durationMs,
            isPlaying = true,
            isDjTransitionActive = false,
            djOutgoingSong = null,
            errorCode = null,
            errorMessage = null
        )
        savePlaybackSnapshotAsync()

        val player = activePlayer
        if (player == null) {
            Log.w(TAG, "ExoPlayer not attached yet. Queuing song and starting PlaybackService if bound.")
            appContext?.let { ctx -> PlaybackService.startService(ctx) }
            return
        }

        val mediaItem = buildMediaItem(song)
        try {
            currentActiveBaseGain = 1.0f
            applyCurrentDeckVolumes()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during playSong: ${e.message}", e)
            _state.value = _state.value.copy(
                isPlaying = false,
                errorCode = PlaybackErrorCode.CANNOT_PLAY_TRACK,
                errorMessage = resolveErrorMessage(PlaybackErrorCode.CANNOT_PLAY_TRACK)
            )
        }
    }

    fun togglePlayPause() {
        val currentlyPlaying = activePlayer?.let { player ->
            player.isPlaying || (
                player.playWhenReady &&
                    player.playbackState == Player.STATE_BUFFERING &&
                    _state.value.currentSong != null
                )
        } ?: _state.value.isPlaying

        if (currentlyPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun next() {
        var queue = _state.value.queue
        if (queue.isEmpty()) {
            val curr = _state.value.currentSong ?: return
            queue = listOf(curr)
        }
        val currentIndex = if (_state.value.queueIndex in queue.indices) _state.value.queueIndex else 0
        val currentSong = _state.value.currentSong ?: queue[currentIndex]

        when {
            _state.value.repeatState == RepeatState.ONE -> {
                playSongAtIndex(queue[currentIndex], currentIndex)
            }
            _state.value.isShuffle -> {
                val candidate = peekNextSmartShuffleCandidate(
                    queue = queue,
                    currentSongId = currentSong.id
                ) ?: NextTrackCandidate(queue[currentIndex], currentIndex)
                commitSmartShuffleTrackActivation(
                    queue = queue,
                    previousSongId = currentSong.id,
                    activatedSongId = candidate.song.id
                )
                playSongAtIndex(candidate.song, candidate.index)
            }
            else -> {
                val nextIndex = (currentIndex + 1) % queue.size
                playSongAtIndex(queue[nextIndex], nextIndex)
            }
        }
    }

    fun previous() {
        cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
        var queue = _state.value.queue
        if (queue.isEmpty()) {
            val curr = _state.value.currentSong ?: return
            queue = listOf(curr)
        }
        val currentIndex = if (_state.value.queueIndex in queue.indices) _state.value.queueIndex else 0

        if (_state.value.isShuffle && _state.value.repeatState != RepeatState.ONE) {
            val currentId = _state.value.currentSong?.id
            val songsById = queue.associateBy { it.id }
            var targetSong: Song? = null
            while (shuffleHistoryBackStack.isNotEmpty() && targetSong == null) {
                val candidatePrevId = shuffleHistoryBackStack.removeLast()
                if (candidatePrevId != currentId) {
                    targetSong = songsById[candidatePrevId]
                } else if (queue.size == 1) {
                    targetSong = songsById[candidatePrevId]
                }
            }
            if (targetSong != null) {
                val prevIdx = queue.indexOfFirst { it.id == targetSong.id }.coerceAtLeast(0)
                playSongAtIndex(targetSong, prevIdx)
            } else {
                // At the very beginning of shuffle history: restart the current song rather than picking randomly
                val currSong = _state.value.currentSong ?: queue[currentIndex]
                val currIdx = queue.indexOfFirst { it.id == currSong.id }.coerceAtLeast(0)
                playSongAtIndex(currSong, currIdx)
            }
            return
        }

        val player = activePlayer
        if (player != null && player.currentPosition > 3000) {
            try {
                lastDjTriggeredInstanceToken = -1L
                player.seekTo(0)
                _state.value = _state.value.copy(currentPositionMs = 0)
                savePlaybackSnapshotAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking to 0: ${e.message}")
            }
            return
        }
        val prevIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
        val prevSong = queue[prevIndex]
        playSongAtIndex(prevSong, prevIndex)
    }

    private fun playSongAtIndex(song: Song, index: Int) {
        cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
        currentTrackInstanceToken++
        lastDjTriggeredInstanceToken = -1L
        VideoPlaybackController.onMusicPlaybackStarted()
        requestMusicAudioFocus()

        _state.value = _state.value.copy(
            currentSong = song,
            queueIndex = index,
            currentPositionMs = 0L,
            durationMs = song.durationMs,
            isPlaying = true,
            isDjTransitionActive = false,
            djOutgoingSong = null,
            errorCode = null,
            errorMessage = null
        )
        savePlaybackSnapshotAsync()

        val player = activePlayer
        if (player == null) {
            appContext?.let { ctx -> PlaybackService.startService(ctx) }
            return
        }
        val mediaItem = buildMediaItem(song)
        try {
            currentActiveBaseGain = 1.0f
            applyCurrentDeckVolumes()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Playback error in playSongAtIndex: ${e.message}", e)
            _state.value = _state.value.copy(
                isPlaying = false,
                errorCode = PlaybackErrorCode.CANNOT_PLAY_TRACK,
                errorMessage = resolveErrorMessage(PlaybackErrorCode.CANNOT_PLAY_TRACK)
            )
        }
    }

    private fun handleTrackEnded() {
        val knownFailedIds = failedPreloadSongIdsForToken.toSet()
        val finishedSongId = _state.value.currentSong?.id
        cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
        when (_state.value.repeatState) {
            RepeatState.ONE -> {
                try {
                    currentTrackInstanceToken++
                    lastDjTriggeredInstanceToken = -1L
                    activePlayer?.seekTo(0)
                    activePlayer?.play()
                } catch (e: Exception) {
                    Log.e(TAG, "Error repeating one: ${e.message}")
                }
            }
            RepeatState.ALL -> {
                val nextCandidate = selectNextCandidateForAutoAdvance(knownFailedIds)
                if (nextCandidate != null) {
                    if (_state.value.isShuffle) {
                        commitSmartShuffleTrackActivation(
                            queue = _state.value.queue,
                            previousSongId = finishedSongId,
                            activatedSongId = nextCandidate.song.id,
                            brokenSkippedIds = knownFailedIds
                        )
                    }
                    playSongAtIndex(nextCandidate.song, nextCandidate.index)
                } else {
                    next()
                }
            }
            RepeatState.OFF -> {
                val nextCandidate = selectNextCandidateForAutoAdvance(knownFailedIds)
                if (nextCandidate != null && (!_state.value.isShuffle || _state.value.queue.size > 1)) {
                    if (_state.value.isShuffle) {
                        commitSmartShuffleTrackActivation(
                            queue = _state.value.queue,
                            previousSongId = finishedSongId,
                            activatedSongId = nextCandidate.song.id,
                            brokenSkippedIds = knownFailedIds
                        )
                    }
                    playSongAtIndex(nextCandidate.song, nextCandidate.index)
                } else {
                    try {
                        activePlayer?.pause()
                        activePlayer?.seekTo(0)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping at queue end: ${e.message}")
                    }
                    abandonMusicAudioFocus()
                    _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0L)
                    savePlaybackSnapshotAsync()
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
        val dur = _state.value.durationMs
        val transitionMs = computeEffectiveTransitionMs(dur, _state.value.djTransitionSeconds) ?: 0L
        if (dur <= 0L || positionMs < dur - transitionMs) {
            lastDjTriggeredInstanceToken = -1L
        }
        try {
            activePlayer?.seekTo(positionMs)
            _state.value = _state.value.copy(currentPositionMs = positionMs)
            savePlaybackSnapshotAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error in seekTo: ${e.message}")
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffle
        _state.value = _state.value.copy(isShuffle = newShuffle)
        if (!isDjTransitioning) {
            clearStandbyPreload()
        }
        if (newShuffle) {
            val curr = _state.value.currentSong
            if (curr != null) {
                initSmartShuffleCycleForCurrentSong(
                    queue = _state.value.queue,
                    currentSongId = curr.id,
                    clearBackHistory = true
                )
            }
        } else {
            resetSmartShuffleState()
        }
        savePlaybackSnapshotAsync()
    }

    fun cycleRepeatMode() {
        val nextRepeat = when (_state.value.repeatState) {
            RepeatState.OFF -> RepeatState.ALL
            RepeatState.ALL -> RepeatState.ONE
            RepeatState.ONE -> RepeatState.OFF
        }
        _state.value = _state.value.copy(repeatState = nextRepeat)
        if (!isDjTransitioning) {
            clearStandbyPreload()
        }
        persistModesIfBound()
    }

    fun toggleReverseQueue() {
        val newReverse = !_state.value.isReverseQueue
        _state.value = _state.value.copy(isReverseQueue = newReverse)
        if (!isDjTransitioning) {
            clearStandbyPreload()
        }
        val currentSong = _state.value.currentSong
        val updatedQueue = buildEffectiveQueue()
        val newIndex = if (currentSong != null) updatedQueue.indexOfFirst { it.id == currentSong.id } else 0
        if (_state.value.isShuffle) {
            reconcileSmartShuffleWithQueue(updatedQueue, currentSong?.id)
        }
        _state.value = _state.value.copy(
            queue = updatedQueue,
            queueIndex = if (newIndex >= 0) newIndex else 0
        )
        savePlaybackSnapshotAsync()
    }

    private fun buildEffectiveQueue(): List<Song> {
        val base = originalQueue
        return if (_state.value.isReverseQueue) base.reversed() else base
    }

    fun pause() {
        pauseInternal(abandonFocus = true)
    }

    private fun pauseInternal(abandonFocus: Boolean) {
        cancelActiveDjTransitionAndPreload(restoreActiveVolume = true)
        progressJob?.cancel()
        progressJob = null
        if (abandonFocus) {
            abandonMusicAudioFocus()
        }
        var preservedPosition = _state.value.currentPositionMs
        try {
            activePlayer?.let { player ->
                if (_state.value.currentSong != null && player.mediaItemCount > 0) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    if (pos > 0L || player.playbackState == Player.STATE_READY) {
                        preservedPosition = pos
                    }
                }
                player.playWhenReady = false
                player.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing player: ${e.message}")
        }
        _state.value = _state.value.copy(
            isPlaying = false,
            currentPositionMs = preservedPosition
        )
        savePlaybackSnapshotAsync()
    }

    fun resume() {
        val song = _state.value.currentSong ?: return
        VideoPlaybackController.onMusicPlaybackStarted()
        requestMusicAudioFocus()
        _state.value = _state.value.copy(
            isPlaying = true,
            errorCode = null,
            errorMessage = null
        )
        val player = activePlayer
        if (player == null) {
            appContext?.let { ctx -> PlaybackService.startService(ctx) }
            return
        }
        try {
            currentActiveBaseGain = 1.0f
            applyCurrentDeckVolumes()
            when {
                player.mediaItemCount == 0 || player.playbackState == Player.STATE_IDLE -> {
                    val resumePos = _state.value.currentPositionMs.coerceAtLeast(0L)
                    player.setMediaItem(buildMediaItem(song), resumePos)
                    player.prepare()
                    player.playWhenReady = true
                    player.play()
                }
                player.playbackState == Player.STATE_ENDED -> {
                    lastDjTriggeredInstanceToken = -1L
                    player.seekTo(0L)
                    _state.value = _state.value.copy(currentPositionMs = 0L)
                    player.playWhenReady = true
                    player.play()
                }
                else -> {
                    val desiredPos = _state.value.currentPositionMs
                    if (desiredPos > 0L && player.currentPosition + 1500L < desiredPos) {
                        player.seekTo(desiredPos)
                    }
                    if (!player.playWhenReady) {
                        player.playWhenReady = true
                        player.play()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming player: ${e.message}")
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    fun handleAudioBecomingNoisy() {
        if (_state.value.isPlaying) {
            pause()
        }
    }
}
