package com.aetherisland.core.dispatcher

import com.aetherisland.core.model.IslandActivity
import com.aetherisland.core.model.IslandState

/** WhatsApp is a transient presentation; it never overwrites the saved media snapshot. */
class ActivityDispatcher {
    fun onMediaChanged(state: IslandState, hasRelevantSession: Boolean): IslandState =
        state.copy(activity = if (hasRelevantSession) IslandActivity.MEDIA else IslandActivity.NONE)

    fun onWhatsAppArrived(state: IslandState): IslandState =
        state.copy(whatsappBannerVisible = true)

    fun onWhatsAppDismissed(state: IslandState): IslandState =
        state.copy(whatsappBannerVisible = false)

    fun primaryActivity(state: IslandState): IslandActivity = when {
        state.whatsappBannerVisible -> IslandActivity.WHATSAPP
        state.mediaSnapshot != null -> IslandActivity.MEDIA
        else -> IslandActivity.NONE
    }
}
