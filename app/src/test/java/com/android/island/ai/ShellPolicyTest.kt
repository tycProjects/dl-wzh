package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellPolicyTest {
    private fun risk(c: String) = ShellPolicy.risk(c)

    @Test
    fun ordinaryCommandsRunFreely() {
        listOf(
            "df -h /data", "pm trim-caches 6G", "ps -A | grep system_server", "getprop ro.build.version.release",
            "ls -la /sdcard/Download", "cat /proc/meminfo | head -20", "rm /sdcard/old.txt", "cp a b && mv b c",
            "echo hi\nls", "input keyevent 66"
        ).forEach {
            assertEquals(it, ShellPolicy.Risk.FREE, risk(it))
            assertNull(it, ShellPolicy.refusal(it))
            assertFalse(it, ShellPolicy.needsConfirmation(it))
        }
    }

    @Test
    fun dumpsysAlwaysRunsWhateverTheKeywords() {
        listOf(
            "dumpsys meminfo com.whatsapp", "dumpsys gfxinfo com.whatsapp reset", "dumpsys gfxinfo com.x framestats",
            "dumpsys package com.fastboot.app", "dumpsys meminfo --reboot", "dumpsys activity | grep -i reboot",
            "dumpsys meminfo | grep rm", "dumpsys battery set level 50"
        ).forEach {
            assertEquals(it, ShellPolicy.Risk.FREE, risk(it))
            assertNull(it, ShellPolicy.refusal(it))
        }
        assertTrue(ShellPolicy.isDumpsys("dumpsys gfxinfo com.x"))
        assertFalse(ShellPolicy.isDumpsys("ls dumpsys"))
    }

    @Test
    fun fatalCommandsAreAlwaysRefused() {
        listOf(
            "rm -rf /", "rm -rf /*", "rm -rf /sdcard", "rm -rf /sdcard/*", "rm -Rf /storage/emulated/0",
            "mkfs.ext4 /dev/block/sda1", "wipefs -a /dev/sda", "dd if=/dev/zero of=/dev/block/mmcblk0",
            "echo x > /dev/block/sda", "sm partition disk:179,64 private", "fastboot erase userdata",
            "pm uninstall com.android.systemui", "pm disable-user --user 0 com.android.phone",
            "am broadcast -a android.intent.action.MASTER_CLEAR", "recovery --wipe_data",
            "dumpsys meminfo; rm -rf /data", "ls && mkfs /dev/x", "sh -c \"rm -rf /\"", "echo \$(rm -rf /data)",
            "nice -n 5 rm -rf /system", "/system/bin/rm -rf /vendor"
        ).forEach {
            assertEquals(it, ShellPolicy.Risk.FATAL, risk(it))
            assertNotNull(it, ShellPolicy.refusal(it))
        }
    }

    @Test
    fun riskyCommandsWaitForConfirmation() {
        listOf(
            "reboot", "svc power shutdown", "pm uninstall com.whatsapp", "pm clear com.whatsapp",
            "rm -rf /sdcard/Download/tmp", "settings put system screen_brightness 80", "chmod 777 /sdcard/a",
            "dumpsys meminfo; reboot", "su -c id"
        ).forEach {
            assertEquals(it, ShellPolicy.Risk.CONFIRM, risk(it))
            assertNull(it, ShellPolicy.refusal(it))
            assertTrue(it, ShellPolicy.needsConfirmation(it))
        }
    }

    @Test
    fun keywordsInArgumentsDoNotTrigger() {
        assertEquals(ShellPolicy.Risk.FREE, risk("grep reboot /sdcard/log.txt"))
        assertEquals(ShellPolicy.Risk.FREE, risk("echo fastboot mkfs recovery"))
        assertEquals(ShellPolicy.Risk.FREE, risk("ls /sdcard/uninstall"))
    }

    @Test
    fun rejectsEmptyOversizedAndNullBytes() {
        assertNotNull(ShellPolicy.refusal("   "))
        assertNotNull(ShellPolicy.refusal("x".repeat(4097)))
        assertNotNull(ShellPolicy.refusal("echo a\u0000b"))
        assertNull(ShellPolicy.refusal("x".repeat(4096)))
    }

    @Test
    fun slashShellPrefixForwardsCommand() {
        assertTrue(ShellPolicy.hasExplicitPrefix("/shell dumpsys meminfo com.android.island"))
        assertEquals("dumpsys meminfo com.android.island", ShellPolicy.stripPrefix("/shell dumpsys meminfo com.android.island"))
        assertTrue(ShellPolicy.hasExplicitPrefix("/shell"))
        assertFalse(ShellPolicy.hasExplicitPrefix("/shelldumpsys meminfo com.android.island"))
        assertEquals("/shelldumpsys meminfo com.android.island", ShellPolicy.stripPrefix("/shelldumpsys meminfo com.android.island"))
    }

    @Test
    fun slashShellRequiresSpaceAfterPrefix() {
        assertFalse(ShellPolicy.hasExplicitPrefix("/shellfoo bar"))
        assertFalse(ShellPolicy.hasExplicitPrefix("/shellfish"))
        assertFalse(ShellPolicy.hasExplicitPrefix("/shelldumpsys meminfo com.android.island"))
        assertEquals("/shelldumpsys meminfo com.android.island", ShellPolicy.stripPrefix("/shelldumpsys meminfo com.android.island"))
        assertFalse(ShellPolicy.hasExplicitPrefix("dumpsys meminfo"))
    }
}
