package com.example

import com.example.autocorrect.AutoCorrectEngine
import com.example.model.KeyBorderStyle
import com.example.model.KeyboardSettings
import com.example.model.KeyboardThemePreset
import com.example.ui.keyboard.ThemePaletteResolver
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAutoCorrectTypos() {
        assertEquals("the", AutoCorrectEngine.checkAutoCorrection("teh"))
        assertEquals("don't", AutoCorrectEngine.checkAutoCorrection("dont"))
        assertEquals("I'm", AutoCorrectEngine.checkAutoCorrection("im"))
        assertEquals("receive", AutoCorrectEngine.checkAutoCorrection("recieve"))
        assertEquals("On my way!", AutoCorrectEngine.checkAutoCorrection("omw"))
    }

    @Test
    fun testWordSuggestions() {
        val suggestions = AutoCorrectEngine.getSuggestions("th", autoCorrect = true)
        assertTrue("Suggestions should not be empty", suggestions.isNotEmpty())
        assertTrue("Suggestions should contain words starting with th", suggestions.any { it.startsWith("th", ignoreCase = true) })
    }

    @Test
    fun testKeyboardSettingsDefaults() {
        val settings = KeyboardSettings()
        assertEquals(KeyBorderStyle.LIQUID_GLASS, settings.keyBorderStyle)
        assertEquals(KeyboardThemePreset.LIQUID_GLASS, settings.themePreset)
        assertTrue(settings.hapticEnabled)
        assertTrue(settings.autoCorrectEnabled)
    }

    @Test
    fun testThemePaletteResolver() {
        val liquidPalette = ThemePaletteResolver.resolvePalette(
            KeyboardThemePreset.LIQUID_GLASS,
            KeyBorderStyle.LIQUID_GLASS,
            0.35f
        )
        assertNotNull(liquidPalette)

        val amoledPalette = ThemePaletteResolver.resolvePalette(
            KeyboardThemePreset.AMOLED_BLACK,
            KeyBorderStyle.BORDERLESS,
            0.35f
        )
        assertNotNull(amoledPalette)
    }
}
