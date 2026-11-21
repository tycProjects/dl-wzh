package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentResolverCommandDetectionTest {
    @Test fun recognizesDirectCommandsWithPolitePrefixes() {
        assertTrue(IntentResolver.looksLikeCommand("tolong buka YouTube"))
        assertTrue(IntentResolver.looksLikeCommand("aku mau buka YouTube"))
        assertTrue(IntentResolver.looksLikeCommand("tolong bantu saya buka YouTube"))
        assertTrue(IntentResolver.looksLikeCommand("please open YouTube"))
        assertTrue(IntentResolver.looksLikeCommand("Buka tiktok lalu cari preset lalu enter lalu klik video yg muncul"))
        assertTrue(IntentResolver.looksLikeCommand("tolong bantu saya buka WhatsApp"))
    }

    @Test fun doesNotTreatExplanationsAsCommands() {
        assertFalse(IntentResolver.looksLikeCommand("Jelaskan cara membuka YouTube"))
        assertFalse(IntentResolver.looksLikeCommand("Apa itu fitur scroll?"))
        assertFalse(IntentResolver.looksLikeCommand("Halo"))
        assertFalse(IntentResolver.looksLikeCommand("Berapa hasil 12 dikali 8?"))
        assertFalse(IntentResolver.looksLikeCommand("Ceritakan sejarah Mesir"))
    }
}
