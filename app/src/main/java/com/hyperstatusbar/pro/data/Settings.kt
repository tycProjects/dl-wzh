package com.hyperstatusbar.pro.data

data class BarSettings(
    val leftOffsetPx: Int = 0,
    val rightOffsetPx: Int = 0,
    val iconSpacingDp: Int = 6,
    val barHeightDp: Int = 28,
    val opacity: Float = 1f,
    val iconScale: Float = 1f,
    val clockScale: Float = 1f,
    val batteryScale: Float = 1f,
    val networkScale: Float = 1f,
    val iconStyle: String = "iOS",
    val colorTheme: String = "Mono",
    val activeProfile: String = "Daily"
)
