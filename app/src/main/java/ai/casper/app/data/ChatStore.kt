package ai.casper.app.data

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// Диалоги хранятся на телефоне (SharedPreferences). Без входа синхронизации между устройствами нет.
object ChatStore {
    private const val PREFS = "casper"
    private const val KEY = "chats"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(ctx: Context): List<StoredChat> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(StoredChat.serializer()), raw) }.getOrDefault(emptyList())
    }

    fun defaultModel(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("default_model", null) ?: "openai/gpt-4o-mini"

    fun setDefaultModel(ctx: Context, id: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("default_model", id).apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    fun save(ctx: Context, chats: List<StoredChat>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, json.encodeToString(ListSerializer(StoredChat.serializer()), chats))
            .apply()
    }
}
