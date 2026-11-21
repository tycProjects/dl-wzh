package com.android.island.ai

import com.android.island.service.RebindPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RebindPolicyTest {
    @Test fun retriesThreeTimesThenStops() {
        assertEquals(1_500L, RebindPolicy.delayAfter(0))
        assertEquals(4_500L, RebindPolicy.delayAfter(1))
        assertEquals(14_000L, RebindPolicy.delayAfter(2))
        assertNull(RebindPolicy.delayAfter(3))
    }
}
