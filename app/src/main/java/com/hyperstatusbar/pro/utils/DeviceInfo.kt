package com.hyperstatusbar.pro.utils

import android.os.Build

data class XiaomiDeviceInfo(val manufacturer: String, val model: String, val supported: Boolean) {
    companion object {
        fun detect(): XiaomiDeviceInfo {
            val manufacturer = Build.MANUFACTURER.orEmpty()
            val brand = Build.BRAND.orEmpty()
            val isXiaomi = manufacturer.contains("xiaomi", true) ||
                brand.contains("redmi", true) || brand.contains("poco", true)
            return XiaomiDeviceInfo(manufacturer, Build.MODEL.orEmpty(), isXiaomi)
        }
    }
}
