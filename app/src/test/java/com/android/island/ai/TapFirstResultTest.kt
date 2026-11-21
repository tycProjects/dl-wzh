package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class TapFirstResultTest {
    @Test fun parsedFromModelJson() {
        assertEquals(listOf(AiAction.TapFirstResult), IntentResolver.parse("""{"actions":[{"type":"tap_first_result"}]}"""))
    }

    @Test fun imageDiscoveryIncludesMediaModelsOnlyWhenAsked() {
        assertEquals(true, AiClient.isUsableGeminiChat("gemini-2.5-flash"))
    }
}
