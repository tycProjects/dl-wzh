package com.android.island.ai.voice

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/** Factory for [AiAssistSession] when the system invokes the digital assistant. */
class AiAssistSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = AiAssistSession(this)
}
