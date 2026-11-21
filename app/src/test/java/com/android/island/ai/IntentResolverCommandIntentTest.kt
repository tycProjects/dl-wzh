package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentResolverCommandIntentTest {
    @Test
    fun ordinaryConversationDoesNotQualifyAsDeviceCommand() {
        assertFalse(IntentResolver.looksLikeCommand("Coba jelasin dong"))
        assertFalse(IntentResolver.looksLikeCommand("Tolong jelaskan apa maksudnya"))
        assertFalse(IntentResolver.looksLikeCommand("Kenapa WhatsApp tidak bisa dibuka?"))
        assertFalse(IntentResolver.looksLikeCommand("Jelaskan cara membuka WhatsApp"))
        assertFalse(IntentResolver.looksLikeCommand("Bagaimana cara menyalakan Wi-Fi"))
        assertFalse(IntentResolver.looksLikeCommand("TikTok adalah aplikasi video pendek"))
        assertFalse(IntentResolver.looksLikeCommand("Kamu bisa membuka TikTok melalui ikon aplikasi"))
        assertFalse(IntentResolver.looksLikeCommand("Saya sedang menjelaskan cara scroll halaman"))
    }

    @Test
    fun explicitDeviceCommandsRemainRecognized() {
        assertTrue(IntentResolver.looksLikeCommand("Buka WhatsApp"))
        assertTrue(IntentResolver.looksLikeCommand("Tolong bukain WhatsApp"))
        assertTrue(IntentResolver.looksLikeCommand("Nyalakan Wi-Fi"))
        assertTrue(IntentResolver.looksLikeCommand("Scroll ke bawah"))
        assertTrue(IntentResolver.looksLikeCommand("Coba buka TikTok"))
        assertTrue(IntentResolver.looksLikeCommand("Aku mau buka YouTube lalu cari musik"))
    }

    @Test fun multiStepOpenAppGoalsDoNotUseDirectOpenShortcut() {
        assertFalse(IntentResolver.isSimpleOpenAppRequest("Buka tiktok lalu cari preset lalu enter lalu buka video pertama yg muncul"))
        assertFalse(IntentResolver.isSimpleOpenAppRequest("Buka YouTube kemudian cari musik"))
        assertTrue(IntentResolver.isSimpleOpenAppRequest("Buka TikTok"))
        assertTrue(IntentResolver.isSimpleOpenAppRequest("Tolong buka WhatsApp"))
    }
}
