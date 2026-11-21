package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickGuardTest {
    @Test fun destructiveLabelBlockedUnlessUserAskedForIt() {
        assertFalse(ClickGuard.allowed("Hapus", "buka galeri"))
        assertTrue(ClickGuard.allowed("Hapus", "hapus foto ini"))
    }
    @Test fun englishDestructiveAndFinancialLabelsAreAlsoBlocked() {
        assertFalse(ClickGuard.allowed("Delete account", "open settings"))
        assertFalse(ClickGuard.allowed("Pay now", "find the product"))
        assertFalse(ClickGuard.allowed("Send message", "open WhatsApp"))
        assertTrue(ClickGuard.allowed("Delete photo", "delete this photo"))
    }
    @Test fun ordinaryLabelAlwaysAllowed() {
        assertTrue(ClickGuard.allowed("Putar", "putar lagu"))
    }
}
