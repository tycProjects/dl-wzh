package com.android.island.ai

import org.junit.Assert.*
import org.junit.Test

class AgentCommandPrefixTest {
    @Test fun clearPhoneCommandIsRoutedToAgentWithoutDot() {
        val parsed = AgentCommandPrefix.parse("buka TikTok lalu cari preset")
        assertTrue(parsed.isAgentRequest)
        assertTrue(parsed.autoDetected)
        assertEquals("buka TikTok lalu cari preset", parsed.command)
    }

    @Test fun reportedMessagesNoLongerBecomePythonScripts() {
        assertTrue(AgentCommandPrefix.parse("Cari di google mediator markaz quran").isAgentRequest)
        assertTrue(AgentCommandPrefix.parse("Buka whatsapp lalu buka kontak ummi faris").isAgentRequest)
    }

    @Test fun dotPrefixStillForcesAgent() {
        val parsed = AgentCommandPrefix.parse(". buka TikTok lalu cari preset lalu tekan Enter")
        assertTrue(parsed.isAgentRequest)
        assertFalse(parsed.autoDetected)
        assertEquals("buka TikTok lalu cari preset lalu tekan Enter", parsed.command)
    }

    @Test fun chatEscapeForcesOrdinaryChat() {
        val parsed = AgentCommandPrefix.parse("/chat buka whatsapp")
        assertFalse(parsed.isAgentRequest)
        assertEquals("buka whatsapp", parsed.command)
    }

    @Test fun attachmentsDoNotEnableAgent() {
        assertFalse(AgentCommandPrefix.parse(". buka TikTok", hasAttachments = true).isAgentRequest)
        assertFalse(AgentCommandPrefix.parse("buka TikTok", hasAttachments = true).isAgentRequest)
    }

    @Test fun loneDotIsNotACommand() {
        assertFalse(AgentCommandPrefix.parse(". ").isAgentRequest)
    }

    @Test fun ordinaryChatStaysChat() {
        assertFalse(AgentCommandPrefix.parse("Halo").isAgentRequest)
        assertFalse(AgentCommandPrefix.parse("Jelaskan cara membuka WhatsApp").isAgentRequest)
        assertFalse(AgentCommandPrefix.parse("Cari tahu sejarah Mesir kuno").isAgentRequest)
        assertFalse(AgentCommandPrefix.parse("Buatkan script python untuk buka whatsapp").isAgentRequest)
        assertFalse(AgentCommandPrefix.parse("Apakah bisa buka whatsapp lewat kode?").isAgentRequest)
    }
}
