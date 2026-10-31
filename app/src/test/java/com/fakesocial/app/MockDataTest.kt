package com.fakesocial.app

import com.fakesocial.app.data.Kind
import com.fakesocial.app.data.MockData
import com.fakesocial.app.data.Platform
import com.fakesocial.app.data.toData
import com.fakesocial.app.data.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDataTest {
    @Test
    fun thereAreTenPlatforms() {
        assertEquals(10, Platform.entries.size)
    }

    @Test
    fun everyPlatformHasUsableDefaults() {
        Platform.entries.forEach { p ->
            val d = MockData.default(p)
            assertTrue("${p.name} has items", d.items.isNotEmpty())
            assertTrue("${p.name} has name", d.name.isNotBlank())
            if (p.kind == Kind.CHAT) assertTrue(d.items.any { it.mine } && d.items.any { !it.mine })
        }
    }

    @Test
    fun watermarkIsOffByDefaultButCanBeEnabled() {
        assertFalse(MockData.default(Platform.WHATSAPP).watermark)
        assertTrue(MockData.default(Platform.WHATSAPP).copy(watermark = true).watermark)
    }

    @Test
    fun instagramDmVariantBehavesLikeChat() {
        val post = MockData.default(Platform.INSTAGRAM)
        assertEquals(Kind.POST, post.effectiveKind)
        assertEquals(Kind.CHAT, post.copy(variant = 1).effectiveKind)
    }

    @Test
    fun entityRoundTripPreservesData() {
        val original = MockData.default(Platform.TELEGRAM).copy(
            id = 7, avatarPath = "/tmp/a.jpg", dark = true, rtl = true, watermark = false, likes = 42
        )
        val back = original.toEntity(now = 123L).toData()
        assertEquals(original.copy(updatedAt = 123L), back)
    }

    @Test
    fun unknownPlatformNameFallsBack() {
        assertEquals(Platform.FACEBOOK, Platform.fromName("nope"))
        assertEquals(Platform.FACEBOOK, Platform.fromName(null))
    }
}
