package com.android.island.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UiPrefsTest {
    @Test fun sameSetupGivesSameFingerprint() {
        val a = UiPrefs.aiFingerprint("groq", "https://x/v1", "llama", "key")
        assertEquals(a, UiPrefs.aiFingerprint("groq", "https://x/v1", "llama", "key"))
    }

    @Test fun changingModelOrKeyInvalidatesVerification() {
        val a = UiPrefs.aiFingerprint("groq", "https://x/v1", "llama", "key")
        assertNotEquals(a, UiPrefs.aiFingerprint("groq", "https://x/v1", "other", "key"))
        assertNotEquals(a, UiPrefs.aiFingerprint("groq", "https://x/v1", "llama", "key2"))
    }

    @Test fun fingerprintNeverContainsTheKey() {
        assertFalse(UiPrefs.aiFingerprint("groq", "u", "m", "SECRET-KEY-123").contains("SECRET"))
    }
}
