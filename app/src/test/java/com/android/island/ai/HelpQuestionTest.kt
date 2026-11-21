package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpQuestionTest {
    @Test
    fun detectsWhatCanYouDo() {
        assertTrue(AiCommandRouter.isHelpQuestion("kamu bisa apa aja?"))
        assertTrue(AiCommandRouter.isHelpQuestion("Perintah apa saja yang tersedia?"))
    }

    @Test
    fun ignoresOrdinaryRequests() {
        assertFalse(AiCommandRouter.isHelpQuestion("buka WhatsApp"))
    }
}
