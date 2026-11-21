package com.android.island.ai

/** Only classifies a known model tool-call incompatibility; it does not calculate quotas or waits. */
object RateLimitPolicy {
    fun isToolCallGlitch(status: Int, message: String?): Boolean =
        status == 400 && message.orEmpty().contains("tool choice is none", ignoreCase = true)
}
