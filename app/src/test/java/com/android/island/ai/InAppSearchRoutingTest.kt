package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppSearchRoutingTest {
    @Test fun searchInNamedAppIsNotAWebSearch() {
        assertTrue(AiCommandRouter.isInAppSearch("carikan kucing lucu di YouTube"))
        assertTrue(AiCommandRouter.isInAppSearch("cari preset di tiktok"))
    }

    @Test fun generalTopicSearchStaysWeb() {
        assertFalse(AiCommandRouter.isInAppSearch("carikan berita tentang gibran terbaru"))
        assertFalse(AiCommandRouter.isInAppSearch("cari tentang cara masak rendang"))
    }
}
