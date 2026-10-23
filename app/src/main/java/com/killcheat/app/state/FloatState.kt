package com.killcheat.app.state

object FloatState {
    var buttonSize: Int = 60
    var buttonOpacity: Int = 85

    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    fun notifyChange() = listeners.forEach { it() }
}
