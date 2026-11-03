package com.aetherisland.platform.notification

import org.junit.Assert.*
import org.junit.Test

class WhatsAppPackageFilterTest {
    @Test fun acceptsOnlyWhatsAppPackages() {
        assertTrue(WhatsAppPackageFilter.accepts("com.whatsapp"))
        assertTrue(WhatsAppPackageFilter.accepts("com.whatsapp.w4b"))
        assertFalse(WhatsAppPackageFilter.accepts("org.telegram.messenger"))
        assertFalse(WhatsAppPackageFilter.accepts("com.instagram.android"))
        assertFalse(WhatsAppPackageFilter.accepts("com.spotify.music"))
    }
}
