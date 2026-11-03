package com.aetherisland.feature.media

import com.aetherisland.core.model.MediaSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the latest media snapshot independently from transient notification presentation. */
class MediaStateStore {
    private val _snapshot = MutableStateFlow<MediaSnapshot?>(null)
    val snapshot: StateFlow<MediaSnapshot?> = _snapshot.asStateFlow()

    fun update(value: MediaSnapshot?) { _snapshot.value = value }
}
