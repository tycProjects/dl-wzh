package com.game.play.data.verifier

import org.junit.Assert.assertEquals
import org.junit.Test

class ApkVerifierTest {
    @Test fun checkStateHasThreeOutcomes() { assertEquals(3, CheckState.entries.size) }
    @Test fun extensionComparisonIsCaseInsensitive() { assertEquals(true, "GAME.APK.ROP".endsWith(".apk.ROP", ignoreCase = true)) }
}
