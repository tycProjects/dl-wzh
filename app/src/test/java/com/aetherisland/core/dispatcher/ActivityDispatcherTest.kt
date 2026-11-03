package com.aetherisland.core.dispatcher

import com.aetherisland.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ActivityDispatcherTest {
    private val dispatcher = ActivityDispatcher()

    @Test fun whatsappDoesNotEraseMediaSnapshot() {
        val media = MediaSnapshot("player", "Track", "Artist", 180_000, 42_000, true)
        val before = IslandState(activity = IslandActivity.MEDIA, mediaSnapshot = media)
        val after = dispatcher.onWhatsAppArrived(before)
        assertEquals(media, after.mediaSnapshot)
        assertTrue(after.whatsappBannerVisible)
        assertEquals(IslandActivity.WHATSAPP, dispatcher.primaryActivity(after))
    }

    @Test fun dismissingWhatsappReturnsToMedia() {
        val media = MediaSnapshot("player", "Track", "Artist", 180_000, 42_000, true)
        val state = IslandState(mediaSnapshot = media, whatsappBannerVisible = true)
        val after = dispatcher.onWhatsAppDismissed(state)
        assertEquals(IslandActivity.MEDIA, dispatcher.primaryActivity(after))
    }

    @Test fun noMediaMeansNoIslandActivity() {
        assertEquals(IslandActivity.NONE, dispatcher.primaryActivity(IslandState()))
    }
}
