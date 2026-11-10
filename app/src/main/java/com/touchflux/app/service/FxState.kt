package com.touchflux.app.service

/** Tiny in-process flag so everywhere-mode doesn't double up with the in-app playground. */
object FxState {
    @Volatile
    var appInForeground = false
}
