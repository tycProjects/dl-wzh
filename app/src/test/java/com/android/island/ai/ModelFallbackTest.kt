package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelFallbackTest {
    private val retired404 = "Endpoint atau model tidak ditemukan (HTTP 404). (This model models/gemini-2.5-pro is no longer available to new users. Please update your code to use models/gemini-2.5-flash for the latest features.)"

    @Test fun retiredMessageIsDetected() {
        assertTrue(ModelFallback.isRetired(retired404))
        assertFalse(ModelFallback.isRetired("Endpoint atau model tidak ditemukan (HTTP 404)."))
    }

    @Test fun suggestsTheReplacementNotTheCurrentModel() {
        assertEquals("gemini-2.5-flash", ModelFallback.suggest(retired404, "gemini-2.5-pro"))
    }

    @Test fun suggestsGemini38WhenGoogleExplicitlyRecommendsIt() {
        val message = "This model models/gemini-2.5-flash is no longer available to new users. Update to models/gemini-3.8-flash."
        assertEquals("gemini-3.8-flash", ModelFallback.suggest(message, "gemini-2.5-flash"))
    }

    @Test fun noSuggestionWhenMessageOnlyNamesCurrentModel() {
        assertNull(ModelFallback.suggest("(models/gemini-2.5-flash is retired)", "gemini-2.5-flash"))
    }

    @Test fun missingModelIsDetectedButNotAuthOrQuota() {
        assertTrue(ModelFallback.isMissing(404, "The model `gpt-x` does not exist or you do not have access to it."))
        assertTrue(ModelFallback.isMissing(404, "Endpoint atau model tidak ditemukan (HTTP 404). Periksa Base URL dan nama model."))
        assertTrue(ModelFallback.isMissing(400, "you must provide a model parameter"))
        assertFalse(ModelFallback.isMissing(401, "model not found"))
        assertFalse(ModelFallback.isMissing(429, "model quota exceeded"))
        assertFalse(ModelFallback.isMissing(404, "page missing"))
    }
}
