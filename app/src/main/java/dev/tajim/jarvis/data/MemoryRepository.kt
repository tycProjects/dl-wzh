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

data class Memory(val id: Long, val text: String, val time: Long)

/**
 * User-controlled memory bank: only what the user adds is stored (nothing is saved silently).
 * It is a plain text list, searched by substring; it is not a semantic/vector memory.
 */
class MemoryRepository(context: Context, scope: CoroutineScope) {
    private val file = File(context.applicationContext.filesDir, "memories.json")
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<Memory>>(emptyList())
    val items: StateFlow<List<Memory>> = _items.asStateFlow()

    init {
        scope.launch {
            _items.value = withContext(Dispatchers.IO) {
                runCatching {
                    if (!file.exists()) return@runCatching emptyList()
                    val arr = JSONArray(file.readText())
                    (0 until arr.length()).map { arr.getJSONObject(it).let { o -> Memory(o.getLong("id"), o.getString("text"), o.getLong("time")) } }
                }.getOrDefault(emptyList())
            }
        }
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        runCatching {
            val arr = JSONArray()
            _items.value.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text).put("time", it.time)) }
            file.writeText(arr.toString())
        }
    }

    suspend fun add(text: String) = mutex.withLock {
        val t = text.trim().take(500)
        if (t.isEmpty()) return@withLock
        _items.value = listOf(Memory(System.currentTimeMillis(), t, System.currentTimeMillis())) + _items.value
        persist()
    }

    suspend fun edit(id: Long, text: String) = mutex.withLock {
        val t = text.trim().take(500)
        if (t.isEmpty()) return@withLock
        _items.value = _items.value.map { if (it.id == id) it.copy(text = t) else it }
        persist()
    }

    suspend fun delete(id: Long) = mutex.withLock {
        _items.value = _items.value.filter { it.id != id }
        persist()
    }

    suspend fun clear() = mutex.withLock {
        _items.value = emptyList()
        persist()
    }
}
