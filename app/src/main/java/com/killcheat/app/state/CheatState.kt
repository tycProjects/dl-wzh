package com.killcheat.app.state

object CheatState {
    @Volatile var isTeleportEnabled: Boolean = false
    @Volatile var isFreezeEnabled: Boolean = false
    @Volatile var isGhostEnabled: Boolean = false
    @Volatile var targetGame: String = "ff" // "ff" | "ffmax"
}
