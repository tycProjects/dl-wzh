package com.puzzleblast.game.core

data class ParsedCommand(
    val raw: String,
    val cmd: String,
    val args: List<String>,
    val sidTag: String?,
    val isAll: Boolean,
    val replyFileId: String?,
    val replyFileName: String?,
    val messageId: Long
) {
    val argString: String get() = args.joinToString(" ")
}

object CommandRouter {
    fun parse(msg: TgMessage): ParsedCommand? {
        val raw = msg.text ?: return null
        val trimmed = raw.trim()
        var isAll = false
        var work = trimmed
        if (work.startsWith("/all", true)) {
            isAll = true
            work = work.removePrefix("/all").trim()
        }
        var sidTag: String? = null
        val tagMatch = Regex("""^([dD]\d{5})\s+""").find(work)
        if (tagMatch != null) {
            sidTag = tagMatch.groupValues[1]
            work = work.removeRange(0, tagMatch.range.last + 1).trim()
        }
        val parts = work.split(Regex("\\s+"))
        val cmd = parts.firstOrNull()?.lowercase() ?: return null
        val args = parts.drop(1)
        return ParsedCommand(work, cmd, args, sidTag, isAll, msg.replyFileId, msg.replyFileName, msg.messageId)
    }
}
