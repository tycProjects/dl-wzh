package com.android.island.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.island.core.DeviceProfile
import com.android.island.core.DeviceProfile.RenderTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Deterministic regression tests for the SettingsRepository initialization race.
 *
 * Determinism: the repository's scope runs on a single-thread executor whose only worker is parked
 * on [gate]. The init coroutine is therefore queued but cannot start, so update()/resetAll() —
 * which run on the test thread — are guaranteed to take the mutex first. Releasing [gate] then lets
 * the late init coroutine run, and every test asserts it changes nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsRepositoryInitRaceTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val lowEndKey = booleanPreferencesKey("low_end_mode")

    private lateinit var executor: ExecutorService
    private lateinit var gate: CountDownLatch
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        // The DataStore delegate is a process-wide singleton, so reset its contents per test.
        runBlocking { context.dataStore.edit { it.clear() } }
        context.getSharedPreferences("island_fast_flags", Context.MODE_PRIVATE).edit().clear().commit()
        executor = Executors.newSingleThreadExecutor()
        gate = CountDownLatch(1)
        executor.execute { gate.await() } // park the only worker
        scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())
    }

    @After
    fun tearDown() {
        gate.countDown()
        scope.cancel()
        executor.shutdownNow()
    }

    /** Lets the parked init coroutine (and the debounced persist, if any) run to completion. */
    private fun releaseInitAndDrain() {
        gate.countDown()
        runBlocking {
            withTimeout(10_000) { scope.coroutineContext.job.children.toList().forEach { it.join() } }
        }
    }

    private fun storedLowEnd(): Boolean? = runBlocking { context.dataStore.data.first()[lowEndKey] }

    @Test
    fun updateWinsRace_unrelatedChange_stillResolvesProfileAndBackfillsFlag() {
        DeviceProfile.init(context) // no fast flag entry -> UNRESOLVED
        assertEquals(RenderTier.UNRESOLVED, DeviceProfile.renderTierState)
        assertNull(SettingsRepository.readLowEndFastFlag(context))

        val repo = SettingsRepository(context, scope)
        runBlocking { repo.update { it.copy(hapticsEnabled = !it.hapticsEnabled) } } // lowEndMode unchanged

        // Init has not run: update() performed the first authoritative read.
        assertEquals(RenderTier.NORMAL, DeviceProfile.renderTierState) // before the fix: UNRESOLVED
        assertEquals(false, SettingsRepository.readLowEndFastFlag(context)) // before the fix: null

        releaseInitAndDrain()
        assertEquals(RenderTier.NORMAL, DeviceProfile.renderTierState)
        assertEquals(false, SettingsRepository.readLowEndFastFlag(context))
    }

    @Test
    fun updateWinsRace_storedLowEnd_resolvesToLowEnd() {
        runBlocking { context.dataStore.edit { it[lowEndKey] = true } }
        DeviceProfile.init(context)
        assertEquals(RenderTier.UNRESOLVED, DeviceProfile.renderTierState)

        val repo = SettingsRepository(context, scope)
        runBlocking { repo.update { it.copy(hapticsEnabled = !it.hapticsEnabled) } }

        assertEquals(RenderTier.LOW_END, DeviceProfile.renderTierState)
        assertEquals(true, SettingsRepository.readLowEndFastFlag(context))
        assertEquals(true, runBlocking { repo.settings.first().lowEndMode })

        releaseInitAndDrain()
        assertEquals(RenderTier.LOW_END, DeviceProfile.renderTierState)
        assertEquals(true, SettingsRepository.readLowEndFastFlag(context))
        assertEquals(true, storedLowEnd()) // the debounced persist kept the preference
    }

    @Test
    fun updateWinsRace_firstUpdateTogglesLowEnd_resolvesToToggledValue() {
        DeviceProfile.init(context)
        val repo = SettingsRepository(context, scope)
        runBlocking { repo.update { it.copy(lowEndMode = true) } }

        assertEquals(RenderTier.LOW_END, DeviceProfile.renderTierState)
        assertEquals(true, SettingsRepository.readLowEndFastFlag(context))

        releaseInitAndDrain()
        assertEquals(RenderTier.LOW_END, DeviceProfile.renderTierState)
        assertEquals(true, SettingsRepository.readLowEndFastFlag(context))
        assertEquals(true, storedLowEnd())
    }

    @Test
    fun validFastFlag_resolvesSynchronouslyAndSurvivesRace() {
        context.getSharedPreferences("island_fast_flags", Context.MODE_PRIVATE)
            .edit().putBoolean("low_end_mode", true).commit()
        runBlocking { context.dataStore.edit { it[lowEndKey] = true } }

        DeviceProfile.init(context) // fast path: resolved before any repository exists
        assertEquals(RenderTier.LOW_END, DeviceProfile.renderTierState)

        val repo = SettingsRepository(context, scope)
        runBlocking { repo.update { it.copy(hapticsEnabled = !it.hapticsEnabled) } }
        assertEquals(RenderTier.LOW_END, DeviceProfile.renderTierState)

        releaseInitAndDrain()
        assertEquals(RenderTier.LOW_END, DeviceProfile.renderTierState)
        assertEquals(true, SettingsRepository.readLowEndFastFlag(context))
    }

    @Test
    fun resetAllWinsRace_resolvesProfile_andLateInitDoesNotResurrectOldValue() {
        runBlocking { context.dataStore.edit { it[lowEndKey] = true } }
        DeviceProfile.init(context)
        assertEquals(RenderTier.UNRESOLVED, DeviceProfile.renderTierState)

        val repo = SettingsRepository(context, scope)
        runBlocking { repo.resetAll() }

        assertEquals(RenderTier.NORMAL, DeviceProfile.renderTierState)
        assertEquals(false, SettingsRepository.readLowEndFastFlag(context))

        releaseInitAndDrain() // late init sees live != null and must skip
        assertEquals(RenderTier.NORMAL, DeviceProfile.renderTierState)
        assertEquals(false, SettingsRepository.readLowEndFastFlag(context))
        assertEquals(false, runBlocking { repo.settings.first().lowEndMode })
        assertNull(storedLowEnd())
    }

    @Test
    fun flush_persistsModeAndFastFlagWithoutWaitingForDebounce() {
        DeviceProfile.init(context)
        val repo = SettingsRepository(context, scope) // scope is parked: the debounced persist cannot have run
        runBlocking {
            repo.update { it.copy(lowEndMode = true) }
            repo.flush()
        }
        assertEquals(true, storedLowEnd())
        assertEquals(true, SettingsRepository.readLowEndFastFlag(context))
    }
}
