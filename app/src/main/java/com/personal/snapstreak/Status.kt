package com.personal.snapstreak

/** Tiny bridge so the service can show progress in the app. Main thread only. */
object Status {
    var listener: ((String) -> Unit)? = null
    var last: String = "Idle."
        private set

    fun post(text: String) {
        last = text
        listener?.invoke(text)
    }
}
