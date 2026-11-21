package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppMatcherTest {
    private val labels = listOf("Samsung Notes", "TikTok", "Auralys", "Camera", "Gallery", "WhatsApp")

    @Test fun indonesianNameMatchesEnglishLabelThroughAlias() {
        assertEquals(0, AppMatcher.match("catatan", labels))
    }

    @Test fun exactLabelMatchesIgnoringCaseAndSpaces() {
        assertEquals(1, AppMatcher.match("tik tok", labels))
        assertEquals(5, AppMatcher.match("WhatsApp", labels))
    }

    @Test fun longerPhraseContainingAppNameStillMatches() {
        assertEquals(2, AppMatcher.match("Auralys Music", labels))
    }

    @Test fun commonIndonesianNamesMapToApps() {
        assertEquals(3, AppMatcher.match("kamera", labels))
        assertEquals(4, AppMatcher.match("galeri", labels))
    }

    @Test fun unknownAppIsNull() {
        assertNull(AppMatcher.match("xyzzy", labels))
    }
}
