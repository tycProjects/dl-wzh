package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberGuardTest {
    private val raw = "TOTAL PSS: 116631 KB\nGL mtrack 26088 KB\nNative Heap 35769 KB"

    @Test fun figuresFromTheOutputPass() {
        assertEquals(emptyList<String>(), NumberGuard.unverifiedFigures("Total PSS 116.631 KB, normal.", raw))
    }

    @Test fun inventedFiguresAreFlagged() {
        val fake = "Total PSS 80.710 KB dan GL 26.088 KB."
        assertEquals(listOf("80.710 KB"), NumberGuard.unverifiedFigures(fake, raw))
    }

    @Test fun acceptsMemoryFiguresConvertedFromDumpsysKilobytes() {
        val dumpsys = "Applications Memory Usage (in Kilobytes):\nTOTAL PSS: 90319 TOTAL RSS: 214113"
        assertEquals(emptyList<String>(), NumberGuard.unverifiedFigures(
            "PSS sekitar 88,2 MB dan RSS sekitar 209,1 MB.", dumpsys
        ))
    }

    @Test fun stillRejectsInventedMemoryFigures() {
        val dumpsys = "Applications Memory Usage (in Kilobytes):\nTOTAL PSS: 90319 TOTAL RSS: 214113"
        assertEquals(listOf("500 MB"), NumberGuard.unverifiedFigures("PSS mencapai 500 MB.", dumpsys))
    }

    @Test fun replyWithoutFiguresPasses() {
        assertEquals(emptyList<String>(), NumberGuard.unverifiedFigures("Memori Anda terlihat normal.", raw))
    }

    @Test fun pidMustAlsoMatch() {
        assertTrue(NumberGuard.unverifiedFigures("PID 99999 berjalan", "PID 27537").isNotEmpty())
    }
}
