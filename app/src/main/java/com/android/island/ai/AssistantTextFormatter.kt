package com.android.island.ai

/** Lightweight, dependency-free spacing cleanup for assistant replies rendered in Compose. */
object AssistantTextFormatter {
    fun format(input: String): String {
        val normalized = input.replace("\r\n", "\n").replace('\r', '\n')
            .lines().joinToString("\n") { it.trimEnd() }
            .trim()
        if (normalized.isBlank()) return normalized

        val source = normalized.lines()
        val out = mutableListOf<String>()
        var inCode = false
        var previousWasHeading = false

        fun isHeading(line: String): Boolean =
            line.matches(Regex("^#{1,6}\\s+.+\$")) ||
                (line.length <= 90 && line.endsWith(":") && line.count { it.isLetter() } >= 3) ||
                (line.length <= 55 && line.split(Regex("\\s+")).size <= 6 &&
                    !line.endsWith(".") && !line.endsWith("?") && !line.endsWith("!") &&
                    !line.matches(Regex("^\\s*(?:[-*+]\\s+|\\d+[.)]\\s+).+\$")) &&
                    line.count { it.isLetter() } >= 3)
        fun isList(line: String): Boolean =
            line.matches(Regex("^\\s*(?:[-*+]\\s+|\\d+[.)]\\s+).+\$"))
        fun isQuote(line: String): Boolean = line.trimStart().startsWith(">")
        fun addBlankIfNeeded() {
            if (out.isNotEmpty() && out.last().isNotBlank()) out += ""
        }

        source.forEachIndexed { index, raw ->
            val line = raw.trimEnd()
            val trimmed = line.trim()
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                if (!inCode) addBlankIfNeeded()
                out += line
                inCode = !inCode
                if (!inCode && source.getOrNull(index + 1)?.isNotBlank() == true) out += ""
                previousWasHeading = false
                return@forEachIndexed
            }
            if (inCode) {
                out += line
                return@forEachIndexed
            }
            if (trimmed.isBlank()) {
                if (out.isNotEmpty() && out.last().isNotBlank()) out += ""
                previousWasHeading = false
                return@forEachIndexed
            }

            val nextLine = source.getOrNull(index + 1)?.trim().orEmpty()
            val heading = isHeading(trimmed) && (trimmed.startsWith("#") || trimmed.endsWith(":") ||
                (nextLine.isNotBlank() && !isList(nextLine) && !nextLine.startsWith("```")))
            val list = isList(trimmed)
            val quote = isQuote(trimmed)
            val previous = source.getOrNull(index - 1)?.trim().orEmpty()
            val previousWasList = previous.matches(Regex("^(?:[-*+]\\s+|\\d+[.)]\\s+).+\$"))
            val previousWasQuote = previous.startsWith(">")
            if (heading || (list && !previousWasList && !previousWasQuote) || (quote && !previousWasQuote)) {
                addBlankIfNeeded()
            } else if (previousWasHeading && out.isNotEmpty() && out.last().isNotBlank()) {
                out += ""
            }
            out += trimmed
            previousWasHeading = heading
        }

        return out.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
    }
}
