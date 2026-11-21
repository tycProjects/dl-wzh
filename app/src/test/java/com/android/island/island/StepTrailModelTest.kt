package com.android.island.island

import org.junit.Assert.assertEquals
import org.junit.Test

class StepTrailModelTest {
    @Test fun lastLineIsActiveWhileRunning() {
        val steps = StepTrailModel.from(listOf("Langkah 1: Membuka TikTok", "Langkah 2: Scroll"), running = true)
        assertEquals(StepTrailModel.State.DONE, steps[0].state)
        assertEquals(StepTrailModel.State.ACTIVE, steps[1].state)
    }
    @Test fun failedLineIsMarked() {
        val steps = StepTrailModel.from(listOf("Langkah 1: Tidak menemukan \"Putar\""), running = false)
        assertEquals(StepTrailModel.State.FAILED, steps[0].state)
    }
}
