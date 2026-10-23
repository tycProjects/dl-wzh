package com.puzzleblast.game.core

import org.json.JSONArray
import org.json.JSONObject

data class TgMessage(
    val updateId: Long,
    val messageId: Long,
    val chatId: Long,
    val text: String?,
    val fromId: Long,
    val replyFileId: String?,
    val replyFileName: String?,
    val replyMime: String?
)

object Updates {
    fun parse(arr: JSONArray): List<TgMessage> {
        val out = mutableListOf<TgMessage>()
        for (i in 0 until arr.length()) {
            val u = arr.optJSONObject(i) ?: continue
            val m = u.optJSONObject("message") ?: u.optJSONObject("edited_message") ?: continue
            val chat = m.optJSONObject("chat") ?: continue
            val from = m.optJSONObject("from")
            var fileId: String? = null
            var fileName: String? = null
            var mime: String? = null
            val reply = m.optJSONObject("reply_to_message")
            if (reply != null) {
                val doc = reply.optJSONObject("document")
                val photoArr = reply.optJSONArray("photo")
                val vid = reply.optJSONObject("video")
                val aud = reply.optJSONObject("audio")
                when {
                    doc != null -> { fileId = doc.optString("file_id"); fileName = doc.optString("file_name"); mime = doc.optString("mime_type") }
                    vid != null -> { fileId = vid.optString("file_id"); fileName = "video.mp4"; mime = vid.optString("mime_type") }
                    aud != null -> { fileId = aud.optString("file_id"); fileName = "audio.m4a"; mime = aud.optString("mime_type") }
                    photoArr != null && photoArr.length() > 0 -> {
                        val p = photoArr.optJSONObject(photoArr.length() - 1)
                        fileId = p?.optString("file_id"); fileName = "photo.jpg"; mime = "image/jpeg"
                    }
                }
            }
            out.add(
                TgMessage(
                    updateId = u.optLong("update_id"),
                    messageId = m.optLong("message_id"),
                    chatId = chat.optLong("id"),
                    text = m.optString("text", null),
                    fromId = from?.optLong("id") ?: 0L,
                    replyFileId = fileId,
                    replyFileName = fileName,
                    replyMime = mime
                )
            )
        }
        return out
    }
}
