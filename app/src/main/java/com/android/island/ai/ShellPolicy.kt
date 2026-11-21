package com.android.island.ai

/**
 * Guard for AI-originated Shizuku shell commands. The terminal is meant to be used freely, so there
 * are only two filters:
 *  - FATAL: wipes or bricks the device. Always refused.
 *  - CONFIRM: risky but legitimate (reboot, uninstall, recursive delete, settings put). Needs "ya".
 * Everything else runs: pipes, grep, multi-line scripts, any dumpsys.
 *
 * Matching looks at the command word of each pipeline segment, never at arguments, so
 * `dumpsys package | grep reboot` or `dumpsys meminfo com.fastboot.app` is not mistaken for a
 * dangerous command. `sh -c "..."` and `eval` payloads are checked the same way.
 */
object ShellPolicy {
    enum class Risk { FREE, CONFIRM, FATAL }

    private const val MAX_LENGTH = 4096
    private val SEPARATORS = Regex("""[;&|\n(){}`]+""")
    private val WHITESPACE = Regex("""\s+""")
    private val NUMERIC = Regex("""^[\d.]+[smhd]?$""")
    private val WRAPPERS = setOf("sudo", "env", "nohup", "exec", "command", "busybox", "toybox", "time", "nice", "timeout", "xargs")
    private val INTERPRETERS = setOf("sh", "bash", "ash", "eval")

    private val FATAL_COMMANDS = setOf(
        "mke2fs", "make_ext4fs", "wipefs", "blkdiscard", "sgdisk", "fdisk", "parted", "fastboot", "recovery"
    )
    private val CONFIRM_COMMANDS = setOf(
        "reboot", "shutdown", "poweroff", "halt", "su", "magisk", "setenforce", "chmod", "chown", "mount", "umount"
    )
    private val PM_CHANGES = setOf("uninstall", "clear", "disable", "disable-user", "hide", "suspend")
    private val CRITICAL_PACKAGES = setOf(
        "android", "com.android.systemui", "com.android.phone", "com.android.settings",
        "com.android.providers.settings", "moe.shizuku.privileged.api"
    )
    private val PROTECTED_ROOTS = setOf(
        "", "/", "*", "~", "/data", "/system", "/vendor", "/product", "/odm", "/boot", "/storage",
        "/storage/emulated", "/storage/emulated/0", "/sdcard", "/mnt", "/mnt/sdcard", "/data/data",
        "/data/system", "/data/media", "/data/media/0", "/data/user", "/data/user/0"
    )
    private val BLOCK_WRITE = Regex("""(>|of=)\s*/dev/(block|mmcblk|sd[a-z]|nvme|mapper|disk)""")
    private val SYSTEM_WRITE = Regex(""">\s*/(system|vendor|product|boot|odm)(/|\s|$)""")
    private val WIPE_WORDS = listOf("MASTER_CLEAR", "--wipe_data", "--wipe_cache", "wipe-data")

    private val typedPrefixes = listOf("/shell", "jalankan shell:", "run shell:", "shell:", "shizuku:")

    // /shell is a standalone command prefix. A space is mandatory before the shell command.
    // Compact forms such as /shelldumpsys are intentionally rejected to avoid surprising parsing.
    private fun prefixMatches(lower: String, prefix: String): Boolean {
        if (!lower.startsWith(prefix)) return false
        return lower.length == prefix.length || lower[prefix.length].isWhitespace()
    }

    fun hasExplicitPrefix(text: String): Boolean {
        val lower = text.trim().lowercase()
        return typedPrefixes.any { prefixMatches(lower, it) }
    }

    fun stripPrefix(text: String): String {
        val t = text.trim()
        val lower = t.lowercase()
        return typedPrefixes.firstOrNull { prefixMatches(lower, it) }?.let { t.substring(it.length).trim() } ?: t
    }

    /** True for diagnostic dumps (meminfo, gfxinfo, battery...). They are never filtered by keyword. */
    fun isDumpsys(command: String): Boolean = command.trim().startsWith("dumpsys")

    fun risk(command: String): Risk = riskOf(command, 0)

    private fun riskOf(command: String, depth: Int): Risk {
        if (WIPE_WORDS.any { command.contains(it) } || BLOCK_WRITE.containsMatchIn(command) ||
            SYSTEM_WRITE.containsMatchIn(command)) return Risk.FATAL
        var worst = Risk.FREE
        for (segment in command.split(SEPARATORS)) {
            val tokens = segment.trim().split(WHITESPACE).filter { it.isNotEmpty() }
            if (tokens.isEmpty()) continue
            val r = segmentRisk(tokens, depth)
            if (r == Risk.FATAL) return Risk.FATAL
            if (r == Risk.CONFIRM) worst = Risk.CONFIRM
        }
        return worst
    }

    private fun segmentRisk(raw: List<String>, depth: Int): Risk {
        var t = raw
        // Skip VAR=value prefixes and wrappers (and their flags/numbers) to reach the real command.
        while (t.isNotEmpty() && ((t[0].contains('=') && !t[0].startsWith("-")) || t[0] in WRAPPERS)) {
            t = t.drop(1)
            while (t.isNotEmpty() && (t[0].startsWith("-") || NUMERIC.matches(t[0]))) t = t.drop(1)
        }
        if (t.isEmpty()) return Risk.FREE
        val cmd = t[0].substringAfterLast('/').lowercase()
        val args = t.drop(1)

        if (cmd == "dumpsys") return Risk.FREE
        if (cmd in INTERPRETERS && depth < 3) {
            val payload = (if (cmd == "eval") args else args.dropWhile { it != "-c" }.drop(1))
                .joinToString(" ").trim().trim('"', '\'')
            if (payload.isNotEmpty()) return riskOf(payload, depth + 1)
        }
        if (cmd in FATAL_COMMANDS || cmd.startsWith("mkfs")) return Risk.FATAL
        if (cmd == "dd" && args.any { it.startsWith("of=/dev/") }) return Risk.FATAL
        if (cmd == "sm" && args.firstOrNull() in setOf("partition", "format")) return Risk.FATAL
        if (cmd == "rm" && recursive(args)) {
            return if (args.any { !it.startsWith("-") && isProtectedRoot(it) }) Risk.FATAL else Risk.CONFIRM
        }
        val pmAction = when {
            cmd == "pm" -> args.firstOrNull()
            cmd == "cmd" && args.firstOrNull() == "package" -> args.getOrNull(1)
            else -> null
        }
        if (pmAction != null && pmAction in PM_CHANGES) {
            return if (args.any { it in CRITICAL_PACKAGES }) Risk.FATAL else Risk.CONFIRM
        }
        if (cmd in CONFIRM_COMMANDS) return Risk.CONFIRM
        if (cmd == "svc" && args.firstOrNull() == "power" && args.getOrNull(1) in setOf("reboot", "shutdown")) return Risk.CONFIRM
        if ((cmd == "settings" || (cmd == "cmd" && args.firstOrNull() == "settings")) &&
            args.any { it == "put" || it == "delete" }) return Risk.CONFIRM
        return Risk.FREE
    }

    private fun recursive(args: List<String>): Boolean =
        args.any { it == "--recursive" || Regex("""^-[a-zA-Z]*[rR][a-zA-Z]*$""").matches(it) }

    private fun isProtectedRoot(path: String): Boolean {
        var p = path.trim('"', '\'')
        while (p.endsWith("/*") || p.endsWith("/.")) p = p.dropLast(2)
        return p.trimEnd('/') in PROTECTED_ROOTS
    }

    /** Null means the command may run (possibly after confirmation, see [needsConfirmation]). */
    fun refusal(command: String): String? {
        val cmd = command.trim()
        if (cmd.isEmpty()) return "Perintah shell kosong."
        if (cmd.length > MAX_LENGTH) return "Perintah shell terlalu panjang (maksimal $MAX_LENGTH karakter)."
        if (cmd.contains('\u0000')) return "Perintah shell mengandung karakter yang tidak didukung."
        if (risk(cmd) == Risk.FATAL) {
            return "Perintah ini ditolak karena bisa menghapus data atau merusak perangkat secara permanen " +
                "(format/penghapusan penyimpanan, hapus folder sistem, tulis ke partisi, reset pabrik, atau hapus paket sistem penting)."
        }
        return null
    }

    fun needsConfirmation(command: String): Boolean = risk(command) == Risk.CONFIRM
}
