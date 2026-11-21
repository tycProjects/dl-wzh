package com.android.island.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class ImageClientErrorTest {
    @Test fun unverifiedOrganizationGetsClearStep() {
        val msg = ImageClient.errorMessage(403, "Your organization must be verified to use this model")
        assertTrue(msg.contains("terverifikasi"))
    }
    @Test fun rejectedKeyGetsSettingsHint() {
        assertTrue(ImageClient.errorMessage(401, "invalid key").contains("pengaturan"))
    }
}
