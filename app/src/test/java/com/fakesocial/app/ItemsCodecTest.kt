package com.fakesocial.app

import com.fakesocial.app.data.ChatItem
import com.fakesocial.app.data.ItemsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemsCodecTest {
    @Test
    fun emptyListRoundTrips() {
        assertEquals("", ItemsCodec.encode(emptyList()))
        assertTrue(ItemsCodec.decode("").isEmpty())
    }

    @Test
    fun roundTripKeepsAllFieldsIncludingEmptyOnes() {
        val items = listOf(
            ChatItem("", "Hello, world", false, "9:38"),
            ChatItem("Sam", "", true, ""),
            ChatItem("", "", false, "")
        )
        assertEquals(items, ItemsCodec.decode(ItemsCodec.encode(items)))
    }

    @Test
    fun textWithNewlinesCommasAndEmojiSurvives() {
        val items = listOf(ChatItem("Ali", "line1\nline2, with \"quotes\" 😍 مرحبا", true, "1h"))
        assertEquals(items, ItemsCodec.decode(ItemsCodec.encode(items)))
    }

    @Test
    fun separatorCharactersInInputAreSanitized() {
        val evil = ChatItem("a\u001Eb", "x\u001Fy", false, "t")
        val decoded = ItemsCodec.decode(ItemsCodec.encode(listOf(evil)))
        assertEquals(1, decoded.size)
        assertEquals("a b", decoded[0].author)
        assertEquals("x y", decoded[0].text)
    }

    @Test
    fun malformedRecordsAreSkipped() {
        assertTrue(ItemsCodec.decode("garbage-without-separators").isEmpty())
    }
}
