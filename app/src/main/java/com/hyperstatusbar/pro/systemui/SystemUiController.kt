package com.hyperstatusbar.pro.systemui

import com.hyperstatusbar.pro.data.BarSettings
import com.hyperstatusbar.pro.root.RootManager
import org.json.JSONObject

class SystemUiController(private val root: RootManager) {
    /** Публикует настройки для backend-модуля. Само по себе не меняет ресурсы SystemUI. */
    suspend fun publish(settings: BarSettings): Result<String> {
        val json = JSONObject().apply {
            put("schema", 1)
            put("leftOffsetPx", settings.leftOffsetPx)
            put("rightOffsetPx", settings.rightOffsetPx)
            put("iconSpacingDp", settings.iconSpacingDp)
            put("barHeightDp", settings.barHeightDp)
            put("opacity", settings.opacity.toDouble())
            put("iconScale", settings.iconScale.toDouble())
            put("clockScale", settings.clockScale.toDouble())
            put("batteryScale", settings.batteryScale.toDouble())
            put("networkScale", settings.networkScale.toDouble())
            put("iconStyle", settings.iconStyle)
            put("colorTheme", settings.colorTheme)
            put("activeProfile", settings.activeProfile)
        }.toString()
        return root.writeConfig(json)
    }
}
