package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppLanguage
import com.example.data.ZPlace
import com.example.localization.ZStrings
import com.example.ui.theme.ZMapsAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies ZMAPS app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZMAPS", appName)
    }

    @Test
    fun `verify accent color lookup`() {
        assertEquals(ZMapsAccent.GREEN, ZMapsAccent.fromId("green"))
        assertEquals(ZMapsAccent.BLUE, ZMapsAccent.fromId("blue"))
        assertEquals(ZMapsAccent.RED, ZMapsAccent.fromId("red"))
        assertEquals(ZMapsAccent.WHITE, ZMapsAccent.fromId("white"))
        assertEquals(ZMapsAccent.CYAN, ZMapsAccent.fromId("cyan"))
        assertEquals(ZMapsAccent.VIOLET, ZMapsAccent.fromId("violet"))
        assertEquals(ZMapsAccent.GREEN, ZMapsAccent.fromId("unknown"))
    }

    @Test
    fun `verify multilingual translations support all 4 languages`() {
        assertEquals("Map", ZStrings.navMap(AppLanguage.ENGLISH))
        assertEquals("Mapa", ZStrings.navMap(AppLanguage.FILIPINO))
        assertEquals("Mapa", ZStrings.navMap(AppLanguage.CEBUANO))
        assertEquals("Peta", ZStrings.navMap(AppLanguage.INDONESIAN))

        assertEquals("By JUSSAR GOLDEN", ZStrings.authorCredits(AppLanguage.ENGLISH))
        assertEquals("Gawa ni JUSSAR GOLDEN", ZStrings.authorCredits(AppLanguage.FILIPINO))
        assertEquals("Gibuhat ni JUSSAR GOLDEN", ZStrings.authorCredits(AppLanguage.CEBUANO))
        assertEquals("Oleh JUSSAR GOLDEN", ZStrings.authorCredits(AppLanguage.INDONESIAN))
    }

    @Test
    fun `verify ZPlace model properties`() {
        val place = ZPlace(
            id = "test_1",
            name = "Cyber District",
            address = "Neon Boulevard",
            latitude = 14.5547,
            longitude = 121.0244,
            category = "transit"
        )
        assertEquals("Cyber District", place.name)
        assertNotNull(place.latLng)
        assertEquals(14.5547, place.latLng.latitude, 0.0001)
    }
}
