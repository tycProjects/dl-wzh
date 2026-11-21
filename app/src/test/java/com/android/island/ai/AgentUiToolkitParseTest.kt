package com.android.island.ai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentUiToolkitParseTest {
    @Test fun parsesPressEnterTapAndClickIndex() {
        val actions = IntentResolver.actionsFrom(JSONArray()
            .put(JSONObject().put("type", "press_enter"))
            .put(JSONObject().put("type", "tap").put("x", 540).put("y", 210))
            .put(JSONObject().put("type", "click_index").put("index", 4)))
        assertEquals(AiAction.PressEnter, actions[0])
        assertEquals(AiAction.TapXY(540, 210), actions[1])
        assertEquals(AiAction.ClickIndex(4), actions[2])
    }

    @Test fun clickWithIndexAndNoTextBecomesClickIndex() {
        val actions = IntentResolver.actionsFrom(JSONArray()
            .put(JSONObject().put("type", "click").put("index", 2))
            .put(JSONObject().put("type", "click").put("text", "Cari")))
        assertEquals(AiAction.ClickIndex(2), actions[0])
        assertTrue(actions[1] is AiAction.Click)
    }

    @Test fun incompleteOrNegativeUiActionsAreDropped() {
        val actions = IntentResolver.actionsFrom(JSONArray()
            .put(JSONObject().put("type", "tap").put("x", 10))
            .put(JSONObject().put("type", "tap").put("x", -5).put("y", 20))
            .put(JSONObject().put("type", "click_index")))
        assertTrue(actions.isEmpty())
    }
}
