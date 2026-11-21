package com.android.island.service

/** Retry schedule for binding the notification listener. Pure logic so it can be unit tested. */
internal object RebindPolicy {
    /** Delay before each retry after the first immediate request. Total span: about 20 s, then it stops. */
    private val DELAYS_MS = longArrayOf(1_500L, 4_500L, 14_000L)

    /** Delay to wait after request number [step] (0 = the immediate one); null = no more retries. */
    fun delayAfter(step: Int): Long? = DELAYS_MS.getOrNull(step)
}
