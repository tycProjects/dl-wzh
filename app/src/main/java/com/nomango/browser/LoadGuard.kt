package com.nomango.browser

/**
 * Decides when a page load has ended for good or has got stuck.
 * It holds no Android classes, so the rules are easy to read and to test.
 *
 * A load starts with [begin] and ends with [end]. Everything that can end a load (page finished, error, stop,
 * a cancelled navigation, a download taking over, a time-out) must end up calling [end].
 */
class LoadGuard {
    var loading = false
    private var startedAt = 0L
    private var lastActivity = 0L

    /** A load, or a redirect of it, began. */
    fun begin(now: Long) {
        if (!loading) startedAt = now
        loading = true
        lastActivity = now
    }

    /** Something happened (progress, title), so the load is not stuck. */
    fun activity(now: Long) {
        lastActivity = now
    }

    fun end() {
        loading = false
    }

    /** 0 = fine, 1 = nothing happened for [STUCK_MS], 2 = busy for [MAX_MS] in total. */
    fun verdict(now: Long): Int {
        if (!loading) return 0
        if (now - lastActivity >= STUCK_MS) return 1
        if (now - startedAt >= MAX_MS) return 2
        return 0
    }

    companion object {
        const val STUCK_MS = 30000L
        const val MAX_MS = 90000L
    }
}
