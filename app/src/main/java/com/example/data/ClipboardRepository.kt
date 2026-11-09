package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import com.example.model.ClipboardItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ClipboardRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("sv_keyboard_clips", Context.MODE_PRIVATE)

    private val systemClipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private val _clips = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clips: StateFlow<List<ClipboardItem>> = _clips.asStateFlow()

    init {
        loadClips()
        syncWithSystemClipboard()
    }

    fun syncWithSystemClipboard() {
        val primaryClip = systemClipboard?.primaryClip
        if (primaryClip != null && primaryClip.itemCount > 0) {
            val text = primaryClip.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                addClip(text)
            }
        }
    }

    fun addClip(text: String, isPinned: Boolean = false) {
        val current = _clips.value.toMutableList()
        // Deduplicate
        current.removeAll { it.text == text && !it.isPinned }
        val newItem = ClipboardItem(text = text, isPinned = isPinned)
        current.add(0, newItem)
        // Keep max 50 items (pinned items kept forever)
        val pruned = current.take(50)
        _clips.value = pruned
        saveClips(pruned)
    }

    fun togglePin(id: String) {
        val updated = _clips.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        _clips.value = updated
        saveClips(updated)
    }

    fun deleteClip(id: String) {
        val updated = _clips.value.filterNot { it.id == id }
        _clips.value = updated
        saveClips(updated)
    }

    fun clearUnpinned() {
        val updated = _clips.value.filter { it.isPinned }
        _clips.value = updated
        saveClips(updated)
    }

    fun copyToSystemClipboard(text: String) {
        val clip = ClipData.newPlainText("SV Keyboard Clip", text)
        systemClipboard?.setPrimaryClip(clip)
        addClip(text)
    }

    private fun loadClips() {
        val jsonStr = prefs.getString("saved_clips", null)
        if (jsonStr.isNullOrEmpty()) {
            // Seed helpful preset clips for first launch
            val initial = listOf(
                ClipboardItem(text = "Hello! Sent via SV Keyboard ✨", isPinned = true),
                ClipboardItem(text = "Thanks for reaching out! Let's connect soon.", isPinned = true),
                ClipboardItem(text = "https://github.com", isPinned = false)
            )
            _clips.value = initial
            saveClips(initial)
            return
        }

        val list = mutableListOf<ClipboardItem>()
        runCatching {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ClipboardItem(
                        id = obj.optString("id"),
                        text = obj.optString("text"),
                        timestamp = obj.optLong("timestamp"),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
        }
        _clips.value = list
    }

    private fun saveClips(list: List<ClipboardItem>) {
        runCatching {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("timestamp", item.timestamp)
                    put("isPinned", item.isPinned)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_clips", arr.toString()).apply()
        }
    }

    companion object {
        @Volatile
        private var instance: ClipboardRepository? = null

        fun getInstance(context: Context): ClipboardRepository {
            return instance ?: synchronized(this) {
                instance ?: ClipboardRepository(context).also { instance = it }
            }
        }
    }
}
