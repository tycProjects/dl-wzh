package com.android.island.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IslandTerminalTest {
    @Test
    fun standaloneCdReturnsTarget() {
        assertEquals("/sdcard/Download", IslandTerminal.cdTarget("cd /sdcard/Download"))
        assertEquals("", IslandTerminal.cdTarget("cd"))
    }

    @Test
    fun nonCdOrCompoundIsNotTracked() {
        assertNull(IslandTerminal.cdTarget("ls /sdcard"))
        assertNull(IslandTerminal.cdTarget("cd /x && ls"))
        assertNull(IslandTerminal.cdTarget("df -h; cd /x"))
    }
}
