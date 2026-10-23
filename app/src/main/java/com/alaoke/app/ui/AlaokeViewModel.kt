package com.alaoke.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alaoke.app.data.AlaokeRepository
import com.alaoke.app.data.FavoriteSong
import com.alaoke.app.data.HistoryItem
import com.alaoke.app.data.QueueItem
import com.alaoke.app.data.Singer
import com.alaoke.app.data.Song
import com.alaoke.app.ui.theme.PartyEvent
import com.alaoke.app.youtube.YouTubeSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A track currently loaded in the Karaoke Player, independent of what's still queued. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val youtubeVideoId: String?,
    val singerName: String? = null
)

class AlaokeViewModel(private val repository: AlaokeRepository) : ViewModel() {

    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }

    // ---- Song library ----
    private val _librarySearch = MutableStateFlow("")
    val librarySearch: StateFlow<String> = _librarySearch.asStateFlow()

    val songs: StateFlow<List<Song>> = repository.observeSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLibrarySearch(query: String) { _librarySearch.value = query }

    val filteredSongs: StateFlow<List<Song>> = repository.observeSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Favorites & history ----
    val favorites: StateFlow<List<FavoriteSong>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(title: String, artist: String, thumbnailUrl: String?, videoId: String?, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(title, artist, thumbnailUrl, videoId, currentlyFavorite)
        }
    }

    // ---- Singers ----
    val singers: StateFlow<List<Singer>> = repository.observeSingers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSinger(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addSinger(name.trim()) }
    }

    fun removeSinger(singer: Singer) {
        viewModelScope.launch { repository.removeSinger(singer) }
    }

    // ---- Party Queue ----
    val queue: StateFlow<List<QueueItem>> = repository.observeQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    fun addToQueue(title: String, artist: String, thumbnailUrl: String?, videoId: String?, singerName: String) {
        viewModelScope.launch {
            repository.addToQueue(title, artist, thumbnailUrl, videoId, singerName.ifBlank { "Guest" })
        }
    }

    fun removeFromQueue(item: QueueItem) {
        viewModelScope.launch { repository.removeFromQueue(item) }
    }

    fun reorderQueue(items: List<QueueItem>) {
        viewModelScope.launch { repository.reorderQueue(items) }
    }

    /** Plays a song immediately in the Karaoke Player without touching the queue. */
    fun playNow(title: String, artist: String, thumbnailUrl: String?, videoId: String?, singerName: String? = null) {
        _nowPlaying.value = NowPlaying(title, artist, thumbnailUrl, videoId, singerName)
        viewModelScope.launch { repository.recordPlayed(title, artist, thumbnailUrl, videoId) }
    }

    /** Pops the top of the queue into Now Playing (used by the queue screen's "Play" and the player's "Next"). */
    fun playNextFromQueue() {
        val next = queue.value.firstOrNull() ?: return
        _nowPlaying.value = NowPlaying(next.songTitle, next.artist, next.thumbnailUrl, next.youtubeVideoId, next.singerName)
        viewModelScope.launch {
            repository.recordPlayed(next.songTitle, next.artist, next.thumbnailUrl, next.youtubeVideoId)
            repository.removeFromQueue(next)
        }
    }

    fun clearQueue() {
        viewModelScope.launch { repository.clearQueue() }
    }

    // ---- Smart Party Mode ----
    private val _selectedEvent = MutableStateFlow<PartyEvent?>(null)
    val selectedEvent: StateFlow<PartyEvent?> = _selectedEvent.asStateFlow()

    fun selectEvent(event: PartyEvent?) { _selectedEvent.value = event }

    fun songsForEvent(event: PartyEvent) = repository.songsForEvent(event.tag)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- YouTube search ----
    private val _ytQuery = MutableStateFlow("")
    val ytQuery: StateFlow<String> = _ytQuery.asStateFlow()

    private val _ytResults = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val ytResults: StateFlow<List<YouTubeSearchResult>> = _ytResults.asStateFlow()

    private val _ytLoading = MutableStateFlow(false)
    val ytLoading: StateFlow<Boolean> = _ytLoading.asStateFlow()

    private val _ytError = MutableStateFlow<String?>(null)
    val ytError: StateFlow<String?> = _ytError.asStateFlow()

    fun setYtQuery(query: String) { _ytQuery.value = query }

    fun searchYouTube() {
        val query = _ytQuery.value
        if (query.isBlank()) return
        viewModelScope.launch {
            _ytLoading.value = true
            _ytError.value = null
            repository.searchYouTube(query)
                .onSuccess { _ytResults.value = it }
                .onFailure { _ytError.value = it.message ?: "Search failed" }
            _ytLoading.value = false
        }
    }
}
