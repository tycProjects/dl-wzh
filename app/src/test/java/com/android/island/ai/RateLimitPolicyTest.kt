package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimitPolicyTest {
    @Test
    fun recognizesOnlyKnownToolCallGlitch() {
        assertTrue(RateLimitPolicy.isToolCallGlitch(400, "Tool choice is none, but model called a tool"))
        assertFalse(RateLimitPolicy.isToolCallGlitch(400, "invalid json body"))
        assertFalse(RateLimitPolicy.isToolCallGlitch(429, "rate limit reached"))
    }
}
