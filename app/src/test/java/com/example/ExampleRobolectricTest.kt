package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.SampleOrganelles
import com.example.model.TonicityState
import com.example.model.YeltiPlatformLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Liquid Glass", appName)
    }

    @Test
    fun `verify tonicity volume ratios`() {
        assertTrue(TonicityState.HYPOTONIC.cellVolumeRatio > TonicityState.ISOTONIC.cellVolumeRatio)
        assertTrue(TonicityState.ISOTONIC.cellVolumeRatio > TonicityState.HYPERTONIC.cellVolumeRatio)
    }

    @Test
    fun `verify sample organelles and creator links loaded`() {
        assertTrue(SampleOrganelles.isNotEmpty())
        assertTrue(YeltiPlatformLinks.any { it.url.contains("gdyelti.tilda.ws") })
    }
}
