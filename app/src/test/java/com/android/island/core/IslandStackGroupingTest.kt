package com.android.island.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IslandStackGroupingTest {
    private fun chat(id: String, pkg: String) =
        IslandActivity.WhatsApp(id = id, senderName = "s$id", message = "m$id", packageName = pkg)

    @Test
    fun stackOnlyContainsTheCurrentApp() {
        val a1 = chat("1", "com.a"); val a2 = chat("2", "com.a"); val b1 = chat("3", "com.b")
        val state = IslandUiState(current = a2, whatsAppItems = listOf(a1, b1, a2))
        assertEquals(listOf("1", "2"), state.stackItems().map { it.id })
    }

    @Test
    fun otherAppsKeepTheirOwnStack() {
        val a1 = chat("1", "com.a"); val b1 = chat("3", "com.b"); val b2 = chat("4", "com.b")
        val state = IslandUiState(current = b2, whatsAppItems = listOf(a1, b1, b2))
        assertEquals(listOf("3", "4"), state.stackItems().map { it.id })
    }

    @Test
    fun nonChatCurrentFallsBackToWholeQueue() {
        val a1 = chat("1", "com.a")
        val state = IslandUiState(current = null, whatsAppItems = listOf(a1))
        assertEquals(1, state.stackItems().size)
    }
}
