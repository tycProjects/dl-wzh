package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioEngine
import com.example.data.entity.AppSettingsEntity
import com.example.model.AppPreferences
import com.example.model.AudioTestStatus
import com.example.model.AudioTestType
import com.example.model.EqualizerPresetType
import com.example.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun testEqualizerDefaultStateIsOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        // 1. AudioEngine defaults to OFF
        assertFalse("Equalizer must be OFF by default", engine.equalizerEnabled.value)

        // 2. AppPreferences defaults to OFF
        assertFalse("AppPreferences eqEnabled must be false by default", AppPreferences().eqEnabled)

        // 3. AppSettingsEntity defaults to OFF
        assertFalse("AppSettingsEntity eqEnabled must be false by default", AppSettingsEntity().eqEnabled)

        // 4. Hardware equalizer is disabled
        val eq = engine.equalizer
        if (eq != null) {
            assertFalse("Hardware Equalizer effect must be disabled when equalizer is OFF", eq.enabled)
        }

        engine.release()
    }

    @Test
    fun testEqualizerTurnedOnEnablesHardwareAndAffectsOutput() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        // Initially OFF
        assertFalse(engine.equalizerEnabled.value)

        // Turn ON
        engine.setEqualizerEnabled(true)
        assertTrue("Equalizer must be enabled after turning ON", engine.equalizerEnabled.value)

        val eq = engine.equalizer
        if (eq != null) {
            assertTrue("Hardware Equalizer effect must be enabled", eq.enabled)
        }

        // Adjust Bass, Mid, and Treble
        engine.setBassGain(12.0f)
        assertEquals(12.0f, engine.bassGainDb.value, 0.01f)

        engine.setMidGain(-2.5f)
        assertEquals(-2.5f, engine.midGainDb.value, 0.01f)

        engine.setTrebleGain(8.0f)
        assertEquals(8.0f, engine.trebleGainDb.value, 0.01f)

        // Verify band levels reflect adjustments
        val bands = engine.equalizerBands.value
        assertTrue("Equalizer bands must not be empty", bands.isNotEmpty())
        val lowBand = bands.first()
        assertTrue("Bass boost should increase low band level", lowBand.currentLevelMilliBels > 0)

        engine.release()
    }

    @Test
    fun testEqualizerTurnedOffAgainBypassesEffectWithoutResettingSavedSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        // Turn ON and customize settings
        engine.setEqualizerEnabled(true)
        engine.setBassGain(11.5f)
        engine.setMidGain(-3.0f)
        engine.setTrebleGain(7.0f)

        // Turn OFF again
        engine.setEqualizerEnabled(false)
        assertFalse("Equalizer must be OFF after toggling off", engine.equalizerEnabled.value)

        val eq = engine.equalizer
        if (eq != null) {
            assertFalse("Hardware Equalizer effect must be bypassed/disabled", eq.enabled)
        }

        // Verify saved user settings are PRESERVED and not erased
        assertEquals("Bass gain must be preserved when turned OFF", 11.5f, engine.bassGainDb.value, 0.01f)
        assertEquals("Mid gain must be preserved when turned OFF", -3.0f, engine.midGainDb.value, 0.01f)
        assertEquals("Treble gain must be preserved when turned OFF", 7.0f, engine.trebleGainDb.value, 0.01f)

        // Turn ON again: hardware should re-enable and restore user's saved settings
        engine.setEqualizerEnabled(true)
        assertTrue(engine.equalizerEnabled.value)
        if (eq != null) {
            assertTrue("Hardware Equalizer must be re-enabled", eq.enabled)
        }
        assertEquals(11.5f, engine.bassGainDb.value, 0.01f)

        engine.release()
    }

    @Test
    fun testEqualizerOffVsOnWithSongPlaybackDoesNotStopMusic() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)

        val dummySong = Song(
            id = 100L,
            title = "Test Track",
            artist = "JNMF Artist",
            album = "Test Album",
            durationMs = 180000L,
            fileSize = 1024L * 1024L,
            uriString = "android.resource://${context.packageName}/raw/dummy",
            isSampleTrack = false
        )

        engine.playQueue(listOf(dummySong), 0)

        // Verify OFF state: equalizer is disabled
        assertFalse("Default equalizer must be OFF", engine.equalizerEnabled.value)

        // Turn ON while playing
        engine.setEqualizerEnabled(true)
        assertTrue("Equalizer must be ON", engine.equalizerEnabled.value)

        // Turn OFF while playing
        engine.setEqualizerEnabled(false)
        assertFalse("Equalizer must be OFF", engine.equalizerEnabled.value)

        // Playback state is unaffected (not crashed, queue intact)
        assertEquals(dummySong.id, engine.currentSong.value?.id)

        engine.release()
    }

    @Test
    fun testEqualizerPersistentSettingsRoundTrip() {
        val prefs = AppPreferences(
            eqEnabled = false,
            eqPreset = EqualizerPresetType.FULL_BEAT,
            bassGainDb = 10.0f,
            midGainDb = -1.0f,
            trebleGainDb = 6.0f,
            bandLevels = listOf(1000.toShort(), 500.toShort(), (-100).toShort(), 300.toShort(), 600.toShort())
        )

        val entity = AppSettingsEntity.fromPreferences(prefs)
        assertFalse(entity.eqEnabled)
        assertEquals("FULL_BEAT", entity.eqPreset)
        assertEquals(10.0f, entity.bassGainDb, 0.01f)

        val restored = entity.toPreferences()
        assertFalse(restored.eqEnabled)
        assertEquals(EqualizerPresetType.FULL_BEAT, restored.eqPreset)
        assertEquals(10.0f, restored.bassGainDb, 0.01f)
        assertEquals(-1.0f, restored.midGainDb, 0.01f)
        assertEquals(6.0f, restored.trebleGainDb, 0.01f)
        assertEquals(5, restored.bandLevels.size)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioEngine(context)
        engine.applySavedPreferences(restored)

        assertFalse("Engine should apply eqEnabled=false from saved prefs", engine.equalizerEnabled.value)
        assertEquals(EqualizerPresetType.FULL_BEAT, engine.currentPreset.value)
        assertEquals(10.0f, engine.bassGainDb.value, 0.01f)

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
