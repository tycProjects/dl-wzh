package com.android.island.ai

import org.junit.Assert.*
import org.junit.Test

/** Regressions from the exported chat: the guard stopped "buka kontak ummi faris" at the search box. */
class AgentGuardRegressionTest {
    private val whatsapp = "Buka whatsapp lalu buka kontak ummi faris"

    @Test fun typingTheContactNameFromTheUserMessageIsAllowed() {
        assertTrue(UserIntentActionGuard.allows(AiAction.TypeText("Ummi Faris"), whatsapp))
    }

    @Test fun typingInventedTextIsStillRefused() {
        assertFalse(UserIntentActionGuard.allows(AiAction.TypeText("transfer sekarang"), "baca layar saja"))
    }

    @Test fun appHomeAndSearchTapAreNavigation() {
        assertTrue(UserIntentActionGuard.allows(AiAction.AppHome("WhatsApp"), whatsapp))
        assertTrue(UserIntentActionGuard.allows(AiAction.Click("Cari"), whatsapp))
    }

    @Test fun googleSearchMayUseChromeAsTheApp() {
        val text = "Cari di google mediator markaz quran"
        assertTrue(UserIntentActionGuard.allows(AiAction.SearchInApp("Google", "mediator markaz quran"), text))
        assertTrue(UserIntentActionGuard.allows(AiAction.SearchInApp("Chrome", "mediator markaz quran"), text))
        assertFalse(UserIntentActionGuard.allows(AiAction.SearchInApp("TikTok", "mediator markaz quran"), text))
    }

    @Test fun routerParsesGoogleAndYoutubeSearchesIntoOneAction() {
        assertEquals(
            listOf<AiAction>(AiAction.SearchInApp("Google", "mediator markaz quran")),
            AiCommandRouter.parseAll("Cari di google mediator markaz quran")
        )
        assertEquals(
            listOf<AiAction>(AiAction.SearchInApp("Youtube", "kucing lucu")),
            AiCommandRouter.parseAll("carikan kucing lucu di youtube")
        )
        assertEquals(
            listOf<AiAction>(AiAction.SearchInApp("Google", "resep nasi dan telur")),
            AiCommandRouter.parseAll("cari di google resep nasi dan telur")
        )
    }
}
