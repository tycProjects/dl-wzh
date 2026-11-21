package com.android.island.ai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentGestureActionTest {
    @Test fun distinguishesSwipeFromScroll() {
        val actions = IntentResolver.actionsFrom(JSONArray()
            .put(JSONObject().put("type", "scroll").put("direction", "DOWN"))
            .put(JSONObject().put("type", "swipe").put("direction", "LEFT")))
        assertTrue(actions[0] is AiAction.Scroll)
        assertEquals(AiAction.Swipe.Direction.LEFT, (actions[1] as AiAction.Swipe).direction)
    }

    @Test fun parsesSearchScrollAndTouchGestures() {
        val actions = IntentResolver.actionsFrom(JSONArray()
            .put(JSONObject().put("type", "scroll_to").put("text", "preset gratis").put("max_swipes", 9))
            .put(JSONObject().put("type", "long_press").put("text", "Video"))
            .put(JSONObject().put("type", "drag").put("from", "Item A").put("to", "Folder"))
            .put(JSONObject().put("type", "pinch").put("zoom_in", false)))
        assertTrue(actions[0] is AiAction.ScrollTo)
        assertTrue(actions[1] is AiAction.LongPress)
        assertTrue(actions[2] is AiAction.Drag)
        assertEquals(false, (actions[3] as AiAction.Pinch).zoomIn)
    }
}
