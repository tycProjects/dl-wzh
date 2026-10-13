package com.flipper.app

/**
 * Operasi scan/tulis yang sama dibutuhkan baik lewat jalur ROOT (MemEngine,
 * via su + mem_engine.c) maupun jalur TANPA ROOT (MemEngineSocket, via
 * VirtualXposed + InProcessEngine.kt). OverlayService memakai interface ini
 * supaya tidak perlu tahu/peduli jalur mana yang sedang dipakai.
 */
interface MemBackend {
    fun scanNew(type: String, value: String): Int?
    fun scanFilter(type: String, op: String, value: String? = null): Int?
    fun scanCount(): Int?
    fun scanPage(offset: Int, limit: Int): List<MemEngine.ScanRow>
    fun scanReset()
    fun writeValue(addrHex: String, type: String, value: String): Boolean
    fun close()
}
