package dev.tajim.jarvis.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class Role { USER, ASSISTANT }

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val time: Long,
    /** True on a user message whose AI request failed, so the UI can offer Retry. */
    val failed: Boolean = false,
)

/** Chat history kept as a local JSON file (private app storage, plain text, not encrypted). */
class ChatRepository(context: Context, scope: CoroutineScope) {
    private val file = File(context.applicationContext.filesDir, "chat.json")
    private val mutex = Mutex()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init { scope.launch { _messages.value = load() } }

    private suspend fun load(): List<ChatMessage> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching emptyList()
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ChatMessage(o.getLong("id"), Role.valueOf(o.getString("role")), o.getString("text"), o.getLong("time"), o.optBoolean("failed"))
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun persist(list: List<ChatMessage>) = withContext(Dispatchers.IO) {
        runCatching {
            val arr = JSONArray()
            list.forEach { arr.put(JSONObject().put("id", it.id).put("role", it.role.name).put("text", it.text).put("time", it.time).put("failed", it.failed)) }
            val tmp = File(file.parentFile, "chat.json.tmp")
            tmp.writeText(arr.toString())
            tmp.renameTo(file)
        }
    }

    suspend fun add(role: Role, text: String, failed: Boolean = false): ChatMessage = mutex.withLock {
        val msg = ChatMessage(System.currentTimeMillis() * 1000 + (_messages.value.size % 1000), role, text, System.currentTimeMillis(), failed)
        _messages.value = _messages.value + msg
        persist(_messages.value)
        msg
    }

    suspend fun update(id: Long, transform: (ChatMessage) -> ChatMessage) = mutex.withLock {
        _messages.value = _messages.value.map { if (it.id == id) transform(it) else it }
        persist(_messages.value)
    }

    suspend fun remove(id: Long) = mutex.withLock {
        _messages.value = _messages.value.filter { it.id != id }
        persist(_messages.value)
    }

    suspend fun clear() = mutex.withLock {
        _messages.value = emptyList()
        persist(emptyList())
    }
}
