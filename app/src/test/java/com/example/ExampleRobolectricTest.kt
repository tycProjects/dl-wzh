package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.MayaCommandAction
import com.example.domain.MayaEmotion
import com.example.ui.components.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Maya AI", appName)
    }

    @Test
    fun `command action and emotion parsing handles valid and unknown inputs`() {
        assertEquals(MayaCommandAction.FLASHLIGHT_ON, MayaCommandAction.fromString("FLASHLIGHT_ON"))
        assertEquals(MayaCommandAction.NONE, MayaCommandAction.fromString("UNKNOWN_CMD"))
        assertEquals(MayaEmotion.PLAYFUL, MayaEmotion.fromKey("playful"))
        assertEquals(MayaEmotion.AFFECTIONATE, MayaEmotion.fromKey(null))
    }

    @Test
    fun `formatBytes formats MB and GB accurately`() {
        assertEquals("0 MB", formatBytes(0L))
        assertEquals("512 MB", formatBytes(512L * 1024L * 1024L))
        assertEquals("2.0 GB", formatBytes(2L * 1024L * 1024L * 1024L))
    }
}
