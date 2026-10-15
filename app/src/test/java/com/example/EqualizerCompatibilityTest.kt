package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioEngine
import com.example.model.AudioTestType
import com.example.model.AudioTestStatus
import com.example.model.EqualizerPresetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EqualizerCompatibilityTest {

    @Test
    fun testAudioEngineInitializesWithValidSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        val activeSession = engine.getActiveAudioSessionId()
        assertTrue("Active session ID should be valid (> 0)", activeSession >= 0)

        val status = engine.engineStatus.value
        assertEquals("JNMF INTERNAL PLAYER", status.playback)
        assertNotNull(status.outputDevice)

        engine.release()
    }

    @Test
    fun testStrictEqualizerCompatibilityCheck() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        val report = engine.runStrictEqualizerCompatibilityCheck()
        assertNotNull("Report should not be null", report)
        assertTrue("Report step logs should not be empty", report.stepLogs.isNotEmpty())
        assertTrue("Should have 5 frequency bands by default or hardware", report.bandsCount >= 5)

        engine.release()
    }

    @Test
    fun testIndoRemixDefaultPresetGains() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        engine.setPreset(EqualizerPresetType.INDO_REMIX)
        val bands = engine.equalizerBands.value
        assertTrue("Bands should not be empty", bands.isNotEmpty())

        // Indo remix sub-bass punch should be +8.5 dB (850 mB)
        val lowBand = bands.firstOrNull()
        assertNotNull(lowBand)
        assertEquals(850.toShort(), lowBand?.currentLevelMilliBels)

        engine.release()
    }

    @Test
    fun testBandLevelClampingDoesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        // Try extreme positive level
        engine.setBandLevel(0.toShort(), 3000.toShort())
        var band = engine.equalizerBands.value.first { it.bandIndex == 0.toShort() }
        assertTrue("Level should be clamped to max allowable level", band.currentLevelMilliBels <= 1500)

        // Try extreme negative level
        engine.setBandLevel(0.toShort(), (-3000).toShort())
        band = engine.equalizerBands.value.first { it.bandIndex == 0.toShort() }
        assertTrue("Level should be clamped to min allowable level", band.currentLevelMilliBels >= -1500)

        engine.release()
    }

    @Test
    fun testThreeBandShelfGains() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        engine.setBassGain(12.0f)
        assertEquals(12.0f, engine.bassGainDb.value, 0.01f)

        engine.setMidGain(-3.0f)
        assertEquals(-3.0f, engine.midGainDb.value, 0.01f)

        engine.setTrebleGain(9.5f)
        assertEquals(9.5f, engine.trebleGainDb.value, 0.01f)

        engine.release()
    }

    @Test
    fun testAudioEffectDiagnosticSuiteNeverCrashes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        AudioTestType.entries.forEach { testType ->
            val result = engine.testAudioEffect(testType)
            assertTrue("Result should be PASS or FALLBACK_ACTIVE",
                result == AudioTestStatus.PASS || result == AudioTestStatus.FALLBACK_ACTIVE)
        }

        engine.release()
    }
}
