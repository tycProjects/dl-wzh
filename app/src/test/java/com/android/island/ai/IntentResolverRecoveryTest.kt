package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentResolverRecoveryTest {
    @Test fun parsesOpenWhatsappAsValidatedOpenAppAction() {
        val actions = IntentResolver.parse("""{"actions":[{"type":"open_app","query":"WhatsApp"}]}""")
        assertEquals(listOf(AiAction.OpenApp("WhatsApp")), actions)
    }

    @Test fun parsesScreenObservationAndContactSearchActions() {
        val actions = IntentResolver.parse("""{"actions":[{"type":"read_screen"},{"type":"search_in_app","app":"WhatsApp","query":"Ummi Faris"}]}""")
        assertEquals(listOf(AiAction.ReadScreen, AiAction.SearchInApp("WhatsApp", "Ummi Faris")), actions)
    }

    @Test fun rejectsUnknownActionsInsteadOfExecutingThem() {
        assertEquals(null, IntentResolver.parse("""{"actions":[{"type":"run_anything","command":"x"}]}"""))
    }

    @Test fun recognizesLongMultiStepDeviceCommand() {
        assertTrue(IntentResolver.looksLikeCommand("Buka tiktok lalu cari preset lalu enter lalu tekan video pertama yang muncul."))
    }
}
