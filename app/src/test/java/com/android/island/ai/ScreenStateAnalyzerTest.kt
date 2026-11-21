package com.android.island.ai

import org.junit.Assert.*
import org.junit.Test

class ScreenStateAnalyzerTest {
    private fun el(i: Int, label: String, editable: Boolean = false, focused: Boolean = false) =
        ScreenElements.Element(i, label, editable, focused, 0, i * 100, 100, i * 100 + 80)

    @Test fun reportsSearchControl() {
        val hint = ScreenStateAnalyzer.hint(listOf(el(1, "Beranda"), el(2, "Cari")))
        assertTrue(hint.contains("elemen 2"))
    }

    @Test fun reportsFocusedSearchField() {
        val hint = ScreenStateAnalyzer.hint(listOf(el(1, "Cari kontak", editable = true, focused = true)))
        assertTrue(hint.contains("kolom pencarian"))
    }

    @Test fun deepPageTellsModelToGoToAppHome() {
        val hint = ScreenStateAnalyzer.hint(listOf(el(1, "Kirim"), el(2, "Beranda")))
        assertTrue(hint.contains("app_home"))
        assertTrue(hint.contains("elemen 2"))
    }

    @Test fun emptyScreenGivesNoHint() {
        assertEquals("", ScreenStateAnalyzer.hint(emptyList()))
    }
}
