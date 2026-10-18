package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StudioMode
import com.example.data.repository.CreativeEngineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OmniStudio AI", appName)
    }

    @Test
    fun `repository loads initial creations`() {
        val repo = CreativeEngineRepository()
        val creations = repo.creations.value
        assertTrue("Initial creations should not be empty", creations.isNotEmpty())
        assertTrue("Contains game item", creations.any { it.mode == StudioMode.GAMES })
        assertTrue("Contains app item", creations.any { it.mode == StudioMode.APPS })
        assertTrue("Contains web item", creations.any { it.mode == StudioMode.WEBSITES })
    }

    @Test
    fun `generate single game item creates playable game content`() = runTest {
        val repo = CreativeEngineRepository()
        val game = repo.generateSingle(StudioMode.GAMES, "لعبة سباق نيون", "أركيد")
        assertNotNull(game)
        assertTrue(game.content.contains("canvas") || game.content.contains("CYBER"))
    }
}
