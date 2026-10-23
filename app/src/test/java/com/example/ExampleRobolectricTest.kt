package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AiProvider
import com.example.model.AppSettings
import com.example.service.SystemActionHelper
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
        assertEquals("ARAFAT AI", appName)
    }

    @Test
    fun `verify default settings`() {
        val settings = AppSettings()
        assertEquals(AiProvider.AUTO, settings.primaryProvider)
        assertEquals("gemini-3.5-flash", settings.geminiModel)
        assertEquals("grok-2-latest", settings.grokModel)
        assertTrue(settings.ttsEnabled)
    }

    @Test
    fun `verify voice command parser for hello arafat`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = SystemActionHelper(context)
        val result = helper.handleVoiceCommand("Hello Arafat") { }
        assertNotNull(result)
        assertTrue(result!!.responseText.contains("ARAFAT AI"))
        assertTrue(result.responseText.contains("ARAFAT MOJMUNDAR"))
    }
}
