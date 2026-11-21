package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageResponseParserTest {
    @Test fun openaiBase64IsDecoded() {
        assertEquals("hello", String(ImageResponseParser.openai("""{"data":[{"b64_json":"aGVsbG8="}]}""")!!))
    }
    @Test fun geminiInlineDataIsDecoded() {
        val json = """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"image/png","data":"aGVsbG8="}}]}}]}"""
        assertEquals("hello", String(ImageResponseParser.gemini(json)!!))
    }
    @Test fun missingDataIsNull() {
        assertNull(ImageResponseParser.openai("""{"data":[]}"""))
        assertNull(ImageResponseParser.gemini("""{"candidates":[]}"""))
    }
}
