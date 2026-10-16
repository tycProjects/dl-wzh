package com.fakelag.ff

object NativeMemory {
    init {
        System.loadLibrary("native_memory")
    }

    external fun freezeRemoteEntities(pid: Int, enable: Boolean): Boolean
    external fun forceKill(pid: Int, damageThreshold: Int): Boolean
}
