package com.android.island.ai.voice

import android.service.voice.VoiceInteractionService

/**
 * Registers AI Island as a system Digital assistant app.
 * Once the user picks "AI Island" under Settings > Apps > Default apps >
 * Digital assistant app (or One UI side-key assistant), long-press / assist
 * gestures land in [AiAssistSession].
 *
 * The meta-data in res/xml/voice_interaction_service.xml must set
 * supportsAssist="true" or the system will not list this service.
 */
class AiVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }

    override fun onShutdown() {
        super.onShutdown()
    }
}
